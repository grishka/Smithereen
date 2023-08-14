package smithereen.routes;

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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.data.Account;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.UserPrivacySettingKey;
import smithereen.data.ViolationReport;
import smithereen.data.WebDeltaResponse;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.feed.GroupedNewsfeedEntry;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.viewmodel.PostViewModel;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
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
		return createWallPost(req, resp, self, user);
	}

	public static Object createGroupWallPost(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=ctx.getGroupsController().getGroupOrThrow(id);
		return createWallPost(req, resp, self, group);
	}

	public static Object createWallPost(Request req, Response resp, Account self, @NotNull Actor owner){
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
		if(StringUtils.isNotEmpty(req.queryParams("attachments")))
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
		else
			attachments=Collections.emptyList();

		Post post=context(req).getWallController().createWallPost(self.user, self.id, owner, replyTo, text, contentWarning, attachments, poll);

		SessionInfo sess=sessionInfo(req);
		sess.postDraftAttachments.clear();
		if(isAjax(req)){
			String formID=req.queryParams("formID");
			HashMap<Integer, UserInteractions> interactions=new HashMap<>();
			interactions.put(post.id, new UserInteractions());
			RenderedTemplateResponse model=new RenderedTemplateResponse(replyTo!=0 ? "wall_reply" : "wall_post", req).with("post", new PostViewModel(post)).with("postInteractions", interactions);
			if(replyTo!=0){
				model.with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0));
				model.with("topLevel", new PostViewModel(context(req).getWallController().getPostOrThrow(post.replyKey.get(0))));
			}
			model.with("users", Map.of(self.user.id, self.user));
			String postHTML=model.renderToString();
			if(req.attribute("mobile")!=null && replyTo==0){
				postHTML="<div class=\"card\">"+postHTML+"</div>";
			}else if(replyTo==0){
				String cl="feed".equals(formID) ? "feedRow" : "wallRow";
				postHTML="<div class=\""+cl+"\">"+postHTML+"</div>";
			}
			WebDeltaResponse rb;
			if(replyTo==0)
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postList", postHTML);
			else
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "postReplies"+replyTo, postHTML).show("postReplies"+replyTo);
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
		model.with("addClasses", "editing").with("isEditing", true).with("id", "edit"+id).with("editingPostID", id);
		model.with("prefilledPostText", ctx.getWallController().getPostSource(post));
		if(post.hasContentWarning())
			model.with("contentWarning", post.contentWarning);
		if(post.poll!=null)
			model.with("poll", post.poll);
		if(post.attachments!=null && !post.attachments.isEmpty()){
			model.with("draftAttachments", post.attachments);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.hide("postInner"+id)
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postInner"+id, model.renderToString())
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postAuthor"+id, "<span class=\"grayText lowercase\" id=\"postEditingLabel"+id+"\">&nbsp;-&nbsp;"+lang(req).get(post.getReplyLevel()==0 ? "editing_post" : "editing_comment")+"</span>")
					.runScript("updatePostForms();");
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
		if(StringUtils.isNotEmpty(req.queryParams("attachments")))
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
		else
			attachments=Collections.emptyList();

		Post post=ctx.getWallController().editPost(self.user, sessionInfo(req).permissions, id, text, contentWarning, attachments, poll);
		if(isAjax(req)){
			if(req.attribute("mobile")!=null)
				return new WebDeltaResponse(resp).replaceLocation(post.getInternalURL().toString());

			PostViewModel postVM=new PostViewModel(post);
			RenderedTemplateResponse model=new RenderedTemplateResponse(post.getReplyLevel()>0 ? "wall_reply" : "wall_post", req).with("post", postVM).with("postInteractions", ctx.getWallController().getUserInteractions(List.of(postVM), self.user));
			model.with("users", Map.of(self.user.id, self.user));
			return new WebDeltaResponse(resp).setContent("postInner"+post.id, model.renderBlock("postInner"))
					.show("postInner"+post.id)
					.remove("wallPostForm_edit"+post.id, "postEditingLabel"+post.id)
					.runScript("delete postForms['wallPostForm_edit"+post.id+"'];");
		}
		resp.redirect(post.getInternalURL().toString());
		return "";
	}

	private static void prepareFeed(ApplicationContext ctx, Request req, Account self, List<NewsfeedEntry> feed, RenderedTemplateResponse model){
		Set<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>();
		for(NewsfeedEntry e:feed){
			needUsers.add(e.authorID);
			if(e.type==NewsfeedEntry.Type.GROUPED){
				GroupedNewsfeedEntry gne=(GroupedNewsfeedEntry) e;
				if(gne.childEntriesType==NewsfeedEntry.Type.ADD_FRIEND){
					for(NewsfeedEntry ce:gne.childEntries){
						needUsers.add(ce.objectID);
					}
				}else if(gne.childEntriesType==NewsfeedEntry.Type.JOIN_GROUP || gne.childEntriesType==NewsfeedEntry.Type.JOIN_EVENT){
					for(NewsfeedEntry ce:gne.childEntries){
						needGroups.add(ce.objectID);
					}
				}
			}else if(e.type==NewsfeedEntry.Type.POST || e.type==NewsfeedEntry.Type.RETOOT){
				needPosts.add(e.objectID);
			}else if(e.type==NewsfeedEntry.Type.ADD_FRIEND){
				needUsers.add(e.objectID);
			}else if(e.type==NewsfeedEntry.Type.JOIN_GROUP || e.type==NewsfeedEntry.Type.JOIN_EVENT || e.type==NewsfeedEntry.Type.CREATE_GROUP || e.type==NewsfeedEntry.Type.CREATE_EVENT){
				needGroups.add(e.objectID);
			}
		}

		List<PostViewModel> feedPosts=ctx.getWallController().getPosts(needPosts).values().stream().map(PostViewModel::new).toList();

		if(req.attribute("mobile")==null && !feedPosts.isEmpty()){
			ctx.getWallController().populateCommentPreviews(feedPosts);
		}

		PostViewModel.collectActorIDs(feedPosts, needUsers, needGroups);
		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(needGroups);

		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(feedPosts, self.user);
		model.with("posts", feedPosts.stream().collect(Collectors.toMap(pvm->pvm.post.id, Function.identity())))
			.with("users", users).with("groups", groups).with("postInteractions", interactions);
	}

	public static Object feed(Request req, Response resp, Account self, ApplicationContext ctx){
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(self, timeZoneForRequest(req), startFromID, offset, 25);
		if(!feed.list.isEmpty() && startFromID==0)
			startFromID=feed.list.get(0).id;
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel");
		Templates.addJsLangForNewPostForm(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed?startFrom="+startFromID+"&offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationPerPage", 25).with("paginationFirstPageUrl", "/feed")
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments);

		prepareFeed(ctx, req, self, feed.list, model);

		return model;
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

		User author=ctx.getUsersController().getUserOrThrow(post.post.authorID);

		int offset=offset(req);
		PaginatedList<PostViewModel> replies=ctx.getWallController().getReplies(replyKey, offset, 100, 50);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_post_standalone", req);
		model.paginate(replies);
		model.with("post", post);
		model.with("isGroup", post.post.ownerID<0);
		SessionInfo info=Utils.sessionInfo(req);
		User self=null;
		if(info!=null && info.account!=null){
			model.with("draftAttachments", info.postDraftAttachments);
			if(owner instanceof Group group && post.post.getReplyLevel()==0){
				model.with("groupAdminLevel", ctx.getGroupsController().getMemberAdminLevel(group, info.account.user));
			}
			self=info.account.user;
		}

		boolean canOverridePrivacy=false;
		if(self!=null && info.permissions.serverAccessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal()){
			int reportID=safeParseInt(req.queryParams("report"));
			if(reportID!=0){
				try{
					ViolationReport report=ctx.getModerationController().getViolationReportByID(reportID);
					canOverridePrivacy=report.contentType==ViolationReport.ContentType.POST && report.contentID==postID;
				}catch(ObjectNotFoundException ignore){}
			}
		}

		if(canOverridePrivacy){
			try{
				ctx.getPrivacyController().enforceObjectPrivacy(self, post.post);
			}catch(UserActionNotAllowedException x){
				model.with("privacyOverridden", true);
			}
		}else{
			ctx.getPrivacyController().enforceObjectPrivacy(self, post.post);
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

		model.with("canSeeOthersPosts", !(owner instanceof User u) || ctx.getPrivacyController().checkUserPrivacy(self, u, UserPrivacySettingKey.WALL_OTHERS_POSTS));

		if(info==null || info.account==null){
			HashMap<String, String> moreMeta=new LinkedHashMap<>();
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:type", "article");
			meta.put("og:title", author.getFullName());
//			meta.put("og:url", post.url.toString());
			meta.put("og:published_time", Utils.formatDateAsISO(post.post.createdAt));
			meta.put("og:author", author.url.toString());
			if(StringUtils.isNotEmpty(post.post.text)){
				String text=Utils.truncateOnWordBoundary(post.post.text, 250);
				meta.put("og:description", text);
				moreMeta.put("description", text);
			}
			boolean hasImage=false;
			if(post.post.attachments!=null && !post.post.attachments.isEmpty()){
				for(Attachment att : post.post.getProcessedAttachments()){
					if(att instanceof PhotoAttachment pa){
						SizedImage.Dimensions size=pa.image.getDimensionsForSize(SizedImage.Type.MEDIUM);
						meta.put("og:image", pa.image.getUriForSizeAndFormat(SizedImage.Type.MEDIUM, SizedImage.Format.JPEG).toString());
						meta.put("og:image:width", String.valueOf(size.width));
						meta.put("og:image:height", String.valueOf(size.height));
						hasImage=true;
						break;
					}
				}
			}
			if(!hasImage){
				if(author.hasAvatar()){
					URI img=author.getAvatar().getUriForSizeAndFormat(SizedImage.Type.LARGE, SizedImage.Format.JPEG);
					if(img!=null){
						SizedImage.Dimensions size=author.getAvatar().getDimensionsForSize(SizedImage.Type.LARGE);
						meta.put("og:image", img.toString());
						meta.put("og:image:width", String.valueOf(size.width));
						meta.put("og:image:height", String.valueOf(size.height));
					}
				}
			}
			model.with("metaTags", meta);
			model.with("moreMetaTags", moreMeta);
		}
		Utils.jsLangKey(req, "yes", "no", "cancel", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw");
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
		ctx.getWallController().deletePost(requireSession(req), post);
		if(isAjax(req)){
			resp.type("application/json");
			return new WebDeltaResponse().remove("post"+post.id);
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object like(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		ctx.getUserInteractionsController().setObjectLiked(post, true, self.user);
		if(isAjax(req)){
			UserInteractions interactions=ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self.user).get(post.id);
			return new WebDeltaResponse(resp)
					.setContent("likeCounterPost"+post.id, String.valueOf(interactions.likeCount))
					.setAttribute("likeButtonPost"+post.id, "href", post.getInternalURL()+"/unlike?csrf="+requireSession(req).csrfToken);
		}
		String back=Utils.back(req);
		resp.redirect(back);
		return "";
	}

	public static Object unlike(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		Post post=ctx.getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		String back=Utils.back(req);
		ctx.getUserInteractionsController().setObjectLiked(post, false, self.user);
		if(isAjax(req)){
			UserInteractions interactions=ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self.user).get(post.id);
			WebDeltaResponse b=new WebDeltaResponse(resp)
					.setContent("likeCounterPost"+post.id, String.valueOf(interactions.likeCount))
					.setAttribute("likeButtonPost"+post.id, "href", post.getInternalURL()+"/like?csrf="+requireSession(req).csrfToken);
			if(interactions.likeCount==0)
				b.hide("likeCounterPost"+post.id);
			return b;
		}
		resp.redirect(back);
		return "";
	}

	private static class LikePopoverResponse{
		public String content;
		public String title;
		public String altTitle;
		public String fullURL;
		public List<WebDeltaResponse.Command> actions;
		public boolean show;
	}

	public static Object likePopover(Request req, Response resp){
		ApplicationContext ctx=context(req);
		req.attribute("noHistory", true);
		Post post=context(req).getWallController().getPostOrThrow(safeParseInt(req.params("postID")));
		SessionInfo info=sessionInfo(req);
		User self=info!=null && info.account!=null ? info.account.user : null;
		int selfID=self!=null ? self.id : 0;
		context(req).getPrivacyController().enforceObjectPrivacy(self, post);
		List<User> users=ctx.getUserInteractionsController().getLikesForObject(post, self, 0, 6).list;
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();
		UserInteractions interactions=ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self).get(post.id);
		WebDeltaResponse b=new WebDeltaResponse(resp)
				.setContent("likeCounterPost"+post.id, String.valueOf(interactions.likeCount));
		if(info!=null && info.account!=null){
			b.setAttribute("likeButtonPost"+post.id, "href", post.getInternalURL()+"/"+(interactions.isLiked ? "un" : "")+"like?csrf="+info.csrfToken);
		}
		if(interactions.likeCount==0)
			b.hide("likeCounterPost"+post.id);
		else
			b.show("likeCounterPost"+post.id);

		LikePopoverResponse o=new LikePopoverResponse();
		o.content=_content;
		o.title=lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount));
		o.altTitle=selfID==0 ? null : lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount+(interactions.isLiked ? -1 : 1)));
		o.actions=b.commands();
		o.show=interactions.likeCount>0;
		o.fullURL="/posts/"+post.id+"/likes";
		return gson.toJson(o);
	}

	public static Object likeList(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=context(req).getWallController().getPostOrThrow(postID);
		context(req).getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		int offset=offset(req);
		PaginatedList<User> likes=context(req).getUserInteractionsController().getLikesForObject(post, null, offset, 100);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req)
				.paginate(likes, "/posts/"+postID+"/likes?fromPagination&offset=", null)
				.with("emptyMessage", lang(req).get("likes_empty"))
				.with("summary", lang(req).get("liked_by_X_people", Map.of("count", likes.total)));
		if(isAjax(req)){
			if(req.queryParams("fromPagination")==null)
				return new WebDeltaResponse(resp).box(lang(req).get("likes_title"), model.renderToString(), "likesList", 474);
			else
				return new WebDeltaResponse(resp).setContent("likesList", model.renderToString());
		}
		model.with("contentTemplate", "user_grid").with("title", lang(req).get("likes_title"));
		return model;
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

	private static void preparePostList(ApplicationContext ctx, List<PostViewModel> wall, RenderedTemplateResponse model){
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		PostViewModel.collectActorIDs(wall, needUsers, needGroups);
		model.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
	}

	private static Object wall(Request req, Response resp, Actor owner, boolean ownOnly){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		if(owner instanceof Group group)
			ctx.getPrivacyController().enforceUserAccessToGroupContent(self!=null ? self.user : null, group);

		int offset=offset(req);
		PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallPosts(owner, ownOnly, offset, 20));
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(wall.list);
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

		preparePostList(ctx, wall.list, model);

		if(owner instanceof User user){
			model.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
		}else{
			model.pageTitle(lang(req).get("wall_of_group"));
		}

		return model;
	}

	public static Object wallToWall(Request req, Response resp){
		ApplicationContext ctx=context(req);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		User otherUser=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":otherUserID")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int offset=offset(req);
		PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallToWallPosts(user, otherUser, offset, 20));
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(wall.list);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page", req)
				.paginate(wall)
				.with("postInteractions", interactions)
				.with("owner", user)
				.with("otherUser", otherUser)
				.with("tab", "wall2wall")
				.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
		preparePostList(ctx, wall.list, model);
		return model;
	}

	public static Object ajaxCommentPreview(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);

		Post post=ctx.getWallController().getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, post);
		int maxID=parseIntOrDefault(req.queryParams("firstID"), 0);
		if(maxID==0)
			throw new BadRequestException();

		PaginatedList<PostViewModel> comments=PostViewModel.wrap(ctx.getWallController().getRepliesExact(List.of(post.id), maxID, 100));
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments.list);
		preparePostList(ctx, comments.list, model);
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(comments.list, self!=null ? self.user : null);
		model.with("postInteractions", interactions)
					.with("preview", true)
					.with("replyFormID", "wallPostForm_commentReplyPost"+post.id);
		PostViewModel topLevel=new PostViewModel(post.replyKey.isEmpty() ? post : ctx.getWallController().getPostOrThrow(post.replyKey.get(0)));
		topLevel.canComment=post.ownerID<0 || ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(post.ownerID), UserPrivacySettingKey.WALL_COMMENTING);
		model.with("topLevel", topLevel);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+post.id, model.renderToString())
				.hide("prevLoader"+post.id);
		if(comments.total>100){
			rb.show("loadPrevBtn"+post.id).setAttribute("loadPrevBtn"+post.id, "data-first-id", String.valueOf(comments.list.get(0).post.id));
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
		List<PostViewModel> comments=ctx.getWallController().getReplies(post.getReplyKeyForReplies(), offset, 100, 50).list;
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments);
		ArrayList<PostViewModel> allReplies=new ArrayList<>();
		for(PostViewModel comment:comments){
			allReplies.add(comment);
			comment.getAllReplies(allReplies);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(allReplies, self!=null ? self.user : null);
		preparePostList(ctx, comments, model);
		model.with("postInteractions", interactions).with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0));
		PostViewModel topLevel=new PostViewModel(post.replyKey.isEmpty() ? post : ctx.getWallController().getPostOrThrow(post.replyKey.get(0)));
		topLevel.canComment=post.ownerID<0 || ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(post.ownerID), UserPrivacySettingKey.WALL_COMMENTING);
		model.with("topLevel", topLevel);
		return new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "postReplies"+post.id, model.renderToString())
				.remove("loadRepliesContainer"+post.id);
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
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req).with("users", users);
		model.with("pageOffset", offset).with("total", option.numVotes).with("paginationUrlPrefix", "/posts/"+postID+"/pollVoters/"+option.id+"?fromPagination&offset=").with("emptyMessage", lang(req).get("poll_option_votes_empty"));
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

		LikePopoverResponse r=new LikePopoverResponse();
		r.actions=Collections.emptyList();
		r.title=lang(req).get("X_people_voted_title", Map.of("count", option.numVotes));
		r.content=_content;
		r.show=true;
		r.fullURL="/posts/"+postID+"/pollVoters/"+optionID;
		return gson.toJson(r);
	}

	public static Object commentsFeed(Request req, Response resp, Account self, ApplicationContext ctx){
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getCommentsFeed(self, offset, 25);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel");
		Templates.addJsLangForNewPostForm(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed/comments?offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationFirstPageUrl", "/feed/comments").with("tab", "comments").with("paginationPerPage", 25)
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments);

		prepareFeed(ctx, req, self, feed.list, model);

		return model;
	}
}
