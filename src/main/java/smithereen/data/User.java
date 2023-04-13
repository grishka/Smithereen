package smithereen.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.storage.DatabaseUtils;
import spark.utils.StringUtils;

public class User extends Actor{
	public static final long FLAG_SUPPORTS_FRIEND_REQS=1;

	public int id;
	public String firstName;
	public String lastName;
	public String middleName;
	public String maidenName;
	public LocalDate birthDate;
	public Gender gender;
	public long flags;

	// additional profile fields
	public boolean manuallyApprovesFollowers;

	public String getFullName(){
		if(StringUtils.isEmpty(lastName))
			return firstName.isEmpty() ? username : firstName;
		return firstName+" "+lastName;
	}

	public String getCompleteName(){
		if(StringUtils.isEmpty(middleName) && StringUtils.isEmpty(maidenName))
			return getFullName();
		StringBuilder sb=new StringBuilder(firstName);
		if(StringUtils.isNotEmpty(middleName)){
			sb.append(' ');
			sb.append(middleName);
		}
		if(StringUtils.isNotEmpty(lastName)){
			sb.append(' ');
			sb.append(lastName);
		}
		if(StringUtils.isNotEmpty(maidenName)){
			sb.append(" (");
			sb.append(maidenName);
			sb.append(')');
		}
		return sb.toString();
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("User{");
		sb.append(super.toString());
		sb.append("id=");
		sb.append(id);
		if(firstName!=null){
			sb.append(", firstName='");
			sb.append(firstName);
			sb.append('\'');
		}
		if(lastName!=null){
			sb.append(", lastName='");
			sb.append(lastName);
			sb.append('\'');
		}
		if(username!=null){
			sb.append(", username='");
			sb.append(username);
			sb.append('\'');
		}
		if(birthDate!=null){
			sb.append(", birthDate=");
			sb.append(birthDate);
		}
		if(gender!=null){
			sb.append(", gender=");
			sb.append(gender);
		}
		if(publicKey!=null){
			sb.append(", publicKey=");
			sb.append(publicKey);
		}
		if(privateKey!=null){
			sb.append(", privateKey=");
			sb.append(privateKey);
		}
		sb.append('}');
		return sb.toString();
	}

	public static User fromResultSet(ResultSet res) throws SQLException{
		if(res.getString("domain").length()>0)
			return ForeignUser.fromResultSet(res);
		User user=new User();
		user.fillFromResultSet(res);
		return user;
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		super.fillFromResultSet(res);
		id=res.getInt("id");
		firstName=res.getString("fname");
		lastName=res.getString("lname");
		middleName=res.getString("middle_name");
		maidenName=res.getString("maiden_name");
		birthDate=DatabaseUtils.getLocalDate(res, "bdate");
		gender=Gender.valueOf(res.getInt("gender"));
		summary=res.getString("about");
		flags=res.getLong("flags");


		activityPubID=Config.localURI("/users/"+id);
		url=Config.localURI(username);

		String fields=res.getString("profile_fields");
		if(StringUtils.isNotEmpty(fields)){
			JsonObject o=JsonParser.parseString(fields).getAsJsonObject();
			manuallyApprovesFollowers=optBoolean(o, "manuallyApprovesFollowers");
			if(o.has("custom")){
				if(attachment==null)
					attachment=new ArrayList<>();
				JsonArray custom=o.getAsJsonArray("custom");
				for(JsonElement _fld:custom){
					JsonObject fld=_fld.getAsJsonObject();
					PropertyValue pv=new PropertyValue();
					pv.name=fld.get("n").getAsString();
					pv.value=fld.get("v").getAsString();
					attachment.add(pv);
				}
			}
		}
	}

	@Override
	public String getType(){
		return "Person";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		name=getFullName();
		obj=super.asActivityPubObject(obj, contextCollector);

		obj.addProperty("firstName", firstName);
		if(StringUtils.isNotEmpty(lastName)){
			obj.addProperty("lastName", lastName);
		}
		if(StringUtils.isNotEmpty(middleName)){
			obj.addProperty("middleName", middleName);
		}
		if(StringUtils.isNotEmpty(maidenName)){
			obj.addProperty("maidenName", maidenName);
		}
		if(birthDate!=null){
			obj.addProperty("vcard:bday", birthDate.toString());
		}
		switch(gender){
			case MALE -> obj.addProperty("gender", "http://schema.org#Male");
			case FEMALE -> obj.addProperty("gender", "http://schema.org#Female");
			case OTHER -> obj.addProperty("gender", "http://schema.org#Other");
		}

		obj.addProperty("supportsFriendRequests", true);
		obj.addProperty("friends", getFriendsURL().toString());
		obj.addProperty("groups", getGroupsURL().toString());

		contextCollector.addAlias("sc", JLD.SCHEMA_ORG);
		contextCollector.addAlias("firstName", "sc:givenName");
		contextCollector.addAlias("lastName", "sc:familyName");
		contextCollector.addAlias("middleName", "sc:additionalName");
		contextCollector.addType("gender", "sc:gender", "sc:GenderType");
		contextCollector.addAlias("sm", JLD.SMITHEREEN);
		contextCollector.addAlias("maidenName", "sm:maidenName");
		contextCollector.addType("friends", "sm:friends", "@id");
		contextCollector.addType("groups", "sm:groups", "@id");
		contextCollector.addAlias("vcard", JLD.VCARD);

		JsonObject capabilities=new JsonObject();
		capabilities.addProperty("supportsFriendRequests", true);
		obj.add("capabilities", capabilities);
		contextCollector.addAlias("capabilities", "litepub:capabilities");
		contextCollector.addAlias("supportsFriendRequests", "sm:supportsFriendRequests");
		contextCollector.addAlias("litepub", JLD.LITEPUB);

		return obj;
	}

	@Override
	public boolean equals(Object other){
		if(other==null)
			return false;
		if(other instanceof User){
			return ((User) other).id==id && ((User) other).activityPubID.equals(activityPubID);
		}
		return false;
	}

	public String serializeProfileFields(){
		JsonObject o=new JsonObject();
		if(manuallyApprovesFollowers)
			o.addProperty("manuallyApprovesFollowers", true);
		JsonArray custom=null;
		if(attachment!=null){
			for(ActivityPubObject att:attachment){
				if(att instanceof PropertyValue){
					if(custom==null)
						custom=new JsonArray();
					PropertyValue pv=(PropertyValue) att;
					JsonObject fld=new JsonObject();
					fld.addProperty("n", pv.name);
					fld.addProperty("v", pv.value);
					custom.add(fld);
				}
			}
		}
		if(custom!=null)
			o.add("custom", custom);
		return o.toString();
	}

	public boolean supportsFriendRequests(){
		return true;
	}

	public String getNameForReply(){
		if(StringUtils.isNotEmpty(firstName))
			return firstName;
		return username;
	}

	@Override
	public int getLocalID(){
		return id;
	}

	@Override
	public int getOwnerID(){
		return id;
	}

	@Override
	public URI getWallURL(){
		return Config.localURI("/users/"+id+"/wall");
	}

	public URI getFriendsURL(){
		return Config.localURI("/users/"+id+"/friends");
	}

	public URI getGroupsURL(){
		return Config.localURI("/users/"+id+"/groups");
	}

	@Override
	public String getTypeAndIdForURL(){
		return "/users/"+id;
	}

	@Override
	public String getName(){
		return getFullName();
	}

	// for templates
	public Map<String, Object> getFirstAndGender(){
		return Map.of("first", firstName, "gender", gender==null ? Gender.UNKNOWN : gender);
	}

	public Map<String, Object> getFirstLastAndGender(){
		return Map.of("first", firstName, "last", lastName==null ? "" : lastName, "gender", gender==null ? Gender.UNKNOWN : gender);
	}

	public enum Gender{
		UNKNOWN,
		MALE,
		FEMALE,
		OTHER;

		public static Gender valueOf(int v){
			return switch(v){
				case 0 -> UNKNOWN;
				case 1 -> MALE;
				case 2 -> FEMALE;
				case 3 -> OTHER;
				default -> throw new IllegalStateException("Unexpected value: "+v);
			};
		}
	}
}
