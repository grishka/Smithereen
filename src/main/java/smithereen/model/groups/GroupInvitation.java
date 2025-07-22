package smithereen.model.groups;

import smithereen.model.Group;
import smithereen.model.User;

public class GroupInvitation{
	public Group group;
	public User inviter;

	public GroupInvitation(Group group, User inviter){
		this.group=group;
		this.inviter=inviter;
	}
}
