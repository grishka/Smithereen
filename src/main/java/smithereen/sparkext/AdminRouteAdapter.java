package smithereen.sparkext;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.SessionInfo;
import spark.Request;
import spark.Response;
import spark.Route;

/*package*/ class AdminRouteAdapter implements Route{

	private final LoggedInRoute target;
	private final Account.AccessLevel requiredAccessLevel;
	private final boolean needCSRF;

	public AdminRouteAdapter(LoggedInRoute target, Account.AccessLevel requiredAccessLevel, boolean needCSRF){
		this.target=target;
		this.requiredAccessLevel=requiredAccessLevel;
		this.needCSRF=needCSRF;
	}

	@Override
	public Object handle(Request request, Response response) throws Exception{
		if(!Utils.requireAccount(request, response) || (needCSRF && !Utils.verifyCSRF(request, response)))
			return "";
		SessionInfo info=Utils.sessionInfo(request);
		return handle(request, response, info.account, Utils.context(request));
	}

	private Object handle(Request req, Response resp, Account self, ApplicationContext ctx) throws Exception{
		if(self.accessLevel.ordinal()<requiredAccessLevel.ordinal()){
			return Utils.wrapError(req, resp, "err_access");
		}
		return target.handle(req, resp, self, ctx);
	}
}
