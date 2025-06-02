package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.jsonld.JLD;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public class ForeignUser extends User implements ForeignActor{

	private URI wall, friends, groups, photoAlbums, taggedPhotos, wallComments;
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
		wallComments=tryParseURL(ep.wallComments);
		friends=tryParseURL(ep.friends);
		groups=tryParseURL(ep.groups);
		collectionQueryEndpoint=tryParseURL(ep.collectionQuery);
		photoAlbums=tryParseURL(ep.photoAlbums);
		taggedPhotos=tryParseURL(ep.taggedPhotos);
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
		if(wall!=null){
			wallComments=tryParseURL(optString(obj, "wallComments"));
			ensureHostMatchesID(wallComments, "wallComments");
		}
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
				privacySettings.put(key, PrivacySetting.parseFromActivityPub(this, setting));
			}
		}
		if(obj.has("movedTo")){
			movedToURL=tryParseURL(obj.get("movedTo").getAsString());
		}
		if(obj.has("alsoKnownAs") || obj.has("as:alsoKnownAs")){
			List<LinkOrObject> aka=tryParseArrayOfLinksOrObjects(obj.has("alsoKnownAs") ? obj.get("alsoKnownAs") : obj.get("as:alsoKnownAs"), parserContext);
			if(aka!=null){
				alsoKnownAs=aka.stream()
						.filter(l->l.link!=null)
						.map(l->l.link)
						.toList();
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

		location=optString(obj, "vcard:Address");
		if(attachment!=null && !attachment.isEmpty()){
			ArrayList<ActivityPubObject> filteredAttachment=new ArrayList<>();
			contacts=new HashMap<>();
			for(ActivityPubObject att:attachment){
				if(att instanceof PropertyValue pv && pv.name!=null){
					// Get rid of Mastodon :emojis: and non-ASCII characters
					String normalizedName=pv.name.toLowerCase().replaceAll(":[a-z0-9_]{2,}:", "").replaceAll("[^a-z0-9 -]", "").trim();
					// Match against popular strings people use for these things
					if(Set.of("website", "web", "web site", "blog", "homepage", "www", "site", "personal page", "personal website", "personal blog").contains(normalizedName)){
						website=TextProcessor.stripHTML(pv.value, false);
						continue;
					}
					ContactInfoKey contactKey=switch(normalizedName){
						case "matrix" -> ContactInfoKey.MATRIX;
						case "xmpp", "jabber" -> ContactInfoKey.XMPP;
						case "telegram", "tg" -> ContactInfoKey.TELEGRAM;
						case "signal" -> ContactInfoKey.SIGNAL;
						case "twitter", "x", "xitter", "x.com", "birdsite" -> ContactInfoKey.TWITTER;
						case "instagram", "insta", "ig" -> ContactInfoKey.INSTAGRAM;
						case "facebook", "fb" -> ContactInfoKey.FACEBOOK;
						case "vkontakte", "vk" -> ContactInfoKey.VKONTAKTE;
						case "snapchat", "snap" -> ContactInfoKey.SNAPCHAT;
						case "discord" -> ContactInfoKey.DISCORD;
						case "git", "github", "gitlab", "codeberg", "gitea" -> ContactInfoKey.GIT;
						case "mastodon" -> ContactInfoKey.MASTODON;
						case "pixelfed" -> ContactInfoKey.PIXELFED;
						case "phone number", "phone", "sms" -> ContactInfoKey.PHONE_NUMBER;
						case "email", "e-mail" -> ContactInfoKey.EMAIL;
						default -> null;
					};
					if(contactKey!=null && !contacts.containsKey(contactKey)){
						String normalizedValue=TextProcessor.normalizeContactInfoValue(contactKey, TextProcessor.stripHTML(pv.value, false));
						if(normalizedValue!=null){
							contacts.put(contactKey, normalizedValue);
							continue;
						}
					}
					filteredAttachment.add(att);
				}else{
					filteredAttachment.add(att);
				}
			}
			attachment=filteredAttachment;
		}

		hometown=optString(obj, "hometown");
		relationship=switch(optString(obj, "relationshipStatus")){
			case "sm:Single" -> RelationshipStatus.SINGLE;
			case "sm:InRelationship" -> RelationshipStatus.IN_RELATIONSHIP;
			case "sm:Engaged" -> RelationshipStatus.ENGAGED;
			case "sm:Married" -> RelationshipStatus.MARRIED;
			case "sm:InLove" -> RelationshipStatus.IN_LOVE;
			case "sm:Complicated" -> RelationshipStatus.COMPLICATED;
			case "sm:ActivelySearching" -> RelationshipStatus.ACTIVELY_SEARCHING;
			case null, default -> null;
		};
		if(relationship!=null && relationship.canHavePartner()){
			relationshipPartnerActivityPubID=tryParseURL(optString(obj, "relationshipPartner"));
		}

		photoAlbums=tryParseURL(optString(obj, "photoAlbums"));
		ensureHostMatchesID(photoAlbums, "photoAlbums");
		taggedPhotos=tryParseURL(optString(obj, "taggedPhotos"));
		ensureHostMatchesID(taggedPhotos, "taggedPhotos");

		if(obj.has("newsfeedUpdatesPrivacy")){
			Set<String> typeIDs=switch(obj.get("newsfeedUpdatesPrivacy")){
				case JsonPrimitive jp when jp.isString() -> Set.of(jp.getAsString());
				case JsonArray arr -> {
					HashSet<String> set=new HashSet<>();
					for(JsonElement el:arr){
						if(el instanceof JsonPrimitive jp && jp.isString()){
							set.add(jp.getAsString());
						}
					}
					yield set.isEmpty() ? null : set;
				}
				default -> null;
			};
			if(typeIDs!=null){
				newsTypesToShow=typeIDs.stream()
						.map(id->switch(id){
							case "sm:Photos" -> FriendsNewsfeedTypeFilter.PHOTOS;
							case "sm:Friends" -> FriendsNewsfeedTypeFilter.FRIENDS;
							case "sm:Groups" -> FriendsNewsfeedTypeFilter.GROUPS;
							case "sm:Events" -> FriendsNewsfeedTypeFilter.EVENTS;
							case "sm:PhotoTags" -> FriendsNewsfeedTypeFilter.PHOTO_TAGS;
							case "sm:PersonalInfo" -> FriendsNewsfeedTypeFilter.PERSONAL_INFO;
							default -> null;
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(()->EnumSet.noneOf(FriendsNewsfeedTypeFilter.class)));
			}
		}

		if(optBoolean(obj, "suspended")){
			if(banInfo==null)
				banInfo=new UserBanInfo(Instant.now(), null, null, false, 0, 0, true);
			else
				banInfo=banInfo.withRemoteSuspensionStatus(true);
		}else if(banInfo!=null){
			if(banStatus==UserBanStatus.NONE)
				banInfo=null;
			else
				banInfo=banInfo.withRemoteSuspensionStatus(false);
		}

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
	public URI getWallCommentsURL(){
		return wallComments;
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
	public URI getPhotoAlbumsURL(){
		return photoAlbums;
	}

	@Override
	public URI getTaggedPhotosURL(){
		return taggedPhotos;
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

	public void setFollowersCount(long count){
		numFollowers=count;
	}

	public void setFollowingCount(long count){
		numFollowing=count;
	}

	public void setFriendsCount(long count){
		numFriends=count;
	}

	public long getRawFollowersCount(){
		return numFollowers;
	}

	public long getRawFollowingCount(){
		return numFollowing;
	}
}
