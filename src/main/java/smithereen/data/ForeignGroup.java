package smithereen.data;

import org.json.JSONObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

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
		return this;
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
}
