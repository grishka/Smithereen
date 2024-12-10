package smithereen.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;

/**
 * A "helper" kind of object passed around everywhere to help determine what a user
 * can and cannot do. Comes from a logged in <code>SessionInfo</code>
 */
public class UserPermissions{
	public int userID;
	public HashMap<Integer, Group.AdminLevel> managedGroups=new HashMap<>();
	public boolean canInviteNewUsers;
	public UserRole role;

	public UserPermissions(Account account){
		userID=account.user.id;
		if(account.roleID>0){
			role=Config.userRoles.get(account.roleID);
		}
	}

	public boolean canDeletePost(PostLikeObject post){
		// Users can always delete their own posts
		if(post.authorID==userID)
			return true;

		// Group moderators can delete any post in their group
		if(post.ownerID<0)
			return managedGroups.containsKey(-post.ownerID);

		// Users can delete any post on their own wall
		return post.ownerID>0 && post.ownerID==userID;
	}

	public boolean canEditPost(PostLikeObject post){
		return post.authorID==userID && System.currentTimeMillis()-post.createdAt.toEpochMilli()<24*3600_000L && (!(post instanceof Post p) || p.action==null);
	}

	public boolean canEditGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.ADMIN);
	}

	public boolean canManageGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.MODERATOR);
	}

	public boolean canReport(Object obj){
		return switch(obj){
			case User u -> u.id!=userID;
			case Group g -> managedGroups.getOrDefault(g.id, Group.AdminLevel.REGULAR)!=Group.AdminLevel.OWNER;
			case Post p -> p.authorID!=userID;
			case MailMessage msg -> msg.senderID!=userID;
			case Photo ph -> ph.authorID!=userID;
			case Comment c -> c.authorID!=userID;
			case null, default -> false;
		};
	}

	public boolean canEditPhotoAlbum(PhotoAlbum album){
		if(album.ownerID>0)
			return userID==album.ownerID;
		return managedGroups.getOrDefault(-album.ownerID, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.MODERATOR);
	}

	public boolean canUploadToPhotoAlbum(PhotoAlbum album){
		if(album==null)
			return false;
		if(album.ownerID>0)
			return userID==album.ownerID;
		return !album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS) || managedGroups.getOrDefault(-album.ownerID, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.MODERATOR);
	}

	public boolean hasPermission(UserRole.Permission permission){
		return role!=null && (role.permissions().contains(permission) || role.permissions().contains(UserRole.Permission.SUPERUSER));
	}

	public boolean hasAnyPermission(EnumSet<UserRole.Permission> permissions){
		if(role==null || permissions.isEmpty())
			return false;
		if(role.permissions().contains(UserRole.Permission.SUPERUSER))
			return true;
		for(UserRole.Permission perm:permissions){
			if(role.permissions().contains(perm))
				return true;
		}
		return false;
	}

	public boolean hasPermission(String permission){
		return hasPermission(UserRole.Permission.valueOf(permission));
	}

	public boolean hasAnyPermission(List<String> permissions){
		return hasAnyPermission(permissions.stream().map(UserRole.Permission::valueOf).collect(Collectors.toCollection(()->EnumSet.noneOf(UserRole.Permission.class))));
	}
}
