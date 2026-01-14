package smithereen.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.api.ApiCallContext;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.SizedImage;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.groups.GroupBanStatus;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.Photo;

public class ApiGroup{
	public static final EnumSet<Field> FIELDS_THAT_REQUIRE_ACCOUNT=EnumSet.of(
			Field.IS_ADMIN,
			Field.ADMIN_LEVEL,
			Field.IS_MEMBER,
			Field.CAN_CREATE_TOPIC,
			Field.CAN_POST,
			Field.IS_FAVORITE,
			Field.MEMBER_STATUS
	);

	public int id;
	public String name;
	public String deactivated;
	public String apId;
	public String accessType;
	public String type;

	public String domain, screenName, status, url, description, place, site;
	public Long startDate, finishDate;
	public Long membersCount;

	public Boolean isAdmin, isMember;
	public String adminLevel, memberStatus;
	public Boolean canPost, canCreateTopic;
	public Boolean isFavorite;

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
	public ApiCropPhoto cropPhoto;
	public Boolean hasPhoto;

	public List<Manager> management;
	public List<Link> links;
	public Counters counters;

	// groups.getInvites only
	public Integer invitedBy;

	public ApiGroup(ApiCallContext actx, Group group, EnumSet<Field> fields, Map<Integer, Group.AdminLevel> adminLevels, Map<Integer, Group.MembershipState> memberStates,
					Set<Integer> canPost, Set<Integer> canCreateTopic, Set<Integer> favoritedGroups, Map<Integer, Photo> profilePhotos){
		id=group.id;
		name=group.name;
		deactivated=switch(group.banStatus){
			case NONE -> null;
			case SUSPENDED -> "banned";
			case SELF_DEACTIVATED -> "deleted";
			case HIDDEN -> actx.self==null ? "hidden" : null;
		};
		apId=group.activityPubID.toString();
		accessType=group.accessType.toString().toLowerCase();
		type=group.isEvent() ? "event" : "group";

		if(group.accessType==Group.AccessType.PRIVATE){
			Group.MembershipState state=memberStates==null || !actx.hasPermission(ClientAppPermission.GROUPS_READ) ? null : memberStates.get(id);
			if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.INVITED){
				name=actx.lang.get(group.isEvent() ? "private_event" : "private_group");
				return;
			}
		}
		if(group.banStatus!=GroupBanStatus.NONE && !(actx.self!=null && group.banStatus==GroupBanStatus.HIDDEN))
			return;

		if(fields.contains(Field.DOMAIN) && group instanceof ForeignGroup fg)
			domain=fg.domain;
		if(fields.contains(Field.SCREEN_NAME))
			screenName=group.username;
		if(fields.contains(Field.STATUS) && group.status!=null && !group.status.isExpired())
			status=group.status.text();
		if(fields.contains(Field.URL))
			url=group.url.toString();
		if(fields.contains(Field.DESCRIPTION))
			description=group.summary;
		if(fields.contains(Field.SITE))
			site=group.website;
		if(group.isEvent()){
			if(fields.contains(Field.PLACE))
				place=group.location;
			if(fields.contains(Field.START_DATE) && group.startTime!=null)
				startDate=group.startTime.getEpochSecond();
			if(fields.contains(Field.FINISH_DATE) && group.endTime!=null)
				finishDate=group.endTime.getEpochSecond();
		}
		if(fields.contains(Field.MEMBERS_COUNT))
			membersCount=(long)group.memberCount;

		if(actx.self!=null){
			if(fields.contains(Field.IS_ADMIN))
				isAdmin=adminLevels.containsKey(id);
			if(fields.contains(Field.ADMIN_LEVEL)){
				adminLevel=switch(adminLevels.get(id)){
					case ADMIN -> "admin";
					case OWNER -> "owner";
					case MODERATOR -> "moderator";
					case REGULAR -> null;
					case null -> null;
				};
			}
			if(fields.contains(Field.IS_MEMBER)){
				Group.MembershipState state=memberStates.get(id);
				isMember=state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER;
			}
			if(fields.contains(Field.MEMBER_STATUS)){
				memberStatus=switch(memberStates.get(id)){
					case MEMBER -> "member";
					case TENTATIVE_MEMBER -> "tentative";
					case INVITED -> "invited";
					case REQUESTED -> "requested";
					case NONE -> "none";
				};
			}
			if(fields.contains(Field.CAN_POST))
				this.canPost=canPost.contains(id);
			if(fields.contains(Field.CAN_CREATE_TOPIC))
				this.canCreateTopic=canCreateTopic.contains(id);
			if(fields.contains(Field.IS_FAVORITE))
				isFavorite=favoritedGroups.contains(id);
		}

		if(fields.contains(Field.HAS_PHOTO))
			hasPhoto=group.hasAvatar();

		if(fields.contains(Field.PHOTO_50) || fields.contains(Field.PHOTO_100) || fields.contains(Field.PHOTO_200) || fields.contains(Field.PHOTO_200_ORIG)
				|| fields.contains(Field.PHOTO_400) || fields.contains(Field.PHOTO_400_ORIG) || fields.contains(Field.PHOTO_ID) || fields.contains(Field.PHOTO_MAX) || fields.contains(Field.PHOTO_MAX_ORIG)
				|| fields.contains(Field.CROP_PHOTO)){
			SizedImage img=group.getAvatar();
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

				if(fields.contains(Field.CROP_PHOTO)){
					Photo photo=profilePhotos.get(id);
					if(photo!=null && photo.metadata!=null && photo.metadata.cropRects!=null){
						AvatarCropRects rects=photo.metadata.cropRects;
						cropPhoto=new ApiCropPhoto(new ApiPhoto(photo, actx, null, null), new ApiImageRect(rects.profile()), new ApiImageRect(rects.thumb()));
					}
				}
			}
		}

	}

	public record Manager(int userId, String description){}
	public record Counters(int photos, int albums, int topics){}
	public record Link(long id, String url, String name, String description, String photo_50, String photo_100, String photo_200, String objectType, String objectId){}

	public enum Field{
		DOMAIN,
		SCREEN_NAME,
		STATUS,
		URL,
		IS_ADMIN,
		ADMIN_LEVEL,
		IS_MEMBER,
		PHOTO_50,
		PHOTO_100,
		PHOTO_200,
		PHOTO_400,
		PHOTO_200_ORIG,
		PHOTO_400_ORIG,
		PHOTO_MAX,
		PHOTO_MAX_ORIG,
		PHOTO_ID,
		CROP_PHOTO,
		CAN_CREATE_TOPIC,
		CAN_POST,
		MANAGEMENT,
		COUNTERS,
		DESCRIPTION,
		HAS_PHOTO,
		IS_FAVORITE,
		LINKS,
		MEMBER_STATUS,
		MEMBERS_COUNT,
		PLACE,
		SITE,
		START_DATE,
		FINISH_DATE,
		;

		public static Field valueOfApi(String v){
			return switch(v){
				case "domain" -> DOMAIN;
				case "screen_name" -> SCREEN_NAME;
				case "status" -> STATUS;
				case "url" -> URL;
				case "is_admin" -> IS_ADMIN;
				case "admin_level" -> ADMIN_LEVEL;
				case "is_member" -> IS_MEMBER;
				case "photo_50" -> PHOTO_50;
				case "photo_100" -> PHOTO_100;
				case "photo_200" -> PHOTO_200;
				case "photo_400" -> PHOTO_400;
				case "photo_200_orig" -> PHOTO_200_ORIG;
				case "photo_400_orig" -> PHOTO_400_ORIG;
				case "photo_max" -> PHOTO_MAX;
				case "photo_max_orig" -> PHOTO_MAX_ORIG;
				case "photo_id" -> PHOTO_ID;
				case "crop_photo" -> CROP_PHOTO;
				case "can_create_topic" -> CAN_CREATE_TOPIC;
				case "can_post" -> CAN_POST;
				case "management" -> MANAGEMENT;
				case "counters" -> COUNTERS;
				case "description" -> DESCRIPTION;
				case "has_photo" -> HAS_PHOTO;
				case "is_favorite" -> IS_FAVORITE;
				case "links" -> LINKS;
				case "member_status" -> MEMBER_STATUS;
				case "members_count" -> MEMBERS_COUNT;
				case "place" -> PLACE;
				case "site" -> SITE;
				case "start_date" -> START_DATE;
				case "finish_date" -> FINISH_DATE;
				default -> null;
			};
		}
	}
}
