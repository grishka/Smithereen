package smithereen.api.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiUser;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.friends.FriendList;
import smithereen.model.friends.FriendRequest;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.friends.PublicFriendList;
import spark.utils.StringUtils;

public class FriendsMethods{
	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		return getFriends(ctx, actx, false);
	}

	public static Object getOnline(ApplicationContext ctx, ApiCallContext actx){
		return getFriends(ctx, actx, true);
	}

	private static Object getFriends(ApplicationContext ctx, ApiCallContext actx, boolean onlineOnly){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");
		boolean isSelf=actx.self!=null && user.id==actx.self.id;
		String order=actx.optParamString("order");
		Set<String> allowedOrder=onlineOnly ? Set.of("hints", "random", "id", "name", "first_name", "last_name") : Set.of("hints", "recent", "random", "id", "name", "first_name", "last_name");
		if(StringUtils.isNotEmpty(order) && !allowedOrder.contains(order))
			throw actx.paramError("order must be one of "+String.join(", ", allowedOrder));
		if("hints".equals(order) || "recent".equals(order)){
			if(!isSelf)
				throw actx.paramError("this ordering is only available for the current user");
			actx.requirePermission(ClientAppPermission.FRIENDS_READ);
		}
		int listID=actx.optParamIntPositive("list_id");
		if(!isSelf && listID!=0 && listID<FriendList.FIRST_PUBLIC_LIST_ID)
			throw actx.paramError("invalid list_id");
		if(isSelf && listID!=0 && listID<FriendList.FIRST_PUBLIC_LIST_ID)
			actx.requirePermission(ClientAppPermission.FRIENDS_READ);

		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		FriendsController.SortOrder actualOrder=switch(order){
			case "hints" -> FriendsController.SortOrder.HINTS;
			case "recent" -> FriendsController.SortOrder.RECENTLY_ADDED;
			case "random" -> FriendsController.SortOrder.RANDOM;
			case "name", "first_name" -> FriendsController.SortOrder.FIRST_NAME;
			case "last_name" -> FriendsController.SortOrder.LAST_NAME;
			case null, default -> FriendsController.SortOrder.ID_ASCENDING;
		};
		PaginatedList<User> friends=ctx.getFriendsController().getFriends(user, offset, count, actualOrder, onlineOnly, listID);
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(friends.total, ApiUtils.getUsers(friends.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(friends.total, friends.list.stream().map(u->u.id).toList());
		}
	}

	public static Object getMutual(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "source_user_id");
		User target;
		try{
			target=ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive("target_user_id"));
		}catch(ObjectNotFoundException x){
			throw actx.error(ApiErrorType.NOT_FOUND, "user with this ID does not exist");
		}
		if(user.id==target.id)
			throw actx.paramError("source_user_id and target_user_id must be different");

		PaginatedList<User> friends=ctx.getFriendsController().getMutualFriends(user, target, actx.getOffset(), actx.getCount(100, 1000), switch(actx.optParamString("order", "id")){
			case "id" -> FriendsController.SortOrder.ID_ASCENDING;
			case "random" -> FriendsController.SortOrder.RANDOM;
			default -> throw actx.paramError("order must be one of id, random");
		});
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(friends.total, ApiUtils.getUsers(friends.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(friends.total, friends.list.stream().map(u->u.id).toList());
		}
	}

	public static Object areFriends(ApplicationContext ctx, ApiCallContext actx){
		List<Integer> ids=actx.requireParamCommaSeparatedIntList("user_ids");
		if(ids.size()>1000)
			ids=ids.subList(0, 1000);
		boolean extended=actx.optParamBoolean("extended");
		Map<Integer, FriendshipStatus> statuses;
		if(extended)
			statuses=ctx.getFriendsController().getFriendshipStatuses(actx.self.user, ids);
		else
			statuses=ctx.getFriendsController().getSimpleFriendshipStatuses(actx.self.user, ids);

		record AreFriendsItem(int userId, String friendStatus, Boolean isRequestUnread){}

		List<AreFriendsItem> resp=new ArrayList<>();
		statuses.forEach((id, status)->{
			resp.add(new AreFriendsItem(id, switch(status){
				case NONE -> "none";
				case FRIENDS -> "friends";
				case FOLLOW_REQUESTED -> "follow_requested";
				case FOLLOWING, REQUEST_SENT -> "following";
				case FOLLOWED_BY, REQUEST_RECVD -> "followed_by";
			}, extended ? status==FriendshipStatus.REQUEST_RECVD : null));
		});
		return resp;
	}

	public static Object getLists(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");
		ArrayList<FriendList> lists=new ArrayList<>();
		if(actx.self!=null && user.id==actx.self.id && actx.hasPermission(ClientAppPermission.FRIENDS_READ)){
			lists.addAll(ctx.getFriendsController().getFriendLists(actx.self.user));
		}
		Arrays.stream(PublicFriendList.values())
				.map(lt->new FriendList(FriendList.FIRST_PUBLIC_LIST_ID+lt.ordinal(), actx.lang.get(lt.getLangKey())))
				.forEach(lists::add);

		record ApiFriendList(int id, String name, boolean isSystem){}

		return lists.stream()
				.map(l->new ApiFriendList(l.id(), l.name(), l.isPublic()))
				.toList();
	}

	public static Object add(ApplicationContext ctx, ApiCallContext actx){
		User target=ApiUtils.getUser(ctx, actx, "user_id");
		if(target.id==actx.self.id)
			throw actx.error(ApiErrorType.CANT_FRIEND_SELF);
		if(ctx.getPrivacyController().isUserBlocked(actx.self.user, target))
			throw actx.error(ApiErrorType.CANT_ADD_FRIEND_BLOCKED);
		if(ctx.getPrivacyController().isUserBlocked(target, actx.self.user))
			throw actx.error(ApiErrorType.CANT_ADD_FRIEND_YOU_BLOCKED);

		FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(actx.self.user, target);

		// The current user already follows this user -- nothing to do, the app probably called friends.add previously but didn't receive the response
		if(status==FriendshipStatus.REQUEST_SENT)
			return "request_sent";
		if(status==FriendshipStatus.FOLLOWING || status==FriendshipStatus.FOLLOW_REQUESTED || status==FriendshipStatus.FRIENDS)
			return "followed";

		if(status==FriendshipStatus.REQUEST_RECVD){
			ctx.getFriendsController().acceptFriendRequest(actx.self.user, target);
			return "request_accepted";
		}

		if(target.supportsFriendRequests()){
			ctx.getFriendsController().sendFriendRequest(actx.self.user, target, actx.optParamString("text"));
			return "request_sent";
		}else{
			ctx.getFriendsController().followUser(actx.self.user, target);
			return "followed";
		}
	}

	public static Object delete(ApplicationContext ctx, ApiCallContext actx){
		User target=ApiUtils.getUser(ctx, actx, "user_id");
		FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(actx.self.user, target);

		// The current user already doesn't follow this user -- nothing to do, the app probably called friends.delete previously but didn't receive the response
		if(status==FriendshipStatus.NONE)
			return "unfollowed";
		if(status==FriendshipStatus.FOLLOWED_BY)
			return "friend_deleted";

		if(status==FriendshipStatus.REQUEST_RECVD){
			ctx.getFriendsController().rejectFriendRequest(actx.self.user, target);
			return "in_request_deleted";
		}

		ctx.getFriendsController().removeFriend(actx.self.user, target);
		if(status==FriendshipStatus.REQUEST_SENT)
			return "out_request_deleted";
		if(status==FriendshipStatus.FRIENDS)
			return "friend_deleted";
		return "unfollowed";
	}

	public static Object addList(ApplicationContext ctx, ApiCallContext actx){
		String name=actx.requireParamString("name");
		List<Integer> userIDs=actx.optParamCommaSeparatedIntList("user_ids");
		Set<Integer> ids=ctx.getFriendsController().getFriendLists(actx.self.user).stream().map(FriendList::id).collect(Collectors.toSet());
		boolean foundID=false;
		for(int i=1;i<FriendList.FIRST_PUBLIC_LIST_ID;i++){
			if(!ids.contains(i)){
				foundID=true;
				break;
			}
		}
		if(!foundID)
			throw actx.error(ApiErrorType.TOO_MANY_FRIEND_LISTS);

		return ctx.getFriendsController().createFriendList(actx.self.user, name, userIDs);
	}

	public static Object deleteList(ApplicationContext ctx, ApiCallContext actx){
		int listID=actx.requireParamIntPositive("list_id");
		if(listID>=FriendList.FIRST_PUBLIC_LIST_ID)
			throw actx.paramError("invalid list_id");
		ctx.getFriendsController().deleteFriendList(actx.self.user, listID);
		return true;
	}

	public static Object edit(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		List<Integer> listIDs=actx.optParamCommaSeparatedIntList("list_ids");
		Set<Integer> validIDs=ctx.getFriendsController().getFriendLists(actx.self.user).stream().map(FriendList::id).collect(Collectors.toSet());

		BitSet lists=new BitSet(64);
		for(int id:listIDs){
			if(validIDs.contains(id) || (id>=FriendList.FIRST_PUBLIC_LIST_ID && id<FriendList.FIRST_PUBLIC_LIST_ID+PublicFriendList.values().length)){
				lists.set(id-1);
			}
		}

		ctx.getFriendsController().setUserFriendLists(actx.self.user, user, lists);
		return true;
	}

	public static Object editList(ApplicationContext ctx, ApiCallContext actx){
		int listID=actx.requireParamIntPositive("list_id");

		FriendList list=null;
		if(listID>=FriendList.FIRST_PUBLIC_LIST_ID){
			int index=listID-FriendList.FIRST_PUBLIC_LIST_ID;
			if(index>=PublicFriendList.values().length)
				throw actx.paramError("invalid list_id");
			list=new FriendList(listID, PublicFriendList.values()[index].name());
		}else{
			List<FriendList> lists=ctx.getFriendsController().getFriendLists(actx.self.user);
			for(FriendList l:lists){
				if(l.id()==listID){
					list=l;
					break;
				}
			}
			if(list==null)
				throw actx.error(ApiErrorType.NOT_FOUND, "friend list with this ID does not exist");
		}

		Set<Integer> friendIDs;
		if(actx.hasParam("user_ids")){
			friendIDs=new HashSet<>(actx.optParamCommaSeparatedIntList("user_ids"));
		}else{
			friendIDs=new HashSet<>(ctx.getFriendsController().getFriendListMemberIDs(actx.self.user, listID));
			friendIDs.addAll(actx.optParamCommaSeparatedIntList("add_user_ids"));
			actx.optParamCommaSeparatedIntList("delete_user_ids").forEach(friendIDs::remove);
		}

		ctx.getFriendsController().updateFriendList(actx.self.user, listID, actx.optParamString("name", list.name()), friendIDs);

		return true;
	}

	public static Object getRequests(ApplicationContext ctx, ApiCallContext actx){
		boolean extended=actx.optParamBoolean("extended");
		boolean needMutual=actx.optParamBoolean("need_mutual");
		PaginatedList<FriendRequest> requests=ctx.getFriendsController().getIncomingFriendRequests(actx.self.user, actx.getOffset(), actx.getCount(20, 100), needMutual ? 10 : 0);
		if(requests.list.isEmpty())
			return new ApiPaginatedList<>(requests);

		if(!extended && !needMutual && !actx.hasParam("fields")){
			return new ApiPaginatedList<>(requests.total, requests.list.stream().map(r->r.from.id).toList());
		}else{
			record MutualFriends(int count, List<Integer> users){}
			record ApiFriendRequest(Integer userId, ApiUser user, String message, MutualFriends mutual){}
			Map<Integer, ApiUser> users;
			if(actx.hasParam("fields")){
				users=ApiUtils.getUsers(requests.list.stream().map(r->r.from).toList(), ctx, actx).stream().collect(Collectors.toMap(u->u.id, Function.identity()));
			}else{
				users=null;
			}

			List<ApiFriendRequest> result=new ArrayList<>();
			for(FriendRequest req:requests.list){
				MutualFriends mutual;
				if(needMutual){
					mutual=new MutualFriends(req.mutualFriendsCount, req.mutualFriends==null ? List.of() : req.mutualFriends.stream().map(u->u.id).toList());
				}else{
					mutual=null;
				}
				result.add(new ApiFriendRequest(users==null ? req.from.id : null, users==null ? null : users.get(req.from.id), extended ? req.message : null, mutual));
			}
			return new ApiPaginatedList<>(requests.total, result);
		}
	}
}
