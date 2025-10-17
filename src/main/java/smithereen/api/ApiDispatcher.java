package smithereen.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.methods.FriendsMethods;
import smithereen.api.methods.GroupsMethods;
import smithereen.api.methods.ServerMethods;
import smithereen.api.methods.UsersMethods;
import smithereen.api.methods.UtilsMethods;
import smithereen.api.model.ApiErrorType;
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
			return methodRecord.impl.call(ctx, actx);
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
