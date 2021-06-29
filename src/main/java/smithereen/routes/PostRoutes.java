package smithereen.routes;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
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
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.ListAndTotal;
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
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.GroupStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.MentionCallback;
import static smithereen.Utils.ensureUserNotBlocked;
import static smithereen.Utils.escapeHTML;
import static smithereen.Utils.gson;
import static smithereen.Utils.isAjax;
import static smithereen.Utils.lang;
import static smithereen.Utils.parseIntOrDefault;
import static smithereen.Utils.preprocessPostHTML;
import static smithereen.Utils.sessionInfo;

public class PostRoutes{

	public static Object createUserWallPost(Request req, Response resp, Account self) throws Exception{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null)
			throw new ObjectNotFoundException("err_user_not_found");
		ensureUserNotBlocked(self.user, user);
		return createWallPost(req, resp, self, user);
	}

	public static Object createGroupWallPost(Request req, Response resp, Account self) throws Exception{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getByID(id);
		if(group==null)
			throw new ObjectNotFoundException("err_group_not_found");
		return createWallPost(req, resp, self, group);
	}

	public static Object createWallPost(Request req, Response resp, Account self, @NotNull Actor owner) throws Exception{
		String text=req.queryParams("text");
		if(text.length()==0 && StringUtils.isEmpty(req.queryParams("attachments")))
			throw new BadRequestException("Empty post");

		final ArrayList<User> mentionedUsers=new ArrayList<>();
		text=preprocessPostHTML(text, new MentionCallback(){
			@Override
			public User resolveMention(String username, String domain){
				try{
					if(domain==null){
						User user=UserStorage.getByUsername(username);
						if(user!=null && !mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
					User user=UserStorage.getByUsername(username+"@"+domain);
					if(user!=null){
						if(!mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
					URI uri=ActivityPub.resolveUsername(username, domain);
					ActivityPubObject obj=ActivityPub.fetchRemoteObject(uri.toString());
					if(obj instanceof ForeignUser){
						ForeignUser _user=(ForeignUser)obj;
						UserStorage.putOrUpdateForeignUser(_user);
						if(!mentionedUsers.contains(_user))
							mentionedUsers.add(_user);
						return _user;
					}
				}catch(Exception x){
					System.out.println("Can't resolve "+username+"@"+domain+": "+x.getMessage());
				}
				return null;
			}

			@Override
			public User resolveMention(String uri){
				try{
					URI u=new URI(uri);
					if("acct".equalsIgnoreCase(u.getScheme())){
						if(u.getSchemeSpecificPart().contains("@")){
							String[] parts=u.getSchemeSpecificPart().split("@");
							return resolveMention(parts[0], parts[1]);
						}
						return resolveMention(u.getSchemeSpecificPart(), null);
					}
					User user=UserStorage.getUserByActivityPubID(u);
					if(user!=null){
						if(!mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
				}catch(Exception x){
					System.out.println("Can't resolve "+uri+": "+x.getMessage());
				}
				return null;
			}
		});
		int userID=self.user.id;
		int replyTo=Utils.parseIntOrDefault(req.queryParams("replyTo"), 0);
		int postID;

		String attachments=null;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();
			String[] aids=req.queryParams("attachments").split(",");
			for(String id:aids){
				if(!id.matches("^[a-fA-F0-9]{32}$"))
					continue;
				ActivityPubObject obj=MediaCache.getAndDeleteDraftAttachment(id, self.id);
				if(obj!=null)
					attachObjects.add(obj);
			}
			if(!attachObjects.isEmpty()){
				if(attachObjects.size()==1){
					attachments=MediaStorageUtils.serializeAttachment(attachObjects.get(0)).toString();
				}else{
					JsonArray ar=new JsonArray();
					for(ActivityPubObject o:attachObjects){
						ar.add(MediaStorageUtils.serializeAttachment(o));
					}
					attachments=ar.toString();
				}
			}
		}
		if(text.length()==0 && StringUtils.isEmpty(attachments))
			throw new BadRequestException("Empty post");

		String contentWarning=req.queryParams("contentWarning");
		if(contentWarning!=null){
			contentWarning=contentWarning.trim();
			if(contentWarning.length()==0)
				contentWarning=null;
		}

		Post parent=null;
		int ownerUserID=owner instanceof User ? ((User) owner).id : 0;
		int ownerGroupID=owner instanceof Group ? ((Group) owner).id : 0;
		if(replyTo!=0){
			parent=PostStorage.getPostByID(replyTo, false);
			if(parent==null)
				throw new ObjectNotFoundException("err_post_not_found");
			int[] replyKey=new int[parent.replyKey.length+1];
			System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
			replyKey[replyKey.length-1]=parent.id;
			// comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
			if(parent.replyKey.length>0 && text.startsWith("<p>"+escapeHTML(parent.user.getNameForReply())+", ")){
				text="<p><a href=\""+escapeHTML(parent.user.url.toString())+"\" class=\"mention\">"+escapeHTML(parent.user.getNameForReply())+"</a>"+text.substring(parent.user.getNameForReply().length()+3);
			}
			if(!mentionedUsers.contains(parent.user))
				mentionedUsers.add(parent.user);
			Post topLevel;
			if(parent.replyKey.length>1){
				topLevel=PostStorage.getPostByID(parent.replyKey[0], false);
				if(topLevel!=null && !mentionedUsers.contains(topLevel.user))
					mentionedUsers.add(topLevel.user);
			}else{
				topLevel=parent;
			}
			if(topLevel!=null){
				if(topLevel.isGroupOwner()){
					ownerGroupID=((Group) topLevel.owner).id;
					ownerUserID=0;
					ensureUserNotBlocked(self.user, (Group) topLevel.owner);
				}else{
					ownerGroupID=0;
					ownerUserID=((User) topLevel.owner).id;
					ensureUserNotBlocked(self.user, (User)topLevel.owner);
				}
			}
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, replyKey, mentionedUsers, attachments, contentWarning);
		}else{
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, null, mentionedUsers, attachments, contentWarning);
		}

		Post post=PostStorage.getPostByID(postID, false);
		if(post==null)
			throw new IllegalStateException("?!");
		if(replyTo==0 && (ownerGroupID!=0 || ownerUserID!=userID) && !(owner instanceof ForeignActor)){
			ActivityPubWorker.getInstance().sendAddPostToWallActivity(post);
		}else{
			ActivityPubWorker.getInstance().sendCreatePostActivity(post);
		}
		NotificationUtils.putNotificationsForPost(post, parent);

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

	public static Object feed(Request req, Response resp, Account self) throws SQLException{
		int userID=self.user.id;
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		int[] total={0};
		List<NewsfeedEntry> feed=PostStorage.getFeed(userID, startFromID, offset, total);
		HashSet<Integer> postIDs=new HashSet<>();
		for(NewsfeedEntry e:feed){
			if(e instanceof PostNewsfeedEntry){
				PostNewsfeedEntry pe=(PostNewsfeedEntry) e;
				if(pe.post!=null){
					postIDs.add(pe.post.id);
				}else{
					System.err.println("No post: "+pe);
				}
			}
		}
		if(req.attribute("mobile")==null && !postIDs.isEmpty()){
			Map<Integer, ListAndTotal<Post>> allComments=PostStorage.getRepliesForFeed(postIDs);
			for(NewsfeedEntry e:feed){
				if(e instanceof PostNewsfeedEntry){
					PostNewsfeedEntry pe=(PostNewsfeedEntry) e;
					if(pe.post!=null){
						ListAndTotal<Post> comments=allComments.get(pe.post.id);
						if(comments!=null){
							pe.post.replies=comments.list;
							pe.post.totalTopLevelComments=comments.total;
							pe.post.getAllReplyIDs(postIDs);
						}
					}
				}
			}
		}
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self.user.id);
		if(!feed.isEmpty() && startFromID==0)
			startFromID=feed.get(0).id;
		Utils.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "attach_menu_photo", "attach_menu_cw");
		return new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed).with("postInteractions", interactions)
				.with("paginationURL", "/feed?startFrom="+startFromID+"&offset=").with("total", total[0]).with("offset", offset)
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
		post.replies=PostStorage.getReplies(replyKey);
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
		ActivityPubWorker.getInstance().sendDeletePostActivity(post, deleteActor);
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
		ActivityPubWorker.getInstance().sendLikeActivity(post, self.user, id);
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
		ActivityPubWorker.getInstance().sendUndoLikeActivity(post, self.user, id);
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
		o.title=lang(req).plural("liked_by_X_people", interactions.likeCount);
		o.altTitle=selfID==0 ? null : lang(req).plural("liked_by_X_people", interactions.likeCount+(interactions.isLiked ? -1 : 1));
		o.actions=b.commands();
		o.show=interactions.likeCount>0;
		o.fullURL="/posts/"+postID+"/likes";
		return gson.toJson(o);
	}

	public static Object likeList(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=getPostOrThrow(postID);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		List<Integer> ids=LikeStorage.getPostLikes(postID, 0, offset, 100);
		ArrayList<User> users=new ArrayList<>();
		for(int id:ids)
			users.add(UserStorage.getById(id));
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req).with("users", users);
		UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(postID), 0).get(postID);
		model.with("pageOffset", offset).with("total", interactions.likeCount).with("paginationUrlPrefix", "/posts/"+postID+"/likes?fromPagination&offset=");
		if(isAjax(req)){
			if(req.queryParams("fromPagination")==null)
				return new WebDeltaResponse(resp).box(lang(req).get("likes_title"), model.renderToString(), "likesList", 610);
			else
				return new WebDeltaResponse(resp).setContent("likesList", model.renderToString());
		}
		model.with("contentTemplate", "user_grid").with("title", lang(req).get("likes_title"));
		return model;
	}

	public static Object wallAll(Request req, Response resp) throws SQLException{
		return wall(req, resp, false);
	}

	public static Object wallOwn(Request req, Response resp) throws SQLException{
		return wall(req, resp, true);
	}

	private static Object wall(Request req, Response resp, boolean ownOnly) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		Group group=null;
		if(user==null){
			group=GroupStorage.getByUsername(username);
			if(group==null){
				throw new ObjectNotFoundException("err_user_not_found");
			}else if(ownOnly){
				resp.redirect(Config.localURI("/"+username+"/wall").toString());
				return "";
			}
		}
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;


		int[] postCount={0};
		int offset=Utils.parseIntOrDefault(req.queryParams("offset"), 0);
		List<Post> wall=PostStorage.getWallPosts(user==null ? group.id : user.id, group!=null, 0, 0, offset, postCount, ownOnly);
		Set<Integer> postIDs=wall.stream().map((Post p)->p.id).collect(Collectors.toSet());

		if(req.attribute("mobile")==null){
			Map<Integer, ListAndTotal<Post>> allComments=PostStorage.getRepliesForFeed(postIDs);
			for(Post post:wall){
				ListAndTotal<Post> comments=allComments.get(post.id);
				if(comments!=null){
					post.replies=comments.list;
					post.totalTopLevelComments=comments.total;
					post.getAllReplyIDs(postIDs);
				}
			}
		}

		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page", req)
				.with("posts", wall)
				.with("postInteractions", interactions)
				.with("owner", user!=null ? user : group)
				.with("isGroup", group!=null)
				.with("postCount", postCount[0])
				.with("pageOffset", offset)
				.with("ownOnly", ownOnly)
				.with("paginationUrlPrefix", Config.localURI("/"+username+"/wall"+(ownOnly ? "/own" : "")))
				.with("tab", ownOnly ? "own" : "all");

		if(user!=null){
			model.with("title", lang(req).inflected("wall_of_X", user.gender, user.firstName, user.lastName, null));
		}else{
			model.with("title", lang(req).get("wall_of_group"));
		}

		return model;
	}

	public static Object wallToWall(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null)
			throw new ObjectNotFoundException("err_user_not_found");
		String otherUsername=req.params(":other_username");
		User otherUser=UserStorage.getByUsername(otherUsername);
		if(otherUser==null)
			throw new ObjectNotFoundException("err_user_not_found");
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int[] postCount={0};
		int offset=Utils.parseIntOrDefault(req.queryParams("offset"), 0);
		List<Post> wall=PostStorage.getWallToWall(user.id, otherUser.id, offset, postCount);
		Set<Integer> postIDs=wall.stream().map((Post p)->p.id).collect(Collectors.toSet());

		if(req.attribute("mobile")==null){
			Map<Integer, ListAndTotal<Post>> allComments=PostStorage.getRepliesForFeed(postIDs);
			for(Post post:wall){
				ListAndTotal<Post> comments=allComments.get(post.id);
				if(comments!=null){
					post.replies=comments.list;
					post.totalTopLevelComments=comments.total;
					post.getAllReplyIDs(postIDs);
				}
			}
		}

		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		return new RenderedTemplateResponse("wall_page", req)
				.with("posts", wall)
				.with("postInteractions", interactions)
				.with("owner", user)
				.with("otherUser", otherUser)
				.with("postCount", postCount[0])
				.with("pageOffset", offset)
				.with("paginationUrlPrefix", Config.localURI("/"+username+"/wall/with/"+otherUsername))
				.with("tab", "wall2wall")
				.with("title", lang(req).inflected("wall_of_X", user.gender, user.firstName, user.lastName, null));
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
}
