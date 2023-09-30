package smithereen.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.Actor;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.MessagesPrivacyGrant;
import smithereen.data.OwnedContentObject;
import smithereen.data.OwnerAndAuthor;
import smithereen.data.Post;
import smithereen.data.PrivacySetting;
import smithereen.data.User;
import smithereen.data.UserPrivacySettingKey;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserContentUnavailableException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.GroupStorage;
import smithereen.storage.MailStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

import static smithereen.Utils.escapeHTML;

public class PrivacyController{
	private static final Logger LOG=LoggerFactory.getLogger(PrivacyController.class);
	private final ApplicationContext context;

	public PrivacyController(ApplicationContext context){
		this.context=context;
	}

	public void enforceObjectPrivacy(@Nullable User self, @NotNull OwnedContentObject object){
		if(object instanceof Post post){
			if(post.ownerID<0){
				enforceUserAccessToGroupContent(self, context.getGroupsController().getGroupOrThrow(-post.ownerID));
			}else if(post.ownerID!=post.authorID){
				if(post.getReplyLevel()==0){
					enforceUserPrivacy(self, context.getUsersController().getUserOrThrow(post.ownerID), UserPrivacySettingKey.WALL_OTHERS_POSTS);
				}else{
					Post top=context.getWallController().getPostOrThrow(post.replyKey.get(0));
					if(top.ownerID!=top.authorID){
						enforceUserPrivacy(self, context.getUsersController().getUserOrThrow(top.ownerID), UserPrivacySettingKey.WALL_OTHERS_POSTS);
					}
				}
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

	public boolean checkUserPrivacy(@Nullable User self, @NotNull User owner, @NotNull UserPrivacySettingKey key){
		boolean r=checkUserPrivacy(self, owner, owner.getPrivacySetting(key));
		if(key==UserPrivacySettingKey.PRIVATE_MESSAGES && !r && self!=null){
			try{
				MessagesPrivacyGrant grant=MailStorage.getPrivacyGrant(owner.id, self.id);
				if(grant!=null && grant.isValid()){
					return true;
				}
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
		return r;
	}

	public boolean checkUserPrivacy(@Nullable User self, @NotNull User owner, @NotNull PrivacySetting setting){
		// Logged-out users are only allowed for "everyone"
		if(self==null)
			return setting.baseRule==PrivacySetting.Rule.EVERYONE;
		// You can always do everything with objects you own
		if(self.id==owner.id)
			return true;
		// Denied users are always denied regardless of the base rule
		if(setting.exceptUsers.contains(self.id))
			return false;

		if(isUserBlocked(self, owner) && setting.baseRule!=PrivacySetting.Rule.EVERYONE)
			return false;

		// Allowed users are always allowed
		if(setting.allowUsers.contains(self.id))
			return true;

		return switch(setting.baseRule){
			case EVERYONE -> true;
			case NONE -> false;
			case FRIENDS -> context.getFriendsController().getSimpleFriendshipStatus(self, owner)==FriendshipStatus.FRIENDS;
			case FOLLOWERS -> {
				FriendshipStatus status=context.getFriendsController().getSimpleFriendshipStatus(self, owner);
				yield status==FriendshipStatus.FOLLOWING || status==FriendshipStatus.FRIENDS;
			}
			case FOLLOWING -> {
				FriendshipStatus status=context.getFriendsController().getSimpleFriendshipStatus(self, owner);
				yield status==FriendshipStatus.FOLLOWED_BY || status==FriendshipStatus.FRIENDS;
			}
			case FRIENDS_OF_FRIENDS -> context.getFriendsController().getMutualFriendsCount(self, owner)>0;
		};
	}

	public void enforceUserPrivacy(@Nullable User self, @NotNull User owner, @NotNull UserPrivacySettingKey key){
		if(!checkUserPrivacy(self, owner, key))
			throw key.isForViewing() ? new UserContentUnavailableException() : new UserActionNotAllowedException();
	}

	public void enforceUserPrivacy(@Nullable User self, @NotNull User owner, @NotNull PrivacySetting setting, boolean forViewing){
		if(!checkUserPrivacy(self, owner, setting))
			throw forViewing ? new UserContentUnavailableException() : new UserActionNotAllowedException();
	}

	public boolean checkUserPrivacyForRemoteServer(@Nullable String serverDomain, @NotNull User owner, @NotNull PrivacySetting setting){
		if(setting.baseRule==PrivacySetting.Rule.EVERYONE)
			return true;
		if(serverDomain==null)
			return false;
		try{
			HashSet<Integer> friendsAtDomain=new HashSet<>(UserStorage.getFriendIDsWithDomain(owner.id, serverDomain));
			friendsAtDomain.removeAll(setting.exceptUsers);
			for(int id:setting.allowUsers){
				if(friendsAtDomain.contains(id))
					return true;
			}
			if(setting.baseRule==PrivacySetting.Rule.FRIENDS){
				return !friendsAtDomain.isEmpty();
			}else if(setting.baseRule==PrivacySetting.Rule.FRIENDS_OF_FRIENDS){
				if(!friendsAtDomain.isEmpty())
					return true;
				return UserStorage.getCountOfFriendsOfFriendsWithDomain(owner.id, serverDomain)>0;
			}else if(setting.baseRule==PrivacySetting.Rule.NONE){
				return false;
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		return false;
	}

	public void enforceContentPrivacyForActivityPub(spark.Request req, OwnedContentObject obj){
		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(obj);
		if(oaa.owner() instanceof Group g){
			enforceGroupContentAccess(req, g);
		}else if(oaa.owner() instanceof User u){
			if(obj instanceof Post post && post.ownerID!=post.authorID && post.getReplyLevel()==0){
				if(!checkUserPrivacyForRemoteServer(getDomainFromRequest(req), u, u.privacySettings.getOrDefault(UserPrivacySettingKey.WALL_OTHERS_POSTS, PrivacySetting.DEFAULT)))
					throw new UserActionNotAllowedException();
			}
		}
	}

	private String getDomainFromRequest(spark.Request req){
		try{
			return ActivityPub.verifyHttpSignature(req, null).domain;
		}catch(Exception x){
			return null;
		}
	}

	public void enforceGroupContentAccess(@NotNull spark.Request req, @NotNull Group group){
		if(group.accessType==Group.AccessType.OPEN)
			return;
		Actor signer;
		try{
			signer=ActivityPub.verifyHttpSignature(req, null);
		}catch(Exception x){
			throw new UserActionNotAllowedException("This object is in a "+group.accessType.toString().toLowerCase()+" group. Valid member HTTP signature is required.", x);
		}
		if(!(signer instanceof ForeignUser user))
			throw new UserActionNotAllowedException("HTTP signature is valid but actor has wrong type: "+signer.getType());
		if(group instanceof ForeignGroup foreignGroup){
			String authHeader=req.headers("Authorization");
			if(StringUtils.isEmpty(authHeader))
				throw new UserActionNotAllowedException("Authorization header with ActivityPubActorToken is required");
			String[] parts=authHeader.split(" ", 2);
			if(parts.length!=2)
				throw new BadRequestException();
			if(!"ActivityPubActorToken".equals(parts[0]))
				throw new BadRequestException("Unsupported auth scheme '"+parts[0]+"'");
			JsonObject token;
			try{
				token=JsonParser.parseString(parts[1]).getAsJsonObject();
			}catch(JsonParseException x){
				throw new BadRequestException("Can't parse actor token: "+x.getMessage(), x);
			}
			ActivityPub.verifyActorToken(token, user, foreignGroup);
		}else{
			try{
				if(!GroupStorage.areThereGroupMembersWithDomain(group.id, user.domain))
					throw new UserActionNotAllowedException("HTTP signature is valid, but this object is in a "+group.accessType.toString().toLowerCase()+" group and "+escapeHTML(user.activityPubID.toString())+" is not its member");
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
		LOG.trace("Actor {} was allowed to access object {} in a {} group {}", signer.activityPubID, req.pathInfo(), group.accessType, group.activityPubID);
	}

	public boolean isUserBlocked(User self, Actor target){
		try{
			if(target instanceof User user){
				if(self instanceof ForeignUser && UserStorage.isDomainBlocked(user.id, self.domain))
					return true;
				return UserStorage.isUserBlocked(user.id, self.id);
			}else if(target instanceof Group group){
				if(self instanceof ForeignUser && GroupStorage.isDomainBlocked(group.id, self.domain))
					return true;
				return GroupStorage.isUserBlocked(group.id, self.id);
			}
			return false;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
