package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.exceptions.UserActionNotAllowedException;

public class PrivacyController{
	private final ApplicationContext context;

	public PrivacyController(ApplicationContext context){
		this.context=context;
	}

	public void enforceObjectPrivacy(@Nullable User self, @NotNull ActivityPubObject object){
		if(object instanceof Post post){
			if(post.owner instanceof Group group){
				enforceUserAccessToGroupContent(self, group);
			}
		}
	}

	public void enforceUserAccessToGroupContent(@Nullable User self, @NotNull Group group){
		if(group.accessType!=Group.AccessType.OPEN){
			if(self==null)
				throw new UserActionNotAllowedException();
			Group.MembershipState state=context.getGroupsController().getUserMembershipState(group, self);
			if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER)
				throw new UserActionNotAllowedException();
		}
	}

	public void enforceUserAccessToGroupProfile(@Nullable User self, @NotNull Group group){
		// For closed groups, the profile is still accessible by everyone.
		// For private groups, the profile is only accessible if you're a member or have a pending invite.
		if(group.accessType==Group.AccessType.PRIVATE){
			if(self==null)
				throw new UserActionNotAllowedException();
			Group.MembershipState state=context.getGroupsController().getUserMembershipState(group, self);
			if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.INVITED)
				throw new UserActionNotAllowedException();
		}
	}

}
