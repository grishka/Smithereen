package smithereen.api.methods;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;

public class UsersMethods{
	private static final Pattern ID_PATTERN=Pattern.compile("^\\d+$");

	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		Set<String> userIDs;
		if(actx.hasParam("user_id")){
			userIDs=Set.of(actx.optParamString("user_id"));
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
}
