package smithereen.api.methods;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiUser;
import smithereen.controllers.GroupsController;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.groups.GroupAdmin;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.groups.GroupInvitation;
import smithereen.model.groups.GroupLink;
import smithereen.model.groups.GroupLinkParseResult;
import spark.utils.StringUtils;

import static smithereen.Utils.isWithinDatabaseLimits;

public class GroupsMethods{
	private static final Pattern ID_PATTERN=Pattern.compile("^\\d+$");

	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		Set<String> groupIDs;
		if(actx.hasParam("group_id")){
			groupIDs=Set.of(Objects.requireNonNull(actx.optParamString("group_id")));
		}else if(actx.hasParam("group_ids")){
			groupIDs=actx.optCommaSeparatedStringSet("group_ids");
			if(groupIDs.size()>1000)
				groupIDs=groupIDs.stream().limit(1000).collect(Collectors.toSet());
		}else{
			throw actx.paramError("group_ids is undefined");
		}

		Set<Integer> needGroups=groupIDs.stream().filter(ID_PATTERN.asMatchPredicate()).map(Utils::safeParseInt).collect(Collectors.toSet());
		if(needGroups.size()!=groupIDs.size()){ // There are usernames
			needGroups=new HashSet<>(needGroups);
			needGroups.addAll(ctx.getGroupsController().getGroupIDsByUsernames(groupIDs.stream().filter(ID_PATTERN.asMatchPredicate().negate()).collect(Collectors.toSet())).values());
		}

		return ApiUtils.getGroups(needGroups, ctx, actx);
	}

	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");

		Set<String> filter=actx.optCommaSeparatedStringSet("filter");
		if(filter.contains("events") || filter.contains("admin") || filter.contains("moder")){
			if(actx.self==null || actx.self.id!=user.id)
				throw actx.error(ApiErrorType.NO_PERMISSION, "these filters can only be used for the current user's groups");
			actx.requirePermission(ClientAppPermission.GROUPS_READ);
		}

		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		PaginatedList<Group> groups;
		if(filter.contains("events")){
			if(filter.contains("admin"))
				groups=ctx.getGroupsController().getUserManagedGroups(actx.self.user, Group.AdminLevel.ADMIN, true, offset, count);
			else if(filter.contains("moder"))
				groups=ctx.getGroupsController().getUserManagedGroups(actx.self.user, Group.AdminLevel.MODERATOR, true, offset, count);
			else
				groups=ctx.getGroupsController().getUserEvents(actx.self.user, GroupsController.EventsType.ALL, offset, count);
		}else{
			if(filter.contains("admin"))
				groups=ctx.getGroupsController().getUserManagedGroups(actx.self.user, Group.AdminLevel.ADMIN, false, offset, count);
			else if(filter.contains("moder"))
				groups=ctx.getGroupsController().getUserManagedGroups(actx.self.user, Group.AdminLevel.MODERATOR, false, offset, count);
			else
				groups=ctx.getGroupsController().getUserGroups(user, actx.self==null || !actx.hasPermission(ClientAppPermission.GROUPS_READ) ? null : actx.self.user, offset, count);
		}
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(groups.total, ApiUtils.getGroups(groups.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(groups.total, groups.list.stream().map(g->g.id).toList());
		}
	}

	public static Object getInvites(ApplicationContext ctx, ApiCallContext actx){
		boolean events="events".equals(actx.optParamString("type", "groups"));
		int offset=actx.getOffset();
		int count=actx.getCount(20, 500);
		PaginatedList<GroupInvitation> invites=ctx.getGroupsController().getUserInvitations(actx.self, events, offset, count);
		List<ApiGroup> apiGroups=ApiUtils.getGroups(invites.list.stream().map(i->i.group).toList(), ctx, actx);
		for(int i=0;i<apiGroups.size();i++){
			apiGroups.get(i).invitedBy=invites.list.get(i).inviter.id;
		}
		if(actx.optParamBoolean("extended")){
			ApiPaginatedListWithActors<ApiGroup> res=new ApiPaginatedListWithActors<>(invites.total, apiGroups);
			Set<Integer> seenUsers=new HashSet<>();
			ArrayList<User> users=new ArrayList<>();
			for(GroupInvitation invite:invites.list){
				if(seenUsers.contains(invite.inviter.id))
					continue;
				seenUsers.add(invite.inviter.id);
				users.add(invite.inviter);
			}
			res.profiles=ApiUtils.getUsers(users, ctx, actx);
			return res;
		}else{
			return new ApiPaginatedList<>(invites.total, apiGroups);
		}
	}

	public static Object join(ApplicationContext ctx, ApiCallContext actx){
		Group group=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
		ctx.getGroupsController().joinGroup(group, actx.self.user, actx.optParamBoolean("not_sure"), false, true);
		return true;
	}

	public static Object leave(ApplicationContext ctx, ApiCallContext actx){
		Group group=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
		ctx.getGroupsController().leaveGroup(group, actx.self.user, true);
		return true;
	}

	public static Object search(ApplicationContext ctx, ApiCallContext actx){
		String query=actx.requireParamString("q").trim();
		if(query.isEmpty())
			throw actx.paramError("q is empty");
		PaginatedList<Integer> ids=ctx.getSearchController().searchAllGroupIDs(query, "events".equals(actx.optParamString("type")), actx.getOffset(), actx.getCount(100, 100));
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(ids.total, ApiUtils.getGroups(ids.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(ids);
		}
	}

	public static Object isMember(ApplicationContext ctx, ApiCallContext actx){
		Group group=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
		User self=actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null;
		ctx.getPrivacyController().enforceUserAccessToGroupProfile(self, group);
		boolean extended=actx.optParamBoolean("extended");
		if(actx.hasParam("user_id") && !extended){
			Group.MembershipState state=ctx.getGroupsController().getUserMembershipState(group, ApiUtils.getUser(ctx, actx, "user_id"));
			return state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER;
		}
		record ResponseItem(int userId, boolean member, Boolean canInvite, Boolean invitation, Boolean request){}
		Set<Integer> userIDs;
		if(actx.hasParam("user_id")){
			userIDs=Set.of(actx.requireParamIntPositive("user_id"));
		}else{
			userIDs=actx.requireCommaSeparatedStringSet("user_ids")
					.stream()
					.map(Utils::safeParseInt)
					.filter(id->id>0)
					.collect(Collectors.toSet());
			if(userIDs.isEmpty())
				throw actx.paramError("user_ids does not contain any valid user IDs");
		}
		List<ResponseItem> items=new ArrayList<>();
		Map<Integer, User> users=ctx.getUsersController().getUsers(userIDs);
		userIDs=userIDs.stream().filter(users::containsKey).collect(Collectors.toSet());
		boolean canManage=false, needCheckPrivacy=false;
		if(extended && actx.self!=null){
			needCheckPrivacy=true;
			canManage=actx.hasPermission(ClientAppPermission.GROUPS_READ) && actx.permissions.canManageGroup(group);
		}
		Map<Integer, Group.MembershipState> states=ctx.getGroupsController().getMembershipStates(userIDs, group, canManage);
		for(int id:userIDs){
			Group.MembershipState state=states.get(id);
			items.add(new ResponseItem(id, state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER,
					needCheckPrivacy ? state==Group.MembershipState.NONE && ctx.getPrivacyController().checkUserPrivacy(actx.self.user, users.get(id), UserPrivacySettingKey.GROUP_INVITE) : null,
					canManage ? state==Group.MembershipState.INVITED : null, canManage ? state==Group.MembershipState.REQUESTED : null));
		}
		return items;
	}

	public static Object getMembers(ApplicationContext ctx, ApiCallContext actx){
		Group group=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
		User self=actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null;
		boolean canManage=self!=null && actx.permissions.canManageGroup(group);
		ctx.getPrivacyController().enforceUserAccessToGroupProfile(self, group);
		GroupsController.MemberSortOrder sort=switch(actx.optParamString("sort", "id_asc")){
			case "id_desc" -> GroupsController.MemberSortOrder.ID_DESC;
			case "random" -> GroupsController.MemberSortOrder.RANDOM;
			case "time_asc" -> GroupsController.MemberSortOrder.TIME_ASC;
			case "time_desc" -> GroupsController.MemberSortOrder.TIME_DESC;
			default -> GroupsController.MemberSortOrder.ID_ASC;
		};
		if((sort==GroupsController.MemberSortOrder.TIME_ASC || sort==GroupsController.MemberSortOrder.TIME_DESC) && !canManage)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "this sorting requires a token with groups:read permission and is only available for group managers");

		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		String filter=actx.optParamString("filter");
		if("managers".equals(filter)){
			if(!canManage)
				throw actx.error(ApiErrorType.ACCESS_DENIED, "this filter requires a token with groups:read permission and is only available for group managers");
			List<GroupAdmin> admins=ctx.getGroupsController().getAdmins(group);
			if(actx.hasParam("fields")){
				List<ApiUser> users=ApiUtils.getUsers(admins.stream().map(ga->ga.userID).toList(), ctx, actx);
				for(int i=0;i<users.size();i++){
					users.get(i).role=switch(admins.get(i).level){
						case OWNER -> "creator";
						case ADMIN -> "administrator";
						case MODERATOR -> "moderator";
						case REGULAR -> throw new IllegalStateException("Group admins aren't supposed to have the REGULAR level, what happened here?");
					};
				}
				return new ApiPaginatedList<>(users.size(), users);
			}else{
				record ApiGroupAdmin(int id, String role){}
				return new ApiPaginatedList<>(admins.size(), admins.stream()
						.map(ga->new ApiGroupAdmin(ga.userID, switch(ga.level){
							case OWNER -> "creator";
							case ADMIN -> "administrator";
							case MODERATOR -> "moderator";
							case REGULAR -> throw new IllegalStateException("Group admins aren't supposed to have the REGULAR level, what happened here?");
						}))
						.toList());
			}
		}

		boolean friendsOnly="friends".equals(filter) || "unsure_friends".equals(filter);
		boolean tentative="unsure".equals(filter) || "unsure_friends".equals(filter);
		PaginatedList<Integer> memberIDs;
		if(friendsOnly){
			if(actx.self.user==null)
				throw actx.error(ApiErrorType.ACCESS_DENIED, "filtering by friends requires an access token");
			memberIDs=ctx.getGroupsController().getFriendsMembers(group, offset, count, tentative, sort, actx.self.user);
		}else{
			memberIDs=ctx.getGroupsController().getMembers(group, offset, count, tentative, sort);
		}

		if(actx.hasParam("fields"))
			return new ApiPaginatedList<>(memberIDs.total, ApiUtils.getUsers(memberIDs.list, ctx, actx));
		else
			return new ApiPaginatedList<>(memberIDs);
	}

	public static Object banUser(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		ctx.getGroupsController().blockUser(group, user);
		return true;
	}

	public static Object unbanUser(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		ctx.getGroupsController().unblockUser(group, user);
		return true;
	}

	public static Object getBannedUsers(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		PaginatedList<User> users=ctx.getGroupsController().getBlockedUsers(group, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(users.total, ApiUtils.getUsers(users.list, ctx, actx));
	}

	public static Object getBannedDomains(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		List<String> domains=ctx.getGroupsController().getBlockedDomains(group);
		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		if(offset>=domains.size())
			return new ApiPaginatedList<>(0, List.of());
		return new ApiPaginatedList<>(domains.size(), domains.subList(offset, Math.min(offset+count, domains.size())));
	}

	public static Object banDomain(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		String domain=actx.requireParamString("domain");
		if(!domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$"))
			throw actx.paramError("invalid domain");
		ctx.getGroupsController().blockDomain(group, domain);
		return true;
	}

	public static Object unbanDomain(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.MODERATOR);
		ctx.getGroupsController().unblockDomain(group, actx.requireParamString("domain"));
		return true;
	}

	public static Object getInvitedUsers(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		PaginatedList<User> users=ctx.getGroupsController().getGroupInvites(actx.self.user, group, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(users.total, ApiUtils.getUsers(users.list, ctx, actx));
	}

	public static Object getRequests(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		PaginatedList<User> users=ctx.getGroupsController().getJoinRequests(actx.self.user, group, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(users.total, ApiUtils.getUsers(users.list, ctx, actx));
	}

	public static Object approveRequest(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().acceptJoinRequest(actx.self.user, group, ApiUtils.getUser(ctx, actx, "user_id"));
		return true;
	}
	
	public static Object removeUser(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		if(ctx.getGroupsController().getUserMembershipState(group, user)==Group.MembershipState.INVITED){
			ctx.getGroupsController().cancelInvitation(actx.self.user, group, user);
		}else{
			ctx.getGroupsController().removeUser(actx.self.user, group, user);
		}
		return true;
	}

	public static Object invite(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		try{
			ctx.getGroupsController().inviteUserToGroup(actx.self.user, user, group);
		}catch(BadRequestException x){
			throw actx.error(ApiErrorType.CANT_INVITE_TO_GROUP, Lang.get(Locale.US).get(x.getMessage()));
		}
		return true;
	}

	public static Object getSettings(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);

		record GroupSettings(String name, String description, String screenName, String site, String accessType, Long startDate, Long finishDate, String place, String wall, String photos, String board){}
		return new GroupSettings(group.name, group.summary, group.username, group.website, group.accessType.name().toLowerCase(),
				group.isEvent() && group.eventStartTime!=null ? group.eventStartTime.getEpochSecond() : null, group.isEvent() && group.eventEndTime!=null ? group.eventEndTime.getEpochSecond() : null,
				group.isEvent() ? group.location : null, group.wallState.asApiValue(), group.photosState.asApiValue(), group.boardState.asApiValue());
	}

	public static Object edit(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);

		String name=actx.optParamString("name", group.name);
		String about=group.aboutSource;
		if(actx.hasParam("description"))
			about=actx.optParamString("description", "");
		if(StringUtils.isEmpty(about))
			about=null;

		Instant eventStart=null, eventEnd=null;
		if(group.isEvent()){
			eventStart=group.eventStartTime;
			if(actx.hasParam("start_date")){
				eventStart=Instant.ofEpochSecond(actx.requireParamLongNonZero("start_date"));
			}
			eventEnd=group.eventEndTime;
			if(actx.hasParam("finish_date")){
				long endDate=actx.optParamLong("finish_date");
				eventEnd=endDate>0 ? Instant.ofEpochSecond(endDate) : null;
			}
			if(eventEnd!=null && eventStart.isAfter(eventEnd))
				throw actx.paramError("finish_date is before start_date");
			if(!eventStart.equals(group.eventStartTime)){
				if(eventStart.isBefore(Instant.now()))
					throw actx.paramError("start_date is in the past");
				if(!isWithinDatabaseLimits(eventStart))
					throw actx.paramError("start_date is too far in the future");
			}
			if(eventEnd!=null && !isWithinDatabaseLimits(eventEnd))
				throw actx.paramError("finish_date is too far in the future");
		}

		String username=group.username;
		if(actx.hasParam("username"))
			username=actx.requireParamString("username");

		Group.AccessType accessType=group.accessType;
		if(actx.hasParam("access_type")){
			accessType=actx.requireParamEnum("access_type", Map.of(
					"open", Group.AccessType.OPEN,
					"closed", Group.AccessType.CLOSED,
					"private", Group.AccessType.PRIVATE
			));
			if(group.isEvent() && accessType==Group.AccessType.CLOSED)
				throw actx.paramError("access_type can only be open or private for events");
		}

		GroupFeatureState wallState=group.wallState;
		if(actx.hasParam("wall")){
			wallState=actx.requireParamEnum("wall", Map.of(
					"open", GroupFeatureState.ENABLED_OPEN,
					"restricted", GroupFeatureState.ENABLED_RESTRICTED,
					"closed", GroupFeatureState.ENABLED_CLOSED,
					"disabled", GroupFeatureState.DISABLED
			));
		}

		GroupFeatureState photosState=group.photosState;
		if(actx.hasParam("photos")){
			photosState=actx.requireParamEnum("photos", Map.of(
					"restricted", GroupFeatureState.ENABLED_RESTRICTED,
					"disabled", GroupFeatureState.DISABLED
			));
		}

		GroupFeatureState boardState=group.wallState;
		if(actx.hasParam("board")){
			boardState=actx.requireParamEnum("board", Map.of(
					"open", GroupFeatureState.ENABLED_OPEN,
					"restricted", GroupFeatureState.ENABLED_RESTRICTED,
					"disabled", GroupFeatureState.DISABLED
			));
		}

		String website=group.website;
		if(actx.hasParam("site"))
			website=actx.optParamString("site");

		String eventLocation=group.location;
		if(group.isEvent() && actx.hasParam("place"))
			eventLocation=actx.optParamString("place");

		ctx.getGroupsController().updateGroupInfo(group, actx.self.user, name, about, eventStart, eventEnd, username, accessType, wallState, photosState, boardState, website, eventLocation);

		return true;
	}

	public static Object addManager(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		Group.MembershipState state=ctx.getGroupsController().getUserMembershipState(group, user);
		if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER)
			throw actx.error(ApiErrorType.CANT_PROMOTE_GROUP_MEMBER, "this user is not a member of this group");
		if(ctx.getGroupsController().getMemberAdminLevel(group, user)!=Group.AdminLevel.REGULAR)
			throw actx.error(ApiErrorType.CANT_PROMOTE_GROUP_MEMBER, "this user is already a manager in this group");
		if(user instanceof ForeignUser)
			throw actx.error(ApiErrorType.CANT_PROMOTE_GROUP_MEMBER, "groups currently cannot be managed by users from other servers");

		ctx.getGroupsController().addOrUpdateAdmin(group, actx.self.user, user, actx.optParamString("title", ""), actx.requireParamEnum("level", Map.of("moderator", Group.AdminLevel.MODERATOR, "admin", Group.AdminLevel.ADMIN)));

		return true;
	}

	public static Object editManager(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, user);
		if(level==Group.AdminLevel.REGULAR)
			throw actx.error(ApiErrorType.CANT_PROMOTE_GROUP_MEMBER, "this user is not a manager in this group");

		ctx.getGroupsController().addOrUpdateAdmin(group, actx.self.user, user, actx.hasParam("title") ? actx.optParamString("title", "") : ctx.getGroupsController().getAdmin(group, user.id).title,
				actx.optParamEnum("level", Map.of("moderator", Group.AdminLevel.MODERATOR, "admin", Group.AdminLevel.ADMIN), level));

		return true;
	}

	public static Object deleteManager(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, user);
		if(level==Group.AdminLevel.REGULAR)
			throw actx.error(ApiErrorType.CANT_DEMOTE_GROUP_MEMBER, "this user is not a manager in this group");
		if(level==Group.AdminLevel.OWNER)
			throw actx.error(ApiErrorType.CANT_DEMOTE_GROUP_MEMBER, "this user is the creator of this group and can not be removed");

		ctx.getGroupsController().removeAdmin(group, actx.self.user, user);

		return true;
	}

	public static Object reorderManager(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, user);
		if(level==Group.AdminLevel.REGULAR)
			throw actx.error(ApiErrorType.CANT_PROMOTE_GROUP_MEMBER, "this user is not a manager in this group");

		int position=0;
		if(!actx.hasParam("after_user_id"))
			throw actx.paramError("after_user_id is undefined");
		int afterID=actx.optParamInt("after_user_id");
		if(afterID>0){
			List<GroupAdmin> admins=ctx.getGroupsController().getAdmins(group);
			int i=0;
			for(GroupAdmin a:admins){
				if(a.userID==afterID){
					position=i+1;
					break;
				}
				if(a.userID!=user.id)
					i++;
			}
		}

		ctx.getGroupsController().setAdminOrder(group, user, position);

		return true;
	}

	public static Object addLink(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		URI uri;
		try{
			uri=new URI(actx.requireParamString("link"));
		}catch(URISyntaxException x){
			throw actx.paramError("link is not a valid URL");
		}
		GroupLinkParseResult parseResult;
		try{
			parseResult=ctx.getGroupsController().parseLink(uri, actx.self.user, group);
		}catch(UserErrorException x){
			switch(x.getCause()){
				case ObjectNotFoundException onfe -> {
					switch(onfe.getCause()){
						case HttpTimeoutException cause -> throw actx.error(ApiErrorType.REMOTE_FETCH_TIMEOUT);
						case IOException cause -> throw actx.error(ApiErrorType.REMOTE_FETCH_NETWORK_ERROR);
						case null, default -> throw actx.error(ApiErrorType.REMOTE_FETCH_NOT_FOUND);
					}
				}
				case null, default -> throw actx.error(ApiErrorType.REMOTE_FETCH_UNSUPPORTED_TYPE);
			}
		}

		long id=ctx.getGroupsController().addLink(actx.self.user, group, uri, parseResult, actx.optParamString("text", parseResult.title()));
		return ApiUtils.getGroupLink(ctx, actx, ctx.getGroupsController().getLink(group, id));
	}

	public static Object editLink(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		GroupLink link=ctx.getGroupsController().getLink(group, actx.requireParamLongNonZero("link_id"));
		ctx.getGroupsController().updateLinkTitle(group, link, actx.optParamString("text"));
		return true;
	}

	public static Object reorderLink(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		GroupLink link=ctx.getGroupsController().getLink(group, actx.requireParamLongNonZero("link_id"));

		int position=0;
		if(!actx.hasParam("after_link_id"))
			throw actx.paramError("after_link_id is undefined");
		int afterID=actx.optParamInt("after_link_id");
		if(afterID>0){
			List<GroupLink> links=ctx.getGroupsController().getLinks(group);
			int i=0;
			for(GroupLink l:links){
				if(l.id==afterID){
					position=i+1;
					break;
				}
				if(l.id!=link.id)
					i++;
			}
		}

		ctx.getGroupsController().setLinkOrder(group, link, position);

		return true;
	}

	public static Object deleteLink(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		GroupLink link=ctx.getGroupsController().getLink(group, actx.requireParamLongNonZero("link_id"));
		ctx.getGroupsController().deleteLink(group, link);
		return true;
	}

	public static Object create(ApplicationContext ctx, ApiCallContext actx){
		String name=actx.requireParamString("name").strip();
		if(name.isEmpty())
			throw actx.paramError("name is empty");
		String description=actx.optParamString("description");
		String type=actx.requireParamString("type");
		int id;
		if("group".equals(type)){
			id=ctx.getGroupsController().createGroup(actx.self.user, name, description).id;
		}else if("event".equals(type)){
			Instant eventStart=Instant.ofEpochSecond(actx.requireParamLongNonZero("start_date"));
			if(eventStart.isBefore(Instant.now()))
				throw actx.paramError("start_date is in the past");
			if(!isWithinDatabaseLimits(eventStart))
				throw actx.paramError("start_date is too far in the future");
			id=ctx.getGroupsController().createEvent(actx.self.user, name, description, eventStart, null).id;
		}else{
			throw actx.paramError("type must be one of group, event");
		}
		return id;
	}
}
