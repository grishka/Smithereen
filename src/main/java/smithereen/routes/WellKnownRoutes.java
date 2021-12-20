package smithereen.routes;

import java.sql.SQLException;
import java.util.List;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.WebfingerResponse;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class WellKnownRoutes{
	public static Object webfinger(Request req, Response resp) throws SQLException{
		resp.header("Access-Control-Allow-Origin", "*");
		String resource=req.queryParams("resource");
		if(StringUtils.isNotEmpty(resource) && resource.startsWith("acct:")){
			String[] parts=resource.substring(5).split("@", 2);
			if(parts.length==2 && parts[1].equals(Config.domain)){
				String username=parts[0];
				if("activitypub_service_actor".equals(username)){
					resp.type("application/json");

					WebfingerResponse wfr=new WebfingerResponse();
					wfr.subject="acct:"+username+"@"+Config.domain;
					WebfingerResponse.Link selfLink=new WebfingerResponse.Link();
					selfLink.rel="self";
					selfLink.type="application/activity+json";
					selfLink.href=Config.localURI("/activitypub/serviceActor");
					wfr.links=List.of(selfLink);
					return Utils.gson.toJson(wfr);
				}
				User user=UserStorage.getByUsername(username);
				if(user!=null && !(user instanceof ForeignUser)){
					resp.type("application/json");

					WebfingerResponse wfr=new WebfingerResponse();
					wfr.subject="acct:"+user.username+"@"+Config.domain;
					WebfingerResponse.Link selfLink=new WebfingerResponse.Link();
					selfLink.rel="self";
					selfLink.type="application/activity+json";
					selfLink.href=user.activityPubID;
					WebfingerResponse.Link authLink=new WebfingerResponse.Link();
					authLink.rel="http://ostatus.org/schema/1.0/subscribe";
					authLink.template=Config.localURI("activitypub/externalInteraction?uri")+"={uri}";
					wfr.links=List.of(selfLink, authLink);
					return Utils.gson.toJson(wfr);
				}else if(user==null){
					Group group=GroupStorage.getByUsername(username);
					if(group!=null && !(group instanceof ForeignGroup)){
						resp.type("application/json");

						WebfingerResponse wfr=new WebfingerResponse();
						wfr.subject="acct:"+group.username+"@"+Config.domain;
						WebfingerResponse.Link selfLink=new WebfingerResponse.Link();
						selfLink.rel="self";
						selfLink.type="application/activity+json";
						selfLink.href=group.activityPubID;
						wfr.links=List.of(selfLink);
						return Utils.gson.toJson(wfr);
					}
				}
			}
		}
		resp.status(404);
		return "";
	}

	public static Object nodeInfo(Request req, Response resp){
		resp.type("application/json");
		return new JsonObjectBuilder()
				.add("links",
						new JsonArrayBuilder()
						.add(new JsonObjectBuilder()
								.add("rel", "http://nodeinfo.diaspora.software/ns/schema/2.1")
								.add("href", Config.localURI("activitypub/nodeinfo/2.1").toString()))
						.add(new JsonObjectBuilder()
								.add("rel", "http://nodeinfo.diaspora.software/ns/schema/2.0")
								.add("href", Config.localURI("activitypub/nodeinfo/2.0").toString()))
			).build();
	}
}
