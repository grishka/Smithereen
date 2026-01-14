package smithereen.routes;

import smithereen.ApplicationContext;
import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.model.api.MastodonApiInstance;
import spark.Request;
import spark.Response;

import static smithereen.Utils.context;

public class MastodonApiRoutes{
	public static Object instance(Request req, Response resp){
		ApplicationContext ctx=context(req);
		MastodonApiInstance inst=new MastodonApiInstance();
		inst.uri=Config.domain;
		inst.title=Config.getServerDisplayName();
		inst.shortDescription=Config.serverShortDescription;
		inst.description=Config.serverDescription;
		inst.email=Config.serverAdminEmail;
		inst.version="Smithereen "+BuildInfo.VERSION;
		inst.registrations=Config.signupMode==Config.SignupMode.OPEN;
		inst.invitesEnabled=Config.signupMode==Config.SignupMode.INVITE_ONLY;
		inst.approvalRequired=Config.signupMode==Config.SignupMode.MANUAL_APPROVAL;
		inst.stats.statusCount=ctx.getWallController().getLocalPostCount(false);
		inst.stats.userCount=ctx.getUsersController().getLocalUserCount();
		inst.stats.domainCount=ctx.getModerationController().getPeerDomainCount();
		return inst;
	}

	public static Object instancePeers(Request req, Response resp){
		return context(req).getModerationController().getPeerDomains();
	}
}
