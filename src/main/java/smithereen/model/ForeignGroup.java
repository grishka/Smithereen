package smithereen.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Event;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.exceptions.BadRequestException;
import smithereen.model.groups.GroupFeatureState;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public class ForeignGroup extends Group implements ForeignActor{

	private URI wall, photoAlbums, wallComments, boardTopics;
	public URI actorTokenEndpoint;
	public URI members;
	public URI tentativeMembers;
	public EnumSet<Capability> capabilities=EnumSet.noneOf(ForeignGroup.Capability.class);

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
		sharedInbox=tryParseURL(res.getString("ap_shared_inbox"));
		lastUpdated=DatabaseUtils.getInstant(res, "last_updated");
		Utils.deserializeEnumSet(capabilities, ForeignGroup.Capability.class, res.getLong("flags"));

		EndpointsStorageWrapper ep=Utils.gson.fromJson(res.getString("endpoints"), EndpointsStorageWrapper.class);
		outbox=tryParseURL(ep.outbox);
		followers=tryParseURL(ep.followers);
		wall=tryParseURL(ep.wall);
		wallComments=tryParseURL(ep.wallComments);
		actorTokenEndpoint=tryParseURL(ep.actorToken);
		collectionQueryEndpoint=tryParseURL(ep.collectionQuery);
		members=tryParseURL(ep.groupMembers);
		tentativeMembers=tryParseURL(ep.tentativeGroupMembers);
		photoAlbums=tryParseURL(ep.photoAlbums);
		boardTopics=tryParseURL(ep.boardTopics);
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(StringUtils.isNotEmpty(summary))
			summary=TextProcessor.sanitizeHTML(summary);

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
		if(wall==null)
			wall=tryParseURL(optString(obj, "sm:wall"));
		ensureHostMatchesID(wall, "wall");
		if(wall!=null){
			wallComments=tryParseURL(optString(obj, "wallComments"));
			ensureHostMatchesID(wallComments, "wallComments");
		}

		if(attachment!=null && !attachment.isEmpty()){
			for(ActivityPubObject att:attachment){
				if(att instanceof Event ev){
					type=Type.EVENT;
					eventStartTime=ev.startTime;
					eventEndTime=ev.endTime;
				}
			}
		}

		if(obj.has("capabilities")){
			JsonObject caps=obj.getAsJsonObject("capabilities");
			if(optBoolean(caps, "acceptsJoins"))
				capabilities.add(Capability.JOIN_LEAVE_ACTIVITIES);
			if(optBoolean(caps, "tentativeMembership"))
				capabilities.add(Capability.TENTATIVE_MEMBERSHIP);
		}

		String access=optString(obj, "accessType");
		if(access!=null){
			try{
				accessType=AccessType.valueOf(access.toUpperCase());
			}catch(IllegalArgumentException ignore){}
		}
		if(accessType==null){
			accessType=optBoolean(obj, "manuallyApprovesFollowers") ? AccessType.CLOSED : AccessType.OPEN;
		}
		JsonObject endpoints=obj.getAsJsonObject("endpoints");
		actorTokenEndpoint=tryParseURL(optString(endpoints, "actorToken"));
		ensureHostMatchesID(actorTokenEndpoint, "endpoints.actorToken");
		members=tryParseURL(optString(obj, "members"));
		ensureHostMatchesID(members, "members");
		tentativeMembers=tryParseURL(optString(obj, "tentativeMembers"));
		ensureHostMatchesID(tentativeMembers, "tentativeMembers");
		photoAlbums=tryParseURL(optString(obj, "photoAlbums"));
		ensureHostMatchesID(photoAlbums, "photoAlbums");
		boardTopics=tryParseURL(optString(obj, "boardTopics"));
		ensureHostMatchesID(boardTopics, "boardTopics");

		JsonObject featureState=optObject(obj, "featureState");
		if(featureState!=null){
			wallState=GroupFeatureState.fromActivityPubValue(optString(featureState, "wall"), wall==null ? GroupFeatureState.DISABLED : GroupFeatureState.ENABLED_OPEN);
			photosState=GroupFeatureState.fromActivityPubValue(optString(featureState, "photoAlbums"), photoAlbums==null ? GroupFeatureState.DISABLED : GroupFeatureState.ENABLED_RESTRICTED);
			boardState=GroupFeatureState.fromActivityPubValue(optString(featureState, "board"), boardTopics==null ? GroupFeatureState.DISABLED : GroupFeatureState.ENABLED_RESTRICTED);
		}else{
			if(wall==null)
				wallState=GroupFeatureState.DISABLED;
			if(photoAlbums==null)
				photosState=GroupFeatureState.DISABLED;
			if(boardTopics==null)
				boardState=GroupFeatureState.DISABLED;
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
	public URI getWallCommentsURL(){
		return wallComments;
	}

	@Override
	public URI getPhotoAlbumsURL(){
		return photoAlbums;
	}

	@Override
	public URI getBoardTopicsURL(){
		return boardTopics;
	}

	@Override
	public boolean needUpdate(){
		return lastUpdated!=null && System.currentTimeMillis()-lastUpdated.toEpochMilli()>24L*60*60*1000;
	}

	@Override
	public EndpointsStorageWrapper getEndpointsForStorage(){
		EndpointsStorageWrapper ep=super.getEndpointsForStorage();
		if(actorTokenEndpoint!=null)
			ep.actorToken=actorTokenEndpoint.toString();
		if(members!=null)
			ep.groupMembers=members.toString();
		if(tentativeMembers!=null)
			ep.tentativeGroupMembers=tentativeMembers.toString();
		if(boardTopics!=null)
			ep.boardTopics=boardTopics.toString();
		return ep;
	}

	public URI getMembersCollection(){
		return members!=null ? members : followers;
	}

	public boolean hasCapability(Capability cap){
		return capabilities.contains(cap);
	}

	// for use from templates
	public boolean hasCapability(String cap){
		return hasCapability(Capability.valueOf(cap));
	}

	public enum Capability{
		/**
		 * Supports Join{Group} and Leave{Group} instead of Follow{Group}/Undo{Follow{Group}}
		 */
		JOIN_LEAVE_ACTIVITIES,
		/**
		 * Supports tentative memberships (sm:TentativeJoin for joining and TentativeAccept for accepting invites)
		 */
		TENTATIVE_MEMBERSHIP
	}
}
