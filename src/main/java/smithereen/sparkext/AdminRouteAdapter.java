package smithereen.sparkext;

import smithereen.Utils;
import smithereen.model.SessionInfo;
import smithereen.model.UserRole;
import spark.Request;
import spark.Response;
import spark.Route;

/*package*/ class AdminRouteAdapter implements Route{

	private final LoggedInRoute target;
	private final UserRole.Permission permission;
	private final boolean needCSRF;

	public AdminRouteAdapter(LoggedInRoute target, UserRole.Permission permission, boolean needCSRF){
		this.target=target;
		this.permission=permission;
		this.needCSRF=needCSRF;
	}

	@Override
	public Object handle(Request request, Response response) throws Exception{
		if(!Utils.requireAccount(request, response) || (needCSRF && !Utils.verifyCSRF(request, response)))
			return "";
		SessionInfo info=Utils.sessionInfo(request);
		if(info.permissions.hasPermission(permission)){
			return target.handle(request, response, info, Utils.context(request));
		}
		return Utils.wrapError(request, response, "err_access");
	}
}
