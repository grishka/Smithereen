package smithereen.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.methods.FriendsMethods;
import smithereen.api.methods.GroupsMethods;
import smithereen.api.methods.LikesMethods;
import smithereen.api.methods.PhotosMethods;
import smithereen.api.methods.ServerMethods;
import smithereen.api.methods.UsersMethods;
import smithereen.api.methods.UtilsMethods;
import smithereen.api.methods.WallMethods;
import smithereen.api.model.ApiErrorType;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserContentUnavailableException;
import smithereen.lang.Lang;
import smithereen.model.apps.ClientAppPermission;

public class ApiDispatcher{
	private static final Map<String, MethodRecord> unprefixedMethods=new HashMap<>();
	private static final Map<String, Map<String, MethodRecord>> methods=new HashMap<>();

	static{
		registerMethod("users.get", UsersMethods::get, false);
		registerMethod("users.getFollowers", UsersMethods::getFollowers, false);
		registerMethod("users.getSubscriptions", UsersMethods::getSubscriptions, false);
		registerMethod("users.search", UsersMethods::search, true);

		registerMethod("groups.get", GroupsMethods::get, false);
		registerMethod("groups.getById", GroupsMethods::getById, false);
		registerMethod("groups.getInvites", GroupsMethods::getInvites, ClientAppPermission.GROUPS_READ);
		registerMethod("groups.join", GroupsMethods::join, ClientAppPermission.GROUPS_WRITE);
		registerMethod("groups.leave", GroupsMethods::leave, ClientAppPermission.GROUPS_WRITE);
		registerMethod("groups.search", GroupsMethods::search, true);
		registerMethod("groups.isMember", GroupsMethods::isMember, false);
		registerMethod("groups.getMembers", GroupsMethods::getMembers, false);

		registerMethod("friends.get", FriendsMethods::get, false);
		registerMethod("friends.getOnline", FriendsMethods::getOnline, false);
		registerMethod("friends.getMutual", FriendsMethods::getMutual, ClientAppPermission.FRIENDS_READ);
		registerMethod("friends.areFriends", FriendsMethods::areFriends, ClientAppPermission.FRIENDS_READ);
		registerMethod("friends.getLists", FriendsMethods::getLists, false);
		registerMethod("friends.add", FriendsMethods::add, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.delete", FriendsMethods::delete, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.addList", FriendsMethods::addList, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.deleteList", FriendsMethods::deleteList, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.edit", FriendsMethods::edit, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.editList", FriendsMethods::editList, ClientAppPermission.FRIENDS_WRITE);
		registerMethod("friends.getRequests", FriendsMethods::getRequests, ClientAppPermission.FRIENDS_READ);

		registerMethod("utils.getServerTime", UtilsMethods::getServerTime, false);

		registerMethod("server.getInfo", ServerMethods::getInfo, false);
		registerMethod("server.getRestrictedServers", ServerMethods::getRestrictedServers, false);

		registerMethod("wall.get", WallMethods::get, false);
		registerMethod("wall.getById", WallMethods::getById, false);
		registerMethod("wall.getComments", WallMethods::getComments, false);
		registerMethod("wall.getReposts", WallMethods::getReposts, false);
		registerMethod("wall.pin", WallMethods::pin, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.unpin", WallMethods::unpin, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.delete", WallMethods::delete, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.post", WallMethods::post, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.repost", WallMethods::repost, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.createComment", WallMethods::createComment, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.getEditSource", WallMethods::getEditSource, ClientAppPermission.WALL_WRITE);
		registerMethod("wall.edit", WallMethods::edit, ClientAppPermission.WALL_WRITE);

		registerMethod("photos.getAttachmentUploadServer", PhotosMethods::getAttachmentUploadServer, true);

		registerMethod("likes.add", LikesMethods::add, ClientAppPermission.LIKES_WRITE);
		registerMethod("likes.delete", LikesMethods::delete, ClientAppPermission.LIKES_WRITE);
		registerMethod("likes.isLiked", LikesMethods::isLiked, false);
		registerMethod("likes.getList", LikesMethods::getList, false);
	}

	private static void registerMethod(String name, ApiMethod impl, boolean requireUser){
		registerMethod(name, new MethodRecord(impl, requireUser));
	}

	private static void registerMethod(String name, ApiMethod impl, ClientAppPermission... permissions){
		registerMethod(name, new MethodRecord(impl, permissions));
	}

	private static void registerMethod(String name, MethodRecord mr){
		int dotIndex=name.indexOf('.');
		if(dotIndex==-1){
			unprefixedMethods.put(name, mr);
		}else{
			String prefix=name.substring(0, dotIndex);
			name=name.substring(dotIndex+1);
			methods.computeIfAbsent(prefix, k->new HashMap<>()).put(name, mr);
		}
	}

	public static Object doApiCall(String method, ApplicationContext ctx, ApiCallContext actx){
		int dotIndex=method.indexOf('.');
		MethodRecord methodRecord=null;
		if(dotIndex>0){
			String prefix=method.substring(0, dotIndex);
			Map<String, MethodRecord> prefixMethods=methods.get(prefix);
			if(prefixMethods!=null){
				String name=method.substring(dotIndex+1);
				methodRecord=prefixMethods.get(name);
			}
		}else{
			methodRecord=unprefixedMethods.get(method);
		}
		if(methodRecord!=null){
			if(methodRecord.requireUser && actx.self==null){
				throw actx.error(ApiErrorType.USER_AUTH_FAILED, "no access token passed");
			}
			if(!methodRecord.requirePermissions.isEmpty() && !actx.token.permissions().containsAll(methodRecord.requirePermissions) && !actx.token.permissions().contains(ClientAppPermission.PASSWORD_GRANT_USED)){
				throw actx.error(ApiErrorType.NO_PERMISSION, "scope "+methodRecord.requirePermissions.stream().map(ClientAppPermission::getScopeValue).collect(Collectors.joining(", "))+" is required");
			}
			try{
				return methodRecord.impl.call(ctx, actx);
			}catch(UserActionNotAllowedException x){
				String msg=x.getMessage();
				if(msg==null){
					if(x instanceof UserContentUnavailableException)
						msg="err_access_user_content";
				}
				throw actx.error(ApiErrorType.ACCESS_DENIED, msg==null ? null : Lang.get(Locale.US).get(msg));
			}catch(ObjectNotFoundException x){
				String msg=x.getMessage();
				throw actx.error(ApiErrorType.NOT_FOUND, msg==null ? null : Lang.get(Locale.US).get(msg));
			}
		}
		throw actx.error(ApiErrorType.UNKNOWN_METHOD);
	}

	private static class MethodRecord{
		public final boolean requireUser;
		public final ApiMethod impl;
		public final EnumSet<ClientAppPermission> requirePermissions;

		private MethodRecord(ApiMethod impl, boolean requireUser){
			this.requireUser=requireUser;
			this.impl=impl;
			this.requirePermissions=EnumSet.noneOf(ClientAppPermission.class);
		}

		private MethodRecord(ApiMethod impl, ClientAppPermission... permissions){
			this.requireUser=true;
			this.impl=impl;
			this.requirePermissions=EnumSet.of(permissions[0], permissions);
		}
	}
}
