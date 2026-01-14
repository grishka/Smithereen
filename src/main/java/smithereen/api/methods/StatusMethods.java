package smithereen.api.methods;

import com.google.gson.annotations.JsonAdapter;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.api.ApiCallContext;
import smithereen.model.Group;
import smithereen.model.apps.ClientAppPermission;
import smithereen.util.SerializeNullAdapter;

public class StatusMethods{
	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		record TextStatus(@JsonAdapter(value=SerializeNullAdapter.class, nullSafe=false) String text){}
		Actor owner=ApiUtils.getOwnerOrSelf(ctx, actx, "owner_id");
		if(owner instanceof Group g){
			ctx.getPrivacyController().enforceUserAccessToGroupProfile(actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null, g);
		}
		return new TextStatus(owner.status==null ? null : owner.status.text());
	}

	public static Object set(ApplicationContext ctx, ApiCallContext actx){
		String text=actx.optParamString("text");
		if(actx.hasParam("group_id")){
			actx.requirePermission(ClientAppPermission.GROUPS_WRITE);
			Group g=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
			ctx.getGroupsController().updateStatus(actx.self.user, g, text);
		}else{
			actx.requirePermission(ClientAppPermission.ACCOUNT_WRITE);
			ctx.getUsersController().updateStatus(actx.self.user, text);
		}
		return true;
	}
}
