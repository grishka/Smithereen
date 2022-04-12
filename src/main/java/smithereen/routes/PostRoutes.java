package smithereen.routes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
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

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.WebDeltaResponse;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.feed.PostNewsfeedEntry;
import smithereen.data.notifications.Notification;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.GroupStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class PostRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(PostRoutes.class);

	public static Object createUserWallPost(Request req, Response resp, Account self) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null)
			throw new ObjectNotFoundException("err_user_not_found");
		ensureUserNotBlocked(self.user, user);
		return createWallPost(req, resp, self, user);
	}

	public static Object createGroupWallPost(Request req, Response resp, Account self) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getById(id);
		if(group==null)
			throw new ObjectNotFoundException("err_group_not_found");
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
					opt.name=o;
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
			RenderedTemplateResponse model=new RenderedTemplateResponse(replyTo!=0 ? "wall_reply" : "wall_post", req).with("post", post).with("postInteractions", interactions);
			if(replyTo!=0){
				model.with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0));
			}
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

	public static Object editPostForm(Request req, Response resp, Account self) throws SQLException{
		int id=parseIntOrDefault(req.params(":postID"), 0);
		Post post=context(req).getWallController().getPostOrThrow(id);
		if(!sessionInfo(req).permissions.canEditPost(post))
			throw new UserActionNotAllowedException();
		RenderedTemplateResponse model;
		if(isAjax(req)){
			model=new RenderedTemplateResponse("wall_post_form", req);
		}else{
			model=new RenderedTemplateResponse("content_wrap", req).with("contentTemplate", "wall_post_form");
		}
		model.with("addClasses", "editing").with("isEditing", true).with("id", "edit"+id).with("editingPostID", id);
		model.with("prefilledPostText", post.source);
		if(post.hasContentWarning())
			model.with("contentWarning", post.summary);
		if(post.poll!=null)
			model.with("poll", post.poll);
		if(post.attachment!=null && !post.attachment.isEmpty()){
			model.with("draftAttachments", post.attachment);
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

	public static Object editPost(Request req, Response resp, Account self) throws SQLException{
		int id=parseIntOrDefault(req.params(":postID"), 0);
		String text=req.queryParams("text");
		Poll poll;
		String pollQuestion=req.queryParams("pollQuestion");
		if(StringUtils.isNotEmpty(pollQuestion)){
			Post post=context(req).getWallController().getPostOrThrow(id);
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
					opt.name=o;
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

		Post post=context(req).getWallController().editPost(sessionInfo(req).permissions, id, text, contentWarning, attachments, poll);
		if(isAjax(req)){
			if(req.attribute("mobile")!=null)
				return new WebDeltaResponse(resp).replaceLocation(post.getInternalURL().toString());

			RenderedTemplateResponse model=new RenderedTemplateResponse(post.getReplyLevel()>0 ? "wall_reply" : "wall_post", req).with("post", post).with("postInteractions", PostStorage.getPostInteractions(List.of(post.id), self.user.id));
			return new WebDeltaResponse(resp).setContent("postInner"+post.id, model.renderBlock("postInner"))
					.show("postInner"+post.id)
					.remove("wallPostForm_edit"+post.id, "postEditingLabel"+post.id)
					.runScript("delete postForms['wallPostForm_edit"+post.id+"'];");
		}
		resp.redirect(post.getInternalURL().toString());
		return "";
	}

	public static Object feed(Request req, Response resp, Account self) throws SQLException{
		int userID=self.user.id;
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		int[] total={0};
		List<NewsfeedEntry> feed=PostStorage.getFeed(userID, startFromID, offset, total);
		List<Post> feedPosts=feed.stream().filter(e->e instanceof PostNewsfeedEntry pe && pe.post!=null).map(e->((PostNewsfeedEntry)e).post).collect(Collectors.toList());
		if(req.attribute("mobile")==null && !feedPosts.isEmpty()){
			context(req).getWallController().populateCommentPreviews(feedPosts);
		}
		Map<Integer, UserInteractions> interactions=context(req).getWallController().getUserInteractions(feedPosts, self.user);
		if(!feed.isEmpty() && startFromID==0)
			startFromID=feed.get(0).id;
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "attach_menu_photo", "attach_menu_cw", "attach_menu_poll", "max_file_size_exceeded", "max_attachment_count_exceeded", "remove_attachment");
		jsLangKey(req, "create_poll_question", "create_poll_options", "create_poll_add_option", "create_poll_delete_option", "create_poll_multi_choice", "create_poll_anonymous", "create_poll_time_limit", "X_days", "X_hours");
		return new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed).with("postInteractions", interactions)
				.with("paginationUrlPrefix", "/feed?startFrom="+startFromID+"&offset=").with("totalItems", total[0]).with("paginationOffset", offset).with("paginationPerPage", 25).with("paginationFirstPageUrl", "/feed")
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments);
	}

	private static Post getPostOrThrow(int postID) throws SQLException{
		if(postID==0){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		return post;
	}

	public static Object standalonePost(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=getPostOrThrow(postID);
		int[] replyKey=new int[post.replyKey.length+1];
		System.arraycopy(post.replyKey, 0, replyKey, 0, post.replyKey.length);
		replyKey[replyKey.length-1]=post.id;
		post.repliesObjects=PostStorage.getReplies(replyKey);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_post_standalone", req);
		model.with("post", post);
		model.with("isGroup", post.owner instanceof Group);
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null){
			model.with("draftAttachments", info.postDraftAttachments);
			if(post.isGroupOwner() && post.getReplyLevel()==0){
				model.with("groupAdminLevel", GroupStorage.getGroupMemberAdminLevel(((Group) post.owner).id, info.account.user.id));
			}
		}
		if(post.replyKey.length>0){
			model.with("prefilledPostText", post.user.getNameForReply()+", ");
		}
		ArrayList<Integer> postIDs=new ArrayList<>();
		postIDs.add(post.id);
		post.getAllReplyIDs(postIDs);
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, info!=null && info.account!=null ? info.account.user.id : 0);
		model.with("postInteractions", interactions);
		if(info==null || info.account==null){
			HashMap<String, String> moreMeta=new LinkedHashMap<>();
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:type", "article");
			meta.put("og:title", post.user.getFullName());
			meta.put("og:url", post.url.toString());
			meta.put("og:published_time", Utils.formatDateAsISO(post.published));
			meta.put("og:author", post.user.url.toString());
			if(StringUtils.isNotEmpty(post.content)){
				String text=Utils.truncateOnWordBoundary(post.content, 250);
				meta.put("og:description", text);
				moreMeta.put("description", text);
			}
			boolean hasImage=false;
			if(post.attachment!=null && !post.attachment.isEmpty()){
				for(Attachment att : post.getProcessedAttachments()){
					if(att instanceof PhotoAttachment){
						PhotoAttachment pa=(PhotoAttachment) att;
						SizedImage.Dimensions size=((PhotoAttachment) att).image.getDimensionsForSize(SizedImage.Type.MEDIUM);
						meta.put("og:image", pa.image.getUriForSizeAndFormat(SizedImage.Type.MEDIUM, SizedImage.Format.JPEG).toString());
						meta.put("og:image:width", size.width+"");
						meta.put("og:image:height", size.height+"");
						hasImage=true;
						break;
					}
				}
			}
			if(!hasImage){
				if(post.user.hasAvatar()){
					URI img=post.user.getAvatar().getUriForSizeAndFormat(SizedImage.Type.LARGE, SizedImage.Format.JPEG);
					if(img!=null){
						SizedImage.Dimensions size=post.user.getAvatar().getDimensionsForSize(SizedImage.Type.LARGE);
						meta.put("og:image", img.toString());
						meta.put("og:image:width", size.width+"");
						meta.put("og:image:height", size.height+"");
					}
				}
			}
			model.with("metaTags", meta);
			model.with("moreMetaTags", moreMeta);
		}
		Utils.jsLangKey(req, "yes", "no", "cancel", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw");
		model.with("title", post.getShortTitle(50)+" | "+post.user.getFullName());
		if(req.attribute("mobile")!=null){
			model.with("toolbarTitle", lang(req).get("wall_post_title"));
			List<User> likers=LikeStorage.getPostLikes(postID, info!=null && info.account!=null ? info.account.user.id : 0, 0, 10).stream().map(id->{
				try{
					return UserStorage.getById(id);
				}catch(SQLException x){}
				return null;
			}).filter(Objects::nonNull).collect(Collectors.toList());
			model.with("likedBy", likers);
		}
		if(post.getReplyLevel()>0){
			model.with("jsRedirect", "/posts/"+post.replyKey[0]+"#comment"+post.id);
		}
		model.with("activityPubURL", post.activityPubID);
		return model;
	}

	public static Object confirmDelete(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI("/posts/"+postID+"/delete?_redir="+URLEncoder.encode(back))).with("back", back);
	}

	public static Object delete(Request req, Response resp, Account self) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			throw new ObjectNotFoundException("err_post_not_found");
		}
		if(!sessionInfo(req).permissions.canDeletePost(post)){
			throw new UserActionNotAllowedException();
		}
		PostStorage.deletePost(post.id);
		NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
		if(Config.isLocal(post.activityPubID) && post.attachment!=null && !post.attachment.isEmpty()){
			MediaStorageUtils.deleteAttachmentFiles(post.attachment);
		}
		User deleteActor=self.user;
		// if the current user is a moderator, and the post isn't made or owned by them, send the deletion as if the author deleted the post themselves
		if(self.accessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal() && post.user.id!=self.id && !post.isGroupOwner() && post.owner.getLocalID()!=self.id && !(post.user instanceof ForeignUser)){
			deleteActor=post.user;
		}
		context(req).getActivityPubWorker().sendDeletePostActivity(post, deleteActor);
		if(isAjax(req)){
			resp.type("application/json");
			return new WebDeltaResponse().remove("post"+postID);
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object like(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0)
			throw new ObjectNotFoundException("err_post_not_found");
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null)
			throw new ObjectNotFoundException("err_post_not_found");
		if(post.owner instanceof User)
			ensureUserNotBlocked(self.user, (User) post.owner);
		else
			ensureUserNotBlocked(self.user, (Group) post.owner);
		String back=Utils.back(req);

		int id=LikeStorage.setPostLiked(self.user.id, postID, true);
		if(id==0) // Already liked
			return "";
		if(!(post.user instanceof ForeignUser) && post.user.id!=self.user.id){
			Notification n=new Notification();
			n.type=Notification.Type.LIKE;
			n.actorID=self.user.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.user.id, n);
		}
		context(req).getActivityPubWorker().sendLikeActivity(post, self.user, id);
		if(isAjax(req)){
			UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(post.id), self.user.id).get(post.id);
			return new WebDeltaResponse(resp)
					.setContent("likeCounterPost"+postID, interactions.likeCount+"")
					.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/unlike?csrf="+sessionInfo(req).csrfToken);
		}
		resp.redirect(back);
		return "";
	}

	public static Object unlike(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0)
			throw new ObjectNotFoundException("err_post_not_found");
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null)
			throw new ObjectNotFoundException("err_post_not_found");
		String back=Utils.back(req);

		int id=LikeStorage.setPostLiked(self.user.id, postID, false);
		if(id==0)
			return "";
		if(!(post.user instanceof ForeignUser) && post.user.id!=self.user.id){
			NotificationsStorage.deleteNotification(Notification.ObjectType.POST, postID, Notification.Type.LIKE, self.user.id);
		}
		context(req).getActivityPubWorker().sendUndoLikeActivity(post, self.user, id);
		if(isAjax(req)){
			UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(post.id), self.user.id).get(post.id);
			WebDeltaResponse b=new WebDeltaResponse(resp)
					.setContent("likeCounterPost"+postID, interactions.likeCount+"")
					.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/like?csrf="+sessionInfo(req).csrfToken);
			if(interactions.likeCount==0)
				b.hide("likeCounterPost"+postID);
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

	public static Object likePopover(Request req, Response resp) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0)
			throw new ObjectNotFoundException("err_post_not_found");
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null)
			throw new ObjectNotFoundException("err_post_not_found");
		SessionInfo info=sessionInfo(req);
		int selfID=info!=null && info.account!=null ? info.account.user.id : 0;
		List<Integer> ids=LikeStorage.getPostLikes(postID, selfID, 0, 6);
		ArrayList<User> users=new ArrayList<>();
		for(int id:ids)
			users.add(UserStorage.getById(id));
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();
		UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(postID), selfID).get(postID);
		WebDeltaResponse b=new WebDeltaResponse(resp)
				.setContent("likeCounterPost"+postID, interactions.likeCount+"");
		if(info!=null && info.account!=null){
			b.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/"+(interactions.isLiked ? "un" : "")+"like?csrf="+sessionInfo(req).csrfToken);
		}
		if(interactions.likeCount==0)
			b.hide("likeCounterPost"+postID);
		else
			b.show("likeCounterPost"+postID);

		LikePopoverResponse o=new LikePopoverResponse();
		o.content=_content;
		o.title=lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount));
		o.altTitle=selfID==0 ? null : lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount+(interactions.isLiked ? -1 : 1)));
		o.actions=b.commands();
		o.show=interactions.likeCount>0;
		o.fullURL="/posts/"+postID+"/likes";
		return gson.toJson(o);
	}

	public static Object likeList(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=context(req).getWallController().getPostOrThrow(postID);
		int offset=offset(req);
		PaginatedList<User> likes=context(req).getUserInteractionsController().getLikesForObject(post, self!=null ? self.user : null, offset, 100);
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

	private static Object wall(Request req, Response resp, Actor owner, boolean ownOnly){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int offset=offset(req);
		PaginatedList<Post> wall=context(req).getWallController().getWallPosts(owner, ownOnly, offset, 20);
		if(req.attribute("mobile")==null){
			context(req).getWallController().populateCommentPreviews(wall.list);
		}
		Map<Integer, UserInteractions> interactions=context(req).getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);

		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page", req)
				.paginate(wall)
				.with("postInteractions", interactions)
				.with("owner", owner)
				.with("isGroup", owner instanceof Group)
				.with("ownOnly", ownOnly)
				.with("tab", ownOnly ? "own" : "all");

		if(owner instanceof User user){
			model.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
		}else{
			model.pageTitle(lang(req).get("wall_of_group"));
		}

		return model;
	}

	public static Object wallToWall(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		User otherUser=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":otherUserID")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int offset=offset(req);
		PaginatedList<Post> wall=context(req).getWallController().getWallToWallPosts(user, otherUser, offset, 20);
		if(req.attribute("mobile")==null){
			context(req).getWallController().populateCommentPreviews(wall.list);
		}
		Map<Integer, UserInteractions> interactions=context(req).getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);

		return new RenderedTemplateResponse("wall_page", req)
				.paginate(wall)
				.with("postInteractions", interactions)
				.with("owner", user)
				.with("otherUser", otherUser)
				.with("tab", "wall2wall")
				.pageTitle(lang(req).get("wall_of_X", Map.of("name", user.getFirstAndGender())));
	}

	public static Object ajaxCommentPreview(Request req, Response resp) throws SQLException{
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		Post post=getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));
		int maxID=parseIntOrDefault(req.queryParams("firstID"), 0);
		if(maxID==0)
			throw new BadRequestException();
		int[] total={0};
		List<Post> comments=PostStorage.getRepliesExact(new int[]{post.id}, maxID, 100, total);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments);
		List<Integer> postIDs=comments.stream().map((Post p)->p.id).collect(Collectors.toList());
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		model.with("postInteractions", interactions)
					.with("preview", true)
					.with("replyFormID", "wallPostForm_commentReplyPost"+post.id);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+post.id, model.renderToString())
				.hide("prevLoader"+post.id);
		if(total[0]>100){
			rb.show("loadPrevBtn"+post.id).setAttribute("loadPrevBtn"+post.id, "data-first-id", comments.get(0).id+"");
		}
		return rb;
	}

	public static Object ajaxCommentBranch(Request req, Response resp) throws SQLException{
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		Post post=getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0));

		List<Post> comments=PostStorage.getReplies(post.getReplyKeyForReplies());
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_reply_list", req);
		model.with("comments", comments);
		ArrayList<Integer> postIDs=new ArrayList<>();
		for(Post comment:comments){
			postIDs.add(comment.id);
			comment.getAllReplyIDs(postIDs);
		}
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		model.with("postInteractions", interactions).with("replyFormID", "wallPostForm_commentReplyPost"+post.getReplyChainElement(0));
		return new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "postReplies"+post.id, model.renderToString())
				.remove("loadRepliesLink"+post.id, "repliesLoader"+post.id);
	}

	public static Object pollOptionVoters(Request req, Response resp) throws SQLException{
		int postID=parseIntOrDefault(req.params(":postID"), 0);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		Post post=getPostOrThrow(postID);
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

		List<User> users=UserStorage.getByIdAsList(PostStorage.getPollOptionVoters(option.id, offset, 100));
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req).with("users", users);
		model.with("pageOffset", offset).with("total", option.getNumVotes()).with("paginationUrlPrefix", "/posts/"+postID+"/pollVoters/"+option.id+"?fromPagination&offset=").with("emptyMessage", lang(req).get("poll_option_votes_empty"));
		if(isAjax(req)){
			if(req.queryParams("fromPagination")==null)
				return new WebDeltaResponse(resp).box(option.name, model.renderToString(), "likesList", 610);
			else
				return new WebDeltaResponse(resp).setContent("likesList", model.renderToString());
		}
		model.with("contentTemplate", "user_grid").with("title", option.name);
		return model;
	}

	public static Object pollOptionVotersPopover(Request req, Response resp) throws SQLException{
		int postID=parseIntOrDefault(req.params(":postID"), 0);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);
		Post post=getPostOrThrow(postID);
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

		List<User> users=UserStorage.getByIdAsList(PostStorage.getPollOptionVoters(option.id, 0, 6));
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();

		LikePopoverResponse r=new LikePopoverResponse();
		r.actions=Collections.emptyList();
		r.title=lang(req).get("X_people_voted_title", Map.of("count", option.getNumVotes()));
		r.content=_content;
		r.show=true;
		r.fullURL="/posts/"+postID+"/pollVoters/"+optionID;
		return gson.toJson(r);
	}

	public static Object commentsFeed(Request req, Response resp, Account self) throws SQLException{
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		PaginatedList<NewsfeedEntry> feed=PostStorage.getCommentsFeed(self.user.id, offset, 25);
		List<Post> feedPosts=feed.list.stream().filter(e->e instanceof PostNewsfeedEntry pe && pe.post!=null).map(e->((PostNewsfeedEntry)e).post).collect(Collectors.toList());
		if(req.attribute("mobile")==null && !feedPosts.isEmpty()){
			context(req).getWallController().populateCommentPreviews(feedPosts);
		}
		Map<Integer, UserInteractions> interactions=context(req).getWallController().getUserInteractions(feedPosts, self.user);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "attach_menu_photo", "attach_menu_cw", "attach_menu_poll", "max_file_size_exceeded", "max_attachment_count_exceeded", "remove_attachment");
		jsLangKey(req, "create_poll_question", "create_poll_options", "create_poll_add_option", "create_poll_delete_option", "create_poll_multi_choice", "create_poll_anonymous", "create_poll_time_limit", "X_days", "X_hours");
		return new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list).with("postInteractions", interactions)
				.with("paginationUrlPrefix", "/feed/comments?offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationFirstPageUrl", "/feed/comments").with("tab", "comments").with("paginationPerPage", 25)
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments);
	}
}
