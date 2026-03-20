package smithereen.api.methods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPrivacySetting;
import smithereen.api.model.ApiUser;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.PaginatedList;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.model.UserPresence;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.notifications.UserNotifications;
import smithereen.routes.ApiRoutes;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public class AccountMethods{
	public static Object getCounters(ApplicationContext ctx, ApiCallContext actx){
		UserNotifications notifications=ctx.getNotificationsController().getUserCounters(actx.self);
		record Counters(int friends, int notifications, int groups, int events, int photos){}
		return new Counters(
				notifications.getNewFriendRequestCount(),
				notifications.getNewNotificationsCount(),
				notifications.getNewGroupInvitationsCount(),
				notifications.getNewEventInvitationsCount(),
				notifications.getNewPhotoTagCount()
		);
	}

	public static Object setOnline(ApplicationContext ctx, ApiCallContext actx){
		ctx.getUsersController().setOnline(actx.self.user, actx.optParamBoolean("mobile") ? UserPresence.PresenceType.MOBILE_API : UserPresence.PresenceType.API,
				Arrays.hashCode(actx.token.id()), actx.token.appID());
		return true;
	}

	public static Object setOffline(ApplicationContext ctx, ApiCallContext actx){
		ctx.getUsersController().setOffline(actx.self.user, Arrays.hashCode(actx.token.id()));
		return true;
	}

	public static Object getAppPermissions(ApplicationContext ctx, ApiCallContext actx){
		EnumSet<ClientAppPermission> permissions=actx.token.permissions();
		if(permissions.contains(ClientAppPermission.PASSWORD_GRANT_USED)){
			permissions=EnumSet.allOf(ClientAppPermission.class);
			permissions.remove(ClientAppPermission.PASSWORD_GRANT_USED);
		}
		return permissions.stream().map(ClientAppPermission::getScopeValue).toList();
	}

	public static Object getBannedUsers(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<User> users=ctx.getPrivacyController().getBlockedUsers(actx.self.user, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(users.total, ApiUtils.getUsers(users.list, ctx, actx));
	}

	public static Object banUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive("user_id"));
		if(user.id==actx.self.user.id)
			throw actx.paramError("can't block self");
		ctx.getFriendsController().blockUser(actx.self.user, user);
		return true;
	}

	public static Object unbanUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive("user_id"));
		ctx.getFriendsController().unblockUser(actx.self.user, user);
		return true;
	}

	public static Object getBannedDomains(ApplicationContext ctx, ApiCallContext actx){
		List<String> domains=ctx.getPrivacyController().getBlockedDomains(actx.self.user);
		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		if(offset>=domains.size())
			return new ApiPaginatedList<>(0, List.of());
		return new ApiPaginatedList<>(domains.size(), domains.subList(offset, Math.min(offset+count, domains.size())));
	}

	public static Object banDomain(ApplicationContext ctx, ApiCallContext actx){
		String domain=actx.requireParamString("domain");
		if(!domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$"))
			throw actx.paramError("invalid domain");
		ctx.getPrivacyController().blockDomain(actx.self.user, domain);
		return true;
	}

	public static Object unbanDomain(ApplicationContext ctx, ApiCallContext actx){
		ctx.getPrivacyController().unblockDomain(actx.self.user, actx.requireParamString("domain"));
		return true;
	}

	public static Object revokeToken(ApplicationContext ctx, ApiCallContext actx){
		ctx.getAppsController().revokeAccessToken(actx.token.id());
		return true;
	}

	public static Object getProfileInfo(ApplicationContext ctx, ApiCallContext actx){
		record ProfileInfoResponse(
				@NotNull String firstName,
				@NotNull String nickname,
				@NotNull String lastName,
				@NotNull String maidenName,
				@NotNull String sex,
				@NotNull String bdate,
				@NotNull String hometown,
				@NotNull String relation,
				@Nullable ApiUser relationPartner
		){
		}
		User self=actx.self.user;
		ApiUser partner=null;
		if(self.relationship!=null && self.relationship.canHavePartner() && self.relationshipPartnerID!=0){
			List<ApiUser> users=ApiUtils.getUsers(List.of(self.relationshipPartnerID), ctx, actx);
			if(!users.isEmpty())
				partner=users.getFirst();
		}
		return new ProfileInfoResponse(
				nonNullStr(self.firstName),
				nonNullStr(self.middleName),
				nonNullStr(self.lastName),
				nonNullStr(self.maidenName),
				switch(self.gender){
					case UNKNOWN -> "none";
					case null -> "none";
					case MALE -> "male";
					case FEMALE -> "female";
					case OTHER -> "other";
				},
				self.birthDate==null ? "" : String.format(Locale.US, "%02d.%02d.%04d", self.birthDate.getDayOfMonth(), self.birthDate.getMonthValue(), self.birthDate.getYear()),
				nonNullStr(self.hometown),
				self.relationship==null ? "none" : ApiUser.mapRelationshipStatus(self.relationship),
				partner
		);
	}

	private static String nonNullStr(String s){
		return s==null ? "" : s;
	}

	private static String nullIfEmpty(ApiCallContext actx, String key){
		String value=actx.optParamString(key, "").strip();
		return StringUtils.isEmpty(value) ? null : value;
	}

	public static Object saveProfileInfo(ApplicationContext ctx, ApiCallContext actx){
		User self=actx.self.user;
		String firstName=self.firstName;
		String lastName=self.lastName;
		String middleName=self.middleName;
		String maidenName=self.maidenName;
		User.Gender gender=self.gender;
		LocalDate bdate=self.birthDate;
		String hometown=self.hometown;
		User.RelationshipStatus relation=self.relationship;
		User partner=null;
		if(actx.hasParam("first_name")){
			String fn=actx.optParamString("first_name", "").strip();
			if(StringUtils.isNotEmpty(fn))
				firstName=fn;
		}
		if(actx.hasParam("nickname")){
			middleName=actx.optParamString("nickname", "").strip();
		}
		if(actx.hasParam("last_name")){
			lastName=actx.optParamString("last_name", "").strip();
		}
		if(actx.hasParam("maiden_name")){
			maidenName=actx.optParamString("maiden_name", "").strip();
		}
		if(actx.hasParam("gender")){
			gender=actx.optParamEnum("gender", Map.of(
					"none", User.Gender.UNKNOWN,
					"male", User.Gender.MALE,
					"female", User.Gender.FEMALE,
					"other", User.Gender.OTHER
			), User.Gender.UNKNOWN);
		}
		if(actx.hasParam("bdate")){
			String rawBDate=actx.optParamString("bdate", "");
			if(StringUtils.isNotEmpty(rawBDate)){
				try{
					bdate=LocalDate.parse(rawBDate, DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US));
				}catch(DateTimeParseException x){
					bdate=null;
				}
			}else{
				bdate=null;
			}
		}
		if(actx.hasParam("relation")){
			relation=actx.optParamEnum("relation", Map.of(
					"single", User.RelationshipStatus.SINGLE,
					"in_relationship", User.RelationshipStatus.IN_RELATIONSHIP,
					"engaged", User.RelationshipStatus.ENGAGED,
					"married", User.RelationshipStatus.MARRIED,
					"in_love", User.RelationshipStatus.IN_LOVE,
					"complicated", User.RelationshipStatus.COMPLICATED,
					"actively_searching", User.RelationshipStatus.ACTIVELY_SEARCHING
			));
		}
		if(relation!=null && relation.canHavePartner()){
			try{
				if(actx.hasParam("relation_partner_id")){
					int partnerID=actx.optParamIntPositive("relation_partner_id", 0);
					if(partnerID>0)
						partner=ctx.getUsersController().getUserOrThrow(partnerID);
				}else if(self.relationshipPartnerID>0){
					partner=ctx.getUsersController().getUserOrThrow(self.relationshipPartnerID);
				}
			}catch(ObjectNotFoundException ignored){}
		}
		ctx.getUsersController().updateBasicProfileInfo(self, firstName, lastName, middleName, maidenName, gender, bdate, hometown, relation, partner);
		actx.self.user=ctx.getUsersController().getUserOrThrow(self.id);
		return true;
	}

	public static Object saveProfileContacts(ApplicationContext ctx, ApiCallContext actx){
		User self=actx.self.user;
		HashMap<User.ContactInfoKey, String> contactInfo=new HashMap<>(self.contacts);
		String location=self.location;
		String website=self.website;
		if(actx.hasParam("city"))
			location=nullIfEmpty(actx, "city");
		if(actx.hasParam("site"))
			website=nullIfEmpty(actx, "site");
		for(User.ContactInfoKey key:User.ContactInfoKey.values()){
			String param=key.getApiParamName();
			if(actx.hasParam(param)){
				String value=actx.optParamString(param, "");
				if(StringUtils.isNotEmpty(value)){
					String normalized=TextProcessor.normalizeContactInfoValue(key, value);
					if(normalized==null)
						throw actx.paramError(param+" is not in an expected format");
					contactInfo.put(key, normalized);
				}else{
					contactInfo.remove(key);
				}
			}
		}

		ctx.getUsersController().updateProfileContacts(self, contactInfo, location, website);
		actx.self.user=ctx.getUsersController().getUserOrThrow(self.id);

		return true;
	}

	public static Object saveProfileInterests(ApplicationContext ctx, ApiCallContext actx){
		User self=actx.self.user;
		String activities=self.activities;
		String interests=self.interests;
		String music=self.favoriteMusic;
		String movies=self.favoriteMovies;
		String tv=self.favoriteTvShows;
		String books=self.favoriteBooks;
		String games=self.favoriteGames;
		String quotes=self.favoriteQuotes;
		String about=self.aboutSource;

		if(actx.hasParam("activities"))
			activities=nullIfEmpty(actx, "activities");
		if(actx.hasParam("interests"))
			interests=nullIfEmpty(actx, "interests");
		if(actx.hasParam("music"))
			music=nullIfEmpty(actx, "music");
		if(actx.hasParam("movies"))
			movies=nullIfEmpty(actx, "movies");
		if(actx.hasParam("tv"))
			tv=nullIfEmpty(actx, "tv");
		if(actx.hasParam("books"))
			books=nullIfEmpty(actx, "books");
		if(actx.hasParam("games"))
			games=nullIfEmpty(actx, "games");
		if(actx.hasParam("quotes"))
			quotes=nullIfEmpty(actx, "quotes");
		if(actx.hasParam("about"))
			about=nullIfEmpty(actx, "about");

		ctx.getUsersController().updateProfileInterests(self, about, activities, interests, music, movies, tv, books, games, quotes);
		actx.self.user=ctx.getUsersController().getUserOrThrow(self.id);

		return true;
	}

	public static Object saveProfilePersonal(ApplicationContext ctx, ApiCallContext actx){
		User self=actx.self.user;
		User.PoliticalViews politicalViews=self.politicalViews;
		String religion=self.religion;
		String inspiredBy=self.inspiredBy;
		User.PeoplePriority peoplePriority=self.peoplePriority;
		User.PersonalPriority personalPriority=self.personalPriority;
		User.HabitsViews smokingViews=self.smokingViews;
		User.HabitsViews alcoholViews=self.alcoholViews;

		if(actx.hasParam("political")){
			politicalViews=actx.optParamEnum("political", Map.of(
					"apathetic", User.PoliticalViews.APATHETIC,
					"communist", User.PoliticalViews.COMMUNIST,
					"socialist", User.PoliticalViews.SOCIALIST,
					"moderate", User.PoliticalViews.MODERATE,
					"liberal", User.PoliticalViews.LIBERAL,
					"conservative", User.PoliticalViews.CONSERVATIVE,
					"monarchist", User.PoliticalViews.MONARCHIST,
					"ultraconservative", User.PoliticalViews.ULTRACONSERVATIVE,
					"libertarian", User.PoliticalViews.LIBERTARIAN
			));
		}
		if(actx.hasParam("religion"))
			religion=nullIfEmpty(actx, "religion");
		if(actx.hasParam("inspired_by"))
			inspiredBy=nullIfEmpty(actx, "inspired_by");
		if(actx.hasParam("people_main")){
			peoplePriority=actx.optParamEnum("people_main", Map.of(
					"intellect_creativity", User.PeoplePriority.INTELLECT_CREATIVITY,
					"kindness_honesty", User.PeoplePriority.KINDNESS_HONESTY,
					"health_beauty", User.PeoplePriority.HEALTH_BEAUTY,
					"wealth_power", User.PeoplePriority.WEALTH_POWER,
					"courage_persistence", User.PeoplePriority.COURAGE_PERSISTENCE,
					"humor_life_love", User.PeoplePriority.HUMOR_LIFE_LOVE
			));
		}
		if(actx.hasParam("life_main")){
			personalPriority=actx.optParamEnum("life_main", Map.of(
					"family_children", User.PersonalPriority.FAMILY_CHILDREN,
					"career_money", User.PersonalPriority.CAREER_MONEY,
					"entertainment_leisure", User.PersonalPriority.ENTERTAINMENT_LEISURE,
					"science_research", User.PersonalPriority.SCIENCE_RESEARCH,
					"improving_world", User.PersonalPriority.IMPROVING_WORLD,
					"personal_development", User.PersonalPriority.PERSONAL_DEVELOPMENT,
					"beauty_art", User.PersonalPriority.BEAUTY_ART,
					"fame_influence", User.PersonalPriority.FAME_INFLUENCE
			));
		}

		Map<String, User.HabitsViews> habitsViewsMapping=Map.of(
				"very_negative", User.HabitsViews.VERY_NEGATIVE,
				"negative", User.HabitsViews.NEGATIVE,
				"tolerant", User.HabitsViews.TOLERANT,
				"neutral", User.HabitsViews.NEUTRAL,
				"positive", User.HabitsViews.POSITIVE
		);
		if(actx.hasParam("smoking"))
			smokingViews=actx.optParamEnum("smoking", habitsViewsMapping);
		if(actx.hasParam("alcohol"))
			alcoholViews=actx.optParamEnum("alcohol", habitsViewsMapping);

		ctx.getUsersController().updateProfilePersonal(self, politicalViews, religion, personalPriority, peoplePriority, smokingViews, alcoholViews, inspiredBy);
		actx.self.user=ctx.getUsersController().getUserOrThrow(self.id);

		return true;
	}

	public static Object getPrivacySettings(ApplicationContext ctx, ApiCallContext actx){
		record PrivacySettingWrapper(String key, String description, ApiPrivacySetting setting, boolean onlyMe, boolean friendsOnly){}
		record PrivacySettingsResponse(List<PrivacySettingWrapper> settings, List<String> feedTypes){}

		List<PrivacySettingWrapper> settings=new ArrayList<>();
		actx.self.user.privacySettings.forEach((key, value)->{
			settings.add(new PrivacySettingWrapper(key.name().toLowerCase(), actx.lang.get(key.getDescriptionLangKey()), new ApiPrivacySetting(value), key.isOnlyMe(), key.isFriendsOnly()));
		});
		EnumSet<FriendsNewsfeedTypeFilter> feedTypes=actx.self.user.newsTypesToShow==null ? EnumSet.allOf(FriendsNewsfeedTypeFilter.class) : actx.self.user.newsTypesToShow;
		List<String> feedTypesStr=feedTypes.stream().map(t->switch(t){
			case POSTS -> null;
			case PHOTOS -> "photo";
			case PHOTO_TAGS -> "photo_tag";
			case FRIENDS -> "friend";
			case GROUPS -> "group";
			case EVENTS -> "event";
			case TOPICS -> "board";
			case PERSONAL_INFO -> "relation";
		}).filter(Objects::nonNull).toList();
		return new PrivacySettingsResponse(settings, feedTypesStr);
	}

	public static Object savePrivacySettings(ApplicationContext ctx, ApiCallContext actx){
		JsonArray rawSettings=actx.optParamJsonArray("settings");
		HashMap<UserPrivacySettingKey, PrivacySetting> settings=new HashMap<>();
		if(rawSettings!=null){
			Map<String, UserPrivacySettingKey> keyMapping=Arrays.stream(UserPrivacySettingKey.values()).collect(Collectors.toMap(k->k.name().toLowerCase(), Function.identity()));
			int i=0;
			for(JsonElement el:rawSettings){
				if(!(el instanceof JsonObject obj))
					throw actx.paramError("settings["+i+"] is not an object");
				if(!(obj.get("key") instanceof JsonPrimitive jkey) || !jkey.isString())
					throw actx.paramError("settings["+i+"].key is not a string");
				if(!(obj.get("setting") instanceof JsonObject jsetting))
					throw actx.paramError("settings["+i+"].setting is not an object");
				ApiPrivacySetting setting=ApiRoutes.gson.fromJson(jsetting, ApiPrivacySetting.class);

				UserPrivacySettingKey key=keyMapping.get(jkey.getAsString());
				if(key!=null)
					settings.put(key, setting.toNativePrivacySetting());

				i++;
			}
		}

		EnumSet<FriendsNewsfeedTypeFilter> feedTypes=actx.self.user.newsTypesToShow;
		if(actx.hasParam("feed_types")){
			feedTypes=EnumSet.noneOf(FriendsNewsfeedTypeFilter.class);
			for(String rawType:actx.optCommaSeparatedStringSet("feed_types")){
				if("all".equals(rawType)){
					feedTypes=null;
					break;
				}
				if("none".equals(rawType)){
					feedTypes.clear();
					break;
				}
				FriendsNewsfeedTypeFilter type=switch(rawType){
					case "photo" -> FriendsNewsfeedTypeFilter.PHOTOS;
					case "photo_tag" -> FriendsNewsfeedTypeFilter.PHOTO_TAGS;
					case "friend" -> FriendsNewsfeedTypeFilter.FRIENDS;
					case "group" -> FriendsNewsfeedTypeFilter.GROUPS;
					case "event" -> FriendsNewsfeedTypeFilter.EVENTS;
					case "board" -> FriendsNewsfeedTypeFilter.TOPICS;
					case "relation" -> FriendsNewsfeedTypeFilter.PERSONAL_INFO;
					default -> null;
				};
				if(type!=null)
					feedTypes.add(type);
			}
		}

		ctx.getPrivacyController().updateUserPrivacySettings(actx.self.user, settings, feedTypes);
		actx.self.user=ctx.getUsersController().getUserOrThrow(actx.self.user.id);

		return true;
	}
}
