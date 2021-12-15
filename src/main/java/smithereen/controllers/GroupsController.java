package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.GroupAdmin;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import spark.utils.StringUtils;

public class GroupsController{
	private static final Logger LOG=LoggerFactory.getLogger(GroupsController.class);

	private final ApplicationContext context;

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

	public PaginatedList<User> getMembers(@NotNull Group group, int offset, int count){
		try{
			return GroupStorage.getMembers(group.id, offset, count);
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

	public List<User> getRandomMembersForProfile(@NotNull Group group){
		try{
			return GroupStorage.getRandomMembersForProfile(group.id);
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
			String about=Utils.preprocessPostHTML(aboutSrc, null);
			GroupStorage.updateGroupGeneralInfo(group, name, aboutSrc, about, eventStart, eventEnd);
			ActivityPubWorker.getInstance().sendUpdateGroupActivity(group);
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
