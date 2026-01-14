package smithereen.api.methods;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.model.PaginatedList;
import smithereen.model.User;

public class UsersMethods{
	private static final Pattern ID_PATTERN=Pattern.compile("^\\d+$");

	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		Set<String> userIDs;
		if(actx.hasParam("user_id")){
			userIDs=Set.of(Objects.requireNonNull(actx.optParamString("user_id")));
		}else if(actx.hasParam("user_ids")){
			userIDs=actx.optCommaSeparatedStringSet("user_ids");
			if(userIDs.size()>1000)
				userIDs=userIDs.stream().limit(1000).collect(Collectors.toSet());
		}else if(actx.self!=null){
			userIDs=Set.of(actx.self.user.id+"");
		}else{
			throw actx.paramError("user_ids is undefined");
		}

		Set<Integer> needUsers=userIDs.stream().filter(ID_PATTERN.asMatchPredicate()).map(Utils::safeParseInt).collect(Collectors.toSet());
		if(needUsers.size()!=userIDs.size()){ // There are usernames
			needUsers=new HashSet<>(needUsers);
			needUsers.addAll(ctx.getUsersController().getUserIDsByUsernames(userIDs.stream().filter(ID_PATTERN.asMatchPredicate().negate()).collect(Collectors.toSet())).values());
		}

		return ApiUtils.getUsers(needUsers, ctx, actx);
	}

	public static Object getFollowers(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");
		PaginatedList<Integer> ids=ctx.getFriendsController().getFollowerIDs(user, actx.getOffset(), actx.getCount(100, 1000));
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(ids.total, ApiUtils.getUsers(ids.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(ids);
		}
	}

	public static Object getSubscriptions(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");
		PaginatedList<Integer> ids=ctx.getFriendsController().getFollowIDs(user, actx.getOffset(), actx.getCount(100, 1000));
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(ids.total, ApiUtils.getUsers(ids.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(ids);
		}
	}

	public static Object search(ApplicationContext ctx, ApiCallContext actx){
		String query=actx.requireParamString("q").trim();
		if(query.isEmpty())
			throw actx.paramError("q is empty");
		PaginatedList<Integer> ids=ctx.getSearchController().searchUserIDs(query, actx.self.user, actx.getCount(100, 100));
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(ids.total, ApiUtils.getUsers(ids.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(ids);
		}
	}
}
