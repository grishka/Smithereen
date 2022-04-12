package smithereen.data;

import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class ForeignUser extends User implements ForeignActor{

	private URI wall, friends, groups;

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
		sharedInbox=tryParseURL(res.getString("ap_shared_inbox"));
		lastUpdated=res.getTimestamp("last_updated");

		EndpointsStorageWrapper ep=Utils.gson.fromJson(res.getString("endpoints"), EndpointsStorageWrapper.class);
		outbox=tryParseURL(ep.outbox);
		followers=tryParseURL(ep.followers);
		following=tryParseURL(ep.following);
		wall=tryParseURL(ep.wall);
		friends=tryParseURL(ep.friends);
		groups=tryParseURL(ep.groups);
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
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(obj.has("firstName") && obj.get("firstName").isJsonPrimitive()){
			firstName=obj.get("firstName").getAsString();
			lastName=optString(obj, "lastName");
			middleName=optString(obj, "middleName");
			maidenName=optString(obj, "maidenName");
		}else{
			firstName=StringUtils.isNotEmpty(name) ? name : username;
		}
		if(obj.has("vcard:bday")){
			birthDate=LocalDate.parse(obj.get("vcard:bday").getAsString());
		}
		if(obj.has("gender")){
			gender=switch(obj.get("gender").getAsString()){
				case "sc:Male", JLD.SCHEMA_ORG+"Male" -> Gender.MALE;
				case "sc:Female", JLD.SCHEMA_ORG+"Female" -> Gender.FEMALE;
				default -> Gender.UNKNOWN;
			};
		}else{
			gender=Gender.UNKNOWN;
		}
		manuallyApprovesFollowers=optBoolean(obj, "manuallyApprovesFollowers");
		if(optBoolean(obj, "supportsFriendRequests")){
			flags|=FLAG_SUPPORTS_FRIEND_REQS;
		}
		if(StringUtils.isNotEmpty(summary))
			summary=Utils.sanitizeHTML(summary);
		wall=tryParseURL(optString(obj, "wall"));
		ensureHostMatchesID(wall, "wall");
		friends=tryParseURL(optString(obj, "friends"));
		ensureHostMatchesID(friends, "friends");
		groups=tryParseURL(optString(obj, "groups"));
		ensureHostMatchesID(groups, "groups");
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
	public URI getFriendsURL(){
		return friends;
	}

	@Override
	public URI getGroupsURL(){
		return groups;
	}

	@Override
	protected NonCachedRemoteImage.Args getAvatarArgs(){
		return new NonCachedRemoteImage.UserProfilePictureArgs(id);
	}

	@Override
	public boolean needUpdate(){
		return lastUpdated!=null && System.currentTimeMillis()-lastUpdated.getTime()>24L*60*60*1000;
	}

	@Override
	public EndpointsStorageWrapper getEndpointsForStorage(){
		EndpointsStorageWrapper ep=super.getEndpointsForStorage();
		if(friends!=null)
			ep.friends=friends.toString();
		if(groups!=null)
			ep.groups=groups.toString();
		return ep;
	}
}
