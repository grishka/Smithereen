package smithereen.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import smithereen.ObjectLinkResolver;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Event;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.exceptions.BadRequestException;
import spark.utils.StringUtils;

public class ForeignGroup extends Group implements ForeignActor{

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
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(StringUtils.isNotEmpty(summary))
			summary=Utils.sanitizeHTML(summary);

		adminsForActivityPub=new ArrayList<>();
		JsonElement _attributedTo=obj.get("attributedTo");
		if(_attributedTo!=null){
			if(_attributedTo.isJsonArray()){
				for(JsonElement adm:_attributedTo.getAsJsonArray()){
					doOneAdmin(adm);
				}
			}else{
				doOneAdmin(_attributedTo);
			}
		}
		wall=tryParseURL(optString(obj, "wall"));

		if(attachment!=null && !attachment.isEmpty()){
			for(ActivityPubObject att:attachment){
				if(att instanceof Event ev){
					type=Type.EVENT;
					eventStartTime=ev.startTime;
					eventEndTime=ev.endTime;
				}
			}
		}

		return this;
	}

	private void doOneAdmin(JsonElement _adm){
		if(_adm==null)
			return;
		if(_adm.isJsonObject()){
			JsonObject adm=_adm.getAsJsonObject();
			if(!"Person".equals(optString(adm, "type")))
				return;
			GroupAdmin admin=new GroupAdmin();
			try{
				admin.activityPubUserID=new URI(adm.get("id").getAsString());
			}catch(URISyntaxException x){
				throw new BadRequestException(x);
			}
			admin.title=Objects.requireNonNullElse(optString(adm, "title"), "");
			adminsForActivityPub.add(admin);
		}else if(_adm.isJsonPrimitive()){
			URI adm=tryParseURL(_adm.getAsString());
			if(adm==null)
				return;
			GroupAdmin admin=new GroupAdmin();
			admin.activityPubUserID=adm;
			adminsForActivityPub.add(admin);
		}
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
	public void resolveDependencies(boolean allowFetching, boolean allowStorage) throws SQLException{
		for(GroupAdmin adm:adminsForActivityPub){
			adm.user=ObjectLinkResolver.resolve(adm.activityPubUserID, User.class, allowFetching, allowStorage, false);
		}
	}

	@Override
	public void storeDependencies() throws SQLException{
		for(GroupAdmin adm:adminsForActivityPub){
			if(adm.user instanceof ForeignUser && adm.user.id==0){
				ObjectLinkResolver.storeOrUpdateRemoteObject(adm.user);
			}
		}
	}

	@Override
	public boolean needUpdate(){
		return lastUpdated!=null && System.currentTimeMillis()-lastUpdated.getTime()>24L*60*60*1000;
	}
}
