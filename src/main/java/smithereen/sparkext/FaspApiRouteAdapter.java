package smithereen.sparkext;

import java.nio.charset.StandardCharsets;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.exceptions.UnauthorizedRequestException;
import smithereen.model.fasp.FASPCapability;
import smithereen.model.fasp.FASPProvider;
import spark.Request;
import spark.Response;
import spark.Route;

public class FaspApiRouteAdapter implements Route{
	private final FaspApiRoute route;
	private final FASPCapability requiredCapability;

	public FaspApiRouteAdapter(FaspApiRoute route, FASPCapability requiredCapability){
		this.route=route;
		this.requiredCapability=requiredCapability;
	}

	@Override
	public Object handle(Request req, Response resp) throws Exception{
		ApplicationContext ctx=Utils.context(req);
		FASPProvider provider=ctx.getFaspController().verifyRequestOrResponseSignature(null, req, req.bodyAsBytes());
		if(!provider.confirmed)
			throw new UnauthorizedRequestException("Provider registration not confirmed");
		if(!provider.enabledCapabilities.containsKey(requiredCapability))
			throw new UnauthorizedRequestException("This API call belongs to the '"+requiredCapability.id+"' capability, which is not enabled for this provider");
		resp.type("application/json");
		Object res=route.handle(req, resp, ctx, provider);
		byte[] body=switch(res){
			case byte[] bytes -> bytes;
			case String s -> s.getBytes(StandardCharsets.UTF_8);
			default -> Utils.gson.toJson(res).getBytes(StandardCharsets.UTF_8);
		};
		ctx.getFaspController().signRequestOrResponse(provider, resp, body);
		return body;
	}
}
