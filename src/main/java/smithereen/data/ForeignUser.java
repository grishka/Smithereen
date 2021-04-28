package smithereen.data;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;

import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class ForeignUser extends User{

	private URI wall;

	public static ForeignUser fromResultSet(ResultSet res) throws SQLException{
		ForeignUser user=new ForeignUser();
		user.fillFromResultSet(res);
		return user;
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
		following=tryParseURL(res.getString("ap_following"));
		lastUpdated=res.getTimestamp("last_updated");
		wall=tryParseURL(res.getString("ap_wall"));
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("ForeignUser{");
		sb.append(super.toString());
		if(domain!=null){
			sb.append("domain='");
			sb.append(domain);
			sb.append('\'');
		}
		if(activityPubID!=null){
			sb.append(", activityPubID='");
			sb.append(activityPubID);
			sb.append('\'');
		}
		if(url!=null){
			sb.append(", url='");
			sb.append(url);
			sb.append('\'');
		}
		if(inbox!=null){
			sb.append(", inbox='");
			sb.append(inbox);
			sb.append('\'');
		}
		if(outbox!=null){
			sb.append(", outbox='");
			sb.append(outbox);
			sb.append('\'');
		}
		if(sharedInbox!=null){
			sb.append(", sharedInbox='");
			sb.append(sharedInbox);
			sb.append('\'');
		}
		if(followers!=null){
			sb.append(", followers='");
			sb.append(followers);
			sb.append('\'');
		}
		if(following!=null){
			sb.append(", following='");
			sb.append(following);
			sb.append('\'');
		}
		if(lastUpdated!=null){
			sb.append(", lastUpdated=");
			sb.append(lastUpdated);
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		if(obj.has("firstName")){
			firstName=obj.getString("firstName");
			lastName=obj.optString("lastName", null);
			middleName=obj.optString("middleName", null);
			maidenName=obj.optString("maidenName", null);
		}else{
			firstName=name!=null ? name : username;
		}
		if(obj.has("birthDate")){
			birthDate=Date.valueOf(obj.getString("birthDate"));
		}
		if(obj.has("gender")){
			switch(obj.getString("gender")){
				case "sc:Male":
				case JLD.SCHEMA_ORG+"Male":
					gender=Gender.MALE;
					break;
				case "sc:Female":
				case JLD.SCHEMA_ORG+"Female":
					gender=Gender.FEMALE;
					break;
				default:
					gender=Gender.UNKNOWN;
					break;
			}
		}else{
			gender=Gender.UNKNOWN;
		}
		manuallyApprovesFollowers=obj.optBoolean("manuallyApprovesFollowers", false);
		if(obj.optBoolean("supportsFriendRequests", false)){
			flags|=FLAG_SUPPORTS_FRIEND_REQS;
		}
		if(StringUtils.isNotEmpty(summary))
			summary=Utils.sanitizeHTML(summary);
		wall=tryParseURL(obj.optString("wall"));
		return this;
	}

	@Override
	public String getFullUsername(){
		return username+"@"+domain;
	}

	@Override
	public URI getFollowersURL(){
		return followers;
	}

	@Override
	public boolean supportsFriendRequests(){
		return (flags & FLAG_SUPPORTS_FRIEND_REQS)==FLAG_SUPPORTS_FRIEND_REQS;
	}

	@Override
	public URI getWallURL(){
		return wall;
	}

	@Override
	protected NonCachedRemoteImage.Args getAvatarArgs(){
		return new NonCachedRemoteImage.UserProfilePictureArgs(id);
	}
}
