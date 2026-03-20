package smithereen.routes.admin;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.WebDeltaResponse;
import smithereen.routes.ActivityPubRoutes;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.TextProcessor;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminDebugRoutes{
	public static Object injectActivity(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("admin_debug_inject_activity", req)
				.pageTitle("Inject activity");
	}

	public static Object doInjectActivity(Request req, Response resp, Account self, ApplicationContext ctx){
		String activity=req.queryParams("activity");
		String result=ActivityPubRoutes.dispatchActivity(activity, req, resp, false).toString();
		resp.status(200);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.messageBox("Result", StringUtils.isEmpty(result) ? "Activity submitted, see server log for details." : TextProcessor.escapeHTML(result), lang(req).get("close"));
		}
		return result;
	}
}
