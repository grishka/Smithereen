package smithereen.sparkext;

import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.debug.DebugLog;
import smithereen.model.Account;
import smithereen.model.SessionInfo;
import spark.Request;
import spark.Response;
import spark.Route;

@FunctionalInterface
public interface LoggedInRoute extends Route{
	@Override
	default Object handle(Request request, Response response) throws Exception{
		if(Config.DEBUG){
			DebugLog.get().setRouteMatched();
		}
		if(!Utils.requireAccount(request, response))
			return "";
		SessionInfo info=Objects.requireNonNull(Utils.sessionInfo(request));
		return handle(request, response, info, Utils.context(request));
	}

	Object handle(Request request, Response response, SessionInfo info, ApplicationContext ctx) throws Exception;
}
