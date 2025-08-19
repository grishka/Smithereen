package smithereen.controllers;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Join;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.ObjectNotFoundExceptionWithFallback;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.Account;
import smithereen.model.ActorStatus;
import smithereen.model.OwnedContentObject;
import smithereen.model.groups.GroupLink;
import smithereen.model.groups.GroupLinkParseResult;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.notifications.EventReminder;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.Group;
import smithereen.model.groups.GroupAdmin;
import smithereen.model.groups.GroupInvitation;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.notifications.UserNotifications;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.notifications.RealtimeNotification;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.FederationStorage;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.utils.IntPair;
import smithereen.storage.utils.Pair;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.MaintenanceScheduler;
import spark.utils.StringUtils;

public class GroupsController{
	private static final Logger LOG=LoggerFactory.getLogger(GroupsController.class);

	private final ApplicationContext context;
	private final LruCache<Integer, EventReminder> eventRemindersCache=new LruCache<>(500);
	private ArrayList<PendingHintsRankIncrement> pendingHintsRankIncrements=new ArrayList<>();


	private final Object groupMembershipLock=new Object();

	public GroupsController(ApplicationContext context){
		this.context=context;
		MaintenanceScheduler.runPeriodically(this::doPendingHintsUpdates, 10, TimeUnit.MINUTES);
	}

	public Group createGroup(@NotNull User admin, @NotNull String name, @Nullable String description){
		return createGroupInternal(admin, name, description, false, null, null);
	}

	public Group createEvent(@NotNull User admin, @NotNull String name, @Nullable String description, @NotNull Instant startTime, @Nullable Instant endTime){
		return createGroupInternal(admin, name, description, true, startTime, endTime);
	}

	@NotNull
	private Group createGroupInternal(User admin, String name, String description, boolean isEvent, Instant startTime, Instant endTime){
		try{
			if(StringUtils.isEmpty(name))
				throw new BadRequestException("name is empty");
			if(isEvent && startTime==null)
				throw new BadRequestException("start time is required for event");
			int id=GroupStorage.createGroup(name, TextProcessor.preprocessPostHTML(description, null), description, admin.id, isEvent, startTime, endTime);
			Group group=Objects.requireNonNull(GroupStorage.getById(id));
			context.getActivityPubWorker().sendAddToGroupsCollectionActivity(admin, group, false);
			context.getNewsfeedController().putFriendsFeedEntry(admin, group.id, isEvent ? NewsfeedEntry.Type.CREATE_EVENT : NewsfeedEntry.Type.CREATE_GROUP);
			return group;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> getUserGroups(@NotNull User user, @Nullable User self, int offset, int count){
		try{
			return GroupStorage.getUserGroups(user.id, offset, count, self!=null && self.id==user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> getUserManagedGroups(@NotNull User user, int offset, int count){
		try{
			return GroupStorage.getUserManagedGroups(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> getUserEvents(@NotNull User user, EventsType type, int offset, int count){
		try{
			return GroupStorage.getUserEvents(user.id, type, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Group getGroupOrThrow(int id){
		try{
			if(id<=0)
				throw new ObjectNotFoundException("err_group_not_found");
			Group group=GroupStorage.getById(id);
			if(group==null)
				throw new ObjectNotFoundException("err_group_not_found");
			return group;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Group getLocalGroupOrThrow(int id){
		Group group=getGroupOrThrow(id);
		if(group instanceof ForeignGroup)
			throw new ObjectNotFoundException("err_group_not_found");
		return group;
	}

	public int tryGetGroupIdForUsername(@NotNull String username){
		try{
			return GroupStorage.getIdByUsername(username);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Group> getGroupsByIdAsList(Collection<Integer> ids){
		if(ids.isEmpty())
			return List.of();
		try{
			return GroupStorage.getByIdAsList(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, Group> getGroupsByIdAsMap(Collection<Integer> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return GroupStorage.getById(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Integer> getMembers(@NotNull Group group, int offset, int count, boolean tentative){
		try{
			return GroupStorage.getMembers(group.id, offset, count, tentative);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Integer> getAllMembers(@NotNull Group group, int offset, int count){
		try{
			return GroupStorage.getMembers(group.id, offset, count, null);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<GroupAdmin> getAdmins(@NotNull Group group){
		try{
			return GroupStorage.getGroupAdmins(group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public GroupAdmin getAdmin(@NotNull Group group, int userID){
		try{
			return GroupStorage.getGroupAdmin(group.id, userID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getRandomMembersForProfile(@NotNull Group group, boolean tentative){
		try{
			return GroupStorage.getRandomMembersForProfile(group.id, tentative);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	@NotNull
	public Group.AdminLevel getMemberAdminLevel(@NotNull Group group, @NotNull User user){
		try{
			return GroupStorage.getGroupMemberAdminLevel(group.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	@NotNull
	public Group.MembershipState getUserMembershipState(@NotNull Group group, @NotNull User user){
		try{
			return GroupStorage.getUserMembershipState(group.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void enforceUserAdminLevel(@NotNull Group group, @NotNull User user, @NotNull Group.AdminLevel atLeastLevel){
		if(!getMemberAdminLevel(group, user).isAtLeast(atLeastLevel))
			throw new UserActionNotAllowedException();
	}

	public void updateGroupInfo(@NotNull Group group, @NotNull User admin, String name, String aboutSrc, Instant eventStart, Instant eventEnd, String username, Group.AccessType accessType,
								GroupFeatureState wallState, GroupFeatureState photosState, GroupFeatureState boardState){
		try{
			enforceUserAdminLevel(group, admin, Group.AdminLevel.ADMIN);
			String about=StringUtils.isNotEmpty(aboutSrc) ? TextProcessor.preprocessPostHTML(aboutSrc, null) : null;
			if(!group.username.equals(username)){
				if(!Utils.isValidUsername(username))
					throw new BadRequestException("err_group_invalid_username");
				if(Utils.isReservedUsername(username))
					throw new BadRequestException("err_group_reserved_username");
				boolean result=DatabaseUtils.runWithUniqueUsername(username, ()->{
					GroupStorage.updateGroupGeneralInfo(group, name, username, aboutSrc, about, eventStart, eventEnd, accessType);
				});
				if(!result)
					throw new BadRequestException("err_group_username_taken");
			}else{
				GroupStorage.updateGroupGeneralInfo(group, name, username, aboutSrc, about, eventStart, eventEnd, accessType);
			}
			if(photosState==GroupFeatureState.ENABLED_OPEN || photosState==GroupFeatureState.ENABLED_CLOSED)
				photosState=GroupFeatureState.ENABLED_RESTRICTED;
			if(boardState==GroupFeatureState.ENABLED_CLOSED)
				boardState=GroupFeatureState.ENABLED_RESTRICTED;
			if(group.wallState!=wallState || group.photosState!=photosState || group.boardState!=boardState){
				group.wallState=wallState;
				group.photosState=photosState;
				group.boardState=boardState;
				GroupStorage.updateProfileFields(group);
				context.getNewsfeedController().clearGroupsFeedCache();
			}
			context.getActivityPubWorker().sendUpdateGroupActivity(group);
			if(group.isEvent()){
				BackgroundTaskRunner.getInstance().submit(()->{
					try{
						IntStream memberIDs=GroupStorage.getAllMembersAsStream(group.id);
						synchronized(eventRemindersCache){
							memberIDs.boxed().forEach(eventRemindersCache::remove);
						}
					}catch(SQLException x){
						LOG.warn("error getting group members", x);
					}
				});
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void joinGroup(@NotNull Group group, @NotNull User user, boolean tentative){
		joinGroup(group, user, tentative, false);
	}

	public void joinGroup(@NotNull Group group, @NotNull User user, boolean tentative, boolean forceAccepted){
		try{
			if(!forceAccepted)
				Utils.ensureUserNotBlocked(user, group);
			boolean autoAccepted=true;

			synchronized(groupMembershipLock){
				Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(!forceAccepted && !getMemberAdminLevel(group, user).isAtLeast(Group.AdminLevel.ADMIN)){
					if(group.accessType==Group.AccessType.PRIVATE){
						if(state!=Group.MembershipState.INVITED)
							throw new UserActionNotAllowedException();
					}else if(group.accessType==Group.AccessType.CLOSED){
						autoAccepted=state==Group.MembershipState.INVITED;
					}
				}

				if(group.isEvent()){
					if((state==Group.MembershipState.MEMBER && !tentative) || (state==Group.MembershipState.TENTATIVE_MEMBER && tentative) || state==Group.MembershipState.REQUESTED){
						if(forceAccepted)
							return;
						throw new UserErrorException("err_group_already_member");
					}
				}else{
					if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER || state==Group.MembershipState.REQUESTED){
						if(forceAccepted)
							return;
						throw new UserErrorException("err_group_already_member");
					}
				}

				if(tentative && (!group.isEvent() || (group instanceof ForeignGroup fg && !fg.hasCapability(ForeignGroup.Capability.TENTATIVE_MEMBERSHIP))))
					throw new BadRequestException();

				// change certain <-> tentative
				if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
					GroupStorage.updateUserEventDecision(group, user.id, tentative);
					if(!forceAccepted && group instanceof ForeignGroup fg)
						context.getActivityPubWorker().sendJoinGroupActivity(user, fg, tentative);
					return;
				}

				GroupStorage.joinGroup(group, user.id, tentative, forceAccepted || (!(group instanceof ForeignGroup) && autoAccepted));
			}
			context.getNotificationsController().sendRealtimeCountersUpdates(user);

			if(!forceAccepted){
				if(group instanceof ForeignGroup fg){
					// Add{Group} will be sent upon receiving Accept{Follow}
					context.getActivityPubWorker().sendJoinGroupActivity(user, fg, tentative);
				}else{
					if(group.accessType!=Group.AccessType.PRIVATE && autoAccepted){
						context.getActivityPubWorker().sendAddToGroupsCollectionActivity(user, group, tentative);
						context.getNewsfeedController().putFriendsFeedEntry(user, group.id, group.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP);
					}
					if(autoAccepted){
						context.getActivityPubWorker().sendAddUserToGroupActivity(user, group, tentative);
					}
				}
			}
			if(group.isEvent()){
				synchronized(eventRemindersCache){
					eventRemindersCache.remove(user.id);
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void leaveGroup(@NotNull Group group, @NotNull User user){
		try{
			Group.MembershipState state;
			synchronized(groupMembershipLock){
				state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.REQUESTED){
					throw new UserErrorException("err_group_not_member");
				}
				GroupStorage.leaveGroup(group, user.id, state==Group.MembershipState.TENTATIVE_MEMBER, state!=Group.MembershipState.REQUESTED);
			}
			if(group instanceof ForeignGroup fg && !(user instanceof ForeignUser)){
				context.getActivityPubWorker().sendLeaveGroupActivity(user, fg);
			}
			if(!(group instanceof ForeignGroup)){
				context.getActivityPubWorker().sendRemoveUserFromGroupActivity(user, group, state==Group.MembershipState.TENTATIVE_MEMBER);
			}
			if(group.accessType!=Group.AccessType.PRIVATE)
				context.getActivityPubWorker().sendRemoveFromGroupsCollectionActivity(user, group);
			context.getNewsfeedController().deleteFriendsFeedEntry(user, group.id, group.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP);
			if(group.isEvent()){
				eventRemindersCache.remove(user.id);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public EventReminder getUserEventReminder(@NotNull User user, @NotNull ZoneId timeZone){
		synchronized(eventRemindersCache){
			EventReminder reminder=eventRemindersCache.get(user.id);
			if(reminder!=null){
				if(System.currentTimeMillis()-reminder.createdAt.toEpochMilli()<3600_000L && LocalDate.ofInstant(reminder.createdAt, timeZone).equals(LocalDate.now(timeZone)))
					return reminder;
				else
					eventRemindersCache.remove(user.id);
			}
		}
		try{
			List<Group> events=GroupStorage.getUserEventsInTimeRange(user.id, Instant.now(), Instant.now().plus(2, ChronoUnit.DAYS));
			EventReminder reminder=new EventReminder();
			reminder.createdAt=Instant.now();
			if(events.isEmpty()){
				reminder.groupIDs=Collections.emptyList();
				eventRemindersCache.put(user.id, reminder);
				return reminder;
			}

			ZonedDateTime now=ZonedDateTime.now(timeZone);
			Instant todayEnd=now.toInstant().plusNanos(now.until(ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 23, 59, 59, 999999999, timeZone), ChronoUnit.NANOS)+1L);
			Instant tomorrowEnd=todayEnd.plus(1, ChronoUnit.DAYS);
			List<Integer> eventsToday=new ArrayList<>(), eventsTomorrow=new ArrayList<>();
			for(Group g:events){
				if(g.eventStartTime.isBefore(todayEnd)){
					eventsToday.add(g.id);
				}else if(g.eventStartTime.isBefore(tomorrowEnd)){
					eventsTomorrow.add(g.id);
				}
			}

			if(!eventsToday.isEmpty()){
				reminder.groupIDs=eventsToday;
				reminder.day=LocalDate.now(timeZone);
			}else if(!eventsTomorrow.isEmpty()){
				reminder.groupIDs=eventsTomorrow;
				reminder.day=LocalDate.now(timeZone).plusDays(1);
			}else{
				reminder.groupIDs=Collections.emptyList();
			}

			eventRemindersCache.put(user.id, reminder);
			return reminder;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Group> getUserEventsInMonth(@NotNull User user, int year, int month, @NotNull ZoneId timeZone){
		try{
			ZonedDateTime start=ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, timeZone);
			int numDays=LocalDate.of(year, month, 1).lengthOfMonth();
			ZonedDateTime end=ZonedDateTime.of(year, month, numDays, 23, 59, 59, 0, timeZone);
			return GroupStorage.getUserEventsInTimeRange(user.id, start.toInstant(), end.toInstant());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Group> getUserEventsOnDay(@NotNull User user, @NotNull LocalDate day, @NotNull ZoneId timeZone){
		try{
			Instant start=day.atStartOfDay(timeZone).toInstant();
			return GroupStorage.getUserEventsInTimeRange(user.id, start, start.plusMillis(24*3600_000));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void inviteUserToGroup(@NotNull User self, @NotNull User who, @NotNull Group group){
		try{
			if(who instanceof ForeignUser && who.getGroupsURL()==null)
				throw new UserErrorException("group_invite_unsupported");
			// This also takes care of checking whether anyone has blocked anyone
			// Two users can't be friends if one blocked the other
			if(context.getFriendsController().getFriendshipStatus(self, who)!=FriendshipStatus.FRIENDS)
				throw new UserActionNotAllowedException();
			context.getPrivacyController().enforceUserPrivacy(self, who, UserPrivacySettingKey.GROUP_INVITE);

			Utils.ensureUserNotBlocked(self, group);
			Utils.ensureUserNotBlocked(who, group);
			Group.MembershipState selfState=GroupStorage.getUserMembershipState(group.id, self.id);
			if(group.accessType==Group.AccessType.PRIVATE){
				enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
			}else{
				if(selfState!=Group.MembershipState.MEMBER && selfState!=Group.MembershipState.TENTATIVE_MEMBER)
					throw new UserActionNotAllowedException();
			}

			int inviteID;
			synchronized(groupMembershipLock){
				switch(GroupStorage.getUserMembershipState(group.id, who.id)){
					case NONE -> {} // allow
					case MEMBER, TENTATIVE_MEMBER -> throw new BadRequestException(group.isEvent() ? "invite_already_in_event" : "invite_already_in_group");
					case INVITED -> throw new BadRequestException(group.isEvent() ? "invite_already_invited_event" : "invite_already_invited_group");
				}
				inviteID=GroupStorage.putInvitation(group.id, self.id, who.id, group.isEvent(), null);
			}
			if(!(who instanceof ForeignUser)){
				UserNotifications notifications=NotificationsStorage.getNotificationsFromCache(who.id);
				if(notifications!=null){
					if(group.isEvent())
						notifications.incNewEventInvitationsCount(1);
					else
						notifications.incNewGroupInvitationsCount(1);
				}
				context.getNotificationsController().sendRealtimeNotifications(who, "groupInvite"+group.id+"_"+self.id, group.isEvent() ? RealtimeNotification.Type.EVENT_INVITE : RealtimeNotification.Type.GROUP_INVITE, group, null, self);
			}
			if(group instanceof ForeignGroup || who instanceof ForeignUser){
				context.getActivityPubWorker().sendGroupInvite(inviteID, self, group, who);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<GroupInvitation> getUserInvitations(@NotNull Account account, boolean isEvent, int offset, int count){
		try{
			UserNotifications ntf=context.getNotificationsController().getUserCounters(account);
			int total=isEvent ? ntf.getNewEventInvitationsCount() : ntf.getNewGroupInvitationsCount();
			return new PaginatedList<>(GroupStorage.getUserInvitations(account.user.id, isEvent, offset, count), total, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void declineInvitation(@NotNull User self, @NotNull Group group){
		try{
			Pair<Integer, URI> apID=GroupStorage.getInvitationInviterAndApID(self.id, group.id);
			int localID=GroupStorage.deleteInvitation(self.id, group.id, group.isEvent());
			if(localID>0 && group instanceof ForeignGroup fg && apID.second()!=null){
				context.getActivityPubWorker().sendRejectGroupInvite(self, fg, localID, context.getUsersController().getUserOrThrow(apID.first()), apID.second());
			}
			context.getNotificationsController().sendRealtimeCountersUpdates(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void runLocked(@NotNull Runnable action){
		synchronized(groupMembershipLock){
			action.run();
		}
	}

	public void removeUser(@NotNull User self, @NotNull Group group, @NotNull User user){
		try{
			enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
			if(GroupStorage.getGroupMemberAdminLevel(group.id, user.id).isAtLeast(Group.AdminLevel.MODERATOR))
				throw new BadRequestException("Can't remove a group manager");

			Group.MembershipState state;
			synchronized(groupMembershipLock){
				state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.REQUESTED){
					throw new UserErrorException("err_group_not_member");
				}
				GroupStorage.leaveGroup(group, user.id, state==Group.MembershipState.TENTATIVE_MEMBER, state!=Group.MembershipState.REQUESTED);
			}
			if(user instanceof ForeignUser fu){
				context.getActivityPubWorker().sendRejectFollowGroup(fu, group, state==Group.MembershipState.TENTATIVE_MEMBER);
			}
			if(!(group instanceof ForeignGroup)){
				context.getActivityPubWorker().sendRemoveUserFromGroupActivity(user, group, state==Group.MembershipState.TENTATIVE_MEMBER);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getJoinRequests(@NotNull User self, @NotNull Group group, int offset, int count){
		enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			return GroupStorage.getGroupJoinRequests(group.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getJoinRequestCount(@NotNull User self, @NotNull Group group){
		enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			return GroupStorage.getJoinRequestCount(group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void acceptJoinRequest(@NotNull User self, @NotNull Group group, @NotNull User user){
		enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			synchronized(groupMembershipLock){
				Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(state!=Group.MembershipState.REQUESTED)
					throw new BadRequestException("No join request from this user");
				GroupStorage.setMemberAccepted(group, user.id, true);
			}
			if(user instanceof ForeignUser fu){
				Join join=new Join(false);
				join.object=new LinkOrObject(group.activityPubID);
				join.actor=new LinkOrObject(user.activityPubID);
				join.to=List.of(new LinkOrObject(group.activityPubID));
				context.getActivityPubWorker().sendAcceptFollowActivity(fu, group, join);
			}else{
				context.getNotificationsController().sendRealtimeNotifications(user, "groupJoinAccept"+group.id, RealtimeNotification.Type.GROUP_REQUEST_ACCEPTED, null, null, group);
			}
			if(!(group instanceof ForeignGroup)){
				context.getActivityPubWorker().sendAddUserToGroupActivity(user, group, false);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getGroupInvites(@NotNull User self, @NotNull Group group, int offset, int count){
		enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			return GroupStorage.getGroupInvitations(group.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void cancelInvitation(@NotNull User self, @NotNull Group group, @NotNull User user){
		enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			Pair<Integer, URI> inviterAndID=GroupStorage.getInvitationInviterAndApID(user.id, group.id);
			int id=GroupStorage.deleteInvitation(user.id, group.id, group.isEvent());
			if(id<0)
				throw new BadRequestException("This user was not invited to this group");
			if(user instanceof ForeignUser fu){
				URI apID=inviterAndID.second();
				if(apID==null){
					apID=Config.localURI("/activitypub/objects/groupInvites/"+id);
				}
				context.getActivityPubWorker().sendUndoGroupInvite(fu, group, id, context.getUsersController().getUserOrThrow(inviterAndID.first()), apID);
			}else{
				context.getNotificationsController().sendRealtimeCountersUpdates(user);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<URI, Integer> getMembersByActivityPubIDs(@NotNull Group group, Collection<URI> query, boolean tentative){
		try{
			return GroupStorage.getMembersByActivityPubIDs(query, group.id, tentative);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<URI, Integer> getUserGroupsByActivityPubIDs(@NotNull User user, Collection<URI> query){
		try{
			return GroupStorage.getUserGroupsByActivityPubIDs(query, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void addOrUpdateAdmin(Group group, User user, String title, Group.AdminLevel level){
		try{
			GroupStorage.addOrUpdateGroupAdmin(group.id, user.id, title, level);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void removeAdmin(Group group, User user){
		try{
			GroupStorage.removeGroupAdmin(group.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setAdminOrder(Group group, User user, int order){
		try{
			GroupStorage.setGroupAdminOrder(group.id, user.id, order);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getBlockedUsers(Group group){
		try{
			return GroupStorage.getBlockedUsers(group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<String> getBlockedDomains(Group group){
		try{
			return GroupStorage.getBlockedDomains(group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void blockDomain(Group group, String domain){
		try{
			if(!domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$"))
				throw new UserErrorException("Invalid domain");
			if(GroupStorage.isDomainBlocked(group.id, domain))
				throw new UserErrorException("err_domain_already_blocked");
			GroupStorage.blockDomain(group.id, domain);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void unblockDomain(Group group, String domain){
		try{
			if(StringUtils.isNotEmpty(domain))
				GroupStorage.unblockDomain(group.id, domain);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void blockUser(Group group, User user){
		try{
			if(GroupStorage.getGroupMemberAdminLevel(group.id, user.id).isAtLeast(Group.AdminLevel.MODERATOR))
				throw new UserErrorException("Can't block a group manager");
			GroupStorage.blockUser(group.id, user.id);
			if(user instanceof ForeignUser fu)
				context.getActivityPubWorker().sendBlockActivity(group, fu);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void unblockUser(Group group, User user){
		try{
			GroupStorage.unblockUser(group.id, user.id);
			if(user instanceof ForeignUser fu)
				context.getActivityPubWorker().sendUndoBlockActivity(group, fu);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void incrementHintsRank(User self, Group group, int amount){
		if(group.isEvent())
			return;
		pendingHintsRankIncrements.add(new PendingHintsRankIncrement(self.id, group.id, amount));
	}

	public void doPendingHintsUpdates(){
		if(pendingHintsRankIncrements.isEmpty())
			return;
		ArrayList<PendingHintsRankIncrement> increments=pendingHintsRankIncrements;
		pendingHintsRankIncrements=new ArrayList<>();
		try{
			HashMap<IntPair, Integer> totals=new HashMap<>();
			for(PendingHintsRankIncrement i:increments){
				IntPair key=new IntPair(i.userID, i.groupID);
				totals.put(key, totals.getOrDefault(key, 0)+i.amount);
			}
			HashSet<Integer> usersToNormalize=new HashSet<>();
			for(Map.Entry<IntPair, Integer> i:totals.entrySet()){
				IntPair key=i.getKey();
				if(GroupStorage.incrementGroupHintsRank(key.first(), key.second(), i.getValue()))
					usersToNormalize.add(key.first());
			}
			if(!usersToNormalize.isEmpty())
				GroupStorage.normalizeGroupHintsRanksIfNeeded(usersToNormalize);
		}catch(SQLException x){
			LOG.error("Failed to update hint ranks", x);
		}
	}

	public String updateStatus(User self, Group group, String status){
		enforceUserAdminLevel(group, self, Group.AdminLevel.ADMIN);
		ActorStatus result=updateStatus(group, new ActorStatus(status, Instant.now(), null, null));
		return result==null ? null : result.text();
	}

	public ActorStatus updateStatus(Group group, ActorStatus status){
		ActorStatus prev=group.status;
		if(status!=null && StringUtils.isNotEmpty(status.text()) && !status.isExpired()){
			if(status.text().length()>100)
				status=status.withText(TextProcessor.truncateOnWordBoundary(status.text(), 100)+"...");
			if(group.status!=null && group.status.text().equals(status.text()))
				return status;
			group.status=status;
		}else{
			if(group.status==null)
				return status;
			group.status=null;
		}
		try{
			GroupStorage.updateProfileFields(group);
			if(group instanceof ForeignGroup){
				if(prev!=null)
					FederationStorage.deleteFromApIdIndex(ObjectLinkResolver.ObjectType.GROUP_STATUS, group.id);
				if(status!=null && status.apId()!=null)
					FederationStorage.addToApIdIndex(status.apId(), ObjectLinkResolver.ObjectType.GROUP_STATUS, group.id);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		if(!(group instanceof ForeignGroup)){
			if(group.status!=null)
				context.getActivityPubWorker().sendCreateStatusActivity(group, group.status);
			else
				context.getActivityPubWorker().sendClearStatusActivity(group, prev);
		}

		return status;
	}

	public LocalImage downloadImageForLink(Group group, URI url){
		try{
			return MediaStorageUtils.downloadRemoteImageForGroupLink(group, url);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}catch(IOException x){
			LOG.debug("Failed to download link image from {}", url, x);
			return null;
		}
	}

	public GroupLinkParseResult parseLink(URI url, User self, Group group){
		ObjectLinkResolver.ObjectTypeAndID apObject;
		try{
			Object apObj=context.getObjectLinkResolver().resolveNative(url, Object.class, true, true, false, (JsonObject) null, false);
			switch(apObj){
				case OwnedContentObject oco -> context.getPrivacyController().enforceObjectPrivacy(self, oco);
				case Group g -> context.getPrivacyController().enforceUserAccessToGroupProfile(self, g);
				case User u -> context.getPrivacyController().enforceUserProfileAccess(self, u);
				default -> {}
			}
			apObject=ObjectLinkResolver.getObjectIdFromObject(apObj);
			if(apObject==null)
				throw new UserErrorException("group_link_error");
			return new GroupLinkParseResult(apObject, null, null, null);
		}catch(ObjectNotFoundExceptionWithFallback x){
			if(x.fallback instanceof Document htmlDoc){
				String title=htmlDoc.title();
				LocalImage image=null;
				Element ogTitle=htmlDoc.selectFirst("meta[property=og:title]");
				Element ogImage=htmlDoc.selectFirst("meta[property=og:image]");
				if(ogTitle!=null){
					String ogTitleValue=ogTitle.attr("content");
					if(StringUtils.isNotEmpty(ogTitleValue))
						title=ogTitleValue;
				}
				String imageURL=null;
				if(ogImage!=null){
					String _imageURL=ogImage.absUrl("content");
					if(StringUtils.isNotEmpty(_imageURL))
						imageURL=_imageURL;
				}
				if(StringUtils.isNotEmpty(imageURL)){
					image=downloadImageForLink(group, URI.create(imageURL));
				}
				return new GroupLinkParseResult(null, title, imageURL, image);
			}else{
				throw new UserErrorException("group_link_error");
			}
		}catch(Exception x){
			if(Config.isLocal(url)){ // Explicitly allow any local links
				return new GroupLinkParseResult(null, url.toString(), null, null);
			}
			throw new UserErrorException("group_link_error", x);
		}
	}

	public long addLink(User self, Group group, URI url, GroupLinkParseResult parseResult, String title){
		try{
			enforceUserAdminLevel(group, self, Group.AdminLevel.ADMIN);
			long imageID=parseResult.image()!=null ? parseResult.image().fileID : 0;
			long id=GroupStorage.createLink(group.id, url.toString(), title, parseResult.apObject(), imageID);
			if(imageID!=0){
				MediaStorage.createMediaFileReference(imageID, id, MediaFileReferenceType.GROUP_LINK_THUMB, -group.id);
			}
			// TODO Update{Group}
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<GroupLink> getLinks(Group group){
		try{
			List<GroupLink> links=GroupStorage.getGroupLinks(group.id);
			for(GroupLink l:links){
				if(l.object!=null){
					l.url=ObjectLinkResolver.getLocalURLForObjectID(l.object);
				}
			}
			return links;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public GroupLink getLink(Group group, long linkID){
		try{
			GroupLink link=GroupStorage.getGroupLink(group.id, linkID);
			if(link==null)
				throw new ObjectNotFoundException();
			return link;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setLinkOrder(Group group, GroupLink link, int order){
		try{
			GroupStorage.setLinkOrder(group.id, link.id, order);
			// TODO Update{Group}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateLinkTitle(Group group, GroupLink link, String title){
		try{
			GroupStorage.updateLinkTitle(group.id, link.id, title);
			// TODO Update{Group}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteLink(Group group, GroupLink link){
		try{
			GroupStorage.deleteLink(group.id, link.id);
			// TODO Update{Group}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public enum EventsType{
		FUTURE,
		PAST,
		ALL
	}

	private record PendingHintsRankIncrement(int userID, int groupID, int amount){}
}
