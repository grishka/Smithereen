package smithereen.api.methods;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;

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
}
