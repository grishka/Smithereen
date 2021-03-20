package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import smithereen.ObjectLinkResolver;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import spark.utils.StringUtils;

public class ForeignGroup extends Group{

	private URI wall;

	public static ForeignGroup fromResultSet(ResultSet res) throws SQLException{
		ForeignGroup g=new ForeignGroup();
		g.fillFromResultSet(res);
		return g;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		super.fillFromResultSet(res);
		domain=res.getString("domain");
		activityPubID=tryParseURL(res.getString("ap_id"));
		url=tryParseURL(res.getString("ap_url"));
		inbox=tryParseURL(res.getString("ap_inbox"));
		outbox=tryParseURL(res.getString("ap_outbox"));
		sharedInbox=tryParseURL(res.getString("ap_shared_inbox"));
		followers=tryParseURL(res.getString("ap_followers"));
		lastUpdated=res.getTimestamp("last_updated");
		wall=tryParseURL(res.getString("ap_wall"));
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		if(StringUtils.isNotEmpty(summary))
			summary=Utils.sanitizeHTML(summary);

		adminsForActivityPub=new ArrayList<>();
		Object _attributedTo=obj.opt("attributedTo");
		if(_attributedTo instanceof JSONArray){
			JSONArray attributedTo=(JSONArray) _attributedTo;
			for(int i=0;i<attributedTo.length();i++){
				JSONObject adm=attributedTo.optJSONObject(i);
				doOneAdmin(adm);
			}
		}else if(_attributedTo instanceof JSONObject){
			doOneAdmin((JSONObject) _attributedTo);
		}

		return this;
	}

	private void doOneAdmin(JSONObject adm) throws Exception{
		if(adm==null)
			return;
		if(!"Person".equals(adm.optString("type")))
			return;
		GroupAdmin admin=new GroupAdmin();
		admin.activityPubUserID=new URI(adm.getString("id"));
		admin.title=adm.optString("title", "");
		adminsForActivityPub.add(admin);
	}

	@Override
	public String getFullUsername(){
		return username+"@"+domain;
	}

	@Override
	protected NonCachedRemoteImage.Args getAvatarArgs(){
		return new NonCachedRemoteImage.GroupProfilePictureArgs(id);
	}

	@Override
	public URI getWallURL(){
		return wall;
	}

	@Override
	public void resolveDependencies(boolean allowFetching) throws SQLException{
		for(GroupAdmin adm:adminsForActivityPub){
			adm.user=ObjectLinkResolver.resolve(adm.activityPubUserID, User.class, allowFetching, false);
		}
	}
}
