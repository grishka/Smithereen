package smithereen.api.methods;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.groups.GroupAdmin;
import smithereen.model.groups.GroupInvitation;

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
		if(actx.booleanParam("extended")){
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
		ctx.getGroupsController().joinGroup(group, actx.self.user, actx.booleanParam("not_sure"), false, true);
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
		boolean extended=actx.booleanParam("extended");
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
}
