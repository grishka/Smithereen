package smithereen.routes;

import java.sql.SQLException;

import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.model.api.ApiInstance;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class MastodonApiRoutes{
	public static Object instance(Request req, Response resp) throws SQLException{
		ApiInstance inst=new ApiInstance();
		inst.uri=Config.domain;
		inst.title=Config.getServerDisplayName();
		inst.shortDescription=Config.serverShortDescription;
		inst.description=Config.serverDescription;
		inst.email=Config.serverAdminEmail;
		inst.version="Smithereen "+BuildInfo.VERSION;
		inst.registrations=Config.signupMode==Config.SignupMode.OPEN;
		inst.invitesEnabled=Config.signupMode==Config.SignupMode.INVITE_ONLY;
		inst.approvalRequired=Config.signupMode==Config.SignupMode.MANUAL_APPROVAL;
		inst.stats.statusCount=PostStorage.getLocalPostCount(false);
		inst.stats.userCount=UserStorage.getLocalUserCount();
		inst.stats.domainCount=UserStorage.getPeerDomainCount();
		return inst;
	}

	public static Object instancePeers(Request req, Response resp) throws SQLException{
		return UserStorage.getPeerDomains();
	}
}
