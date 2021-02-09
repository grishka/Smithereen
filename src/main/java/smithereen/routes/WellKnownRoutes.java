package smithereen.routes;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import smithereen.Config;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;
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
				User user=UserStorage.getByUsername(username);
				if(user!=null && !(user instanceof ForeignUser)){
					resp.type("application/json");
					JSONObject root=new JSONObject();
					root.put("subject", "acct:"+user.username+"@"+Config.domain);

					JSONObject selfLink=new JSONObject();
					selfLink.put("rel", "self");
					selfLink.put("type", "application/activity+json");
					selfLink.put("href", user.activityPubID);
					JSONObject authLink=new JSONObject();
					authLink.put("rel", "http://ostatus.org/schema/1.0/subscribe");
					authLink.put("template", Config.localURI("activitypub/externalInteraction?uri")+"={uri}");

					root.put("links", Arrays.asList(selfLink, authLink));
					return root;
				}else if(user==null){
					Group group=GroupStorage.getByUsername(username);
					if(group!=null && !(group instanceof ForeignGroup)){
						resp.type("application/json");
						JSONObject root=new JSONObject();
						root.put("subject", "acct:"+group.username+"@"+Config.domain);

						JSONObject selfLink=new JSONObject();
						selfLink.put("rel", "self");
						selfLink.put("type", "application/activity+json");
						selfLink.put("href", group.activityPubID);
						root.put("links", Collections.singletonList(selfLink));
						return root;
					}
				}
			}
		}
		resp.status(404);
		return "";
	}

	public static Object nodeInfo(Request req, Response resp){
		resp.type("application/json");
		JSONObject link=new JSONObject();
		link.put("rel", "http://nodeinfo.diaspora.software/ns/schema/2.0");
		link.put("href", Config.localURI("activitypub/nodeinfo/2.0"));
		JSONObject root=new JSONObject();
		root.put("links", Collections.singletonList(link));
		return root;
	}
}
