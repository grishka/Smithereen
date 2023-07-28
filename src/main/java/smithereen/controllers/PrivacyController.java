package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.PrivacySetting;
import smithereen.data.User;
import smithereen.data.UserPrivacySettingKey;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.UserStorage;

public class PrivacyController{
	private final ApplicationContext context;

	public PrivacyController(ApplicationContext context){
		this.context=context;
	}

	public void enforceObjectPrivacy(@Nullable User self, @NotNull Object object){
		if(object instanceof Post post){
			if(post.ownerID<0){
				enforceUserAccessToGroupContent(self, context.getGroupsController().getGroupOrThrow(-post.ownerID));
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

	public void updateUserPrivacySettings(@NotNull User self, @NotNull Map<UserPrivacySettingKey, PrivacySetting> settings){
		try{
			// Collect user IDs to check their friendship statuses
			Set<Integer> friendIDs=new HashSet<>();
			for(PrivacySetting ps:settings.values()){
				friendIDs.addAll(ps.allowUsers);
				friendIDs.addAll(ps.exceptUsers);
			}
			if(!friendIDs.isEmpty()){
				friendIDs=UserStorage.intersectWithFriendIDs(self.id, friendIDs);
			}
			// Remove any user IDs that are not friends
			for(PrivacySetting ps:settings.values()){
				ps.allowUsers.removeIf(Predicate.not(friendIDs::contains));
				ps.exceptUsers.removeIf(Predicate.not(friendIDs::contains));
			}

			// TODO check if settings actually changed
			UserStorage.setPrivacySettings(self, settings);
			self.privacySettings=settings;
			context.getActivityPubWorker().sendUpdateUserActivity(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
