package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.EventReminder;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.GroupAdmin;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

import static smithereen.Utils.wrapError;

public class GroupsController{
	private static final Logger LOG=LoggerFactory.getLogger(GroupsController.class);

	private final ApplicationContext context;
	private final LruCache<Integer, EventReminder> eventRemindersCache=new LruCache<>(500);

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
			ActivityPubWorker.getInstance().sendAddToGroupsCollectionActivity(admin, group);
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

	public void updateGroupInfo(@NotNull Group group, @NotNull User admin, String name, String aboutSrc, Instant eventStart, Instant eventEnd){
		try{
			enforceUserAdminLevel(group, admin, Group.AdminLevel.ADMIN);
			String about=StringUtils.isNotEmpty(aboutSrc) ? Utils.preprocessPostHTML(aboutSrc, null) : null;
			GroupStorage.updateGroupGeneralInfo(group, name, aboutSrc, about, eventStart, eventEnd);
			ActivityPubWorker.getInstance().sendUpdateGroupActivity(group);
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

			Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
			if(group.isEvent()){
				if((state==Group.MembershipState.MEMBER && !tentative) || (state==Group.MembershipState.TENTATIVE_MEMBER && tentative))
					throw new UserErrorException("err_group_already_member");
			}else{
				if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER)
					throw new UserErrorException("err_group_already_member");
			}

			if(tentative && (!group.isEvent() || (group instanceof ForeignGroup fg && !fg.hasCapability(ForeignGroup.Capability.TENTATIVE_MEMBERSHIP))))
				throw new BadRequestException();

			// change certain <-> tentative
			if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
				GroupStorage.updateUserEventDecision(group, user.id, tentative);
				if(group instanceof ForeignGroup fg)
					ActivityPubWorker.getInstance().sendJoinGroupActivity(user, fg, tentative);
				return;
			}

			GroupStorage.joinGroup(group, user.id, tentative, !(group instanceof ForeignGroup));
			if(group instanceof ForeignGroup fg){
				// Add{Group} will be sent upon receiving Accept{Follow}
				ActivityPubWorker.getInstance().sendJoinGroupActivity(user, fg, tentative);
			}else{
				ActivityPubWorker.getInstance().sendAddToGroupsCollectionActivity(user, group);
			}
			NewsfeedStorage.putEntry(user.id, group.id, group.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP, null);
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
			Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, user.id);
			if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER){
				throw new UserErrorException("err_group_not_member");
			}
			GroupStorage.leaveGroup(group, user.id, state==Group.MembershipState.TENTATIVE_MEMBER);
			if(group instanceof ForeignGroup fg){
				ActivityPubWorker.getInstance().sendLeaveGroupActivity(user, fg);
			}
			ActivityPubWorker.getInstance().sendRemoveFromGroupsCollectionActivity(user, group);
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
			List<Group> events=GroupStorage.getUpcomingEvents(user.id);
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

	public enum EventsType{
		FUTURE,
		PAST,
		ALL
	}
}
