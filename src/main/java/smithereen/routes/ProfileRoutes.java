package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jtwig.JtwigModel;

import java.sql.SQLException;
import java.util.List;

import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.lang.Lang;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class ProfileRoutes{
	public static Object profile(Request req, Response resp) throws SQLException{
		@Nullable Account self=req.session().attribute("account");
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			List<Post> wall=PostStorage.getUserWall(user.id, 0, 0, null);
			JtwigModel model=JtwigModel.newModel().with("title", user.getFullName()).with("user", user).with("wall", wall).with("own", self!=null && self.user.id==user.id);
			int[] friendCount={0};
			List<User> friends=UserStorage.getRandomFriendsForProfile(user.id, friendCount);
			model.with("friendCount", friendCount[0]).with("friends", friends);
			if(self!=null){
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.FRIENDS)
					model.with("isFriend", true);
				else if(status==FriendshipStatus.REQUEST_SENT)
					model.with("friendRequestSent", true);
				else if(status==FriendshipStatus.REQUEST_RECVD)
					model.with("friendRequestRecvd", true);
			}
			return Utils.renderTemplate(req, "profile", model);
		}else{
			resp.status(404);
			return Utils.wrapError(req, "err_user_not_found");
		}
	}

	public static Object confirmSendFriendRequest(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			String username=req.params(":username");
			User user=UserStorage.getByUsername(username);
			if(user!=null){
				if(user.id==self.user.id){
					return Utils.wrapError(req, "err_cant_friend_self");
				}
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.NONE){
					JtwigModel model=JtwigModel.newModel();
					model.with("targetUser", user);
					return Utils.renderTemplate(req, "send_friend_request", model);
				}else if(status==FriendshipStatus.FRIENDS){
					return Utils.wrapError(req, "err_already_friends");
				}else if(status==FriendshipStatus.REQUEST_RECVD){
					return Utils.wrapError(req, "err_have_incoming_friend_req");
				}else{ // REQ_SENT
					return Utils.wrapError(req, "err_friend_req_already_sent");
				}
			}else{
				resp.status(404);
				return Utils.wrapError(req, "user_not_found");
			}
		}
		return "";
	}

	public static Object doSendFriendRequest(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
			String username=req.params(":username");
			User user=UserStorage.getByUsername(username);
			if(user!=null){
				if(user.id==self.user.id){
					return Utils.wrapError(req, "err_cant_friend_self");
				}
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.NONE){
					UserStorage.putFriendRequest(self.user.id, user.id, req.queryParams("message"));
					resp.redirect("/"+user.username);
					return "";
				}else if(status==FriendshipStatus.FRIENDS){
					return Utils.wrapError(req, "err_already_friends");
				}else if(status==FriendshipStatus.REQUEST_RECVD){
					return Utils.wrapError(req, "err_have_incoming_friend_req");
				}else{ // REQ_SENT
					return Utils.wrapError(req, "err_friend_req_already_sent");
				}
			}else{
				resp.status(404);
				return Utils.wrapError(req, "user_not_found");
			}
		}
		return "";
	}

	public static Object confirmRemoveFriend(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			String username=req.params(":username");
			User user=UserStorage.getByUsername(username);
			if(user!=null){
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.FRIENDS){
					Lang l=Utils.lang(req);
					JtwigModel model=JtwigModel.newModel().with("message", l.get("confirm_unfriend_X", user.getFullName())).with("formAction", user.getProfileURL("doRemoveFriend")).with("back", user.url.toString());
					return Utils.renderTemplate(req, "generic_confirm", model);
				}else{
					return Utils.wrapError(req, "err_not_friends");
				}
			}else{
				resp.status(404);
				return Utils.wrapError(req, "user_not_found");
			}
		}
		return "";
	}

	public static Object friends(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", UserStorage.getFriendListForUser(user.id)).with("owner", user);
			return Utils.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.wrapError(req, "user_not_found");
	}

	public static Object incomingFriendRequests(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			if(!self.user.username.equalsIgnoreCase(username)){
				resp.redirect("/"+username);
				return "";
			}
			List<FriendRequest> requests=UserStorage.getIncomingFriendRequestsForUser(self.user.id, 0, 100);
			JtwigModel model=JtwigModel.newModel();
			model.with("friendRequests", requests);
			return Utils.renderTemplate(req, "friend_requests", model);
		}
		return "";
	}

	public static Object respondToFriendRequest(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
			User user=UserStorage.getByUsername(username);
			if(user!=null){
				if(req.queryParams("accept")!=null){
					UserStorage.acceptFriendRequest(self.user.id, user.id);
				}else if(req.queryParams("decline")!=null){
					UserStorage.deleteFriendRequest(self.user.id, user.id);
				}
				resp.redirect(self.user.getProfileURL("incomingFriendRequests"));
			}else{
				resp.status(404);
				return Utils.wrapError(req, "user_not_found");
			}
		}
		return "";
	}

	public static Object doRemoveFriend(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
			User user=UserStorage.getByUsername(username);
			if(user!=null){
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
				if(status==FriendshipStatus.FRIENDS){
					UserStorage.unfriendUser(self.user.id, user.id);
					resp.redirect(self.user.getProfileURL("friends"));
					if(user instanceof ForeignUser){
						ActivityPubWorker.getInstance().sendUnfriendActivity(self.user, user);
					}
				}else{
					return Utils.wrapError(req, "err_not_friends");
				}
			}else{
				resp.status(404);
				return Utils.wrapError(req, "user_not_found");
			}
		}
		return "";
	}
}
