package smithereen.sparkext;

import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.SessionInfo;
import spark.Request;
import spark.Response;
import spark.Route;

@FunctionalInterface
public interface LoggedInSimpleRoute extends LoggedInRoute{
	@Override
	default Object handle(Request request, Response response, SessionInfo info, ApplicationContext ctx) throws Exception{
		return handle(request, response, info.account, ctx);
	}

	Object handle(Request request, Response response, Account self, ApplicationContext ctx) throws Exception;
}
