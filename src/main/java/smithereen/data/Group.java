package smithereen.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Event;
import smithereen.jsonld.JLD;
import smithereen.storage.DatabaseUtils;
import spark.utils.StringUtils;

public class Group extends Actor{

	public int id;
	public int memberCount, tentativeMemberCount;
	public Type type=Type.GROUP;
	public Instant eventStartTime, eventEndTime;
	public AccessType accessType;

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
	public URI getWallURL(){
		return Config.localURI("/groups/"+id+"/wall");
	}

	@Override
	public String getTypeAndIdForURL(){
		return "/groups/"+id;
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
			attachment=List.of(event);
		}
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);

		String userURL=activityPubID.toString();
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

		obj.addProperty("members", userURL+"/members");
		contextCollector.addType("members", "sm:members", "@id");
		JsonObject capabilities=new JsonObject();

		if(type==Type.EVENT){
			obj.addProperty("tentativeMembers", userURL+"/tentativeMembers");
			contextCollector.addType("tentativeMembers", "sm:tentativeMembers", "@id");
			capabilities.addProperty("tentativeMembership", true);
			contextCollector.addAlias("tentativeMembership", "sm:tentativeMembership");
		}

		contextCollector.addAlias("accessType", "sm:accessType");
		obj.addProperty("accessType", accessType.toString().toLowerCase());
		obj.addProperty("manuallyApprovesFollowers", accessType!=AccessType.OPEN);

		capabilities.addProperty("acceptsJoins", true);
		obj.add("capabilities", capabilities);
		contextCollector.addAlias("capabilities", "litepub:capabilities");
		contextCollector.addAlias("acceptsJoins", "litepub:acceptsJoins");
		contextCollector.addAlias("litepub", JLD.LITEPUB);

		JsonObject endpoints=obj.getAsJsonObject("endpoints");
		if(accessType!=AccessType.OPEN){
			endpoints.addProperty("actorToken", userURL+"/actorToken");
			contextCollector.addAlias("actorToken", "sm:actorToken");
		}

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
