package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.storage.MediaCache;
import spark.utils.StringUtils;

public class User extends Actor{
	public static final long FLAG_SUPPORTS_FRIEND_REQS=1;


	public int id;
	public String firstName;
	public String lastName;
	public String middleName;
	public String maidenName;
	public java.sql.Date birthDate;
	public Gender gender;
	public long flags;

	// additional profile fields
	public boolean manuallyApprovesFollowers;

	public String getFullName(){
		if(StringUtils.isEmpty(lastName))
			return firstName.isEmpty() ? ('@'+username) : firstName;
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
		birthDate=res.getDate("bdate");
		gender=Gender.valueOf(res.getInt("gender"));
		summary=res.getString("about");
		flags=res.getLong("flags");


		activityPubID=Config.localURI("/users/"+id);
		url=Config.localURI(username);

		String fields=res.getString("profile_fields");
		if(StringUtils.isNotEmpty(fields)){
			JSONObject o=new JSONObject(fields);
			manuallyApprovesFollowers=o.optBoolean("manuallyApprovesFollowers", false);
			if(o.has("custom")){
				if(attachment==null)
					attachment=new ArrayList<>();
				JSONArray custom=o.getJSONArray("custom");
				for(int i=0;i<custom.length();i++){
					JSONObject fld=custom.getJSONObject(i);
					PropertyValue pv=new PropertyValue();
					pv.name=fld.getString("n");
					pv.value=fld.getString("v");
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
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		name=getFullName();
		JSONObject root=super.asActivityPubObject(obj, contextCollector);

		root.put("firstName", firstName);
		if(StringUtils.isNotEmpty(lastName)){
			root.put("lastName", lastName);
		}
		if(StringUtils.isNotEmpty(middleName)){
			root.put("middleName", middleName);
		}
		if(StringUtils.isNotEmpty(maidenName)){
			root.put("maidenName", maidenName);
		}
		if(birthDate!=null){
			root.put("birthDate", birthDate.toString());
		}
		switch(gender){
			case MALE -> root.put("gender", "http://schema.org#Male");
			case FEMALE -> root.put("gender", "http://schema.org#Female");
		}

		root.put("supportsFriendRequests", true);

		contextCollector.addAlias("sc", JLD.SCHEMA_ORG);
		contextCollector.addType("firstName", "sc:givenName", "sc:Text");
		contextCollector.addType("lastName", "sc:familyName", "sc:Text");
		contextCollector.addType("middleName", "sc:additionalName", "sc:Text");
		contextCollector.addType("gender", "sc:gender", "sc:GenderType");
		contextCollector.addType("birthDate", "sc:birthDate", "sc:Date");
		contextCollector.addAlias("sm", JLD.SMITHEREEN);
		contextCollector.addAlias("supportsFriendRequests", "sm:supportsFriendRequests");
		contextCollector.addAlias("maidenName", "sm:maidenName");

		return root;
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
		JSONObject o=new JSONObject();
		if(manuallyApprovesFollowers)
			o.put("manuallyApprovesFollowers", true);
		JSONArray custom=null;
		if(attachment!=null){
			for(ActivityPubObject att:attachment){
				if(att instanceof PropertyValue){
					if(custom==null)
						custom=new JSONArray();
					PropertyValue pv=(PropertyValue) att;
					custom.put(Map.of("n", pv.name, "v", pv.value));
				}
			}
		}
		if(custom!=null)
			o.put("custom", custom);
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
	public URI getWallURL(){
		return Config.localURI("/users/"+id+"/wall");
	}

	@Override
	public String getTypeAndIdForURL(){
		return "/users/"+id;
	}

	public enum Gender{
		UNKNOWN,
		MALE,
		FEMALE;

		public static Gender valueOf(int v){
			return switch(v){
				case 0 -> UNKNOWN;
				case 1 -> MALE;
				case 2 -> FEMALE;
				default -> throw new IllegalStateException("Unexpected value: "+v);
			};
		}
	}
}
