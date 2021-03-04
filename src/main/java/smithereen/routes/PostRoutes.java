package smithereen.routes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Config;
import static smithereen.Utils.*;

import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.UserInteractions;
import smithereen.data.WebDeltaResponseBuilder;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.Post;
import smithereen.data.feed.PostNewsfeedEntry;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.data.notifications.NotificationUtils;
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

public class PostRoutes{

	public static Object createUserWallPost(Request req, Response resp, Account self) throws Exception{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
		return createWallPost(req, resp, self, user);
	}

	public static Object createGroupWallPost(Request req, Response resp, Account self) throws Exception{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getByID(id);
		if(group==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_group_not_found");
		}
		return createWallPost(req, resp, self, group);
	}

	public static Object createWallPost(Request req, Response resp, Account self, @NotNull Actor owner) throws Exception{
		String text=req.queryParams("text");
		if(text.length()==0 && StringUtils.isEmpty(req.queryParams("attachments")))
			return "Empty post";

		final ArrayList<User> mentionedUsers=new ArrayList<>();
		text=preprocessPostHTML(text, new MentionCallback(){
			@Override
			public User resolveMention(String username, String domain){
				try{
					if(domain==null){
						User user=UserStorage.getByUsername(username);
						if(!mentionedUsers.contains(user))
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
					JSONArray ar=new JSONArray();
					for(ActivityPubObject o:attachObjects){
						ar.put(MediaStorageUtils.serializeAttachment(o));
					}
					attachments=ar.toString();
				}
			}
		}
		if(text.length()==0 && StringUtils.isEmpty(attachments))
			return "Empty post";

		Post parent=null;
		int ownerUserID=owner instanceof User ? ((User) owner).id : 0;
		int ownerGroupID=owner instanceof Group ? ((Group) owner).id : 0;
		if(replyTo!=0){
			parent=PostStorage.getPostByID(replyTo, false);
			if(parent==null){
				resp.status(404);
				return Utils.wrapError(req, resp, "err_post_not_found");
			}
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
				}else{
					ownerGroupID=0;
					ownerUserID=((User) topLevel.owner).id;
				}
			}
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, replyKey, mentionedUsers, attachments);
		}else{
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, null, mentionedUsers, attachments);
		}

		Post post=PostStorage.getPostByID(postID, false);
		if(post==null)
			throw new IllegalStateException("?!");
		ActivityPubWorker.getInstance().sendCreatePostActivity(post);
		NotificationUtils.putNotificationsForPost(post, parent);

		SessionInfo sess=sessionInfo(req);
		sess.postDraftAttachments.clear();
		if(isAjax(req)){
			HashMap<Integer, UserInteractions> interactions=new HashMap<>();
			interactions.put(post.id, new UserInteractions());
			String postHTML=new RenderedTemplateResponse(replyTo!=0 ? "wall_reply" : "wall_post").with("post", post).with("postInteractions", interactions).renderToString(req);
			resp.type("application/json");
			WebDeltaResponseBuilder rb;
			if(replyTo==0)
				rb=new WebDeltaResponseBuilder().insertHTML(WebDeltaResponseBuilder.ElementInsertionMode.AFTER_BEGIN, "postList", postHTML);
			else
				rb=new WebDeltaResponseBuilder().insertHTML(WebDeltaResponseBuilder.ElementInsertionMode.BEFORE_END, "postReplies"+replyTo, postHTML);
			String formID=req.queryParams("formID");
			return rb.setInputValue("postFormText_"+formID, "").setContent("postFormAttachments_"+formID, "").json();
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
		ArrayList<Integer> postIDs=new ArrayList<>();
		for(NewsfeedEntry e:feed){
			if(e instanceof PostNewsfeedEntry){
				PostNewsfeedEntry pe=(PostNewsfeedEntry) e;
				if(pe.post!=null){
					postIDs.add(pe.post.id);
					if(req.attribute("mobile")==null){
						pe.post.replies=PostStorage.getRepliesForFeed(e.objectID);
						pe.post.getAllReplyIDs(postIDs);
					}
				}else{
					System.err.println("No post: "+pe);
				}
			}
		}
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self.user.id);
		if(!feed.isEmpty() && startFromID==0)
			startFromID=feed.get(0).id;
		Utils.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete");
		return new RenderedTemplateResponse("feed").with("title", Utils.lang(req).get("feed")).with("feed", feed).with("postInteractions", interactions)
				.with("paginationURL", "/feed?startFrom="+startFromID+"&offset=").with("total", total[0]).with("offset", offset)
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments)
				.renderToString(req);
	}

	public static Object standalonePost(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		int[] replyKey=new int[post.replyKey.length+1];
		System.arraycopy(post.replyKey, 0, replyKey, 0, post.replyKey.length);
		replyKey[replyKey.length-1]=post.id;
		post.replies=PostStorage.getReplies(replyKey);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_post_standalone");
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
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:type", "article");
			meta.put("og:title", post.user.getFullName());
			meta.put("og:url", post.url.toString());
			meta.put("og:published_time", Utils.formatDateAsISO(post.published));
			meta.put("og:author", post.user.url.toString());
			if(StringUtils.isNotEmpty(post.content)){
				meta.put("og:description", Utils.truncateOnWordBoundary(post.content, 250));
			}
			boolean hasImage=false;
			if(post.attachment!=null && !post.attachment.isEmpty()){
				for(Attachment att : post.getProcessedAttachments()){
					if(att instanceof PhotoAttachment){
//						PhotoSize size=MediaStorageUtils.findBestPhotoSize(((PhotoAttachment) att).sizes, PhotoSize.Format.JPEG, PhotoSize.Type.MEDIUM);
//						if(size!=null){
//							meta.put("og:image", size.src.toString());
//							meta.put("og:image:width", size.width+"");
//							meta.put("og:image:height", size.height+"");
//							hasImage=true;
//						}
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
		}
		Utils.jsLangKey(req, "yes", "no", "cancel", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete");
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
		return model.renderToString(req);
	}

	public static Object confirmDelete(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm").with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI("/posts/"+postID+"/delete?_redir="+URLEncoder.encode(back))).with("back", back).renderToString(req);
	}

	public static Object delete(Request req, Response resp, Account self) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		if(!post.canBeManagedBy(self.user)){
			resp.status(403);
			return Utils.wrapError(req, resp, "err_access");
		}
		PostStorage.deletePost(post.id);
		NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
		if(Config.isLocal(post.activityPubID) && post.attachment!=null && !post.attachment.isEmpty()){
			MediaStorageUtils.deleteAttachmentFiles(post.attachment);
		}
		ActivityPubWorker.getInstance().sendDeletePostActivity(post);
		if(isAjax(req)){
			resp.type("application/json");
			return new WebDeltaResponseBuilder().remove("post"+postID).json();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object like(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
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
			return new WebDeltaResponseBuilder(resp)
					.setContent("likeCounterPost"+postID, interactions.likeCount+"")
					.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/unlike?csrf="+sessionInfo(req).csrfToken)
					.json();
		}
		resp.redirect(back);
		return "";
	}

	public static Object unlike(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
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
			WebDeltaResponseBuilder b=new WebDeltaResponseBuilder(resp)
					.setContent("likeCounterPost"+postID, interactions.likeCount+"")
					.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/like?csrf="+sessionInfo(req).csrfToken);
			if(interactions.likeCount==0)
				b.hide("likeCounterPost"+postID);
			return b.json();
		}
		resp.redirect(back);
		return "";
	}

	public static Object likePopover(Request req, Response resp) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		SessionInfo info=sessionInfo(req);
		int selfID=info!=null && info.account!=null ? info.account.user.id : 0;
		List<Integer> ids=LikeStorage.getPostLikes(postID, selfID, 0, 6);
		ArrayList<User> users=new ArrayList<>();
		for(int id:ids)
			users.add(UserStorage.getById(id));
		String content=new RenderedTemplateResponse("like_popover").with("users", users).renderToString(req);
		UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(postID), selfID).get(postID);
		JSONObject o=new JSONObject();
		o.put("content", content);
		o.put("title", lang(req).plural("liked_by_X_people", interactions.likeCount));
		if(selfID!=0){
			o.put("altTitle", lang(req).plural("liked_by_X_people", interactions.likeCount+(interactions.isLiked ? -1 : 1)));
		}
		WebDeltaResponseBuilder b=new WebDeltaResponseBuilder(resp)
				.setContent("likeCounterPost"+postID, interactions.likeCount+"");
		if(info!=null && info.account!=null){
			b.setAttribute("likeButtonPost"+postID, "href", post.getInternalURL()+"/"+(interactions.isLiked ? "un" : "")+"like?csrf="+sessionInfo(req).csrfToken);
		}
		if(interactions.likeCount==0)
			b.hide("likeCounterPost"+postID);
		else
			b.show("likeCounterPost"+postID);
		o.put("actions", b.json());
		o.put("show", interactions.likeCount>0);
		o.put("fullURL", "/posts/"+postID+"/likes");
		return o;
	}

	public static Object likeList(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID, false);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_post_not_found");
		}
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		List<Integer> ids=LikeStorage.getPostLikes(postID, 0, offset, 100);
		ArrayList<User> users=new ArrayList<>();
		for(int id:ids)
			users.add(UserStorage.getById(id));
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap").with("users", users);
		UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(postID), 0).get(postID);
		model.with("pageOffset", offset).with("total", interactions.likeCount).with("paginationUrlPrefix", "/posts/"+postID+"/likes?fromPagination&offset=");
		if(isAjax(req)){
			if(req.queryParams("fromPagination")==null)
				return new WebDeltaResponseBuilder(resp).box(lang(req).get("likes_title"), model.renderToString(req), "likesList", 610);
			else
				return new WebDeltaResponseBuilder(resp).setContent("likesList", model.renderToString(req));
		}
		model.with("contentTemplate", "user_grid").with("title", lang(req).get("likes_title"));
		return model.renderToString(req);
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
				resp.status(404);
				return Utils.wrapError(req, resp, "err_user_not_found");
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
		List<Integer> postIDs=wall.stream().map((Post p)->p.id).collect(Collectors.toList());
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		RenderedTemplateResponse model=new RenderedTemplateResponse("wall_page")
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

		return model.renderToString(req);
	}

	public static Object wallToWall(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
		String otherUsername=req.params(":other_username");
		User otherUser=UserStorage.getByUsername(otherUsername);
		if(otherUser==null){
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		int[] postCount={0};
		int offset=Utils.parseIntOrDefault(req.queryParams("offset"), 0);
		List<Post> wall=PostStorage.getWallToWall(user.id, otherUser.id, offset, postCount);
		List<Integer> postIDs=wall.stream().map((Post p)->p.id).collect(Collectors.toList());
		HashMap<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.user.id : 0);
		return new RenderedTemplateResponse("wall_page")
				.with("posts", wall)
				.with("postInteractions", interactions)
				.with("owner", user)
				.with("otherUser", otherUser)
				.with("postCount", postCount[0])
				.with("pageOffset", offset)
				.with("paginationUrlPrefix", Config.localURI("/"+username+"/wall/with"+otherUsername))
				.with("tab", "wall2wall")
				.with("title", lang(req).inflected("wall_of_X", user.gender, user.firstName, user.lastName, null))
				.renderToString(req);
	}
}
