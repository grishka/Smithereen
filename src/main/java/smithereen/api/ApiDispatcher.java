package smithereen.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.methods.AccountMethods;
import smithereen.api.methods.BoardMethods;
import smithereen.api.methods.ExecuteMethods;
import smithereen.api.methods.FriendsMethods;
import smithereen.api.methods.GroupsMethods;
import smithereen.api.methods.LikesMethods;
import smithereen.api.methods.NewsfeedMethods;
import smithereen.api.methods.PhotosMethods;
import smithereen.api.methods.ServerMethods;
import smithereen.api.methods.StatusMethods;
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
		registerMethod("utils.loadRemoteObject", UtilsMethods::loadRemoteObject, true);
		registerMethod("utils.resolveScreenName", UtilsMethods::resolveScreenName, false);

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
		registerMethod("photos.getAlbums", PhotosMethods::getAlbums, false);
		registerMethod("photos.getAlbumsById", PhotosMethods::getAlbumsById, false);
		registerMethod("photos.get", PhotosMethods::get, false);
		registerMethod("photos.getById", PhotosMethods::getById, false);
		registerMethod("photos.getAll", PhotosMethods::getAll, false);
		registerMethod("photos.getUserPhotos", PhotosMethods::getUserPhotos, false);
		registerMethod("photos.createAlbum", PhotosMethods::createAlbum, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.editAlbum", PhotosMethods::editAlbum, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.deleteAlbum", PhotosMethods::deleteAlbum, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getNewTags", PhotosMethods::getNewTags, ClientAppPermission.PHOTOS_READ);
		registerMethod("photos.edit", PhotosMethods::edit, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.makeCover", PhotosMethods::makeCover, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.delete", PhotosMethods::delete, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.copy", PhotosMethods::copy, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getTags", PhotosMethods::getTags, false);
		registerMethod("photos.putTag", PhotosMethods::putTag, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.removeTag", PhotosMethods::removeTag, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.confirmTag", PhotosMethods::confirmTag, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getComments", PhotosMethods::getComments, false);
		registerMethod("photos.createComment", PhotosMethods::createComment, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.deleteComment", PhotosMethods::deleteComment, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getCommentEditSource", PhotosMethods::getCommentEditSource, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.editComment", PhotosMethods::editComment, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getFeedEntry", PhotosMethods::getFeedEntry, ClientAppPermission.PHOTOS_READ);
		registerMethod("photos.getUploadServer", PhotosMethods::getUploadServer, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.save", PhotosMethods::save, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.getOwnerPhotoUploadServer", PhotosMethods::getOwnerPhotoUploadServer, ClientAppPermission.PHOTOS_WRITE);
		registerMethod("photos.saveOwnerPhoto", PhotosMethods::saveOwnerPhoto, ClientAppPermission.PHOTOS_WRITE);

		registerMethod("likes.add", LikesMethods::add, ClientAppPermission.LIKES_WRITE);
		registerMethod("likes.delete", LikesMethods::delete, ClientAppPermission.LIKES_WRITE);
		registerMethod("likes.isLiked", LikesMethods::isLiked, false);
		registerMethod("likes.getList", LikesMethods::getList, false);

		registerMethod("newsfeed.get", NewsfeedMethods::get, ClientAppPermission.NEWSFEED);
		registerMethod("newsfeed.getGroups", NewsfeedMethods::getGroups, ClientAppPermission.NEWSFEED);
		registerMethod("newsfeed.getComments", NewsfeedMethods::getComments, ClientAppPermission.NEWSFEED);

		registerMethod("execute", ExecuteMethods::execute, true);

		registerMethod("status.get", StatusMethods::get, false);
		registerMethod("status.set", StatusMethods::set, true);

		registerMethod("board.getTopics", BoardMethods::getTopics, false);
		registerMethod("board.getTopicsById", BoardMethods::getTopicsById, false);
		registerMethod("board.getComments", BoardMethods::getComments, false);
		registerMethod("board.createComment", BoardMethods::createComment, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.getCommentEditSource", BoardMethods::getCommentEditSource, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.editComment", BoardMethods::editComment, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.deleteComment", BoardMethods::deleteComment, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.createTopic", BoardMethods::createTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.editTopic", BoardMethods::editTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.deleteTopic", BoardMethods::deleteTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.openTopic", BoardMethods::openTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.closeTopic", BoardMethods::closeTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.pinTopic", BoardMethods::pinTopic, ClientAppPermission.GROUPS_WRITE);
		registerMethod("board.unpinTopic", BoardMethods::unpinTopic, ClientAppPermission.GROUPS_WRITE);

		registerMethod("account.getCounters", AccountMethods::getCounters, true);
		registerMethod("account.setOnline", AccountMethods::setOnline, true);
		registerMethod("account.setOffline", AccountMethods::setOffline, true);
		registerMethod("account.getAppPermissions", AccountMethods::getAppPermissions, true);
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
