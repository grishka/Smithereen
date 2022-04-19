package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Join;
import smithereen.data.Account;
import smithereen.data.EventReminder;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.GroupAdmin;
import smithereen.data.GroupInvitation;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.UserNotifications;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

import static smithereen.Utils.wrapError;

public class GroupsController{
	private static final Logger LOG=LoggerFactory.getLogger(GroupsController.class);

	private final ApplicationContext context;
	private final LruCache<Integer, EventReminder> eventRemindersCache=new LruCache<>(500);

	private final Object groupMembershipLock=new Object();

	public GroupsController(ApplicationContext context){
		this.context=context;
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
				throw new IllegalArgumentException("name is empty");
			if(isEvent && startTime==null)
				throw new IllegalArgumentException("start time is required for event");
			int id=GroupStorage.createGroup(name, Utils.preprocessPostHTML(description, null), description, admin.id, isEvent, startTime, endTime);
			Group group=Objects.requireNonNull(GroupStorage.getById(id));
			context.getActivityPubWorker().sendAddToGroupsCollectionActivity(admin, group);
			NewsfeedStorage.putEntry(admin.id, group.id, isEvent ? NewsfeedEntry.Type.CREATE_EVENT : NewsfeedEntry.Type.CREATE_GROUP, null);
			return group;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> getUserGroups(@NotNull User user, int offset, int count){
		try{
			return GroupStorage.getUserGroups(user.id, offset, count);
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

	public List<Group> getGroupsByIdAsList(Collection<Integer> ids){
		try{
			return GroupStorage.getByIdAsList(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, Group> getGroupsByIdAsMap(Collection<Integer> ids){
		try{
			return GroupStorage.getById(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getMembers(@NotNull Group group, int offset, int count, boolean tentative){
		try{
			return GroupStorage.getMembers(group.id, offset, count, tentative);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getAllMembers(@NotNull Group group, int offset, int count){
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

	public void updateGroupInfo(@NotNull Group group, @NotNull User admin, String name, String aboutSrc, Instant eventStart, Instant eventEnd, String username, Group.AccessType accessType){
		try{
			enforceUserAdminLevel(group, admin, Group.AdminLevel.ADMIN);
			String about=StringUtils.isNotEmpty(aboutSrc) ? Utils.preprocessPostHTML(aboutSrc, null) : null;
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
			context.getActivityPubWorker().sendUpdateGroupActivity(group);
			if(group.isEvent()){
				BackgroundTaskRunner.getInstance().submit(()->{
					try{
						synchronized(eventRemindersCache){
							GroupStorage.getAllMembersAsStream(group.id).boxed().forEach(eventRemindersCache::remove);
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
		try{
			Utils.ensureUserNotBlocked(user, group);
			boolean autoAccepted=true;

			synchronized(groupMembershipLock){
				Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(!getMemberAdminLevel(group, user).isAtLeast(Group.AdminLevel.ADMIN)){
					if(group.accessType==Group.AccessType.PRIVATE){
						if(state!=Group.MembershipState.INVITED)
							throw new UserActionNotAllowedException();
					}else if(group.accessType==Group.AccessType.CLOSED){
						autoAccepted=state==Group.MembershipState.INVITED;
					}
				}

				if(group.isEvent()){
					if((state==Group.MembershipState.MEMBER && !tentative) || (state==Group.MembershipState.TENTATIVE_MEMBER && tentative) || state==Group.MembershipState.REQUESTED)
						throw new UserErrorException("err_group_already_member");
				}else{
					if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER || state==Group.MembershipState.REQUESTED)
						throw new UserErrorException("err_group_already_member");
				}

				if(tentative && (!group.isEvent() || (group instanceof ForeignGroup fg && !fg.hasCapability(ForeignGroup.Capability.TENTATIVE_MEMBERSHIP))))
					throw new BadRequestException();

				// change certain <-> tentative
				if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
					GroupStorage.updateUserEventDecision(group, user.id, tentative);
					if(group instanceof ForeignGroup fg)
						context.getActivityPubWorker().sendJoinGroupActivity(user, fg, tentative);
					return;
				}

				GroupStorage.joinGroup(group, user.id, tentative, !(group instanceof ForeignGroup) && autoAccepted);
			}

			if(group instanceof ForeignGroup fg){
				// Add{Group} will be sent upon receiving Accept{Follow}
				context.getActivityPubWorker().sendJoinGroupActivity(user, fg, tentative);
			}else{
				if(group.accessType!=Group.AccessType.PRIVATE && autoAccepted){
					context.getActivityPubWorker().sendAddToGroupsCollectionActivity(user, group);
					NewsfeedStorage.putEntry(user.id, group.id, group.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP, null);
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
			synchronized(groupMembershipLock){
				Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
				if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.REQUESTED){
					throw new UserErrorException("err_group_not_member");
				}
				GroupStorage.leaveGroup(group, user.id, state==Group.MembershipState.TENTATIVE_MEMBER, state!=Group.MembershipState.REQUESTED);
			}
			if(group instanceof ForeignGroup fg){
				context.getActivityPubWorker().sendLeaveGroupActivity(user, fg);
			}
			if(group.accessType!=Group.AccessType.PRIVATE)
				context.getActivityPubWorker().sendRemoveFromGroupsCollectionActivity(user, group);
			NewsfeedStorage.deleteEntry(user.id, group.id, group.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP);
			if(group.isEvent()){
				synchronized(eventRemindersCache){
					eventRemindersCache.remove(user.id);
				}
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
				synchronized(eventRemindersCache){
					eventRemindersCache.put(user.id, reminder);
					return reminder;
				}
			}

			ZonedDateTime now=ZonedDateTime.now(timeZone);
			Instant todayEnd=now.toInstant().plusNanos(now.until(ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth()+1, 0, 0, 0, 0, timeZone), ChronoUnit.NANOS));
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

			synchronized(eventRemindersCache){
				eventRemindersCache.put(user.id, reminder);
				return reminder;
			}
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
			// This also takes care of checking whether anyone has blocked anyone
			// Two users can't be friends if one blocked the other
			if(context.getFriendsController().getFriendshipStatus(self, who)!=FriendshipStatus.FRIENDS)
				throw new UserActionNotAllowedException();

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
			UserNotifications ntf=NotificationsStorage.getNotificationsForUser(account.user.id, account.prefs.lastSeenNotificationID);
			int total=isEvent ? ntf.getNewEventInvitationsCount() : ntf.getNewGroupInvitationsCount();
			return new PaginatedList<>(GroupStorage.getUserInvitations(account.user.id, isEvent, offset, count), total, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void declineInvitation(@NotNull User self, @NotNull Group group){
		try{
			URI apID=GroupStorage.getInvitationApID(self.id, group.id);
			int localID=GroupStorage.deleteInvitation(self.id, group.id, group.isEvent());
			if(localID>0 && group instanceof ForeignGroup fg){
				context.getActivityPubWorker().sendRejectGroupInvite(self, fg, localID, apID);
			}
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
			URI apID=GroupStorage.getInvitationApID(user.id, group.id);
			int id=GroupStorage.deleteInvitation(user.id, group.id, group.isEvent());
			if(id<0)
				throw new BadRequestException("This user was not invited to this group");
			if(user instanceof ForeignUser fu){
				if(apID==null){
					apID=Config.localURI("/activitypub/objects/groupInvites/"+id);
				}
				context.getActivityPubWorker().sendUndoGroupInvite(fu, group, id, apID);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public enum EventsType{
		FUTURE,
		PAST,
		ALL
	}
}
