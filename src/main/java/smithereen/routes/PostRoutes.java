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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.model.WebDeltaResponse;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.storage.utils.Pair;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
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
			RenderedTemplateResponse model=new RenderedTemplateResponse(replyTo!=0 ? "wall_reply" : "wall_post", req).with("post", pvm);
			if(replyTo!=0){
				PostViewModel topLevel=new PostViewModel(context(req).getWallController().getPostOrThrow(post.replyKey.getFirst()));
				model.with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0));
				model.with("topLevel", topLevel);
				needInteractions.add(topLevel);
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
			if(req.attribute("mobile")!=null && replyTo==0){
				postHTML="<div class=\"card\">"+postHTML+"</div>";
			}else if(replyTo==0){
				// TODO correctly handle day headers in feed
				String cl="feed".equals(formID) ? "feedRow" : "wallRow";
				postHTML="<div class=\""+cl+"\">"+postHTML+"</div>";
			}
			WebDeltaResponse rb;
			if(replyTo==0)
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postList", postHTML);
			else{
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "postReplies"+switch(self.prefs.commentViewType){
					case THREADED -> replyTo;
					case TWO_LEVEL -> post.replyKey.get(Math.min(post.getReplyLevel()-1, 1));
					case FLAT -> post.replyKey.getFirst();
				}, postHTML).show("postReplies"+replyTo);
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
		model.with("addClasses", "editing nonCollapsible").with("isEditing", true).with("id", "edit"+id).with("editingPostID", id);
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
			return new WebDeltaResponse(resp)
					.hide("postInner"+id)
					.hide("postFloatingActions"+id)
					.hide("inReplyTo"+id)
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postInner"+id, model.renderToString())
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postAuthor"+id, "<span class=\"grayText lowercase\" id=\"postEditingLabel"+id+"\">&nbsp;-&nbsp;"+lang(req).get(post.getReplyLevel()==0 ? "editing_post" : "editing_comment")+"</span>")
					.runScript("updatePostForms(); ge(\"postFormText_edit"+id+"\").focus();");
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
			RenderedTemplateResponse model=new RenderedTemplateResponse(post.getReplyLevel()>0 ? "wall_reply" : "wall_post", req).with("post", postVM);
			model.with("users", Map.of(self.user.id, self.user));
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
			return new WebDeltaResponse(resp).setContent("postInner"+post.id, model.renderBlock("postInner"))
					.show("postInner"+post.id)
					.show("postFloatingActions"+id)
					.show("inReplyTo"+id)
					.remove("wallPostForm_edit"+post.id, "postEditingLabel"+post.id);
		}
		resp.redirect(post.getInternalURL().toString());
		return "";
	}

	private static void prepareFeed(ApplicationContext ctx, Request req, Account self, List<NewsfeedEntry> feed, RenderedTemplateResponse model, boolean needNonPostInteractions){
		Set<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>();
		for(NewsfeedEntry e:feed){
			needUsers.add(e.authorID);
			switch(e.type){
				case GROUPED -> {
					GroupedNewsfeedEntry gne=(GroupedNewsfeedEntry) e;
					switch(gne.childEntriesType){
						case ADD_FRIEND -> {
							for(NewsfeedEntry ce: gne.childEntries){
								needUsers.add((int) ce.objectID);
							}
						}
						case JOIN_GROUP, JOIN_EVENT -> {
							for(NewsfeedEntry ce: gne.childEntries){
								needGroups.add((int) ce.objectID);
							}
						}
						case null, default -> {}
					}
				}
				case POST, RETOOT -> needPosts.add((int) e.objectID);
				case ADD_FRIEND -> needUsers.add((int) e.objectID);
				case JOIN_GROUP, JOIN_EVENT, CREATE_GROUP, CREATE_EVENT -> needGroups.add((int) e.objectID);

				case null, default -> {}
			}
		}

		List<PostViewModel> feedPosts=ctx.getWallController().getPosts(needPosts).values().stream().map(PostViewModel::new).toList();

		ctx.getWallController().populateReposts(self.user, feedPosts, 2);
		if(req.attribute("mobile")==null && !feedPosts.isEmpty()){
			ctx.getWallController().populateCommentPreviews(self.user, feedPosts, self.prefs.commentViewType);
		}

		PostViewModel.collectActorIDs(feedPosts, needUsers, needGroups);

		Set<Long> needPhotos=feed.stream()
				.filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG ||
						(e instanceof GroupedNewsfeedEntry gne && (gne.childEntriesType==NewsfeedEntry.Type.ADD_PHOTO || gne.childEntriesType==NewsfeedEntry.Type.PHOTO_TAG)))
				.flatMap(e->switch(e){
					case GroupedNewsfeedEntry gne -> gne.childEntries.stream().map(ce->ce.objectID);
					default -> Stream.of(e.objectID);
				})
				.collect(Collectors.toSet());

		if(!needPhotos.isEmpty()){
			Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
			for(Photo photo:photos.values()){
				if(photo.ownerID>0)
					needUsers.add(photo.ownerID);
				else
					needGroups.add(-photo.ownerID);
				needUsers.add(photo.authorID);
			}
			model.with("photos", photos);
			if(needNonPostInteractions)
				model.with("photosInteractions", ctx.getUserInteractionsController().getUserInteractions(photos.values(), self.user));
		}

		if(needNonPostInteractions && !needPhotos.isEmpty()){
			Map<CommentParentObjectID, PaginatedList<CommentViewModel>> comments=ctx.getCommentsController().getCommentsForFeed(needPhotos.stream()
					.map(id->new CommentParentObjectID(CommentableObjectType.PHOTO, id))
					.collect(Collectors.toSet()), self.prefs.commentViewType==CommentViewType.FLAT, 3);

			CommentViewModel.collectUserIDs(comments.values().stream().flatMap(pl->pl.list.stream()).toList(), needUsers);
			Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(comments.values().stream().flatMap(l->l.list.stream().map(cvm->cvm.post)).toList(), self.user);
			model.with("commentInteractions", interactions);

			model.with("photosComments", comments.entrySet().stream()
					.filter(e->e.getKey().type()==CommentableObjectType.PHOTO)
					.collect(Collectors.toMap(e->e.getKey().id(), Map.Entry::getValue)));
		}

		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(needGroups);

		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(feedPosts, self.user);
		model.with("posts", feedPosts.stream().collect(Collectors.toMap(pvm->pvm.post.id, Function.identity())))
				.with("users", users).with("groups", groups).with("postInteractions", interactions);
		model.with("maxReplyDepth", getMaxReplyDepth(self)).with("commentViewType", self.prefs.commentViewType);
	}

	public static Object feed(Request req, Response resp, Account self, ApplicationContext ctx){
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		EnumSet<FriendsNewsfeedTypeFilter> filter=self.prefs.friendFeedFilter;
		if(filter==null)
			filter=EnumSet.allOf(FriendsNewsfeedTypeFilter.class);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(self, filter, timeZoneForRequest(req), startFromID, offset, 25);
		if(!feed.list.isEmpty() && startFromID==0)
			startFromID=feed.list.getFirst().id;
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "feed_filters");
		Templates.addJsLangForNewPostForm(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed?startFrom="+startFromID+"&offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationPerPage", 25).with("paginationFirstPageUrl", "/feed")
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments)
				.with("feedFilter", filter.stream().map(Object::toString).collect(Collectors.toSet()));

		prepareFeed(ctx, req, self, feed.list, model, false);

		return model;
	}

	public static Object setFeedFilters(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<FriendsNewsfeedTypeFilter> filter=EnumSet.noneOf(FriendsNewsfeedTypeFilter.class);
		for(FriendsNewsfeedTypeFilter type:FriendsNewsfeedTypeFilter.values()){
			if(req.queryParams(type.toString())!=null)
				filter.add(type);
		}
		ctx.getNewsfeedController().setFriendsFeedFilters(self, filter);
		if(isMobile(req)){
			return new WebDeltaResponse(resp).refresh();
		}else{
			RenderedTemplateResponse model=(RenderedTemplateResponse) feed(req, resp, self, ctx);
			return new WebDeltaResponse(resp)
					.setContent("feedContent", model.renderBlock("feedContent"))
					.setContent("feedTopSummary", model.renderBlock("topSummary"))
					.setContent("feedBottomSummary", model.renderBlock("bottomSummary"));
		}
	}

	public static Object standalonePost(Request req, Response resp){
		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		PostViewModel post=new PostViewModel(ctx.getWallController().getPostOrThrow(postID));
		List<Integer> replyKey=post.post.getReplyKeyForReplies();
		Actor owner;
		if(post.post.ownerID<0)
			owner=ctx.getGroupsController().getGroupOrThrow(-post.post.ownerID);
		else
			owner=ctx.getUsersController().getUserOrThrow(post.post.ownerID);

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_post_standalone", req);
		SessionInfo info=Utils.sessionInfo(req);
		Account self=null;
		if(info!=null && info.account!=null){
			model.with("draftAttachments", info.postDraftAttachments);
			if(owner instanceof Group group && post.post.getReplyLevel()==0){
				model.with("groupAdminLevel", ctx.getGroupsController().getMemberAdminLevel(group, info.account.user));
			}
			self=info.account;
		}

		if(post.post.repostOf!=0){
			if(post.post.isMastodonStyleRepost()){
				resp.redirect("/posts/"+post.post.repostOf);
				return "";
			}
			ctx.getWallController().populateReposts(self!=null ? self.user : null, List.of(post), 10);
		}

		User author=ctx.getUsersController().getUserOrThrow(post.post.authorID);

		int offset=offset(req);
		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> replies=ctx.getWallController().getReplies(self!=null ? self.user : null, replyKey, offset, 100, 50, viewType);
		model.paginate(replies);
		model.with("post", post);
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
		model.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));

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
			List<User> likers=ctx.getUserInteractionsController().getLikesForObject(post.post, info!=null && info.account!=null ? info.account.user : null, 0, 10).list;
			model.with("likedBy", likers);
		}
		if(post.post.getReplyLevel()>0){
			model.with("jsRedirect", "/posts/"+post.post.replyKey.get(0)+"#comment"+post.post.id);
		}
		model.with("activityPubURL", post.post.getActivityPubID());
		if(!post.post.isLocal() && owner instanceof ForeignActor)
			model.with("noindex", true);
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
			resp.type("application/json");
			return new WebDeltaResponse().remove("post"+post.id);
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
		List<User> users=ctx.getUserInteractionsController().getRepostedUsers(post, 6);
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();
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
		return wall(req, resp, group, false);
	}

	public static void preparePostList(ApplicationContext ctx, List<PostViewModel> wall, RenderedTemplateResponse model, Account self){
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(wall, needUsers, needGroups);
		model.with("users", ctx.getUsersController().getUsers(needUsers))
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
				.with("tab", ownOnly ? "own" : "all");

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
				.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
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

		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> comments;
		if(viewType==CommentViewType.FLAT){
			comments=ctx.getWallController().getRepliesFlat(self!=null ? self.user : null, post.getReplyKeyForReplies(), maxID, 100);
		}else{
			comments=PostViewModel.wrap(ctx.getWallController().getRepliesExact(self!=null ? self.user : null, post.getReplyKeyForReplies(), maxID, 100));
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments.list).with("baseReplyLevel", post.getReplyLevel());
		preparePostList(ctx, comments.list, model, self);
		PostViewModel topLevel;
		if(requestedPost.isMastodonStyleRepost())
			topLevel=new PostViewModel(requestedPost);
		else
			topLevel=new PostViewModel(post.replyKey.isEmpty() ? post : ctx.getWallController().getPostOrThrow(post.replyKey.getFirst()));
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(topLevel), comments.list).flatMap(List::stream).toList(), self!=null ? self.user : null);
		model.with("postInteractions", interactions)
					.with("preview", true)
					.with("replyFormID", "wallPostForm_commentReplyPost"+postID)
					.with("commentViewType", viewType);
		model.with("topLevel", topLevel);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+postID, model.renderToString())
				.hide("prevLoader"+postID);
		if(comments.total>comments.list.size()){
			rb.show("loadPrevBtn"+postID).setAttribute("loadPrevBtn"+postID, "data-first-id", String.valueOf(comments.list.getFirst().post.id));
			if(viewType==CommentViewType.FLAT){
				rb.setContent("loadPrevBtn"+postID, lang(req).get("comments_show_X_more_comments", Map.of("count", comments.total-comments.list.size())));
			}
		}else{
			rb.remove("prevLoader"+postID, "loadPrevBtn"+postID);
		}
		return rb;
	}

	public static Object ajaxCommentBranch(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		int offset=offset(req);

		Post post=ctx.getWallController().getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<PostViewModel> comments=ctx.getWallController().getReplies(self!=null ? self.user : null, post.getReplyKeyForReplies(), offset, 100, 50, viewType);
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
		model.with("postInteractions", interactions).with("replyFormID", "wallPostForm_commentReplyPost"+topLevel.post.id);
		model.with("topLevel", topLevel);
		model.with("commentViewType", viewType);
		WebDeltaResponse wdr=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "loadRepliesContainer"+post.id, model.renderToString());
		if(comments.list.size()+offset==comments.total){
			wdr.remove("loadRepliesContainer"+post.id);
		}else{
			wdr.hide("repliesLoader"+post.id)
					.show("loadRepliesLink"+post.id)
					.setAttribute("loadRepliesLink"+post.id, "data-offset", String.valueOf(offset+comments.list.size()))
					.setContent("loadRepliesLink"+post.id, lang(req).get("comments_show_X_more_replies", Map.of("count", comments.total-comments.list.size()-offset)));
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

		List<User> users=ctx.getWallController().getPollOptionVoters(option, offset, 100);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req);
		model.paginate(new PaginatedList<>(users, option.numVotes, offset, 100), "/posts/"+postID+"/pollVoters/"+option.id+"?fromPagination&offset=", null);
		model.with("emptyMessage", lang(req).get("poll_option_votes_empty")).with("summary", lang(req).get("X_people_voted_title", Map.of("count", option.numVotes)));
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(users);
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

		List<User> users=ctx.getWallController().getPollOptionVoters(option, 0, 6);
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();

		UserInteractionsRoutes.LikePopoverResponse r=new UserInteractionsRoutes.LikePopoverResponse();
		r.actions=Collections.emptyList();
		r.title=lang(req).get("X_people_voted_title", Map.of("count", option.numVotes));
		r.content=_content;
		r.show=true;
		r.fullURL="/posts/"+postID+"/pollVoters/"+optionID;
		return gson.toJson(r);
	}

	public static Object commentsFeed(Request req, Response resp, Account self, ApplicationContext ctx){
		int offset=offset(req);
		EnumSet<CommentsNewsfeedObjectType> filter=self.prefs.commentsFeedFilter!=null ? self.prefs.commentsFeedFilter : EnumSet.allOf(CommentsNewsfeedObjectType.class);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getCommentsFeed(self, offset, 25, filter);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "cancel", "feed_filters");
		Templates.addJsLangForNewPostForm(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("feed_comments", req)
				.pageTitle(Utils.lang(req).get("feed"))
				.with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed/comments?offset=")
				.with("totalItems", feed.total)
				.with("paginationOffset", offset)
				.with("paginationFirstPageUrl", "/feed/comments")
				.with("paginationPerPage", 25)
				.with("feedFilter", filter.stream().map(Object::toString).collect(Collectors.toSet()));

		prepareFeed(ctx, req, self, feed.list, model, true);

		return model;
	}

	public static Object setCommentsFeedFilters(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<CommentsNewsfeedObjectType> filter=EnumSet.noneOf(CommentsNewsfeedObjectType.class);
		for(CommentsNewsfeedObjectType type:CommentsNewsfeedObjectType.values()){
			if(req.queryParams(type.toString())!=null)
				filter.add(type);
		}
		ctx.getNewsfeedController().setCommentsFeedFilters(self, filter);
		if(isMobile(req)){
			return new WebDeltaResponse(resp).refresh();
		}else{
			RenderedTemplateResponse model=(RenderedTemplateResponse) commentsFeed(req, resp, self, ctx);
			return new WebDeltaResponse(resp)
					.setContent("feedContent", model.renderBlock("feedContent"))
					.setContent("feedTopSummary", model.renderBlock("topSummary"))
					.setContent("feedBottomSummary", model.renderBlock("bottomSummary"));
		}
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
				.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
	}

	public static int getMaxReplyDepth(@Nullable Account self){
		return switch(self!=null ? self.prefs.commentViewType : CommentViewType.THREADED){
			case THREADED -> 10;
			case TWO_LEVEL -> 2;
			case FLAT -> 1;
		};
	}
}
