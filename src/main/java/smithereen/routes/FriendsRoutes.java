package smithereen.routes;

import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.controllers.FriendsController;
import smithereen.data.Account;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.User;
import smithereen.data.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class FriendsRoutes{
	public static Object confirmSendFriendRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user.id==self.user.id){
			return wrapError(req, resp, "err_cant_friend_self");
		}
		ctx.getUsersController().ensureUserNotBlocked(self.user, user);
		FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(self.user, user);
		Lang l=lang(req);
		switch(status){
			case FOLLOWED_BY:
				if(isAjax(req) && verifyCSRF(req, resp)){
					ctx.getFriendsController().followUser(self.user, user);
					return new WebDeltaResponse(resp).refresh();
				}else{
					RenderedTemplateResponse model=new RenderedTemplateResponse("form_page", req);
					model.with("targetUser", user);
					model.with("contentTemplate", "send_friend_request").with("formAction", "/users/"+user.id+"/doSendFriendRequest").with("submitButton", l.get("add_friend"));
					return model;
				}
			case NONE:
				if(user.supportsFriendRequests()){
					RenderedTemplateResponse model=new RenderedTemplateResponse("send_friend_request", req);
					model.with("targetUser", user);
					return wrapForm(req, resp, "send_friend_request", "/users/"+user.id+"/doSendFriendRequest", l.get("send_friend_req_title"), "add_friend", model);
				}else{
					return doSendFriendRequest(req, resp, self, ctx);
				}
			case FRIENDS:
				return wrapError(req, resp, "err_already_friends");
			case REQUEST_RECVD:
				return wrapError(req, resp, "err_have_incoming_friend_req");
			default:  // REQ_SENT
				return wrapError(req, resp, "err_friend_req_already_sent");
		}
	}

	public static Object doSendFriendRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		ctx.getFriendsController().sendFriendRequest(self.user, user, req.queryParams("message"));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object confirmRemoveFriend(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(self.user, user);
		if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING || status==FriendshipStatus.FOLLOW_REQUESTED){
			Lang l=Utils.lang(req);
			String back=Utils.back(req);
			return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unfriend_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/users/"+user.id+"/doRemoveFriend?_redir="+URLEncoder.encode(back)).with("back", back);
		}else{
			return Utils.wrapError(req, resp, "err_not_friends");
		}
	}

	private static Object friends(Request req, Response resp, User user, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("owner", user);
		model.pageTitle(lang(req).get("friends"));
		model.paginate(ctx.getFriendsController().getFriends(user, offset(req), 100, FriendsController.SortOrder.ID_ASCENDING));
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriends(self.user, user, 0, 0, FriendsController.SortOrder.ID_ASCENDING).total;
			model.with("mutualCount", mutualCount);
		}
		model.with("tab", "friends");
		@Nullable
		String act=req.queryParams("act");
		if("groupInvite".equals(act)){
			int groupID=safeParseInt(req.queryParams("group"));
			Group group=ctx.getGroupsController().getGroupOrThrow(groupID);
			model.with("selectionMode", true);
			model.with("customActions", List.of(
					Map.of(
							"href", "/groups/"+groupID+"/invite?csrf="+sessionInfo(req).csrfToken+"&user=",
							"title", lang(req).get("send_invitation"),
							"ajax", true
					)
			));
			model.addNavBarItem(group.name, group.getProfileURL()).addNavBarItem(lang(req).get("invite_friends_title"));
			model.pageTitle(lang(req).get("invite_friends_title"));
		}
		jsLangKey(req, "remove_friend", "yes", "no");
		return model;
	}

	public static Object friends(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		return friends(req, resp, user, self, context(req));
	}

	public static Object ownFriends(Request req, Response resp, Account self, ApplicationContext ctx){
		return friends(req, resp, self.user, self, ctx);
	}

	public static Object mutualFriends(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user.id==self.user.id)
			throw new ObjectNotFoundException("err_user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		PaginatedList<User> friends=ctx.getFriendsController().getMutualFriends(user, self.user, offset(req), 100, FriendsController.SortOrder.ID_ASCENDING);
		model.paginate(friends);
		model.with("owner", user);
		model.pageTitle(lang(req).get("friends"));
		model.with("tab", "mutual");
		model.with("mutualCount", friends.total);
		jsLangKey(req, "remove_friend", "yes", "no");
		return model;
	}

	public static Object followers(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		String _id=req.params(":id");
		User user;
		if(_id==null){
			if(requireAccount(req, resp)){
				user=self.user;
			}else{
				return "";
			}
		}else{
			user=context(req).getUsersController().getUserOrThrow(safeParseInt(_id));
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("title", lang(req).get("followers")).with("toolbarTitle", lang(req).get("friends"));
		model.paginate(context(req).getFriendsController().getFollowers(user, offset(req), 100));
		model.with("owner", user).with("followers", true).with("tab", "followers");
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriends(self.user, user, 0, 0, FriendsController.SortOrder.ID_ASCENDING).total;
			model.with("mutualCount", mutualCount);
		}
		return model;
	}

	public static Object following(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);

		String _id=req.params(":id");
		User user;
		if(_id==null){
			if(requireAccount(req, resp)){
				user=self.user;
			}else{
				return "";
			}
		}else{
			user=context(req).getUsersController().getUserOrThrow(safeParseInt(_id));
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("title", lang(req).get("following")).with("toolbarTitle", lang(req).get("friends"));
		model.paginate(context(req).getFriendsController().getFollows(user, offset(req), 100));
		model.with("owner", user).with("following", true).with("tab", "following");
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriendsCount(self.user, user);
			model.with("mutualCount", mutualCount);
		}
		jsLangKey(req, "unfollow", "yes", "no");
		return model;
	}

	public static Object incomingFriendRequests(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("friend_requests", req);
		model.paginate(ctx.getFriendsController().getIncomingFriendRequests(self.user, offset(req), 20));
		model.with("title", lang(req).get("friend_requests")).with("toolbarTitle", lang(req).get("friends")).with("owner", self.user);
		return model;
	}

	public static Object respondToFriendRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		boolean accept;
		if(req.queryParams("accept")!=null){
			accept=true;
			ctx.getFriendsController().acceptFriendRequest(self.user, user);
		}else if(req.queryParams("decline")!=null){
			accept=false;
			ctx.getFriendsController().rejectFriendRequest(self.user, user);
		}else{
			throw new BadRequestException();
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).setContent("friendReqBtns"+user.id,
					"<div class=\"settingsMessage\">"+lang(req).get(accept ? "friend_req_accepted" : "friend_req_declined")+"</div>");
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object doRemoveFriend(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		ctx.getFriendsController().removeFriend(self.user, user);
		if(isAjax(req)){
			resp.type("application/json");
			if("list".equals(req.queryParams("from")))
				return new WebDeltaResponse().remove("frow"+user.id);
			else
				return new WebDeltaResponse().refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object ajaxFriendsForPrivacyBoxes(Request req, Response resp, Account self, ApplicationContext ctx){
		// response is an array of these things:
		// [
		//  id,
		//  name,
		//  profile URL,
		//  name for "except" (inflected in Russian locale),
		//  [avatar URL: 50x50 jpg, 50x50 webp, 100x100 jpg, 100x100 webp] (null if user has no avatar)
		// ]
		Lang l=lang(req);
		List<?> res=ctx.getFriendsController().getFriends(self.user, 0, 10_000, FriendsController.SortOrder.ID_ASCENDING).list
				.stream()
				.map(u->{
					SizedImage ava=u.getAvatar();
					List<String> avaUrls;
					if(ava==null){
						avaUrls=null;
					}else{
						avaUrls=List.of(
								ava.getUriForSizeAndFormat(SizedImage.Type.SQUARE_SMALL, SizedImage.Format.JPEG).toString(),
								ava.getUriForSizeAndFormat(SizedImage.Type.SQUARE_SMALL, SizedImage.Format.WEBP).toString(),
								ava.getUriForSizeAndFormat(SizedImage.Type.SQUARE_MEDIUM, SizedImage.Format.JPEG).toString(),
								ava.getUriForSizeAndFormat(SizedImage.Type.SQUARE_MEDIUM, SizedImage.Format.WEBP).toString()
						);
					}
					return Arrays.asList(
							u.id,
							u.getFullName(),
							u.getProfileURL(),
							l.get("privacy_settings_value_except_name", Map.of("name", u.getFirstLastAndGender())),
							avaUrls
					);
				})
				.toList();
		resp.type("application/json");
		return gson.toJson(res);
	}
}
