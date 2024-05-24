package smithereen.sparkext;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.SessionInfo;
import spark.Request;
import spark.Response;

@FunctionalInterface
public interface CSRFSimpleRoute extends CSRFRoute{
	@Override
	default Object handle(Request request, Response response, SessionInfo info, ApplicationContext ctx) throws Exception{
		return handle(request, response, info.account, ctx);
	}

	Object handle(Request request, Response response, Account self, ApplicationContext ctx) throws Exception;
}
