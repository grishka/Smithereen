package smithereen.api.methods;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiPaginatedList;
import smithereen.controllers.GroupsController;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.apps.ClientAppPermission;

public class GroupsMethods{
	private static final Pattern ID_PATTERN=Pattern.compile("^\\d+$");

	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		Set<String> groupIDs;
		if(actx.hasParam("group_id")){
			groupIDs=Set.of(actx.optParamString("group_id"));
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
}
