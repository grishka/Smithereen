package smithereen.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.Actor;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class Group extends Actor{

	public int id;
	public int memberCount;

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
		summary=res.getString("about");
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

		obj.addProperty("members", userURL+"/followers");
		contextCollector.addType("members", "sm:members", "@id");

		JsonObject capabilities=new JsonObject();
		capabilities.addProperty("acceptsJoins", true);
		obj.add("capabilities", capabilities);
		contextCollector.addAlias("capabilities", "litepub:capabilities");
		contextCollector.addAlias("litepub", JLD.LITEPUB);

		return obj;
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
	}

	public enum Type{
		GROUP,
		EVENT
	}
}
