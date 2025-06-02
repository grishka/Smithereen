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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.utils.Pair;
import smithereen.text.TextProcessor;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
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
	public URI movedToApID;
	public List<URI> alsoKnownAs=new ArrayList<>();
	public UserBanStatus banStatus=UserBanStatus.NONE;
	public UserBanInfo banInfo;
	public EnumSet<FriendsNewsfeedTypeFilter> newsTypesToShow;
	protected long numFriends;
	protected long numFollowers; // Followers and following include friends (mutual follows)
	protected long numFollowing;

	// additional profile fields
	public boolean manuallyApprovesFollowers;
	public String hometown;
	public RelationshipStatus relationship;
	public URI relationshipPartnerActivityPubID;
	public int relationshipPartnerID;
	// "interests" tab
	public String activities, interests, favoriteMusic, favoriteMovies, favoriteTvShows, favoriteBooks, favoriteGames, favoriteQuotes;
	// "personal views" tab
	public PoliticalViews politicalViews;
	public String religion;
	public PersonalPriority personalPriority;
	public PeoplePriority peoplePriority;
	public HabitsViews smokingViews, alcoholViews;
	public String inspiredBy;
	// "contacts" tab
	public Map<ContactInfoKey, String> contacts=Map.of();
	public String website, location;

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
			movedToApID=tryParseURL(optString(o, "movedToAP"));
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

			if(o.has("contacts")){
				contacts=o.getAsJsonObject("contacts").entrySet()
						.stream()
						.map(e->new Pair<>(ContactInfoKey.valueOf(e.getKey()), e.getValue().getAsString()))
						.collect(Collectors.toMap(Pair::first, Pair::second));
			}
			website=optString(o, "web");
			location=optString(o, "loc");

			hometown=optString(o, "hometown");
			if(o.has("relation")){
				relationship=RelationshipStatus.values()[o.get("relation").getAsInt()];
				relationshipPartnerID=optInt(o, "partner");
				if(relationshipPartnerID==-1)
					relationshipPartnerID=0;
				relationshipPartnerActivityPubID=tryParseURL(optString(o, "partnerAP"));
			}

			if(o.has("feedTypes")){
				newsTypesToShow=Utils.gson.fromJson(o.get("feedTypes"), new TypeToken<>(){});
			}

			if(o.has("status")){
				status=Utils.gson.fromJson(o.get("status"), ActorStatus.class);
			}
		}

		String privacy=res.getString("privacy");
		if(StringUtils.isNotEmpty(privacy)){
			privacySettings=Utils.gson.fromJson(privacy, new TypeToken<>(){});
		}
		banStatus=UserBanStatus.values()[res.getInt("ban_status")];
		String _banInfo=res.getString("ban_info");
		if(StringUtils.isNotEmpty(_banInfo)){
			banInfo=Utils.gson.fromJson(_banInfo, UserBanInfo.class);
		}

		numFollowers=res.getLong("num_followers");
		numFollowing=res.getLong("num_following");
		numFriends=res.getLong("num_friends");
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
			if(!privacySettings.containsKey(key)){
				privacy.add(alias, new JsonObjectBuilder().add("allowedTo", new JsonArrayBuilder().add(ActivityPub.AS_PUBLIC.toString())).build());
				continue;
			}

			privacy.add(alias, privacySettings.get(key).serializeForActivityPub(this, serializerContext));
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

		if(StringUtils.isNotEmpty(location))
			obj.addProperty("vcard:Address", location);
		JsonArrayBuilder attachment=new JsonArrayBuilder();
		if(StringUtils.isNotEmpty(website)){
			String url=TextProcessor.escapeHTML(website);
			PropertyValue pv=new PropertyValue();
			pv.name="Website";
			pv.value="<a href=\""+url+"\" rel=\"me\">"+url+"</a>";
			attachment.add(pv.asActivityPubObject(new JsonObject(), serializerContext));
		}
		for(ContactInfoKey key:ContactInfoKey.values()){
			if(contacts.containsKey(key)){
				String value=contacts.get(key);
				String url=TextProcessor.getContactInfoValueURL(key, value);
				PropertyValue pv=new PropertyValue();
				pv.name=key.getFieldName();
				if(url!=null){
					pv.value="<a href=\""+TextProcessor.escapeHTML(url)+"\" rel=\"me\">"+TextProcessor.escapeHTML(value)+"</a>";
				}else{
					pv.value=TextProcessor.escapeHTML(value);
				}
				attachment.add(pv.asActivityPubObject(new JsonObject(), serializerContext));
			}
		}
		JsonArray att=attachment.build();
		if(!att.isEmpty())
			obj.add("attachment", att);

		if(StringUtils.isNotEmpty(hometown)){
			serializerContext.addSmAlias("hometown");
			obj.addProperty("hometown", hometown);
		}
		if(relationship!=null){
			serializerContext.addSmIdType("relationshipStatus");
			serializerContext.addSmIdType("relationshipPartner");
			obj.addProperty("relationshipStatus", switch(relationship){
				case SINGLE -> "sm:Single";
				case IN_RELATIONSHIP -> "sm:InRelationship";
				case ENGAGED -> "sm:Engaged";
				case MARRIED -> "sm:Married";
				case IN_LOVE -> "sm:InLove";
				case COMPLICATED -> "sm:Complicated";
				case ACTIVELY_SEARCHING -> "sm:ActivelySearching";
			});
			if(relationshipPartnerID!=0)
				obj.addProperty("relationshipPartner", relationshipPartnerActivityPubID.toString());
		}

		serializerContext.addSmIdType("photoAlbums");
		obj.addProperty("photoAlbums", getPhotoAlbumsURL().toString());

		serializerContext.addAlias("manuallyApprovesFollowers", "as:manuallyApprovesFollowers");
		obj.addProperty("manuallyApprovesFollowers", false);

		serializerContext.addSmIdType("taggedPhotos");
		obj.addProperty("taggedPhotos", getTaggedPhotosURL().toString());

		if(newsTypesToShow!=null){
			serializerContext.addSmIdType("newsfeedUpdatesPrivacy");
			JsonArrayBuilder jb=new JsonArrayBuilder();
			for(FriendsNewsfeedTypeFilter type:newsTypesToShow){
				if(type==FriendsNewsfeedTypeFilter.POSTS)
					continue;
				jb.add(switch(type){
					case POSTS -> null;
					case PHOTOS -> "sm:Photos";
					case FRIENDS -> "sm:Friends";
					case GROUPS -> "sm:Groups";
					case EVENTS -> "sm:Events";
					case PHOTO_TAGS -> "sm:PhotoTags";
					case PERSONAL_INFO -> "sm:PersonalInfo";
				});
			}
			obj.add("newsfeedUpdatesPrivacy", jb.build());
		}

		if(!alsoKnownAs.isEmpty()){
			JsonArray aka=new JsonArray();
			alsoKnownAs.stream().map(Object::toString).forEach(aka::add);
			obj.add("alsoKnownAs", aka);
			serializerContext.addType("alsoKnownAs", "as:alsoKnownAs", "@id");
		}

		if(movedToApID!=null){
			obj.addProperty("movedTo", movedToApID.toString());
			serializerContext.addType("movedTo", "as:movedTo", "@id");
		}

		serializerContext.addAlias("toot", JLD.MASTODON);
		serializerContext.addAlias("suspended", "toot:suspended");
		obj.addProperty("suspended", banStatus==UserBanStatus.SUSPENDED);

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

	@Override
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
		if(movedToApID!=null)
			o.addProperty("movedToAP", movedToApID.toString());
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

		if(!contacts.isEmpty()){
			o.add("contacts", new JsonObjectBuilder().addAll(contacts).build());
		}
		if(StringUtils.isNotEmpty(website))
			o.addProperty("web", website);
		if(StringUtils.isNotEmpty(location))
			o.addProperty("loc", location);

		if(relationship!=null){
			o.addProperty("relation", relationship.ordinal());
			if(relationshipPartnerID!=0){
				o.addProperty("partner", relationshipPartnerID);
				o.addProperty("partnerAP", relationshipPartnerActivityPubID.toString());
			}
		}
		if(StringUtils.isNotEmpty(hometown))
			o.addProperty("hometown", hometown);

		if(newsTypesToShow!=null)
			o.add("feedTypes", Utils.gson.toJsonTree(newsTypesToShow));

		if(status!=null)
			o.add("status", Utils.gson.toJsonTree(status));

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

	@Override
	public URI getWallCommentsURL(){
		return Config.localURI("/users/"+id+"/wallComments");
	}

	public URI getFriendsURL(){
		return Config.localURI("/users/"+id+"/friends");
	}

	public URI getGroupsURL(){
		return Config.localURI("/users/"+id+"/groups");
	}

	@Override
	public URI getPhotoAlbumsURL(){
		return Config.localURI("/users/"+id+"/albums");
	}

	public URI getTaggedPhotosURL(){
		return Config.localURI("/users/"+id+"/tagged");
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
		return privacySettings.getOrDefault(key, key.getDefaultValue());
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
		movedToApID=previous.movedToApID;
		movedFrom=previous.movedFrom;
		movedAt=previous.movedAt;
		banStatus=previous.banStatus;
	}

	public long getFriendsCount(){
		return numFriends;
	}

	public long getFollowersCount(){
		return numFollowers-numFriends;
	}

	public long getFollowingCount(){
		return numFollowing-numFriends;
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

	public enum ContactInfoKey{
		MATRIX,
		XMPP,
		TELEGRAM,
		SIGNAL,
		TWITTER,
		INSTAGRAM,
		FACEBOOK,
		VKONTAKTE,
		SNAPCHAT,
		DISCORD,
		GIT,
		MASTODON,
		PIXELFED,
		PHONE_NUMBER,
		EMAIL;

		public String getFieldName(){
			return switch(this){
				case MATRIX -> "Matrix";
				case XMPP -> "XMPP";
				case TELEGRAM -> "Telegram";
				case SIGNAL -> "Signal";
				case TWITTER -> "Twitter";
				case INSTAGRAM -> "Instagram";
				case FACEBOOK -> "Facebook";
				case VKONTAKTE -> "VKontakte";
				case SNAPCHAT -> "Snapchat";
				case DISCORD -> "Discord";
				case GIT -> "Git";
				case MASTODON -> "Mastodon";
				case PIXELFED -> "Pixelfed";
				case PHONE_NUMBER -> "Phone number";
				case EMAIL -> "E-mail";
			};
		}

		public boolean isLocalizable(){
			return this==PHONE_NUMBER || this==EMAIL;
		}

		public String getLangKey(){
			return switch(this){
				case PHONE_NUMBER -> "profile_phone_number";
				case EMAIL -> "email";
				default -> throw new IllegalStateException("Unexpected value: " + this);
			};
		}

		public String getEditLangKey(){
			return switch(this){
				case PHONE_NUMBER -> "profile_edit_phone_number";
				case EMAIL -> "email";
				default -> throw new IllegalStateException("Unexpected value: " + this);
			};
		}

		public List<String> getValueExamples(){
			return switch(this){
				case MATRIX -> List.of("@john_appleseed:matrix.org");
				case XMPP -> List.of("vasya@example.im");
				case TELEGRAM -> List.of("pavel", "t.me/pavel");
				case SIGNAL -> List.of("moxie.99", "signal.me/#abcd...");
				case TWITTER -> List.of("jack", "twitter.com/jack", "x.com/elonmusk");
				case INSTAGRAM -> List.of("kevin", "instagram.com/kevin");
				case FACEBOOK -> List.of("zuck", "www.facebook.com/zuck");
				case VKONTAKTE -> List.of("durov", "vk.com/durov", "id1");
				case SNAPCHAT -> List.of("evan", "www.snapchat.com/add/evan");
				case DISCORD -> List.of("jason");
				case GIT -> List.of("github.com/octocat", "gitlab.com/gitlab-org");
				case MASTODON -> List.of("@Gargron@mastodon.social", "mastodon.social/@Gargron");
				case PIXELFED -> List.of("@dansup@pixelfed.social", "pixelfed.social/dansup");
				case PHONE_NUMBER, EMAIL -> List.of();
			};
		}
	}

	public enum RelationshipStatus implements TranslatableEnum<RelationshipStatus>{
		SINGLE,
		IN_RELATIONSHIP,
		ENGAGED,
		MARRIED,
		IN_LOVE,
		COMPLICATED,
		ACTIVELY_SEARCHING;

		@Override
		public String getLangKey(){
			return "profile_relationship_"+toString().toLowerCase();
		}

		public String getLangKeyForFeed(boolean withPartner){
			return "relationship_"+switch(this){
				case SINGLE -> "single";
				case IN_RELATIONSHIP -> withPartner ? "in_relationship_with" : "in_relationship";
				case ENGAGED -> withPartner ? "engaged_with" : "engaged";
				case MARRIED -> withPartner ? "married_to" : "married";
				case IN_LOVE -> withPartner ? "in_love_with" : "in_love";
				case COMPLICATED -> withPartner ? "complicated_with" : "complicated";
				case ACTIVELY_SEARCHING -> "searching";
			};
		}

		public boolean canHavePartner(){
			return this!=SINGLE && this!=ACTIVELY_SEARCHING;
		}

		public boolean needsPartnerApproval(){
			return canHavePartner() && this!=IN_LOVE;
		}
	}
}
