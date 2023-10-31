package smithereen.model;

public class GroupInvitation{
	public Group group;
	public User inviter;

	public GroupInvitation(Group group, User inviter){
		this.group=group;
		this.inviter=inviter;
	}
}
