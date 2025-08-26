package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Event;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.model.groups.GroupAdmin;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.groups.GroupLink;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import spark.utils.StringUtils;

public class Group extends Actor{

	public int id;
	public int memberCount, tentativeMemberCount;
	public Type type=Type.GROUP;
	public Instant eventStartTime, eventEndTime;
	public AccessType accessType;
	public GroupFeatureState wallState=GroupFeatureState.ENABLED_OPEN;
	public GroupFeatureState photosState=GroupFeatureState.ENABLED_RESTRICTED;
	public GroupFeatureState boardState=GroupFeatureState.ENABLED_RESTRICTED;
	public String website, location;

	public List<GroupAdmin> adminsForActivityPub;

	public static Group fromResultSet(ResultSet res) throws SQLException{
		Group g;
		if(StringUtils.isNotEmpty(res.getString("domain")))
			g=new ForeignGroup();
		else
			g=new Group();
		g.fillFromResultSet(res);
		return g;
	}

	@Override
	public String getType(){
		return "Group";
	}

	@Override
	public int getLocalID(){
		return id;
	}

	@Override
	public int getOwnerID(){
		return -id;
	}

	@Override
	public URI getWallURL(){
		return Config.localURI("/groups/"+id+"/wall");
	}

	@Override
	public URI getWallCommentsURL(){
		return Config.localURI("/groups/"+id+"/wallComments");
	}

	@Override
	public URI getPhotoAlbumsURL(){
		return Config.localURI("/groups/"+id+"/albums");
	}

	public URI getBoardTopicsURL(){
		return Config.localURI("/group/"+id+"/topics");
	}

	@Override
	public String getTypeAndIdForURL(){
		return "/groups/"+id;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public String serializeProfileFields(){
		JsonObject o=new JsonObject();
		if(status!=null)
			o.add("status", Utils.gson.toJsonTree(status));
		o.addProperty("wall", wallState.toString());
		o.addProperty("photos", photosState.toString());
		o.addProperty("board", boardState.toString());
		if(StringUtils.isNotEmpty(website))
			o.addProperty("web", website);
		if(StringUtils.isNotEmpty(location))
			o.addProperty("loc", location);
		return o.toString();
	}

	@Override
	protected boolean canFollowOtherActors(){
		return false;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		super.fillFromResultSet(res);
		id=res.getInt("id");
		name=res.getString("name");
		activityPubID=Config.localURI("/groups/"+id);
		url=Config.localURI(username);
		memberCount=res.getInt("member_count");
		tentativeMemberCount=res.getInt("tentative_member_count");
		summary=res.getString("about");
		eventStartTime=DatabaseUtils.getInstant(res, "event_start_time");
		eventEndTime=DatabaseUtils.getInstant(res, "event_end_time");
		type=Type.values()[res.getInt("type")];
		accessType=AccessType.values()[res.getInt("access_type")];

		if(type==Type.EVENT){
			Event event=new Event();
			event.startTime=eventStartTime;
			event.endTime=eventEndTime;
			attachment=new ArrayList<>(List.of(event));
		}

		String fields=res.getString("profile_fields");
		if(fields!=null){
			JsonObject o=JsonParser.parseString(fields).getAsJsonObject();
			if(o.has("status")){
				status=Utils.gson.fromJson(o.get("status"), ActorStatus.class);
			}
			if(o.has("wall"))
				wallState=Utils.enumValue(o.get("wall").getAsString(), GroupFeatureState.class);
			if(o.has("photos"))
				photosState=Utils.enumValue(o.get("photos").getAsString(), GroupFeatureState.class);
			if(o.has("board"))
				boardState=Utils.enumValue(o.get("board").getAsString(), GroupFeatureState.class);
			website=optString(o, "web");
			location=optString(o, "loc");
		}

		if(StringUtils.isNotEmpty(website)){
			if(attachment==null)
				attachment=new ArrayList<>();
			String url=TextProcessor.escapeHTML(website);
			PropertyValue pv=new PropertyValue();
			pv.name="Website";
			pv.value="<a href=\""+url+"\" rel=\"me\">"+url+"</a>";
			pv.parsed=true;
			attachment.add(pv);
		}
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);

		String groupURL=activityPubID.toString();
		JsonArray ar=new JsonArray();
		for(GroupAdmin admin : adminsForActivityPub){
			JsonObject ja=new JsonObject();
			ja.addProperty("type", "Person");
			ja.addProperty("id", admin.user.activityPubID.toString());
			if(StringUtils.isNotEmpty(admin.title))
				ja.addProperty("title", admin.title);
			ar.add(ja);
		}
		obj.add("attributedTo", ar);

		obj.addProperty("members", groupURL+"/members");
		serializerContext.addType("members", "sm:members", "@id");
		JsonObject capabilities=new JsonObject();

		if(type==Type.EVENT){
			obj.addProperty("tentativeMembers", groupURL+"/tentativeMembers");
			serializerContext.addType("tentativeMembers", "sm:tentativeMembers", "@id");
			capabilities.addProperty("tentativeMembership", true);
			serializerContext.addAlias("tentativeMembership", "sm:tentativeMembership");
		}

		serializerContext.addAlias("accessType", "sm:accessType");
		obj.addProperty("accessType", accessType.toString().toLowerCase());
		serializerContext.addAlias("manuallyApprovesFollowers", "as:manuallyApprovesFollowers");
		obj.addProperty("manuallyApprovesFollowers", accessType!=AccessType.OPEN);

		capabilities.addProperty("acceptsJoins", true);
		obj.add("capabilities", capabilities);
		serializerContext.addAlias("capabilities", "litepub:capabilities");
		serializerContext.addAlias("acceptsJoins", "litepub:acceptsJoins");
		serializerContext.addAlias("litepub", JLD.LITEPUB);
		serializerContext.addAlias("vcard", JLD.VCARD);

		JsonObject endpoints=obj.getAsJsonObject("endpoints");
		if(accessType!=AccessType.OPEN){
			endpoints.addProperty("actorToken", groupURL+"/actorToken");
			serializerContext.addAlias("actorToken", "sm:actorToken");
		}

		serializerContext.addSmIdType("photoAlbums");
		obj.addProperty("photoAlbums", getPhotoAlbumsURL().toString());

		serializerContext.addSmAlias("featureState");
		serializerContext.addSmIdType("board");
		obj.add("featureState", new JsonObjectBuilder()
				.add("wall", wallState.asActivityPubValue())
				.add("photoAlbums", photosState.asActivityPubValue())
				.add("board", boardState.asActivityPubValue())
				.build());

		serializerContext.addSmIdType("boardTopics");
		serializerContext.addSmIdType("pinnedBoardTopics");
		obj.addProperty("boardTopics", groupURL+"/topics");
		obj.addProperty("pinnedBoardTopics", groupURL+"/pinnedTopics");

		List<GroupLink> links=serializerContext.appContext.getGroupsController().getLinks(this);
		if(!links.isEmpty()){
			serializerContext.addSmIdType("links");
			serializerContext.addSmAlias("displayOrder");
			JsonArrayBuilder linkArr=new JsonArrayBuilder();
			for(GroupLink link:links){
				JsonObjectBuilder linkBuilder=new JsonObjectBuilder()
						.add("type", "Link")
						.add("href", link.url.toString())
						.add("id", Config.localURI("/groups/"+id+"/links/"+link.id).toString())
						.add("name", link.title)
						.add("displayOrder", link.displayOrder);
				if(link.image instanceof LocalImage li){
					linkBuilder.add("icon", li.asActivityPubObject(new JsonObject(), serializerContext));
				}
				if(link.object!=null){
					linkBuilder.add("mediaType", ActivityPub.CONTENT_TYPE);
				}
				linkArr.add(linkBuilder);
			}
			obj.add("links", linkArr.build());
		}
		if(StringUtils.isNotEmpty(location))
			obj.addProperty("vcard:Address", location);

		return obj;
	}

	public boolean isEvent(){
		return type==Type.EVENT;
	}

	@Override
	public URI getFollowersURL(){
		String userURL=activityPubID.toString();
		return URI.create(userURL+"/members");
	}

	public enum AdminLevel{
		REGULAR,
		MODERATOR,
		ADMIN,
		OWNER;

		public boolean isAtLeast(AdminLevel lvl){
			return ordinal()>=lvl.ordinal();
		}

		public boolean isAtLeast(String lvl){
			return isAtLeast(valueOf(lvl));
		}
	}

	public enum MembershipState{
		NONE,
		MEMBER,
		TENTATIVE_MEMBER,
		INVITED,
		REQUESTED,
	}

	public enum Type{
		GROUP,
		EVENT
	}

	public enum AccessType{
		/**
		 * Fully public
		 */
		OPEN,
		/**
		 * Public profile, private content, admins accept new joins
		 */
		CLOSED,
		/**
		 * Doesn't acknowledge its existence unless you're a member, invite-only
		 */
		PRIVATE
	}
}
