package smithereen.data;

import java.util.HashMap;

/**
 * A "helper" kind of object passed around everywhere to help determine what a user
 * can and cannot do. Comes from a logged in <code>SessionInfo</code>
 */
public class UserPermissions{
	public int userID;
	public HashMap<Integer, Group.AdminLevel> managedGroups=new HashMap<>();
	public Account.AccessLevel serverAccessLevel;

	public UserPermissions(Account account){
		userID=account.user.id;
		serverAccessLevel=account.accessLevel;
	}

	public boolean canDeletePost(Post post){
		// Moderators can delete any local posts
		if(post.local && serverAccessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal())
			return true;
		// Users can always delete their own posts
		if(post.user.id==userID)
			return true;

		// Group moderators can delete any post in their group
		if(post.isGroupOwner())
			return managedGroups.containsKey(((Group) post.owner).id);

		// Users can delete any post on their own wall
		return post.owner instanceof User && ((User)post.owner).id==userID;
	}

	public boolean canEditGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.ADMIN);
	}

	public boolean canManageGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.MODERATOR);
	}
}
