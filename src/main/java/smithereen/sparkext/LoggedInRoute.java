package smithereen.sparkext;

import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.SessionInfo;
import spark.Request;
import spark.Response;
import spark.Route;

@FunctionalInterface
public interface LoggedInRoute extends Route{
	@Override
	default Object handle(Request request, Response response) throws Exception{
		if(!Utils.requireAccount(request, response))
			return "";
		SessionInfo info=Utils.sessionInfo(request);
		return handle(request, response, info.account);
	}

	Object handle(Request request, Response response, Account self) throws Exception;
}
