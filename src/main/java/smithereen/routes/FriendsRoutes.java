package smithereen.routes;

import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.controllers.FriendsController;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.FriendRequest;
import smithereen.model.FriendshipStatus;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class FriendsRoutes{
	public static Object confirmSendFriendRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user.id==self.user.id){
			return wrapError(req, resp, "err_cant_friend_self");
		}
		ctx.getUsersController().ensureUserNotBlocked(self.user, user);
		ctx.getPrivacyController().enforceUserProfileAccess(self.user, user);
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
		ctx.getPrivacyController().enforceUserProfileAccess(self.user, user);
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

	private static Object friends(Request req, Response resp, User user, Account self, ApplicationContext ctx, boolean onlineOnly){
		ctx.getPrivacyController().enforceUserProfileAccess(self!=null ? self.user : null, user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("owner", user);
		model.pageTitle(lang(req).get("friends"));
		PaginatedList<User> friends;
		String query=req.queryParams("q");
		FriendsController.SortOrder order=self!=null && user.id==self.user.id ? FriendsController.SortOrder.HINTS : FriendsController.SortOrder.ID_ASCENDING;
		if(StringUtils.isEmpty(query)){
			if(onlineOnly)
				friends=ctx.getFriendsController().getOnlineFriends(user, offset(req), 100, order);
			else
				friends=ctx.getFriendsController().getFriends(user, offset(req), 100, order);
		}else{
			friends=ctx.getSearchController().searchFriends(query, user, offset(req), 100, order);
		}
		model.paginate(friends);
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriends(self.user, user, 0, 0, FriendsController.SortOrder.HINTS).total;
			model.with("mutualCount", mutualCount);
		}
		model.with("tab", onlineOnly ? "online" : "friends");
		model.with("urlPath", req.raw().getPathInfo())
				.with("query", query);
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
		if(user instanceof ForeignUser)
			model.with("noindex", true);
		jsLangKey(req, "remove_friend", "yes", "no", "send", "mail_tab_compose");
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(friends.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		if(isAjax(req)){
			String baseURL=getRequestPathAndQuery(req);
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("friendsInner"));
				if(friends.offset+friends.perPage>=friends.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", new UriBuilder(baseURL).replaceQueryParam("offset", friends.offset+friends.perPage+"").build().toString());
				}
				return r;
			}
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"))
					.setAttribute("friendsSearch", "data-base-url", baseURL)
					.setURL(baseURL);
		}
		return model;
	}

	public static Object friends(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		return friends(req, resp, user, self, context(req), false);
	}

	public static Object ownFriends(Request req, Response resp, Account self, ApplicationContext ctx){
		return friends(req, resp, self.user, self, ctx, false);
	}

	public static Object friendsOnline(Request req, Response resp){
		User user=context(req).getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		return friends(req, resp, user, self, context(req), true);
	}

	public static Object ownFriendsOnline(Request req, Response resp, Account self, ApplicationContext ctx){
		return friends(req, resp, self.user, self, ctx, true);
	}

	public static Object mutualFriends(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user.id==self.user.id)
			throw new ObjectNotFoundException("err_user_not_found");
		ctx.getPrivacyController().enforceUserProfileAccess(self.user, user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		PaginatedList<User> friends=ctx.getFriendsController().getMutualFriends(user, self.user, offset(req), 100, FriendsController.SortOrder.ID_ASCENDING);
		model.paginate(friends);
		model.with("owner", user);
		model.pageTitle(lang(req).get("friends"));
		model.with("tab", "mutual");
		model.with("mutualCount", friends.total);
		if(user instanceof ForeignUser)
			model.with("noindex", true);
		jsLangKey(req, "remove_friend", "yes", "no");
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(friends.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		if(isAjax(req)){
			String baseURL=getRequestPathAndQuery(req);
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("friendsInner"));
				if(friends.offset+friends.perPage>=friends.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", new UriBuilder(baseURL).replaceQueryParam("offset", friends.offset+friends.perPage+"").build().toString());
				}
				return r;
			}
		}
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
		ctx.getPrivacyController().enforceUserProfileAccess(self!=null ? self.user : null, user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("title", lang(req).get("followers")).with("toolbarTitle", lang(req).get("friends"));
		PaginatedList<User> followers=context(req).getFriendsController().getFollowers(user, offset(req), 100);
		model.paginate(followers);
		model.with("owner", user).with("followers", true).with("tab", "followers");
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriends(self.user, user, 0, 0, FriendsController.SortOrder.ID_ASCENDING).total;
			model.with("mutualCount", mutualCount);
		}
		if(user instanceof ForeignUser)
			model.with("noindex", true);
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(followers.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		if(isAjax(req)){
			String baseURL=getRequestPathAndQuery(req);
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("friendsInner"));
				if(followers.offset+followers.perPage>=followers.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", new UriBuilder(baseURL).replaceQueryParam("offset", followers.offset+followers.perPage+"").build().toString());
				}
				return r;
			}
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
		ctx.getPrivacyController().enforceUserProfileAccess(self!=null ? self.user : null, user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("friends", req);
		model.with("title", lang(req).get("following")).with("toolbarTitle", lang(req).get("friends"));
		PaginatedList<User> follows=context(req).getFriendsController().getFollows(user, offset(req), 100);
		model.paginate(follows);
		model.with("owner", user).with("following", true).with("tab", "following");
		if(self!=null && user.id!=self.user.id){
			int mutualCount=ctx.getFriendsController().getMutualFriendsCount(self.user, user);
			model.with("mutualCount", mutualCount);
		}
		if(user instanceof ForeignUser)
			model.with("noindex", true);
		jsLangKey(req, "unfollow", "yes", "no");
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(follows.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		if(isAjax(req)){
			String baseURL=getRequestPathAndQuery(req);
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("friendsInner"));
				if(follows.offset+follows.perPage>=follows.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", new UriBuilder(baseURL).replaceQueryParam("offset", follows.offset+follows.perPage+"").build().toString());
				}
				return r;
			}
		}
		return model;
	}

	public static Object incomingFriendRequests(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("friend_requests", req);
		PaginatedList<FriendRequest> requests=ctx.getFriendsController().getIncomingFriendRequests(self.user, offset(req), 20);
		model.paginate(requests);
		model.with("title", lang(req).get("friend_requests")).with("toolbarTitle", lang(req).get("friends")).with("owner", self.user);
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(requests.list.stream().map(r->r.from).toList());
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
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
								Objects.toString(ava.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.JPEG)),
								Objects.toString(ava.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.WEBP)),
								Objects.toString(ava.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.JPEG)),
								Objects.toString(ava.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.WEBP))
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
