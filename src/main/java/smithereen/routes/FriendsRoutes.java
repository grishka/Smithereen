package smithereen.routes;

import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
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
import smithereen.model.friends.FriendList;
import smithereen.model.friends.PublicFriendList;
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
		String section=req.queryParams("section");
		FriendsController.SortOrder order;
		int listID=0;
		if(!onlineOnly && "recent".equals(section) && self!=null && user.id==self.user.id){
			order=FriendsController.SortOrder.RECENTLY_ADDED;
		}else{
			if("list".equals(section) && !onlineOnly){
				listID=safeParseInt(req.queryParams("list"));
				if((self==null || self.user.id!=user.id) && listID<FriendList.FIRST_PUBLIC_LIST_ID){
					listID=0;
					section=null;
				}
			}else{
				section=null; // In case an unknown value is passed, so that "all friends" still gets highlighted
			}
			order=self!=null && user.id==self.user.id ? FriendsController.SortOrder.HINTS : FriendsController.SortOrder.ID_ASCENDING;
		}
		if(StringUtils.isEmpty(query)){
			friends=ctx.getFriendsController().getFriends(user, offset(req), 100, order, onlineOnly, listID);
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
				.with("query", query)
				.with("section", section);

		Lang l=lang(req);

		HashMap<Integer, FriendList> allLists=new HashMap<>();
		if(self!=null && self.user.id==user.id){
			List<FriendList> lists=ctx.getFriendsController().getFriendLists(user);
			model.with("lists", lists);
			for(FriendList fl:lists)
				allLists.put(fl.id(), fl);

			jsLangKey(req, "select_friends_title", "friends_search_placeholder", "friend_list_your_friends", "friends_in_list", "select_friends_empty_selection", "friends_list_name", "friends_public_list", "friends_public_list_explanation");
		}
		List<FriendList> publicLists=Arrays.stream(PublicFriendList.values())
				.map(lt->new FriendList(FriendList.FIRST_PUBLIC_LIST_ID+lt.ordinal(), l.get(lt.getLangKey())))
				.toList();
		model.with("publicLists", publicLists);
		for(FriendList fl:publicLists)
			allLists.put(fl.id(), fl);
		model.with("allLists", allLists)
				.with("listID", listID);

		if(!isMobile(req)){
			Map<Integer, BitSet> userLists=ctx.getFriendsController().getFriendListsForUsers(self==null ? null : self.user, user, friends.list.stream().map(u->u.id).collect(Collectors.toSet()));
			HashMap<Integer, int[]> actualUserLists=new HashMap<>();
			userLists.forEach((id, lists)->actualUserLists.put(id, lists.stream().map(i->i+1).toArray()));
			model.with("userLists", actualUserLists);
		}

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
		}else if(self==null || user.id!=self.user.id){
			model.headerBack(user);
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

		model.with("onlines", ctx.getUsersController().getUserPresencesOnlineOnly(friends.list.stream().map(u->u.id).toList()));

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
		model.headerBack(user);
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

		model.with("onlines", ctx.getUsersController().getUserPresencesOnlineOnly(friends.list.stream().map(u->u.id).toList()));

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
		if(self==null || user.id!=self.user.id){
			model.headerBack(user);
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

		model.with("onlines", ctx.getUsersController().getUserPresencesOnlineOnly(followers.list.stream().map(u->u.id).toList()));

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
		if(self==null || user.id!=self.user.id){
			model.headerBack(user);
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

		model.with("onlines", ctx.getUsersController().getUserPresencesOnlineOnly(follows.list.stream().map(u->u.id).toList()));

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
		model.with("onlines", ctx.getUsersController().getUserPresencesOnlineOnly(requests.list.stream().map(r->r.from.id).toList()));
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(requests.list.stream().map(r->r.from).toList());
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
			addFriendLists(self.user, lang(req), ctx, model);
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
			Lang l=lang(req);
			String content;
			if(accept){
				content=l.get("friend_req_accepted")+" <span class=\"popupMenuW\" id=\"friendListsButton"+user.id+"\" data-lists=\"\">" +
						"<a href=\"javascript:void(0)\" onclick=\"showFriendListsMenu('"+user.id+"')\" class=\"opener\">"+l.get("friend_req_accepted_specify_lists")+
						"</a></span>";
			}else{
				content=l.get("friend_req_declined");
			}
			return new WebDeltaResponse(resp).setContent("friendReqBtns"+user.id, "<div class=\"grayText marginBefore\">"+content+"</div>");
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

	public static Object setUserFriendLists(Request req, Response resp, Account self, ApplicationContext ctx){
		User friend=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		BitSet lists=new BitSet(64);
		String listsStr=req.queryParams("lists");
		if(StringUtils.isNotEmpty(listsStr)){
			Arrays.stream(listsStr.split(","))
					.map(Utils::safeParseInt)
					.filter(i->i>0 && i<=64)
					.forEach(id->lists.set(id-1));
		}else{
			for(int i=1;i<=64;i++){
				if("on".equals(req.queryParams("list"+i)))
					lists.set(i-1);
			}
		}
		ctx.getFriendsController().setUserFriendLists(self.user, friend, lists);
		resp.type("application/json");
		return "{}";
	}

	public static Object createFriendList(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "name");
		Set<Integer> ids=ctx.getFriendsController().getFriendLists(self.user).stream().map(FriendList::id).collect(Collectors.toSet());
		boolean foundID=false;
		for(int i=1;i<FriendList.FIRST_PUBLIC_LIST_ID;i++){
			if(!ids.contains(i)){
				foundID=true;
				break;
			}
		}
		if(!foundID)
			throw new UserErrorException("friend_lists_limit_reached");

		String membersStr=req.queryParams("members");
		Set<Integer> members;
		if(StringUtils.isNotEmpty(membersStr)){
			members=Arrays.stream(membersStr.split(","))
					.map(Utils::safeParseInt)
					.filter(id->id>0)
					.collect(Collectors.toSet());
		}else{
			members=Set.of();
		}

		int id=ctx.getFriendsController().createFriendList(self.user, req.queryParams("name"), members);
		return ajaxAwareRedirect(req, resp, "/my/friends?section=list&list="+id);
	}

	public static Object confirmDeleteFriendList(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "id");
		int id=safeParseInt(req.queryParams("id"));
		if(id<=0)
			throw new ObjectNotFoundException();
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("friends_delete_list_title"), l.get("friends_delete_list_confirm"), "/my/friends/deleteList?id="+id);
	}

	public static Object deleteFriendList(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "id");
		int id=safeParseInt(req.queryParams("id"));
		ctx.getFriendsController().deleteFriendList(self.user, id);
		return ajaxAwareRedirect(req, resp, "/my/friends");
	}

	public static Object ajaxFriendListMemberIDs(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "id");
		int id=safeParseInt(req.queryParams("id"));
		resp.header("content-type", "application/json");
		return gson.toJson(ctx.getFriendsController().getFriendListMemberIDs(self.user, id));
	}

	public static Object updateFriendList(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "id");
		int id=safeParseInt(req.queryParams("id"));
		if(id<=0)
			throw new ObjectNotFoundException();

		String name;
		if(id<FriendList.FIRST_PUBLIC_LIST_ID){
			requireQueryParams(req, "name");
			name=req.queryParams("name");
		}else{
			name=null;
		}

		String membersStr=req.queryParams("members");
		Set<Integer> members;
		if(StringUtils.isNotEmpty(membersStr)){
			members=Arrays.stream(membersStr.split(","))
					.map(Utils::safeParseInt)
					.filter(mid->mid>0)
					.collect(Collectors.toSet());
		}else{
			members=Set.of();
		}

		ctx.getFriendsController().updateFriendList(self.user, id, name, members);

		return ajaxAwareRedirect(req, resp, "/my/friends?section=list&list="+id);
	}

	public static Object setUserListsMobileBox(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		User user=ctx.getUsersController().getUserOrThrow(id);
		RenderedTemplateResponse model=new RenderedTemplateResponse("friend_lists_selector", req);
		addFriendLists(self.user, lang(req), ctx, model);
		Set<Integer> lists=ctx.getFriendsController().getFriendListsForUsers(self.user, self.user, List.of(user.id))
				.getOrDefault(user.id, new BitSet())
				.stream()
				.map(i->i+1)
				.boxed()
				.collect(Collectors.toSet());
		model.with("userLists", lists);
		return wrapForm(req, resp, "friend_lists_selector", "/users/"+id+"/setFriendLists", lang(req).get("friend_set_lists"), "save", model);
	}
}
