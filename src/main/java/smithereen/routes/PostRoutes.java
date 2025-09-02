package smithereen.routes;

import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.PostSource;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.admin.UserRole;
import smithereen.model.admin.ViolationReport;
import smithereen.model.WebDeltaResponse;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.storage.utils.Pair;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class PostRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(PostRoutes.class);

	public static Object createUserWallPost(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=ctx.getUsersController().getUserOrThrow(id);
		ctx.getUsersController().ensureUserNotBlocked(self.user, user);
		return createWallPost(req, resp, self, user, ctx);
	}

	public static Object createGroupWallPost(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=ctx.getGroupsController().getGroupOrThrow(id);
		return createWallPost(req, resp, self, group, ctx);
	}

	public static Object createWallPost(Request req, Response resp, Account self, @NotNull Actor owner, ApplicationContext ctx){
		String text=req.queryParams("text");
		Poll poll;
		String pollQuestion=req.queryParams("pollQuestion");
		if(StringUtils.isNotEmpty(pollQuestion)){
			List<String> pollOptions=Arrays.stream(req.queryParams("pollOption")!=null ? req.queryMap("pollOption").values() : new String[0]).map(String::trim).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
			if(pollOptions.size()>=2){
				poll=new Poll();
				poll.question=pollQuestion;
				poll.anonymous="on".equals(req.queryParams("pollAnonymous"));
				poll.multipleChoice="on".equals(req.queryParams("pollMultiChoice"));
				boolean timeLimit="on".equals(req.queryParams("pollTimeLimit"));
				if(timeLimit){
					int seconds=parseIntOrDefault(req.queryParams("pollTimeLimitValue"), 0);
					if(seconds>60)
						poll.endTime=Instant.now().plusSeconds(seconds);
				}
				poll.options=pollOptions.stream().map(o->{
					PollOption opt=new PollOption();
					opt.text=o;
					return opt;
				}).collect(Collectors.toList());
			}else{
				poll=null;
			}
		}else{
			poll=null;
		}
		int replyTo=Utils.parseIntOrDefault(req.queryParams("replyTo"), 0);
		String contentWarning=req.queryParams("contentWarning");
		List<String> attachments;
		Map<String, String> attachmentAltTexts;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
			String altTextsJson=req.queryParams("attachAltTexts");
			if(StringUtils.isNotEmpty(altTextsJson)){
				try{
					attachmentAltTexts=gson.fromJson(altTextsJson, new TypeToken<>(){});
				}catch(Exception x){
					attachmentAltTexts=Map.of();
				}
			}else{
				attachmentAltTexts=Map.of();
			}
		}else{
			attachments=Collections.emptyList();
			attachmentAltTexts=Map.of();
		}

		int repostID=safeParseInt(req.queryParams("repost"));
		Post repost=repostID>0 ? ctx.getWallController().getPostOrThrow(repostID) : null;
		Post inReplyTo=null;
		if(replyTo!=0){
			inReplyTo=ctx.getWallController().getPostOrThrow(replyTo);
			if(inReplyTo.isMastodonStyleRepost())
				inReplyTo=ctx.getWallController().getPostOrThrow(inReplyTo.repostOf);
		}

		Post post=ctx.getWallController().createWallPost(self.user, self.id, owner, inReplyTo, text, self.prefs.textFormat, contentWarning, attachments, poll, repost, attachmentAltTexts, null);

		SessionInfo sess=sessionInfo(req);
		sess.postDraftAttachments.clear();
		if(isAjax(req)){
			String rid=req.queryParams("rid");
			String ridSuffix="";
			if(StringUtils.isNotEmpty(rid))
				ridSuffix="_"+rid;

			boolean fromNotifications="notifications".equals(req.queryParams("from"));

			String formID=req.queryParams("formID");
			if(repost!=null){
				return new WebDeltaResponse(resp)
						.dismissBox("repostFormBox")
						.setInputValue("postFormText_"+formID, "").setContent("postFormAttachments_"+formID, "")
						.showSnackbar(lang(req).get(repost.getReplyLevel()>0 ? "repost_comment_done" : "repost_post_done"));
			}
			PostViewModel pvm=new PostViewModel(post);
			ArrayList<PostViewModel> needInteractions=new ArrayList<>();
			needInteractions.add(pvm);
			if(inReplyTo!=null)
				pvm.parentAuthorID=inReplyTo.authorID;
			ctx.getWallController().populateReposts(self.user, List.of(pvm), 2);
			RenderedTemplateResponse model;
			if(fromNotifications){
				model=new RenderedTemplateResponse("wall_reply_notifications", req).with("post", pvm);
			}else{
				model=new RenderedTemplateResponse(replyTo!=0 ? "wall_reply" : "wall_post", req).with("post", pvm);
			}
			if(replyTo!=0){
				PostViewModel topLevel=new PostViewModel(context(req).getWallController().getPostOrThrow(post.replyKey.getFirst()));
				model.with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0)+ridSuffix);
				model.with("topLevel", topLevel);
				needInteractions.add(topLevel);
				if(StringUtils.isNotEmpty(rid))
					model.with("randomID", rid);
			}
			Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(needInteractions, self.user);
			model.with("postInteractions", interactions);
			Map<Integer, User> users=new HashMap<>();
			users.put(self.user.id, self.user);
			if(inReplyTo!=null && inReplyTo.authorID!=self.user.id){
				try{
					users.put(inReplyTo.authorID, ctx.getUsersController().getUserOrThrow(inReplyTo.authorID));
				}catch(ObjectNotFoundException ignore){}
			}
			model.with("users", users);
			model.with("posts", Map.of(post.id, post));
			model.with("commentViewType", self.prefs.commentViewType).with("maxReplyDepth", getMaxReplyDepth(self));
			String postHTML=model.renderToString();
			if(!fromNotifications){
				if(req.attribute("mobile")!=null && replyTo==0){
					postHTML="<div class=\"card\">"+postHTML+"</div>";
				}else if(replyTo==0){
					// TODO correctly handle day headers in feed
					String cl="feed".equals(formID) ? "feedRow" : "wallRow";
					postHTML="<div class=\""+cl+"\">"+postHTML+"</div>";
				}
			}
			WebDeltaResponse rb;
			if(fromNotifications){
				rb=new WebDeltaResponse(resp)
						.remove("notificationsOwnReply"+ridSuffix)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "wallPostForm_"+formID, postHTML)
						.addClass("wallPostForm_"+formID, "collapsed");
			}else if(replyTo==0){
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postList", postHTML);
				int newPostCount=Utils.parseIntOrDefault(req.queryParams("wallPostCount"), -1)+1;
				if(newPostCount>0){
					// When creating a new post, update the wall header with the incremented number.
					// It is deliberate that we just increment the previous value instead of fetching the up-to-date value
					// from the DB, because as far as the user is concerned, only one new post has appeared,
					// even if some other user created a post on the same wall at the same time.
					// The post by the other user will appear only after the page refreshes,
					// so there is no point in counting it in the updated wall header.
					rb.setContent("wallPostCount", lang(req).get("X_posts", Map.of("count", newPostCount)))
							.setInputValue("wallPostCountInput", Integer.toString(newPostCount));
				}
			}else{
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "postReplies"+switch(self.prefs.commentViewType){
					case THREADED -> replyTo;
					case TWO_LEVEL -> post.replyKey.get(Math.min(post.getReplyLevel()-1, 1));
					case FLAT -> post.replyKey.getFirst();
				}+ridSuffix, postHTML)
						.show("postReplies"+replyTo+ridSuffix)
						.show("postCommentsSummary"+post.replyKey.getFirst()+ridSuffix);
				try{
					Post topLevel=ctx.getWallController().getPostOrThrow(post.replyKey.getFirst());
					rb.setContent("postCommentsTotal"+post.replyKey.getFirst()+ridSuffix, lang(req).get("X_comments", Map.of("count", topLevel.replyCount)));
				}catch(ObjectNotFoundException ignore){}
			}
			if(req.attribute("mobile")==null && replyTo==0){
				rb.runScript("updatePostForms();");
			}
			return rb.setInputValue("postFormText_"+formID, "").setContent("postFormAttachments_"+formID, "");
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object editPostForm(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=parseIntOrDefault(req.params(":postID"), 0);
		Post post=ctx.getWallController().getPostOrThrow(id);
		if(!sessionInfo(req).permissions.canEditPost(post))
			throw new UserActionNotAllowedException();
		ctx.getPrivacyController().enforceObjectPrivacy(self.user, post);
		RenderedTemplateResponse model;
		if(isAjax(req)){
			model=new RenderedTemplateResponse("wall_post_form", req);
		}else{
			model=new RenderedTemplateResponse("content_wrap", req).with("contentTemplate", "wall_post_form");
		}

		String rid=req.queryParams("rid");
		String ridSuffix="";
		if(StringUtils.isNotEmpty(rid)){
			ridSuffix="_"+rid;
			model.with("randomID", rid);
		}

		model.with("addClasses", "editing nonCollapsible").with("isEditing", true).with("id", "edit"+id+ridSuffix).with("editingPostID", id);
		PostSource source=ctx.getWallController().getPostSource(post);
		model.with("prefilledPostText", source.text()).with("sourceFormat", source.format());
		if(post.hasContentWarning())
			model.with("contentWarning", post.contentWarning);
		if(post.poll!=null)
			model.with("poll", post.poll);
		if(post.attachments!=null && !post.attachments.isEmpty()){
			model.with("draftAttachments", post.attachments);
			model.with("attachAltTexts", post.attachments.stream()
					.map(att->att instanceof LocalImage li && li.photoID==0 && li.name!=null ? new Pair<>(li.fileRecord.id().getIDForClient(), li.name) : null)
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::first, Pair::second))
			);
			Map<String, PhotoViewerInlineData> pvData=post.attachments.stream()
					.map(att->att instanceof LocalImage li && li.photoID==0 ? new Pair<>(li.getLocalID(), new PhotoViewerInlineData(0, "rawFile/"+li.getLocalID(), li.getURLsForPhotoViewer())) : null)
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::first, Pair::second, (a, b)->b));
			model.with("attachPvData", pvData);
		}
		if(post.repostOf!=0)
			model.with("allowEmpty", true);
		if(isAjax(req)){
			if(req.queryParams("fromLayer")!=null){
				model.with("action", "/posts/"+id+"/edit?fromLayer");
			}
			return new WebDeltaResponse(resp)
					.hide("postInner"+id+ridSuffix)
					.hide("postFloatingActions"+id+ridSuffix)
					.hide("inReplyTo"+id+ridSuffix)
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postInner"+id+ridSuffix, model.renderToString())
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postAuthor"+id+ridSuffix, "<span class=\"grayText lowercase\" id=\"postEditingLabel"+id+ridSuffix+"\">&nbsp;-&nbsp;"+lang(req).get(post.getReplyLevel()==0 ? "editing_post" : "editing_comment")+"</span>")
					.runScript("updatePostForms(); ge(\"postFormText_edit"+id+ridSuffix+"\").focus();");
		}
		return model.pageTitle(lang(req).get(post.getReplyLevel()>0 ? "editing_comment" : "editing_post"));
	}

	public static Object editPost(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=parseIntOrDefault(req.params(":postID"), 0);
		String text=req.queryParams("text");
		Poll poll;
		String pollQuestion=req.queryParams("pollQuestion");
		if(StringUtils.isNotEmpty(pollQuestion)){
			Post post=ctx.getWallController().getPostOrThrow(id);
			List<String> pollOptions=Arrays.stream(req.queryParams("pollOption")!=null ? req.queryMap("pollOption").values() : new String[0]).map(String::trim).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
			if(pollOptions.size()>=2){
				poll=new Poll();
				poll.question=pollQuestion;
				poll.anonymous="on".equals(req.queryParams("pollAnonymous"));
				poll.multipleChoice="on".equals(req.queryParams("pollMultiChoice"));
				boolean timeLimit="on".equals(req.queryParams("pollTimeLimit"));
				if(timeLimit){
					int seconds=parseIntOrDefault(req.queryParams("pollTimeLimitValue"), 0);
					if(seconds>60)
						poll.endTime=Instant.now().plusSeconds(seconds);
					else if(seconds==-1 && post.poll!=null)
						poll.endTime=post.poll.endTime;
				}
				poll.options=pollOptions.stream().map(o->{
					PollOption opt=new PollOption();
					opt.text=o;
					return opt;
				}).collect(Collectors.toList());
			}else{
				poll=null;
			}
		}else{
			poll=null;
		}
		String contentWarning=req.queryParams("contentWarning");
		List<String> attachments;
		Map<String, String> attachmentAltTexts;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
			String altTextsJson=req.queryParams("attachAltTexts");
			if(StringUtils.isNotEmpty(altTextsJson)){
				try{
					attachmentAltTexts=gson.fromJson(altTextsJson, new TypeToken<>(){});
				}catch(Exception x){
					attachmentAltTexts=Map.of();
				}
			}else{
				attachmentAltTexts=Map.of();
			}
		}else{
			attachments=Collections.emptyList();
			attachmentAltTexts=Map.of();
		}

		Post post=ctx.getWallController().editPost(self.user, sessionInfo(req).permissions, id, text, enumValue(req.queryParams("format"), FormattedTextFormat.class), contentWarning, attachments, poll, attachmentAltTexts);
		if(isAjax(req)){
			if(req.attribute("mobile")!=null)
				return new WebDeltaResponse(resp).replaceLocation(post.getInternalURL().toString());

			PostViewModel postVM=new PostViewModel(post);
			ctx.getWallController().populateReposts(self.user, List.of(postVM), 2);
			String templateName;
			boolean fromLayer;
			if(req.queryParams("fromLayer")!=null && !isMobile(req)){
				templateName="wall_post_inner";
				fromLayer=true;
			}else{
				templateName=post.getReplyLevel()>0 ? "wall_reply" : "wall_post";
				fromLayer=false;
			}
			RenderedTemplateResponse model=new RenderedTemplateResponse(templateName, req).with("post", postVM);
			HashSet<Integer> needUsers=new HashSet<>();
			PostViewModel.collectActorIDs(List.of(postVM), needUsers, null);
			model.with("users", ctx.getUsersController().getUsers(needUsers));

			String rid=req.queryParams("rid");
			String ridSuffix="";
			if(StringUtils.isNotEmpty(rid)){
				ridSuffix="_"+rid;
				model.with("randomID", rid);
			}

			ArrayList<PostViewModel> needInteractions=new ArrayList<>();
			needInteractions.add(postVM);
			if(post.getReplyLevel()>0){
				try{
					PostViewModel topLevel=new PostViewModel(ctx.getWallController().getPostOrThrow(post.replyKey.getFirst()));
					model.with("topLevel", topLevel);
					needInteractions.add(topLevel);
				}catch(ObjectNotFoundException ignore){}
			}
			Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(needInteractions, self.user);
			model.with("postInteractions", interactions);
			return new WebDeltaResponse(resp).setContent("postInner"+post.id+ridSuffix, fromLayer ? model.renderToString() : model.renderBlock("postInner"))
					.show("postInner"+post.id+ridSuffix)
					.show("postFloatingActions"+id+ridSuffix)
					.show("inReplyTo"+id+ridSuffix)
					.remove("wallPostForm_edit"+post.id+ridSuffix, "postEditingLabel"+post.id+ridSuffix);
		}
		resp.redirect(post.getInternalURL().toString());
		return "";
	}

	public static Object standalonePost(Request req, Response resp){
		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		PostViewModel post=new PostViewModel(ctx.getWallController().getPostOrThrow(postID));

		boolean isLayer=!isMobile(req) && req.queryParams("ajaxLayer")!=null;

		RenderedTemplateResponse model=new RenderedTemplateResponse(isLayer ? "wall_post_standalone_layer" : "wall_post_standalone", req);
		SessionInfo info=Utils.sessionInfo(req);
		Account self=null;
		if(info!=null && info.account!=null){
			self=info.account;
		}

		if(post.post.repostOf!=0){
			if(post.post.isMastodonStyleRepost()){
				if(isAjaxLayout(req) || isLayer){
					post=new PostViewModel(ctx.getWallController().getPostOrThrow(post.post.repostOf));
					req.attribute("alFinalURL", post.post.getInternalURL().toString());
				}else{
					resp.redirect("/posts/"+post.post.repostOf);
					return "";
				}
			}else{
				ctx.getWallController().populateReposts(self!=null ? self.user : null, List.of(post), 10);
			}
		}
		if(post.post.getReplyLevel()>0 && (isAjaxLayout(req) || isLayer)){
			int commentID=post.post.id;
			post=new PostViewModel(ctx.getWallController().getPostOrThrow(post.post.replyKey.getFirst()));
			req.attribute("alFinalURL", post.post.getInternalURL().toString()+"#comment"+commentID);
		}

		List<Integer> replyKey=post.post.getReplyKeyForReplies();
		Actor owner;
		if(post.post.ownerID<0)
			owner=ctx.getGroupsController().getGroupOrThrow(-post.post.ownerID);
		else
			owner=ctx.getUsersController().getUserOrThrow(post.post.ownerID);

		if(self!=null && owner instanceof Group group && post.post.getReplyLevel()==0){
			model.with("groupAdminLevel", ctx.getGroupsController().getMemberAdminLevel(group, self.user));
		}

		User author=ctx.getUsersController().getUserOrThrow(post.post.authorID);

		boolean reverseComments=isLayer || isMobile(req);
		int offset=offset(req);
		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> replies=ctx.getWallController().getReplies(self!=null ? self.user : null, replyKey, offset, 100, 50, viewType, reverseComments);
		if(reverseComments){
			replies.list=replies.list.reversed();
			replies.offset=replies.total-replies.list.size();
		}
		model.paginate(replies);
		model.with("post", post);
		model.with("isPostInLayer", isLayer);
		model.with("isGroup", post.post.ownerID<0);
		model.with("maxRepostDepth", 10).with("maxReplyDepth", getMaxReplyDepth(self));
		model.with("commentViewType", viewType);
		int cwCount=0;
		for(PostViewModel reply:replies.list){
			if(reply.post.hasContentWarning())
				cwCount++;
		}
		model.with("needExpandCWsButton", cwCount>1);

		boolean canOverridePrivacy=false;
		if(self!=null && info.permissions.hasPermission(UserRole.Permission.MANAGE_REPORTS)){
			int reportID=safeParseInt(req.queryParams("report"));
			if(reportID!=0){
				try{
					ViolationReport report=ctx.getModerationController().getViolationReportByID(reportID, false);
					for(ReportableContentObject c:report.content){
						if(c instanceof Post p && p.id==postID){
							canOverridePrivacy=true;
							break;
						}
					}
				}catch(ObjectNotFoundException ignore){}
			}
		}

		if(canOverridePrivacy){
			try{
				ctx.getPrivacyController().enforceObjectPrivacy(self.user, post.post);
			}catch(UserActionNotAllowedException x){
				model.with("privacyOverridden", true);
			}
		}else{
			ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post.post);
		}

		if(!post.post.replyKey.isEmpty()){
			model.with("prefilledPostText", author.getNameForReply()+", ");
		}
		ArrayList<PostViewModel> postIDs=new ArrayList<>();
		postIDs.add(post);
		for(PostViewModel reply:replies.list){
			postIDs.add(reply);
			reply.getAllReplies(postIDs);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(postIDs, info!=null && info.account!=null ? info.account.user : null);
		model.with("postInteractions", interactions);

		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(List.of(post), needUsers, needGroups);
		PostViewModel.collectActorIDs(replies.list, needUsers, needGroups);

		model.with("canSeeOthersPosts", !(owner instanceof User u) || ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, u, UserPrivacySettingKey.WALL_OTHERS_POSTS));

		if(info==null || info.account==null){
			HashMap<String, String> moreMeta=new LinkedHashMap<>();
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:type", "article");
			meta.put("og:title", author.getFullName());
			meta.put("og:url", post.post.getInternalURL().toString());
			meta.put("og:published_time", Utils.formatDateAsISO(post.post.createdAt));
			meta.put("og:author", author.url.toString());
			meta.put("profile:username", author.username+"@"+Config.domain);
			if(StringUtils.isNotEmpty(post.post.text)){
				String text=post.post.hasContentWarning() ? post.post.contentWarning : TextProcessor.truncateOnWordBoundary(post.post.text, 250);
				meta.put("og:description", text);
				moreMeta.put("description", text);
			}
			boolean hasImage=false;
			if(post.post.attachments!=null && !post.post.attachments.isEmpty()){
				for(Attachment att : post.post.getProcessedAttachments()){
					if(att instanceof PhotoAttachment pa){
						SizedImage.Dimensions size=pa.image.getDimensionsForSize(SizedImage.Type.PHOTO_MEDIUM);
						meta.put("og:image", pa.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_MEDIUM, SizedImage.Format.JPEG).toString());
						meta.put("og:image:width", String.valueOf(size.width));
						meta.put("og:image:height", String.valueOf(size.height));
						meta.put("og:image:type", "image/jpeg");
						meta.put("twitter:card", "summary_large_image");
						hasImage=true;
						break;
					}
				}
			}
			if(!hasImage){
				meta.put("twitter:card", "summary");
				if(author.hasAvatar()){
					URI img=author.getAvatar().getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_XLARGE, SizedImage.Format.JPEG);
					if(img!=null){
						SizedImage.Dimensions size=author.getAvatar().getDimensionsForSize(SizedImage.Type.AVA_SQUARE_XLARGE);
						meta.put("og:image", img.toString());
						meta.put("og:image:width", String.valueOf(size.width));
						meta.put("og:image:height", String.valueOf(size.height));
						meta.put("og:image:type", "image/jpeg");
					}
				}
			}
			model.with("metaTags", meta);
			model.with("moreMetaTags", moreMeta);
		}
		Utils.jsLangKey(req, "yes", "no", "cancel", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw");
		if(Utils.isMobile(req)){
			Utils.jsLangKey(req, "attach_menu_photo_upload", "attach_menu_photo_from_album");
		}
		model.with("title", post.post.getShortTitle(50)+" | "+author.getFullName());
		if(req.attribute("mobile")!=null){
			model.with("toolbarTitle", lang(req).get("wall_post_title"));
			List<Integer> likers=ctx.getUserInteractionsController().getLikesForObject(post.post, info!=null && info.account!=null ? info.account.user : null, 0, 10).list;
			model.with("likedBy", likers);
			needUsers.addAll(likers);
		}
		if(post.post.getReplyLevel()>0){
			model.with("jsRedirect", "/posts/"+post.post.replyKey.get(0)+"#comment"+post.post.id);
		}
		model.with("activityPubURL", post.post.getActivityPubID());
		if(!post.post.isLocal() && owner instanceof ForeignActor)
			model.with("noindex", true);

		model.with("users", ctx.getUsersController().getUsers(needUsers, true))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups))
				.headerBack(owner);
		return model;
	}

	public static Object confirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI("/posts/"+postID+"/delete?_redir="+URLEncoder.encode(back))).with("back", back);
	}

	public static Object delete(Request req, Response resp, Account self, ApplicationContext ctx){
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		ctx.getWallController().deletePost(self.user, post);
		if(isAjax(req)){
			String rid=req.queryParams("rid");
			String ridSuffix="";
			if(StringUtils.isNotEmpty(rid))
				ridSuffix="_"+rid;
			WebDeltaResponse wdr=new WebDeltaResponse(resp);
			if(post.getReplyLevel()>0){
				try{
					Post topLevel=ctx.getWallController().getPostOrThrow(post.replyKey.getFirst());
					if(topLevel.replyCount==0){
						wdr.hide("postCommentsSummary"+post.replyKey.getFirst()+ridSuffix);
					}else{
						wdr.setContent("postCommentsTotal"+post.replyKey.getFirst()+ridSuffix, lang(req).get("X_comments", Map.of("count", topLevel.replyCount)));
					}
				}catch(ObjectNotFoundException ignore){}
			}
			if(req.queryParams("fromLayer")!=null){
				return wdr.runScript("LayerManager.getMediaInstance().dismissByID(\"postLayer"+post.id+ridSuffix+"\");");
			}else if(req.queryParams("elid")!=null){
				return wdr.remove(req.queryParams("elid"));
			}
			return wdr.remove("post"+post.id+ridSuffix, "postReplies"+post.id+ridSuffix);
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object like(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		return UserInteractionsRoutes.like(req, resp, post);
	}

	public static Object unlike(Request req, Response resp, Account self, ApplicationContext ctx){
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		return UserInteractionsRoutes.setLiked(req, resp, self, ctx, post, false);
	}

	public static Object likePopover(Request req, Response resp){
		req.attribute("noHistory", true);
		Post post=context(req).getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		return UserInteractionsRoutes.likePopover(req, resp, post);
	}

	public static Object sharePopover(Request req, Response resp){
		ApplicationContext ctx=context(req);
		req.attribute("noHistory", true);
		Post post=context(req).getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		SessionInfo info=sessionInfo(req);
		User self=info!=null && info.account!=null ? info.account.user : null;
		context(req).getPrivacyController().enforceObjectPrivacy(self, post);
		List<Integer> users=ctx.getUserInteractionsController().getRepostedUsers(post, 6);
		String _content=new RenderedTemplateResponse("like_popover", req).with("ids", users).with("users", ctx.getUsersController().getUsers(users)).renderToString();
		UserInteractions interactions=ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self).get(post.getIDForInteractions());
		WebDeltaResponse b=new WebDeltaResponse(resp)
				.setContent("shareCounterPost"+post.id, String.valueOf(interactions.repostCount));
		if(interactions.repostCount==0)
			b.hide("shareCounterPost"+post.id);
		else
			b.show("shareCounterPost"+post.id);

		UserInteractionsRoutes.LikePopoverResponse o=new UserInteractionsRoutes.LikePopoverResponse();
		o.content=_content;
		o.title=lang(req).get("shared_by_X_people", Map.of("count", interactions.repostCount));
		o.actions=b.commands();
		o.show=interactions.repostCount>0;
		o.fullURL="/posts/"+post.id+"/reposts";
		return gson.toJson(o);
	}

	public static Object likeList(Request req, Response resp){
		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=ctx.getWallController().getPostOrThrow(postID);
		return UserInteractionsRoutes.likeList(req, resp, post);
	}

	public static Object userWallAll(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		ctx.getPrivacyController().enforceUserPrivacy(self==null ? null : self.user, user, UserPrivacySettingKey.WALL_OTHERS_POSTS);
		return wall(req, resp, user, false);
	}

	public static Object userWallOwn(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		return wall(req, resp, user, true);
	}

	public static Object groupWall(Request req, Response resp){
		Group group=context(req).getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
		if(group.wallState==GroupFeatureState.DISABLED)
			throw new UserActionNotAllowedException("err_access_content");
		return wall(req, resp, group, false);
	}

	public static void preparePostList(ApplicationContext ctx, List<PostViewModel> wall, RenderedTemplateResponse model, Account self){
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(wall, needUsers, needGroups);
		model.with("users", ctx.getUsersController().getUsers(needUsers, true))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups))
				.with("maxReplyDepth", getMaxReplyDepth(self));
	}

	private static Object wall(Request req, Response resp, Actor owner, boolean ownOnly){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		if(owner instanceof Group group)
			ctx.getPrivacyController().enforceUserAccessToGroupContent(self!=null ? self.user : null, group);

		int offset=offset(req);
		PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallPosts(self!=null ? self.user : null, owner, ownOnly, offset, 20));
		ctx.getWallController().populateReposts(self!=null ? self.user : null, wall.list, 2);
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(self!=null ? self.user : null, wall.list, self!=null ? self.prefs.commentViewType : CommentViewType.THREADED);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page", req)
				.paginate(wall)
				.with("postInteractions", interactions)
				.with("owner", owner)
				.with("isGroup", owner instanceof Group)
				.with("ownOnly", ownOnly)
				.with("canSeeOthersPosts", !(owner instanceof User u) || ctx.getPrivacyController().checkUserPrivacy(self==null ? null : self.user, u, UserPrivacySettingKey.WALL_OTHERS_POSTS))
				.with("tab", ownOnly ? "own" : "all")
				.headerBack(owner);

		preparePostList(ctx, wall.list, model, self);

		if(isAjax(req) && !isMobile(req)){
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				model.setName("post_list");
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("wallInner"));
				if(wall.offset+wall.perPage>=wall.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(wall.offset+wall.perPage));
				}
				return r;
			}
		}

		if(owner instanceof User user){
			model.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
		}else{
			model.pageTitle(lang(req).get("wall_of_group"));
		}

		if(owner instanceof ForeignActor)
			model.with("noindex", true);

		return model;
	}

	public static Object wallToWall(Request req, Response resp){
		ApplicationContext ctx=context(req);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		User otherUser=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":otherUserID")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int offset=offset(req);
		PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallToWallPosts(self!=null ? self.user : null, user, otherUser, offset, 20));
		ctx.getWallController().populateReposts(self!=null ? self.user : null, wall.list, 2);
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(self!=null ? self.user : null, wall.list, self!=null ? self.prefs.commentViewType : CommentViewType.THREADED);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page", req)
				.paginate(wall)
				.with("postInteractions", interactions)
				.with("owner", user)
				.with("otherUser", otherUser)
				.with("canSeeOthersPosts", ctx.getPrivacyController().checkUserPrivacy(self==null ? null : self.user, user, UserPrivacySettingKey.WALL_OTHERS_POSTS))
				.with("tab", "wall2wall")
				.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())))
				.headerBack(user);
		preparePostList(ctx, wall.list, model, self);
		return model;
	}

	public static Object ajaxCommentPreview(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);

		Post requestedPost;
		Post post=requestedPost=ctx.getWallController().getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));
		int postID=post.id;
		if(post.isMastodonStyleRepost())
			post=ctx.getWallController().getPostOrThrow(post.repostOf);
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		int maxID=parseIntOrDefault(req.queryParams("firstID"), 0);
		if(maxID==0)
			throw new BadRequestException();

		String rid=req.queryParams("rid");
		String ridSuffix="";
		if(StringUtils.isNotEmpty(rid))
			ridSuffix="_"+rid;

		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> comments;
		if(viewType==CommentViewType.FLAT){
			comments=ctx.getWallController().getRepliesFlat(self!=null ? self.user : null, post.getReplyKeyForReplies(), maxID, 100);
		}else{
			comments=PostViewModel.wrap(ctx.getWallController().getRepliesExact(self!=null ? self.user : null, post.getReplyKeyForReplies(), maxID, 100));
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments.list).with("baseReplyLevel", post.getReplyLevel());
		if(StringUtils.isNotEmpty(rid))
			model.with("randomID", rid);
		preparePostList(ctx, comments.list, model, self);
		PostViewModel topLevel;
		if(requestedPost.isMastodonStyleRepost())
			topLevel=new PostViewModel(requestedPost);
		else
			topLevel=new PostViewModel(post.replyKey.isEmpty() ? post : ctx.getWallController().getPostOrThrow(post.replyKey.getFirst()));
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(topLevel), comments.list).flatMap(List::stream).toList(), self!=null ? self.user : null);
		model.with("postInteractions", interactions)
					.with("preview", true)
					.with("replyFormID", "wallPostForm_commentReplyPost"+postID+ridSuffix)
					.with("commentViewType", viewType);
		model.with("topLevel", topLevel);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+postID+ridSuffix, model.renderToString())
				.hide("prevLoader"+postID+ridSuffix);
		if(comments.total>comments.list.size()){
			rb.show("loadPrevBtn"+postID).setAttribute("loadPrevBtn"+postID+ridSuffix, "data-first-id", String.valueOf(comments.list.getFirst().post.id));
			if(viewType==CommentViewType.FLAT){
				rb.setContent("loadPrevBtn"+postID+ridSuffix, lang(req).get("comments_show_X_more_comments", Map.of("count", comments.total-comments.list.size())));
			}
		}else{
			rb.remove("prevLoader"+postID+ridSuffix, "loadPrevBtn"+postID+ridSuffix);
		}
		return rb;
	}

	public static Object ajaxCommentBranch(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		int offset=offset(req);

		String rid=req.queryParams("rid");
		String ridSuffix="";
		if(StringUtils.isNotEmpty(rid))
			ridSuffix="_"+rid;

		Post post=ctx.getWallController().getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0), true);
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> comments=ctx.getWallController().getReplies(self!=null ? self.user : null, post.getReplyKeyForReplies(), offset, 100, 50, viewType, false);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments.list);
		ArrayList<PostViewModel> allReplies=new ArrayList<>();
		for(PostViewModel comment:comments.list){
			allReplies.add(comment);
			comment.getAllReplies(allReplies);
		}
		PostViewModel topLevel=null;
		if(post.replyKey.isEmpty()){
			topLevel=new PostViewModel(post);
		}else{
			int realTopLevelID=post.replyKey.getFirst();
			if(req.queryParams("topLevel")!=null){
				Post topLevelSupposedly=ctx.getWallController().getPostOrThrow(safeParseInt(req.queryParams("topLevel")));
				if(topLevelSupposedly.isMastodonStyleRepost() && post.replyKey.contains(topLevelSupposedly.repostOf)){
					topLevel=new PostViewModel(topLevelSupposedly);
					model.with("baseReplyLevel", post.getReplyLevel()-1);
				}
			}
			if(topLevel==null)
				topLevel=new PostViewModel(ctx.getWallController().getPostOrThrow(realTopLevelID));
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(topLevel), allReplies).flatMap(List::stream).toList(), self!=null ? self.user : null);
		preparePostList(ctx, comments.list, model, self);
		model.with("postInteractions", interactions).with("replyFormID", "wallPostForm_commentReplyPost"+topLevel.post.id+ridSuffix);
		model.with("topLevel", topLevel);
		model.with("commentViewType", viewType);
		if(StringUtils.isNotEmpty(rid))
			model.with("randomID", rid);
		WebDeltaResponse wdr=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "loadRepliesContainer"+post.id+ridSuffix, model.renderToString());
		if(comments.list.size()+offset==comments.total){
			wdr.remove("loadRepliesContainer"+post.id+ridSuffix);
		}else{
			wdr.hide("repliesLoader"+post.id+ridSuffix)
					.show("loadRepliesLink"+post.id+ridSuffix)
					.setAttribute("loadRepliesLink"+post.id+ridSuffix, "data-offset", String.valueOf(offset+comments.list.size()))
					.setContent("loadRepliesLink"+post.id+ridSuffix, lang(req).get("comments_show_X_more_replies", Map.of("count", comments.total-comments.list.size()-offset)));
		}
		return wdr;
	}

	public static Object pollOptionVoters(Request req, Response resp){
		int postID=parseIntOrDefault(req.params(":postID"), 0);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getPostOrThrow(postID);
		if(post.poll==null)
			throw new ObjectNotFoundException();
		if(post.poll.anonymous)
			throw new UserActionNotAllowedException();

		PollOption option=null;
		for(PollOption opt:post.poll.options){
			if(opt.id==optionID){
				option=opt;
				break;
			}
		}
		if(option==null)
			throw new ObjectNotFoundException();

		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		context(req).getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);

		List<Integer> userIDs=ctx.getWallController().getPollOptionVoters(option, offset, 100);
		Map<Integer, User> users=ctx.getUsersController().getUsers(userIDs);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req);
		model.paginate(new PaginatedList<>(userIDs, option.numVotes, offset, 100), "/posts/"+postID+"/pollVoters/"+option.id+"?fromPagination&offset=", null);
		model.with("emptyMessage", lang(req).get("poll_option_votes_empty"))
				.with("summary", lang(req).get("X_people_voted_title", Map.of("count", option.numVotes)))
				.with("users", users);
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(users.values());
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		if(isAjax(req)){
			if(req.queryParams("fromPagination")==null)
				return new WebDeltaResponse(resp).box(option.text, model.renderToString(), "likesList", 610);
			else
				return new WebDeltaResponse(resp).setContent("likesList", model.renderToString());
		}
		model.with("contentTemplate", "user_grid").with("title", option.text);
		return model;
	}

	public static Object pollOptionVotersPopover(Request req, Response resp){
		int postID=parseIntOrDefault(req.params(":postID"), 0);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getPostOrThrow(postID);
		if(post.poll==null)
			throw new ObjectNotFoundException();
		if(post.poll.anonymous)
			throw new UserActionNotAllowedException();

		PollOption option=null;
		for(PollOption opt:post.poll.options){
			if(opt.id==optionID){
				option=opt;
				break;
			}
		}
		if(option==null)
			throw new ObjectNotFoundException();

		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		context(req).getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);

		List<Integer> userIDs=ctx.getWallController().getPollOptionVoters(option, 0, 6);
		String _content=new RenderedTemplateResponse("like_popover", req)
				.with("ids", userIDs)
				.with("users", ctx.getUsersController().getUsers(userIDs))
				.renderToString();

		UserInteractionsRoutes.LikePopoverResponse r=new UserInteractionsRoutes.LikePopoverResponse();
		r.actions=Collections.emptyList();
		r.title=lang(req).get("X_people_voted_title", Map.of("count", option.numVotes));
		r.content=_content;
		r.show=true;
		r.fullURL="/posts/"+postID+"/pollVoters/"+optionID;
		return gson.toJson(r);
	}

	public static Object repostForm(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Lang l=lang(req);
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params(":postID")));
		if(post.isMastodonStyleRepost())
			post=ctx.getWallController().getPostOrThrow(post.repostOf);
		if(!requireAccount(req, null)){
			String url=post.getActivityPubURL().toString();
			String title=l.get(post.getReplyLevel()>0 ? "share_comment_title" : "share_post_title");
			return UserInteractionsRoutes.remoteInteraction(req, resp, url, title, post, false);
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("repost_form", req);
		model.with("repostedPost", post)
				.with("users", ctx.getUsersController().getUsers(Set.of(post.authorID)));
		if(isAjax(req)){
			WebDeltaResponse wdr=new WebDeltaResponse(resp)
					.box(l.get(post.getReplyLevel()>0 ? "share_comment_title" : "share_post_title"), model.renderToString(), "repostFormBox", isMobile(req) ? 0 : 502)
					.runScript("updatePostForms();");
			if(!isMobile(req) && post.isLocal()){
				wdr.runScript("initTabbedBox(ge(\"repostTabBar"+post.id+"\"), ge(\"repostTabContent"+post.id+"\")); initEmbedPreview("+post.id+");");
			}
			return wdr;
		}
		return "";
	}

	public static Object repostList(Request req, Response resp){
		ApplicationContext ctx=context(req);
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=ctx.getWallController().getPostOrThrow(postID);
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		int offset=offset(req);
		PaginatedList<PostViewModel> reposts=PostViewModel.wrap(ctx.getWallController().getPostReposts(post, offset, 20));
		ctx.getWallController().populateReposts(self!=null ? self.user : null, reposts.list, 2);
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(self!=null ? self.user : null, reposts.list.stream().filter(p->!p.post.isMastodonStyleRepost()).toList(), self!=null ? self.prefs.commentViewType : CommentViewType.THREADED);
		}
		UserInteractions interactions=ctx.getWallController().getUserInteractions(Stream.of(reposts.list, List.of(new PostViewModel(post))).flatMap(List::stream).toList(), self!=null ? self.user : null).get(post.getIDForInteractions());
		RenderedTemplateResponse model;
		if(isMobile(req)){
			model=new RenderedTemplateResponse("content_interactions_reposts", req);
		}else{
			for(PostViewModel p:reposts.list){
				if(p.post.isMastodonStyleRepost()){
//					p.canComment=false;
				}
			}
			model=new RenderedTemplateResponse(isAjax(req) ? "content_interactions_box" : "content_wrap", req);
		}
		model.paginate(reposts)
				.with("interactions", interactions)
				.with("post", post)
				.with("tab", "reposts")
				.with("url", "/posts/"+post.id)
				.with("elementID", "Post"+post.id)
				.with("maxRepostDepth", 0);
		preparePostList(ctx, reposts.list, model, self);
		if(isMobile(req))
			return model.pageTitle(lang(req).get("likes_title"));
		if(isAjax(req)){
			String paginationID=req.queryParams("pagination");
			boolean fromTab=req.queryParams("fromTab")!=null;
			if(fromTab){
				return model.renderBlock("reposts");
			}else if(paginationID!=null){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("repostsInner"));
				if(offset+reposts.list.size()<reposts.total){
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", "/posts/"+postID+"/reposts?offset="+(offset+reposts.perPage));
				}else{
					r.remove("ajaxPagination_"+paginationID);
				}
				return r;
			}else{
				return new WebDeltaResponse(resp)
						.box(lang(req).get("likes_title"), model.renderToString(), "likesList", 620)
						.runScript("initTabbedBox(ge(\"interactionsTabsPost"+post.id+"\"), ge(\"interactionsContentPost"+post.id+"\")); initDynamicControls(ge(\"likesList\"));");
			}
		}
		model.with("contentTemplate", "content_interactions_box").with("title", lang(req).get("likes_title"));
		return model;
	}

	public static Object postEmbedURL(Request req, Response resp){
		resp.header("Access-Control-Allow-Origin", "*");
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getLocalPostOrThrow(safeParseInt(req.params(":postID")));
		ctx.getPrivacyController().enforceObjectPrivacy(null, post);
		return UriBuilder.local().appendPath("posts").appendPath(String.valueOf(post.id)).appendPath("embed").build().toString();
	}

	public static Object postEmbed(Request req, Response resp){
		req.attribute("noPreload", true);
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getLocalPostOrThrow(safeParseInt(req.params(":postID")));
		if(!post.isLocal() || post.privacy!=Post.Privacy.PUBLIC || (post.getReplyLevel()==0 && post.ownerID!=post.authorID)){
			throw new UserActionNotAllowedException();
		}
		PostViewModel pvm=new PostViewModel(post);
		ctx.getWallController().populateReposts(null, List.of(pvm), 2);
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(List.of(pvm), null);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(Set.of(pvm), needUsers, needGroups);
		RenderedTemplateResponse model=new RenderedTemplateResponse("post_embed", req)
				.with("post", pvm)
				.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups))
				.with("interactions", interactions);
		if(post.getReplyLevel()>0){
			try{
				Post topLevel=ctx.getWallController().getPostOrThrow(post.replyKey.getFirst());
				if(topLevel.privacy!=Post.Privacy.PUBLIC || topLevel.ownerID!=post.authorID){
					throw new UserActionNotAllowedException();
				}
				model.with("topLevelPost", topLevel);
			}catch(ObjectNotFoundException ignore){}
		}
		return model;
	}

	public static Object embedBox(Request req, Response resp){
		if(isMobile(req))
			return "";
		RenderedTemplateResponse model=new RenderedTemplateResponse("post_embed_form", req);
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params(":postID")));
		model.with("repostedPost", post)
				.with("users", ctx.getUsersController().getUsers(Set.of(post.authorID)));
		if(!isAjax(req)){
			model.setName("content_wrap");
			model.with("contentTemplate", "post_embed_form");
			return model;
		}
		return new WebDeltaResponse(resp)
				.box(lang(req).get("embed_post"), model.renderToString(), null, false)
				.runScript("actuallyInitEmbedPreview("+post.id+");");
	}

	public static Object commentHoverCard(Request req, Response resp){
		if(isMobile(req) || !isAjax(req))
			return "";
		ApplicationContext ctx=context(req);
		Post post=ctx.getWallController().getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));
		if(post.getReplyLevel()==0)
			return "";
		Account self=null;
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			self=info.account;
		}
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		PostViewModel pvm=new PostViewModel(post);
		pvm.parentAuthorID=ctx.getWallController().getPostOrThrow(post.replyKey.getLast()).authorID;
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(Set.of(pvm), needUsers, needGroups);
		return new RenderedTemplateResponse("wall_reply_hover_card", req)
				.with("post", pvm)
				.with("maxReplyDepth", getMaxReplyDepth(self))
				.with("users", ctx.getUsersController().getUsers(needUsers, true))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
	}

	public static int getMaxReplyDepth(@Nullable Account self){
		return switch(self!=null ? self.prefs.commentViewType : CommentViewType.THREADED){
			case THREADED -> 10;
			case TWO_LEVEL -> 2;
			case FLAT -> 1;
		};
	}

	public static Object ajaxLayerPrevComments(Request req, Response resp){
		if(!isAjax(req))
			throw new BadRequestException();

		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		PostViewModel post=new PostViewModel(ctx.getWallController().getPostOrThrow(postID));
		SessionInfo info=Utils.sessionInfo(req);
		Account self=null;
		if(info!=null && info.account!=null){
			self=info.account;
		}
		ctx.getPrivacyController().enforcePostPrivacy(self==null ? null : self.user, post.post);
		if(post.post.getReplyLevel()>0 || post.post.isMastodonStyleRepost())
			throw new BadRequestException();

		String rid=req.queryParams("rid");
		String ridSuffix="";
		if(StringUtils.isNotEmpty(rid))
			ridSuffix="_"+rid;

		int offset=offset(req);
		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> comments=ctx.getWallController().getReplies(self!=null ? self.user : null, post.post.getReplyKeyForReplies(), offset, 100, 50, viewType, true);
		comments.list=comments.list.reversed();

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments.list).with("baseReplyLevel", post.post.getReplyLevel());
		if(StringUtils.isNotEmpty(rid))
			model.with("randomID", rid);
		preparePostList(ctx, comments.list, model, self);
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(post), comments.list).flatMap(List::stream).toList(), self!=null ? self.user : null);
		model.with("postInteractions", interactions)
				.with("replyFormID", "wallPostForm_commentReplyPost"+postID+ridSuffix)
				.with("commentViewType", viewType);
		model.with("topLevel", post);
		boolean mobile=isMobile(req);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.runScript(mobile ? "window._layerScrollHeight=document.scrollingElement.scrollHeight;" : "window._layerScrollHeight=ge(\"postReplies"+postID+ridSuffix+"\").closest(\".layerContent\").scrollHeight;")
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+postID+ridSuffix, model.renderToString())
				.hide("prevLoader"+postID+ridSuffix);
		if(comments.total>comments.list.size()+offset){
			rb.show("loadPrevBtn"+postID).setAttribute("loadPrevBtn"+postID+ridSuffix, "href", "/posts/"+postID+"/layerPrevComments?offset="+(offset+comments.list.size())+(StringUtils.isNotEmpty(rid) ? "&rid="+rid : ""));
			if(viewType==CommentViewType.FLAT){
				rb.setContent("loadPrevBtn"+postID+ridSuffix, lang(req).get("comments_show_X_more_comments", Map.of("count", comments.total-comments.list.size()-offset)));
			}
		}else{
			if(mobile)
				rb.remove("loadPrevWrap"+postID);
			else
				rb.remove("prevLoader"+postID+ridSuffix, "loadPrevBtn"+postID+ridSuffix)
						.show("postCommentsTotal"+postID+ridSuffix);
		}
		if(mobile)
			rb.runScript("var layerCont=document.scrollingElement; layerCont.scrollTop+=layerCont.scrollHeight-window._layerScrollHeight; delete window._layerScrollHeight;");
		else
			rb.runScript("var layerCont=ge(\"postReplies"+postID+ridSuffix+"\").closest(\".layerContent\"); layerCont.scrollTop+=layerCont.scrollHeight-window._layerScrollHeight; delete window._layerScrollHeight;");
		return rb;
	}
}
