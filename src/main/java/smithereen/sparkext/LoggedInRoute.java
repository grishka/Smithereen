package smithereen.sparkext;

import java.util.Objects;

import smithereen.ApplicationContext;
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
		SessionInfo info=Objects.requireNonNull(Utils.sessionInfo(request));
		return handle(request, response, info.account, Utils.context(request));
	}

	Object handle(Request request, Response response, Account self, ApplicationContext ctx) throws Exception;
}
