package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.storage.DatabaseUtils;
import smithereen.util.TranslatableEnum;
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
	public Map<UserPrivacySettingKey, PrivacySetting> privacySettings=new HashMap<>();
	public int movedTo;
	public int movedFrom;
	public Instant movedAt;
	public Set<URI> alsoKnownAs=new HashSet<>();
	public UserBanStatus banStatus=UserBanStatus.NONE;
	public UserBanInfo banInfo;

	// additional profile fields
	public boolean manuallyApprovesFollowers;
	// "interests" tab
	public String activities, interests, favoriteMusic, favoriteMovies, favoriteTvShows, favoriteBooks, favoriteGames, favoriteQuotes;
	// "personal views" tab
	public PoliticalViews politicalViews;
	public String religion;
	public PersonalPriority personalPriority;
	public PeoplePriority peoplePriority;
	public HabitsViews smokingViews, alcoholViews;
	public String inspiredBy;

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
			if(o.has("aka")){
				JsonArray aka=o.getAsJsonArray("aka");
				for(JsonElement el:aka){
					alsoKnownAs.add(URI.create(el.getAsString()));
				}
			}
			movedTo=optInt(o, "movedTo");
			movedFrom=optInt(o, "movedFrom");
			if(o.has("movedAt")){
				long moved=o.get("movedAt").getAsLong();
				movedAt=Instant.ofEpochSecond(moved);
			}

			activities=optString(o, "activities");
			interests=optString(o, "interests");
			favoriteMusic=optString(o, "music");
			favoriteMovies=optString(o, "movies");
			favoriteTvShows=optString(o, "tv");
			favoriteBooks=optString(o, "books");
			favoriteGames=optString(o, "games");
			favoriteQuotes=optString(o, "quotes");

			if(o.has("political"))
				politicalViews=PoliticalViews.values()[o.get("political").getAsInt()];
			religion=optString(o, "religion");
			if(o.has("personalPri"))
				personalPriority=PersonalPriority.values()[o.get("personalPri").getAsInt()];
			if(o.has("peoplePri"))
				peoplePriority=PeoplePriority.values()[o.get("peoplePri").getAsInt()];
			if(o.has("smoking"))
				smokingViews=HabitsViews.values()[o.get("smoking").getAsInt()];
			if(o.has("alcohol"))
				alcoholViews=HabitsViews.values()[o.get("alcohol").getAsInt()];
			inspiredBy=optString(o, "inspired");
		}

		String privacy=res.getString("privacy");
		if(StringUtils.isNotEmpty(privacy)){
			privacySettings=Utils.gson.fromJson(privacy, new TypeToken<>(){});
		}
		banStatus=UserBanStatus.values()[res.getInt("ban_status")];
		if(banStatus!=UserBanStatus.NONE){
			String _banInfo=res.getString("ban_info");
			if(StringUtils.isNotEmpty(_banInfo)){
				banInfo=Utils.gson.fromJson(_banInfo, UserBanInfo.class);
			}
		}
	}

	@Override
	public String getType(){
		return "Person";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		name=getFullName();
		obj=super.asActivityPubObject(obj, serializerContext);

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

		serializerContext.addAlias("sc", JLD.SCHEMA_ORG);
		serializerContext.addAlias("firstName", "sc:givenName");
		serializerContext.addAlias("lastName", "sc:familyName");
		serializerContext.addAlias("middleName", "sc:additionalName");
		serializerContext.addType("gender", "sc:gender", "sc:GenderType");
		serializerContext.addAlias("sm", JLD.SMITHEREEN);
		serializerContext.addAlias("maidenName", "sm:maidenName");
		serializerContext.addType("friends", "sm:friends", "@id");
		serializerContext.addType("groups", "sm:groups", "@id");
		serializerContext.addAlias("vcard", JLD.VCARD);

		JsonObject capabilities=new JsonObject();
		capabilities.addProperty("supportsFriendRequests", true);
		obj.add("capabilities", capabilities);
		serializerContext.addAlias("capabilities", "litepub:capabilities");
		serializerContext.addAlias("supportsFriendRequests", "sm:supportsFriendRequests");
		serializerContext.addAlias("litepub", JLD.LITEPUB);

		serializerContext.addAlias("privacySettings", "sm:privacySettings");
		serializerContext.addAlias("allowedTo", "sm:allowedTo");
		serializerContext.addAlias("except", "sm:except");
		JsonObject privacy=new JsonObject();
		for(UserPrivacySettingKey key:UserPrivacySettingKey.values()){
			String apKey=key.getActivityPubKey();
			String alias=apKey.substring(apKey.indexOf(':')+1);
			serializerContext.addAlias(alias, apKey);

			JsonArray allowed=new JsonArray();
			JsonArray except=new JsonArray();

			if(privacySettings.containsKey(key)){
				PrivacySetting setting=privacySettings.get(key);
				switch(setting.baseRule){
					case EVERYONE -> allowed.add(ActivityPub.AS_PUBLIC.toString());
					case FRIENDS -> allowed.add(getFriendsURL().toString());
					case FRIENDS_OF_FRIENDS -> {
						allowed.add(getFriendsURL().toString());
						allowed.add("sm:FriendsOfFriends");
					}
				}
				if(!setting.allowUsers.isEmpty() || !setting.exceptUsers.isEmpty()){
					String domain=serializerContext.getRequesterDomain();
					if(domain!=null){
						HashSet<Integer> needUsers=new HashSet<>();
						needUsers.addAll(setting.allowUsers);
						needUsers.addAll(setting.exceptUsers);
						Map<Integer, User> users=serializerContext.appContext.getUsersController().getUsers(needUsers);
						for(int id:setting.allowUsers){
							User user=users.get(id);
							if(user!=null && user.domain.equalsIgnoreCase(domain))
								allowed.add(user.activityPubID.toString());
						}
						for(int id:setting.exceptUsers){
							User user=users.get(id);
							if(user!=null && user.domain.equalsIgnoreCase(domain))
								except.add(user.activityPubID.toString());
						}
					}
				}
			}else{
				allowed.add(ActivityPub.AS_PUBLIC.toString());
			}

			JsonObject setting=new JsonObject();
			setting.add("allowedTo", allowed);
			if(!except.isEmpty())
				setting.add("except", except);
			privacy.add(alias, setting);
		}
		obj.add("privacySettings", privacy);

		if(StringUtils.isNotEmpty(activities)){
			serializerContext.addSmAlias("activities");
			obj.addProperty("activities", activities);
		}
		if(StringUtils.isNotEmpty(interests)){
			serializerContext.addSmAlias("interests");
			obj.addProperty("interests", interests);
		}
		if(StringUtils.isNotEmpty(favoriteMusic)){
			serializerContext.addSmAlias("favoriteMusic");
			obj.addProperty("favoriteMusic", favoriteMusic);
		}
		if(StringUtils.isNotEmpty(favoriteMovies)){
			serializerContext.addSmAlias("favoriteMovies");
			obj.addProperty("favoriteMovies", favoriteMovies);
		}
		if(StringUtils.isNotEmpty(favoriteTvShows)){
			serializerContext.addSmAlias("favoriteTvShows");
			obj.addProperty("favoriteTvShows", favoriteTvShows);
		}
		if(StringUtils.isNotEmpty(favoriteBooks)){
			serializerContext.addSmAlias("favoriteBooks");
			obj.addProperty("favoriteBooks", favoriteBooks);
		}
		if(StringUtils.isNotEmpty(favoriteGames)){
			serializerContext.addSmAlias("favoriteGames");
			obj.addProperty("favoriteGames", favoriteGames);
		}
		if(StringUtils.isNotEmpty(favoriteQuotes)){
			serializerContext.addSmAlias("favoriteQuotes");
			obj.addProperty("favoriteQuotes", favoriteQuotes);
		}

		if(politicalViews!=null){
			serializerContext.addSmIdType("politicalViews");
			obj.addProperty("politicalViews", "sm:"+switch(politicalViews){
				case APATHETIC -> "Apathetic";
				case COMMUNIST -> "Communist";
				case SOCIALIST -> "Socialist";
				case MODERATE -> "Moderate";
				case LIBERAL -> "Liberal";
				case CONSERVATIVE -> "Conservative";
				case MONARCHIST -> "Monarchist";
				case ULTRACONSERVATIVE -> "Ultraconservative";
				case LIBERTARIAN -> "Libertarian";
			});
		}
		if(StringUtils.isNotEmpty(religion)){
			serializerContext.addSmAlias("religion");
			obj.addProperty("religion", religion);
		}
		if(personalPriority!=null){
			serializerContext.addSmIdType("personalPriority");
			obj.addProperty("personalPriority", "sm:"+switch(personalPriority){
				case FAMILY_CHILDREN -> "FamilyAndChildren";
				case CAREER_MONEY -> "CareerAndMoney";
				case ENTERTAINMENT_LEISURE -> "EntertainmentAndLeisure";
				case SCIENCE_RESEARCH -> "ScienceAndResearch";
				case IMPROVING_WORLD -> "ImprovingTheWorld";
				case PERSONAL_DEVELOPMENT -> "PersonalDevelopment";
				case BEAUTY_ART -> "BeautyAndArt";
				case FAME_INFLUENCE -> "FameAndInfluence";
			});
		}
		if(peoplePriority!=null){
			serializerContext.addSmIdType("peoplePriority");
			obj.addProperty("peoplePriority", "sm:"+switch(peoplePriority){
				case INTELLECT_CREATIVITY -> "IntellectAndCreativity";
				case KINDNESS_HONESTY -> "KindnessAndHonesty";
				case HEALTH_BEAUTY -> "HealthAndBeauty";
				case WEALTH_POWER -> "WealthAndPower";
				case COURAGE_PERSISTENCE -> "CourageAndPersistence";
				case HUMOR_LIFE_LOVE -> "HumorAndLoveForLife";
			});
		}
		if(smokingViews!=null){
			serializerContext.addSmIdType("smokingViews");
			obj.addProperty("smokingViews", "sm:"+switch(smokingViews){
				case VERY_NEGATIVE -> "VeryNegative";
				case NEGATIVE -> "Negative";
				case TOLERANT -> "Tolerant";
				case NEUTRAL -> "Neutral";
				case POSITIVE -> "Positive";
			});
		}
		if(alcoholViews!=null){
			serializerContext.addSmIdType("alcoholViews");
			obj.addProperty("alcoholViews", "sm:"+switch(alcoholViews){
				case VERY_NEGATIVE -> "VeryNegative";
				case NEGATIVE -> "Negative";
				case TOLERANT -> "Tolerant";
				case NEUTRAL -> "Neutral";
				case POSITIVE -> "Positive";
			});
		}
		if(StringUtils.isNotEmpty(inspiredBy)){
			serializerContext.addSmAlias("inspiredBy");
			obj.addProperty("inspiredBy", inspiredBy);
		}

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
		if(!alsoKnownAs.isEmpty()){
			JsonArray aka=new JsonArray(alsoKnownAs.size());
			for(URI uri:alsoKnownAs){
				aka.add(uri.toString());
			}
			o.add("aka", aka);
		}
		if(movedTo>0)
			o.addProperty("movedTo", movedTo);
		if(movedFrom>0)
			o.addProperty("movedFrom", movedFrom);
		if(movedAt!=null)
			o.addProperty("movedAt", movedAt.getEpochSecond());

		if(StringUtils.isNotEmpty(activities))
			o.addProperty("activities", activities);
		if(StringUtils.isNotEmpty(interests))
			o.addProperty("interests", interests);
		if(StringUtils.isNotEmpty(favoriteMusic))
			o.addProperty("music", favoriteMusic);
		if(StringUtils.isNotEmpty(favoriteMovies))
			o.addProperty("movies", favoriteMovies);
		if(StringUtils.isNotEmpty(favoriteTvShows))
			o.addProperty("tv", favoriteTvShows);
		if(StringUtils.isNotEmpty(favoriteBooks))
			o.addProperty("books", favoriteBooks);
		if(StringUtils.isNotEmpty(favoriteGames))
			o.addProperty("games", favoriteGames);
		if(StringUtils.isNotEmpty(favoriteQuotes))
			o.addProperty("quotes", favoriteQuotes);

		if(politicalViews!=null)
			o.addProperty("political", politicalViews.ordinal());
		if(StringUtils.isNotEmpty(religion))
			o.addProperty("religion", religion);
		if(personalPriority!=null)
			o.addProperty("personalPri", personalPriority.ordinal());
		if(peoplePriority!=null)
			o.addProperty("peoplePri", peoplePriority.ordinal());
		if(smokingViews!=null)
			o.addProperty("smoking", smokingViews.ordinal());
		if(alcoholViews!=null)
			o.addProperty("alcohol", alcoholViews.ordinal());
		if(StringUtils.isNotEmpty(inspiredBy))
			o.addProperty("inspired", inspiredBy);

		return o.toString();
	}

	public boolean supportsFriendRequests(){
		return true;
	}

	public String getNameForReply(){
		if(StringUtils.isNotEmpty(firstName))
			return firstName.trim();
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

	public PrivacySetting getPrivacySetting(UserPrivacySettingKey key){
		return privacySettings.getOrDefault(key, PrivacySetting.DEFAULT);
	}

	// for templates
	public Map<String, Object> getFirstAndGender(){
		return Map.of("first", firstName, "gender", gender==null ? Gender.UNKNOWN : gender);
	}

	public Map<String, Object> getFirstLastAndGender(){
		return Map.of("first", firstName, "last", lastName==null ? "" : lastName, "gender", gender==null ? Gender.UNKNOWN : gender);
	}

	public void copyLocalFields(User previous){
		movedTo=previous.movedTo;
		movedFrom=previous.movedFrom;
		movedAt=previous.movedAt;
		banStatus=previous.banStatus;
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

	public enum PoliticalViews implements TranslatableEnum<PoliticalViews>{
		APATHETIC,
		COMMUNIST,
		SOCIALIST,
		MODERATE,
		LIBERAL,
		CONSERVATIVE,
		MONARCHIST,
		ULTRACONSERVATIVE,
		LIBERTARIAN;

		@Override
		public String getLangKey(){
			return "profile_political_"+toString().toLowerCase();
		}
	}

	public enum PersonalPriority implements TranslatableEnum<PersonalPriority>{
		FAMILY_CHILDREN,
		CAREER_MONEY,
		ENTERTAINMENT_LEISURE,
		SCIENCE_RESEARCH,
		IMPROVING_WORLD,
		PERSONAL_DEVELOPMENT,
		BEAUTY_ART,
		FAME_INFLUENCE;

		@Override
		public String getLangKey(){
			return "profile_personal_priority_"+toString().toLowerCase();
		}
	}

	public enum PeoplePriority implements TranslatableEnum<PeoplePriority>{
		INTELLECT_CREATIVITY,
		KINDNESS_HONESTY,
		HEALTH_BEAUTY,
		WEALTH_POWER,
		COURAGE_PERSISTENCE,
		HUMOR_LIFE_LOVE;

		@Override
		public String getLangKey(){
			return "profile_people_priority_"+toString().toLowerCase();
		}
	}

	public enum HabitsViews implements TranslatableEnum<HabitsViews>{
		VERY_NEGATIVE,
		NEGATIVE,
		TOLERANT,
		NEUTRAL,
		POSITIVE;

		@Override
		public String getLangKey(){
			return "profile_habits_"+toString().toLowerCase();
		}
	}
}
