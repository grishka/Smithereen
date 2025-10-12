package smithereen.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import smithereen.activitypub.objects.PropertyValue;
import smithereen.api.ApiCallContext;
import smithereen.lang.Inflector;
import smithereen.model.ForeignUser;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserPresence;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.photos.Photo;
import spark.utils.StringUtils;

public class ApiUser{
	public static final EnumSet<Field> FIELDS_THAT_REQUIRE_ACCOUNT=EnumSet.of(
			Field.BLOCKED,
			Field.BLOCKED_BY_ME,
			Field.CAN_POST,
			Field.CAN_SEE_ALL_POSTS,
			Field.CAN_SEND_FRIEND_REQUEST,
			Field.CAN_WRITE_PRIVATE_MESSAGE,
			Field.MUTUAL_COUNT,
			Field.FRIEND_STATUS,
			Field.IS_FAVORITE,
			Field.IS_FRIEND,
			Field.IS_HIDDEN_FROM_FEED,
			Field.LISTS
	);

	public int id;
	public String firstName, lastName;
	public String deactivated;
	public String apId;

	public String domain, screenName, status, url;

	public String nickname, maidenName, sex, bdate, homeTown;
	public String relation;
	public RelationshipPartner relationPartner;
	public List<CustomProfileField> custom;

	public String city;
	public String matrix, xmpp, telegram, signal, twitter, instagram, facebook, vkontakte, snapchat, discord, git, mastodon, pixelfed, phoneNumber, email;
	public String site;

	public String activities, interests, music, movies, tv, books, games, quotes, about;

	public PersonalViews personal;

	public Boolean online;
	public Boolean onlineMobile;
	public LastSeen lastSeen;

	public Boolean blocked, blockedByMe;

	public Boolean canPost, canSeeAllPosts, canSendFriendRequest, canWritePrivateMessage;
	public Integer mutualCount;
	public String friendStatus;
	public Boolean isFriend, isFavorite;
	public List<Integer> lists;
	public Boolean isHiddenFromFeed;

	public Long followersCount;
	public Boolean isNoIndex;
	public String wallDefault;

	@SerializedName("photo_50")
	public String photo50;
	@SerializedName("photo_100")
	public String photo100;
	@SerializedName("photo_200")
	public String photo200;
	@SerializedName("photo_400")
	public String photo400;
	@SerializedName("photo_200_orig")
	public String photo200Orig;
	@SerializedName("photo_400_orig")
	public String photo400Orig;
	public String photoMaxOrig, photoMax;
	public String photoId;

	public String timezone;

	public String firstNameNom, firstNameGen, firstNameDat, firstNameAcc, firstNameIns, firstNameAbl;
	public String lastNameNom, lastNameGen, lastNameDat, lastNameAcc, lastNameIns, lastNameAbl;
	public String nicknameNom, nicknameGen, nicknameDat, nicknameAcc, nicknameIns, nicknameAbl;

	public Counters counters;

	public ApiUser(ApiCallContext actx, User user, EnumSet<Field> fields, Map<Integer, User> extraUsers, Map<Integer, UserPresence> onlines, Set<Integer> blockingIDs,
				   Set<Integer> blockedIDs, Map<Integer, Set<UserPrivacySettingKey>> allowedPrivacySettings, Map<Integer, Integer> mutualCounts,
				   Map<Integer, FriendshipStatus> friendStatuses, Set<Integer> bookmarkedIDs, Map<Integer, BitSet> friendLists, Set<Integer> mutedIDs,
				   Map<Integer, Photo> profilePhotos){
		id=user.id;
		firstName=user.firstName;
		lastName=user.lastName;
		deactivated=switch(user.banStatus){
			case NONE -> null;
			case FROZEN, SUSPENDED -> "banned";
			case HIDDEN -> "hidden";
			case SELF_DEACTIVATED -> "deleted";
		};
		apId=user.activityPubID.toString();

		if(fields.contains(Field.DOMAIN) && user instanceof ForeignUser fu)
			domain=fu.domain;
		if(fields.contains(Field.SCREEN_NAME))
			screenName=user.username;
		if(fields.contains(Field.STATUS) && user.status!=null)
			status=user.status.text();
		if(fields.contains(Field.URL))
			url=user.url.toString();

		if(fields.contains(Field.NICKNAME))
			nickname=user.middleName;
		if(fields.contains(Field.MAIDEN_NAME))
			maidenName=user.maidenName;
		if(fields.contains(Field.SEX)){
			sex=switch(user.gender){
				case UNKNOWN -> null;
				case MALE -> "male";
				case FEMALE -> "female";
				case OTHER -> "other";
			};
		}
		if(fields.contains(Field.BIRTH_DATE) && user.birthDate!=null)
			bdate=String.format(Locale.ROOT, "%02d.%02d.%04d", user.birthDate.getDayOfMonth(), user.birthDate.getMonthValue(), user.birthDate.getYear());
		if(fields.contains(Field.HOME_TOWN))
			homeTown=user.hometown;
		if(fields.contains(Field.RELATION) && user.relationship!=null){
			relation=switch(user.relationship){
				case SINGLE -> "single";
				case IN_RELATIONSHIP -> "in_relationship";
				case ENGAGED -> "engaged";
				case MARRIED -> "married";
				case IN_LOVE -> "in_love";
				case COMPLICATED -> "complicated";
				case ACTIVELY_SEARCHING -> "actively_searching";
			};
			if(user.relationship.canHavePartner() && user.relationshipPartnerID!=0){
				User partner=extraUsers.get(user.relationshipPartnerID);
				if(user.relationship.needsPartnerApproval()){
					if(partner!=null && partner.relationshipPartnerID!=id){
						partner=null;
					}
				}
				if(partner!=null){
					String partnerFirstName=partner.firstName;
					String partnerLastName=partner.lastName;
					String relationCase=actx.optParamString("relation_case");
					if(StringUtils.isNotEmpty(relationCase)){
						Inflector.Case nameCase=Inflector.Case.NOMINATIVE;
						if("def".equals(relationCase)){
							if("ru".equals(actx.lang.getLocale().getLanguage())){
								nameCase=switch(user.relationship){
									case IN_RELATIONSHIP, ENGAGED, COMPLICATED -> Inflector.Case.INSTRUMENTAL;
									case MARRIED -> partner.gender==User.Gender.FEMALE ? Inflector.Case.PREPOSITIONAL : Inflector.Case.INSTRUMENTAL;
									case IN_LOVE -> Inflector.Case.GENITIVE;
									default -> Inflector.Case.NOMINATIVE;
								};
							}
						}else{
							nameCase=switch(relationCase){
								case "gen" -> Inflector.Case.GENITIVE;
								case "dat" -> Inflector.Case.DATIVE;
								case "acc" -> Inflector.Case.ACCUSATIVE;
								case "ins" -> Inflector.Case.INSTRUMENTAL;
								case "abl" -> Inflector.Case.PREPOSITIONAL;
								default -> Inflector.Case.NOMINATIVE;
							};
						}
						partnerFirstName=actx.lang.inflectNamePart(partnerFirstName, Inflector.NamePart.FIRST, partner.gender, nameCase);
						partnerLastName=actx.lang.inflectNamePart(partnerLastName, Inflector.NamePart.LAST, partner.gender, nameCase);
					}
					relationPartner=new RelationshipPartner(user.relationshipPartnerID, partnerFirstName, partnerLastName);
				}
			}
		}
		if(fields.contains(Field.CUSTOM) && user.attachment!=null){
			custom=user.attachment.stream()
					.map(a->a instanceof PropertyValue pv && !pv.parsed ? pv : null)
					.filter(Objects::nonNull)
					.map(pv->new CustomProfileField(pv.name, pv.value))
					.toList();
			if(custom.isEmpty())
				custom=null;
		}

		if(fields.contains(Field.CITY))
			city=user.location;
		if(fields.contains(Field.CONNECTIONS)){
			user.contacts.forEach((k, v)->{
				switch(k){
					case MATRIX -> matrix=v;
					case XMPP -> xmpp=v;
					case TELEGRAM -> telegram=v;
					case SIGNAL -> signal=v;
					case TWITTER -> twitter=v;
					case INSTAGRAM -> instagram=v;
					case FACEBOOK -> facebook=v;
					case VKONTAKTE -> vkontakte=v;
					case SNAPCHAT -> snapchat=v;
					case DISCORD -> discord=v;
					case GIT -> git=v;
					case MASTODON -> mastodon=v;
					case PIXELFED -> pixelfed=v;
					case PHONE_NUMBER -> phoneNumber=v;
					case EMAIL -> email=v;
				}
			});
		}
		if(fields.contains(Field.SITE))
			site=user.website;

		if(fields.contains(Field.ACTIVITIES))
			activities=user.activities;
		if(fields.contains(Field.INTERESTS))
			interests=user.interests;
		if(fields.contains(Field.MUSIC))
			music=user.favoriteMusic;
		if(fields.contains(Field.MOVIES))
			movies=user.favoriteMovies;
		if(fields.contains(Field.TV))
			tv=user.favoriteTvShows;
		if(fields.contains(Field.BOOKS))
			books=user.favoriteBooks;
		if(fields.contains(Field.GAMES))
			games=user.favoriteGames;
		if(fields.contains(Field.QUOTES))
			quotes=user.favoriteQuotes;
		if(fields.contains(Field.ABOUT))
			about=user.summary;

		if(fields.contains(Field.PERSONAL)){
			personal=new PersonalViews(
					enumToLowercaseString(user.politicalViews),
					user.religion,
					user.inspiredBy,
					enumToLowercaseString(user.peoplePriority),
					enumToLowercaseString(user.personalPriority),
					enumToLowercaseString(user.smokingViews),
					enumToLowercaseString(user.alcoholViews)
			);
		}

		if(fields.contains(Field.ONLINE)){
			UserPresence presence=onlines.get(id);
			if(presence!=null && presence.isOnline()){
				online=true;
				if(presence.isMobile())
					onlineMobile=true;
			}
		}
		if(fields.contains(Field.LAST_SEEN)){
			UserPresence presence=onlines.get(id);
			if(presence!=null && !presence.isOnline()){
				lastSeen=new LastSeen(presence.lastUpdated().getEpochSecond(), presence.isMobile() ? "mobile" : "desktop");
			}
		}
		// TODO API online status, return app ID

		if(fields.contains(Field.BLOCKED))
			blocked=blockingIDs.contains(id);
		if(fields.contains(Field.BLOCKED_BY_ME))
			blockedByMe=blockedIDs.contains(id);

		if(fields.contains(Field.CAN_SEND_FRIEND_REQUEST))
			canSendFriendRequest=user.supportsFriendRequests();
		if(fields.contains(Field.CAN_POST))
			canPost=allowedPrivacySettings.get(id).contains(UserPrivacySettingKey.WALL_POSTING);
		if(fields.contains(Field.CAN_SEE_ALL_POSTS))
			canSeeAllPosts=allowedPrivacySettings.get(id).contains(UserPrivacySettingKey.WALL_OTHERS_POSTS);
		if(fields.contains(Field.CAN_WRITE_PRIVATE_MESSAGE))
			canWritePrivateMessage=allowedPrivacySettings.get(id).contains(UserPrivacySettingKey.PRIVATE_MESSAGES);
		if(fields.contains(Field.MUTUAL_COUNT))
			mutualCount=mutualCounts.get(id);
		if(fields.contains(Field.IS_FRIEND))
			isFriend=friendStatuses.get(id)==FriendshipStatus.FRIENDS;
		if(fields.contains(Field.FRIEND_STATUS)){
			friendStatus=switch(friendStatuses.get(id)){
				case NONE -> "none";
				case REQUEST_SENT, REQUEST_RECVD -> null;
				case FRIENDS -> "friends";
				case FOLLOWING -> "following";
				case FOLLOWED_BY -> "followed_by";
				case FOLLOW_REQUESTED -> "follow_requested";
			};
		}
		if(fields.contains(Field.IS_FAVORITE))
			isFavorite=bookmarkedIDs.contains(id);
		if(fields.contains(Field.LISTS)){
			BitSet listSet=friendLists.get(id);
			lists=listSet==null ? List.of() : listSet.stream().map(i->i+1).boxed().toList();
		}
		if(fields.contains(Field.IS_HIDDEN_FROM_FEED))
			isHiddenFromFeed=mutedIDs.contains(id);

		if(fields.contains(Field.FOLLOWERS_COUNT))
			followersCount=user.getFollowersCount();
		if(fields.contains(Field.IS_NO_INDEX))
			isNoIndex=!user.isIndexable();
		if(fields.contains(Field.WALL_DEFAULT)){
			if(!user.hasWall()){
				wallDefault="owner";
			}else{
				boolean canSeeAll;
				if(allowedPrivacySettings!=null)
					canSeeAll=allowedPrivacySettings.get(id).contains(UserPrivacySettingKey.WALL_OTHERS_POSTS);
				else
					canSeeAll=user.getPrivacySetting(UserPrivacySettingKey.WALL_OTHERS_POSTS).isFullyPublic();
				wallDefault=canSeeAll ? "all" : "owner";
			}
		}

		if(fields.contains(Field.PHOTO_50) || fields.contains(Field.PHOTO_100) || fields.contains(Field.PHOTO_200) || fields.contains(Field.PHOTO_200_ORIG)
				|| fields.contains(Field.PHOTO_400) || fields.contains(Field.PHOTO_400_ORIG) || fields.contains(Field.PHOTO_ID) || fields.contains(Field.PHOTO_MAX) || fields.contains(Field.PHOTO_MAX_ORIG)){
			SizedImage img=user.getAvatar();
			if(img!=null){
				if(fields.contains(Field.PHOTO_50))
					photo50=img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_100))
					photo100=img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_200))
					photo200=img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_LARGE, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_400))
					photo400=img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_XLARGE, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_200_ORIG))
					photo200Orig=img.getUriForSizeAndFormat(SizedImage.Type.AVA_RECT, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_400_ORIG))
					photo400Orig=img.getUriForSizeAndFormat(SizedImage.Type.AVA_RECT_LARGE, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_MAX_ORIG))
					photoMaxOrig=img.getUriForSizeAndFormat(SizedImage.Type.AVA_RECT_LARGE, actx.imageFormat).toString();
				if(fields.contains(Field.PHOTO_MAX))
					photoMax=img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_XLARGE, actx.imageFormat).toString();

				if(fields.contains(Field.PHOTO_ID)){
					Photo photo=profilePhotos.get(id);
					if(photo!=null)
						photoId=photo.getIdString();
				}

				// TODO crop_photo (needs ApiPhoto object)
			}
		}

		if(fields.contains(Field.FIRST_NAME_NOM))
			firstNameNom=user.firstName;
		if(fields.contains(Field.FIRST_NAME_GEN))
			firstNameGen=actx.lang.inflectNamePart(user.firstName, Inflector.NamePart.FIRST, user.gender, Inflector.Case.GENITIVE);
		if(fields.contains(Field.FIRST_NAME_DAT))
			firstNameDat=actx.lang.inflectNamePart(user.firstName, Inflector.NamePart.FIRST, user.gender, Inflector.Case.DATIVE);
		if(fields.contains(Field.FIRST_NAME_ACC))
			firstNameAcc=actx.lang.inflectNamePart(user.firstName, Inflector.NamePart.FIRST, user.gender, Inflector.Case.ACCUSATIVE);
		if(fields.contains(Field.FIRST_NAME_INS))
			firstNameIns=actx.lang.inflectNamePart(user.firstName, Inflector.NamePart.FIRST, user.gender, Inflector.Case.INSTRUMENTAL);
		if(fields.contains(Field.FIRST_NAME_ABL))
			firstNameAbl=actx.lang.inflectNamePart(user.firstName, Inflector.NamePart.FIRST, user.gender, Inflector.Case.PREPOSITIONAL);

		if(fields.contains(Field.LAST_NAME_NOM))
			lastNameNom=user.lastName;
		if(fields.contains(Field.LAST_NAME_GEN))
			lastNameGen=actx.lang.inflectNamePart(user.lastName, Inflector.NamePart.LAST, user.gender, Inflector.Case.GENITIVE);
		if(fields.contains(Field.LAST_NAME_DAT))
			lastNameDat=actx.lang.inflectNamePart(user.lastName, Inflector.NamePart.LAST, user.gender, Inflector.Case.DATIVE);
		if(fields.contains(Field.LAST_NAME_ACC))
			lastNameAcc=actx.lang.inflectNamePart(user.lastName, Inflector.NamePart.LAST, user.gender, Inflector.Case.ACCUSATIVE);
		if(fields.contains(Field.LAST_NAME_INS))
			lastNameIns=actx.lang.inflectNamePart(user.lastName, Inflector.NamePart.LAST, user.gender, Inflector.Case.INSTRUMENTAL);
		if(fields.contains(Field.LAST_NAME_ABL))
			lastNameAbl=actx.lang.inflectNamePart(user.lastName, Inflector.NamePart.LAST, user.gender, Inflector.Case.PREPOSITIONAL);

		if(fields.contains(Field.NICKNAME_NOM))
			nicknameNom=user.middleName;
		if(fields.contains(Field.NICKNAME_GEN))
			nicknameGen=actx.lang.inflectNamePart(user.middleName, Inflector.NamePart.MIDDLE, user.gender, Inflector.Case.GENITIVE);
		if(fields.contains(Field.NICKNAME_DAT))
			nicknameDat=actx.lang.inflectNamePart(user.middleName, Inflector.NamePart.MIDDLE, user.gender, Inflector.Case.DATIVE);
		if(fields.contains(Field.NICKNAME_ACC))
			nicknameAcc=actx.lang.inflectNamePart(user.middleName, Inflector.NamePart.MIDDLE, user.gender, Inflector.Case.ACCUSATIVE);
		if(fields.contains(Field.NICKNAME_INS))
			nicknameIns=actx.lang.inflectNamePart(user.middleName, Inflector.NamePart.MIDDLE, user.gender, Inflector.Case.INSTRUMENTAL);
		if(fields.contains(Field.NICKNAME_ABL))
			nicknameAbl=actx.lang.inflectNamePart(user.middleName, Inflector.NamePart.MIDDLE, user.gender, Inflector.Case.PREPOSITIONAL);
	}

	private static String enumToLowercaseString(Enum<?> e){
		return e==null ? null : e.toString().toLowerCase();
	}

	public record RelationshipPartner(int id, String firstName, String lastName){}
	public record CustomProfileField(String name, String value){}
	public record PersonalViews(String political, String religion, String inspiredBy, String peopleMain, String lifeMain, String smoking, String alcohol){}
	public record LastSeen(long time, String platform){}
	public record Counters(int albums, int photos, long friends, long groups, int onlineFriends, int mutualFriends, int userPhotos, long followers, long subscriptions){}

	public enum Field{
		// Basics
		DOMAIN,
		SCREEN_NAME,
		STATUS,
		URL,

		// Profile fields -- main
		NICKNAME,
		MAIDEN_NAME,
		SEX,
		BIRTH_DATE,
		HOME_TOWN,
		RELATION,
		CUSTOM,

		// Profile fields -- contacts
		CITY,
		CONNECTIONS,
		SITE,

		// Profile fields -- interests
		ACTIVITIES,
		INTERESTS,
		MUSIC,
		MOVIES,
		TV,
		BOOKS,
		GAMES,
		QUOTES,
		ABOUT,

		// Profile fields -- personal
		PERSONAL,

		// Presence
		ONLINE,
		LAST_SEEN,

		// Relationship with current user
		BLOCKED,
		BLOCKED_BY_ME,
		CAN_POST,
		CAN_SEE_ALL_POSTS,
		CAN_SEND_FRIEND_REQUEST,
		CAN_WRITE_PRIVATE_MESSAGE,
		MUTUAL_COUNT,
		FRIEND_STATUS,
		IS_FAVORITE,
		IS_HIDDEN_FROM_FEED,
		LISTS,
		IS_FRIEND,

		// Stats
		COUNTERS,
		FOLLOWERS_COUNT,

		// Preferences
		IS_NO_INDEX,
		TIMEZONE,
		WALL_DEFAULT,

		// Profile picture
		PHOTO_50,
		PHOTO_100,
		PHOTO_200_ORIG,
		PHOTO_200,
		PHOTO_400_ORIG,
		PHOTO_400,
		PHOTO_MAX,
		PHOTO_MAX_ORIG,
		PHOTO_ID,
		CROP_PHOTO,
		HAS_PHOTO,

		// Slavic name inflections
		FIRST_NAME_NOM,
		FIRST_NAME_GEN,
		FIRST_NAME_DAT,
		FIRST_NAME_ACC,
		FIRST_NAME_INS,
		FIRST_NAME_ABL,
		LAST_NAME_NOM,
		LAST_NAME_GEN,
		LAST_NAME_DAT,
		LAST_NAME_ACC,
		LAST_NAME_INS,
		LAST_NAME_ABL,
		NICKNAME_NOM,
		NICKNAME_GEN,
		NICKNAME_DAT,
		NICKNAME_ACC,
		NICKNAME_INS,
		NICKNAME_ABL,
		;

		public static Field valueOfApi(String v){
			return switch(v){
				case "domain" -> DOMAIN;
				case "screen_name" -> SCREEN_NAME;
				case "status" -> STATUS;
				case "url" -> URL;

				case "nickname" -> NICKNAME;
				case "maiden_name" -> MAIDEN_NAME;
				case "sex" -> SEX;
				case "bdate", "birth_date" -> BIRTH_DATE;
				case "home_town" -> HOME_TOWN;
				case "relation" -> RELATION;
				case "custom" -> CUSTOM;

				case "city" -> CITY;
				case "connections" -> CONNECTIONS;
				case "site" -> SITE;

				case "activities" -> ACTIVITIES;
				case "interests" -> INTERESTS;
				case "music" -> MUSIC;
				case "movies" -> MOVIES;
				case "tv" -> TV;
				case "books" -> BOOKS;
				case "games" -> GAMES;
				case "quotes" -> QUOTES;
				case "about" -> ABOUT;

				case "personal" -> PERSONAL;

				case "online" -> ONLINE;
				case "last_seen" -> LAST_SEEN;

				case "blocked", "blacklisted" -> BLOCKED;
				case "blocked_by_me", "blacklisted_by_me" -> BLOCKED_BY_ME;
				case "can_post" -> CAN_POST;
				case "can_see_all_posts" -> CAN_SEE_ALL_POSTS;
				case "can_send_friend_request" -> CAN_SEND_FRIEND_REQUEST;
				case "can_write_private_message" -> CAN_WRITE_PRIVATE_MESSAGE;
				case "mutual_count" -> MUTUAL_COUNT;
				case "friend_status" -> FRIEND_STATUS;
				case "is_favorite" -> IS_FAVORITE;
				case "is_friend" -> IS_FRIEND;
				case "is_hidden_from_feed" -> IS_HIDDEN_FROM_FEED;
				case "lists" -> LISTS;

				case "counters" -> COUNTERS;
				case "followers_count" -> FOLLOWERS_COUNT;

				case "is_no_index" -> IS_NO_INDEX;
				case "timezone" -> TIMEZONE;
				case "wall_default" -> WALL_DEFAULT;

				case "photo_50" -> PHOTO_50;
				case "photo_100" -> PHOTO_100;
				case "photo_200_orig" -> PHOTO_200_ORIG;
				case "photo_200" -> PHOTO_200;
				case "photo_400_orig" -> PHOTO_400_ORIG;
				case "photo_400" -> PHOTO_400;
				case "photo_max" -> PHOTO_MAX;
				case "photo_max_orig" -> PHOTO_MAX_ORIG;
				case "photo_id" -> PHOTO_ID;
				case "crop_photo" -> CROP_PHOTO;
				case "has_photo" -> HAS_PHOTO;

				case "first_name_nom" -> FIRST_NAME_NOM;
				case "first_name_gen" -> FIRST_NAME_GEN;
				case "first_name_dat" -> FIRST_NAME_DAT;
				case "first_name_acc" -> FIRST_NAME_ACC;
				case "first_name_ins" -> FIRST_NAME_INS;
				case "first_name_abl" -> FIRST_NAME_ABL;
				case "last_name_nom" -> LAST_NAME_NOM;
				case "last_name_gen" -> LAST_NAME_GEN;
				case "last_name_dat" -> LAST_NAME_DAT;
				case "last_name_acc" -> LAST_NAME_ACC;
				case "last_name_ins" -> LAST_NAME_INS;
				case "last_name_abl" -> LAST_NAME_ABL;
				case "nickname_nom" -> NICKNAME_NOM;
				case "nickname_gen" -> NICKNAME_GEN;
				case "nickname_dat" -> NICKNAME_DAT;
				case "nickname_acc" -> NICKNAME_ACC;
				case "nickname_ins" -> NICKNAME_INS;
				case "nickname_abl" -> NICKNAME_ABL;

				default -> null;
			};
		}
	}
}
