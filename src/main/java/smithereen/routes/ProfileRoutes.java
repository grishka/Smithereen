package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jtwig.JtwigModel;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.PhotoSize;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.lang.Lang;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class ProfileRoutes{
	public static Object profile(Request req, Response resp) throws SQLException{
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			int[] postCount={0};
			List<Post> wall=PostStorage.getUserWall(user.id, 0, 0, postCount);
			JtwigModel model=JtwigModel.newModel().with("title", user.getFullName()).with("user", user).with("wall", wall).with("own", self!=null && self.user.id==user.id).with("postCount", postCount[0]);

			int[] friendCount={0};
			List<User> friends=UserStorage.getRandomFriendsForProfile(user.id, friendCount);
			model.with("friendCount", friendCount[0]).with("friends", friends);
			if(info!=null && self!=null){
				model.with("draftAttachments", info.postDraftAttachments);
			}
			if(self!=null){
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.FRIENDS)
					model.with("isFriend", true);
				else if(status==FriendshipStatus.REQUEST_SENT)
					model.with("friendRequestSent", true);
				else if(status==FriendshipStatus.REQUEST_RECVD)
					model.with("friendRequestRecvd", true);
			}else{
				HashMap<String, String> meta=new LinkedHashMap<>();
				meta.put("og:type", "profile");
				meta.put("og:site_name", "Smithereen");
				meta.put("og:title", user.getFullName());
				meta.put("og:url", user.url.toString());
				meta.put("og:username", user.getFullUsername());
				if(StringUtils.isNotEmpty(user.firstName))
					meta.put("og:first_name", user.firstName);
				if(StringUtils.isNotEmpty(user.lastName))
					meta.put("og:last_name", user.lastName);
				Lang l=Utils.lang(req);
				String descr=l.plural("X_friends", friendCount[0])+", "+l.plural("X_posts", postCount[0]);
				if(StringUtils.isNotEmpty(user.summary))
					descr+="\n"+user.summary;
				meta.put("og:description", descr);
				if(user.gender==User.Gender.MALE)
					meta.put("og:gender", "male");
				else if(user.gender==User.Gender.FEMALE)
					meta.put("og:gender", "female");
				if(user.hasAvatar()){
					PhotoSize size=MediaStorageUtils.findBestPhotoSize(user.getAvatar(), PhotoSize.Format.JPEG, PhotoSize.Type.XLARGE);
					if(size!=null){
						meta.put("og:image", size.src.toString());
						meta.put("og:image:width", size.width+"");
						meta.put("og:image:height", size.height+"");
					}
				}
				model.with("metaTags", meta);
			}
			Utils.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm");
			return Utils.renderTemplate(req, "profile", model);
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
	}

	public static Object confirmSendFriendRequest(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			if(user.id==self.user.id){
				return Utils.wrapError(req, resp, "err_cant_friend_self");
			}
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
			if(status==FriendshipStatus.NONE){
				JtwigModel model=JtwigModel.newModel();
				model.with("targetUser", user);
				return Utils.renderTemplate(req, "send_friend_request", model);
			}else if(status==FriendshipStatus.FRIENDS){
				return Utils.wrapError(req, resp, "err_already_friends");
			}else if(status==FriendshipStatus.REQUEST_RECVD){
				return Utils.wrapError(req, resp, "err_have_incoming_friend_req");
			}else{ // REQ_SENT
				return Utils.wrapError(req, resp, "err_friend_req_already_sent");
			}
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object doSendFriendRequest(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			if(user.id==self.user.id){
				return Utils.wrapError(req, resp, "err_cant_friend_self");
			}
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
			if(status==FriendshipStatus.NONE){
				UserStorage.putFriendRequest(self.user.id, user.id, req.queryParams("message"), true);
				resp.redirect(Utils.back(req));
				return "";
			}else if(status==FriendshipStatus.FRIENDS){
				return Utils.wrapError(req, resp, "err_already_friends");
			}else if(status==FriendshipStatus.REQUEST_RECVD){
				return Utils.wrapError(req, resp, "err_have_incoming_friend_req");
			}else{ // REQ_SENT
				return Utils.wrapError(req, resp, "err_friend_req_already_sent");
			}
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object confirmRemoveFriend(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
			if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
				Lang l=Utils.lang(req);
				String back=Utils.back(req);
				JtwigModel model=JtwigModel.newModel().with("message", l.get("confirm_unfriend_X", user.getFullName())).with("formAction", user.getProfileURL("doRemoveFriend")+"?_redir="+URLEncoder.encode(back)).with("back", back);
				return Utils.renderTemplate(req, "generic_confirm", model);
			}else{
				return Utils.wrapError(req, resp, "err_not_friends");
			}
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object friends(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", UserStorage.getFriendListForUser(user.id)).with("owner", user).with("tab", 0);
			return Utils.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.wrapError(req, resp, "user_not_found");
	}

	public static Object followers(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", UserStorage.getNonMutualFollowers(user.id, true, true)).with("owner", user).with("followers", true).with("tab", 1);
			return Utils.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.wrapError(req, resp, "user_not_found");
	}

	public static Object following(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", UserStorage.getNonMutualFollowers(user.id, false, true)).with("owner", user).with("following", true).with("tab", 2);
			return Utils.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.wrapError(req, resp, "user_not_found");
	}

	public static Object incomingFriendRequests(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		if(!self.user.username.equalsIgnoreCase(username)){
			resp.redirect(Utils.back(req));
			return "";
		}
		List<FriendRequest> requests=UserStorage.getIncomingFriendRequestsForUser(self.user.id, 0, 100);
		JtwigModel model=JtwigModel.newModel();
		model.with("friendRequests", requests);
		return Utils.renderTemplate(req, "friend_requests", model);
	}

	public static Object respondToFriendRequest(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			if(req.queryParams("accept")!=null){
				if(user instanceof ForeignUser){
					UserStorage.acceptFriendRequest(self.user.id, user.id, false);
					ActivityPubWorker.getInstance().sendFollowActivity(self.user, (ForeignUser) user);
				}else{
					UserStorage.acceptFriendRequest(self.user.id, user.id, true);
				}
			}else if(req.queryParams("decline")!=null){
				UserStorage.deleteFriendRequest(self.user.id, user.id);
				if(user instanceof ForeignUser){
					ActivityPubWorker.getInstance().sendRejectFriendRequestActivity(self.user, (ForeignUser) user);
				}
			}
			resp.redirect(Utils.back(req));
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
		return "";
	}

	public static Object doRemoveFriend(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
			if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
				UserStorage.unfriendUser(self.user.id, user.id);
				resp.redirect(Utils.back(req));
				if(user instanceof ForeignUser){
					ActivityPubWorker.getInstance().sendUnfriendActivity(self.user, user);
				}
			}else{
				return Utils.wrapError(req, resp, "err_not_friends");
			}
		}else{
			resp.status(404);
			return Utils.wrapError(req, resp, "user_not_found");
		}
		return "";
	}
}
