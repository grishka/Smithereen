package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.jsonld.JLD;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public class ForeignUser extends User implements ForeignActor{

	private URI wall, friends, groups;
	public URI movedToURL;
	public boolean isServiceActor;

	public static ForeignUser fromResultSet(ResultSet res) throws SQLException{
		ForeignUser user=new ForeignUser();
		user.fillFromResultSet(res);
		return user;
	}

	public ForeignUser(){}

	public ForeignUser(boolean isServiceActor){
		this.isServiceActor=isServiceActor;
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

		EndpointsStorageWrapper ep=Utils.gson.fromJson(res.getString("endpoints"), EndpointsStorageWrapper.class);
		outbox=tryParseURL(ep.outbox);
		followers=tryParseURL(ep.followers);
		following=tryParseURL(ep.following);
		wall=tryParseURL(ep.wall);
		friends=tryParseURL(ep.friends);
		groups=tryParseURL(ep.groups);
		collectionQueryEndpoint=tryParseURL(ep.collectionQuery);
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
			// If there's a single space somewhere in the name, assume it's first and last names
			int spaceIdx=firstName.indexOf(' ');
			if(spaceIdx>0 && spaceIdx==firstName.lastIndexOf(' ')){
				lastName=firstName.substring(spaceIdx+1);
				firstName=firstName.substring(0, spaceIdx);
			}
		}
		if(obj.has("vcard:bday")){
			birthDate=LocalDate.parse(obj.get("vcard:bday").getAsString());
		}
		if(obj.has("gender")){
			gender=switch(obj.get("gender").getAsString()){
				case "sc:Male", JLD.SCHEMA_ORG+"Male" -> Gender.MALE;
				case "sc:Female", JLD.SCHEMA_ORG+"Female" -> Gender.FEMALE;
				case "sc:Other", JLD.SCHEMA_ORG+"Other" -> Gender.OTHER;
				default -> Gender.UNKNOWN;
			};
		}else{
			gender=Gender.UNKNOWN;
		}
		manuallyApprovesFollowers=optBoolean(obj, "manuallyApprovesFollowers");
		JsonObject capabilities=optObject(obj, "capabilities");
		if(capabilities!=null){
			if(optBoolean(capabilities, "supportsFriendRequests"))
				flags|=FLAG_SUPPORTS_FRIEND_REQS;
		}else if(optBoolean(obj, "supportsFriendRequests")){
			flags|=FLAG_SUPPORTS_FRIEND_REQS;
		}
		if(StringUtils.isNotEmpty(summary))
			summary=TextProcessor.sanitizeHTML(summary);
		wall=tryParseURL(optString(obj, "wall"));
		if(wall==null)
			wall=tryParseURL(optString(obj, "sm:wall"));
		ensureHostMatchesID(wall, "wall");
		friends=tryParseURL(optString(obj, "friends"));
		ensureHostMatchesID(friends, "friends");
		groups=tryParseURL(optString(obj, "groups"));
		ensureHostMatchesID(groups, "groups");

		JsonObject privacy=optObject(obj, "privacySettings");
		if(privacy!=null){
			privacySettings=new HashMap<>();
			for(UserPrivacySettingKey key:UserPrivacySettingKey.values()){
				String jKey=key.getActivityPubKey();
				JsonObject setting=optObject(privacy, jKey.substring(jKey.indexOf(':')+1));
				if(setting==null)
					continue;
				JsonArray allowedTo=optArrayCompact(setting, "allowedTo");
				if(allowedTo==null)
					continue;
				PrivacySetting ps=new PrivacySetting();
				ps.baseRule=PrivacySetting.Rule.NONE;
				ps.allowUsers=new HashSet<>();
				ps.exceptUsers=new HashSet<>();
				for(int i=0;i<allowedTo.size();i++){
					String e=allowedTo.get(i).getAsString();
					if(ActivityPub.AS_PUBLIC.toString().equals(e) || "as:Public".equals(e)){
						ps.baseRule=PrivacySetting.Rule.EVERYONE;
						continue;
					}else if("sm:FriendsOfFriends".equals(e) || (JLD.SMITHEREEN+"sm").equals(e)){
						ps.baseRule=PrivacySetting.Rule.FRIENDS_OF_FRIENDS;
						continue;
					}
					URI uri;
					try{
						uri=new URI(e);
					}catch(URISyntaxException x){
						continue;
					}
					if(Objects.equals(uri, friends) && ps.baseRule!=PrivacySetting.Rule.FRIENDS_OF_FRIENDS){
						ps.baseRule=PrivacySetting.Rule.FRIENDS;
					}else if(Objects.equals(uri, followers)){
						ps.baseRule=PrivacySetting.Rule.FOLLOWERS;
					}else if(Objects.equals(uri, following)){
						ps.baseRule=PrivacySetting.Rule.FOLLOWING;
					}else if(Config.isLocal(uri)){
						int id=ObjectLinkResolver.getUserIDFromLocalURL(uri);
						if(id>0)
							ps.allowUsers.add(id);
					}
				}
				JsonArray except=optArrayCompact(setting, "except");
				if(except!=null){
					for(int i=0;i<except.size();i++){
						String e=allowedTo.get(i).getAsString();
						URI uri;
						try{
							uri=new URI(e);
						}catch(URISyntaxException x){
							continue;
						}
						if(Config.isLocal(uri)){
							int id=ObjectLinkResolver.getUserIDFromLocalURL(uri);
							if(id>0)
								ps.exceptUsers.add(id);
						}
					}
				}
				privacySettings.put(key, ps);
			}
		}
		if(obj.has("movedTo")){
			movedToURL=tryParseURL(obj.get("movedTo").getAsString());
		}
		if(obj.has("alsoKnownAs")){
			List<LinkOrObject> aka=tryParseArrayOfLinksOrObjects(obj.get("alsoKnownAs"), parserContext);
			if(aka!=null){
				alsoKnownAs=aka.stream()
						.filter(l->l.link!=null)
						.map(l->l.link)
						.collect(Collectors.toSet());
			}
		}

		activities=optString(obj, "activities");
		interests=optString(obj, "interests");
		favoriteMusic=optString(obj, "favoriteMusic");
		favoriteMovies=optString(obj, "favoriteMovies");
		favoriteTvShows=optString(obj, "favoriteTvShows");
		favoriteBooks=optString(obj, "favoriteBooks");
		favoriteGames=optString(obj, "favoriteGames");
		favoriteQuotes=optString(obj, "favoriteQuotes");

		politicalViews=switch(optString(obj, "politicalViews")){
			case "sm:Apathetic" -> PoliticalViews.APATHETIC;
			case "sm:Communist" -> PoliticalViews.COMMUNIST;
			case "sm:Socialist" -> PoliticalViews.SOCIALIST;
			case "sm:Moderate" -> PoliticalViews.MODERATE;
			case "sm:Liberal" -> PoliticalViews.LIBERAL;
			case "sm:Conservative" -> PoliticalViews.CONSERVATIVE;
			case "sm:Monarchist" -> PoliticalViews.MONARCHIST;
			case "sm:Ultraconservative" -> PoliticalViews.ULTRACONSERVATIVE;
			case "sm:Libertarian" -> PoliticalViews.LIBERTARIAN;
			case null, default -> null;
		};
		religion=optString(obj, "religion");
		personalPriority=switch(optString(obj, "personalPriority")){
			case "sm:FamilyAndChildren" -> PersonalPriority.FAMILY_CHILDREN;
			case "sm:CareerAndMoney" -> PersonalPriority.CAREER_MONEY;
			case "sm:EntertainmentAndLeisure" -> PersonalPriority.ENTERTAINMENT_LEISURE;
			case "sm:ScienceAndResearch" -> PersonalPriority.SCIENCE_RESEARCH;
			case "sm:ImprovingTheWorld" -> PersonalPriority.IMPROVING_WORLD;
			case "sm:PersonalDevelopment" -> PersonalPriority.PERSONAL_DEVELOPMENT;
			case "sm:BeautyAndArt" -> PersonalPriority.BEAUTY_ART;
			case "sm:FameAndInfluence" -> PersonalPriority.FAME_INFLUENCE;
			case null, default -> null;
		};
		peoplePriority=switch(optString(obj, "peoplePriority")){
			case "sm:IntellectAndCreativity" -> PeoplePriority.INTELLECT_CREATIVITY;
			case "sm:KindnessAndHonesty" -> PeoplePriority.KINDNESS_HONESTY;
			case "sm:HealthAndBeauty" -> PeoplePriority.HEALTH_BEAUTY;
			case "sm:WealthAndPower" -> PeoplePriority.WEALTH_POWER;
			case "sm:CourageAndPersistence" -> PeoplePriority.COURAGE_PERSISTENCE;
			case "sm:HumorAndLoveForLife" -> PeoplePriority.HUMOR_LIFE_LOVE;
			case null, default -> null;
		};
		smokingViews=switch(optString(obj, "smokingViews")){
			case "sm:VeryNegative" -> HabitsViews.VERY_NEGATIVE;
			case "sm:Negative" -> HabitsViews.NEGATIVE;
			case "sm:Tolerant" -> HabitsViews.TOLERANT;
			case "sm:Neutral" -> HabitsViews.NEUTRAL;
			case "sm:Positive" -> HabitsViews.POSITIVE;
			case null, default -> null;
		};
		alcoholViews=switch(optString(obj, "alcoholViews")){
			case "sm:VeryNegative" -> HabitsViews.VERY_NEGATIVE;
			case "sm:Negative" -> HabitsViews.NEGATIVE;
			case "sm:Tolerant" -> HabitsViews.TOLERANT;
			case "sm:Neutral" -> HabitsViews.NEUTRAL;
			case "sm:Positive" -> HabitsViews.POSITIVE;
			case null, default -> null;
		};
		inspiredBy=optString(obj, "inspiredBy");

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
		return lastUpdated!=null && System.currentTimeMillis()-lastUpdated.toEpochMilli()>24L*60*60*1000;
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
