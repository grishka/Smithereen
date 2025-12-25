package smithereen.api.methods;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPhoto;
import smithereen.api.model.ApiPhotoAlbum;
import smithereen.api.model.ApiPrivacySetting;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.media.MediaFileUploadPurpose;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.ImageRect;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoTag;
import smithereen.storage.MediaStorageUtils;
import smithereen.text.FormattedTextFormat;
import smithereen.util.NamedMutexCollection;
import smithereen.util.XTEA;

import static smithereen.Utils.*;

public class PhotosMethods{
	private static NamedMutexCollection uploadMutex=new NamedMutexCollection();

	public static Object getAttachmentUploadServer(ApplicationContext ctx, ApiCallContext actx){
		return Map.of("upload_url", ApiUtils.getUploadURL(actx, "uploadAttachmentPhoto", Map.of()));
	}

	public static Object getUploadServer(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("album_id"), ObfuscatedObjectIDType.PHOTO_ALBUM);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(id, actx.self.user);
		if(album.systemType!=null || !actx.permissions.canUploadToPhotoAlbum(album))
			throw new UserActionNotAllowedException();
		return Map.of("upload_url", ApiUtils.getUploadURL(actx, "uploadAlbumPhoto", Map.of("oid", album.ownerID)));
	}

	public static Object getOwnerPhotoUploadServer(ApplicationContext ctx, ApiCallContext actx){
		int groupID=actx.optParamIntPositive("group_id");
		if(groupID!=0){
			actx.requirePermission(ClientAppPermission.GROUPS_WRITE);
			Group g=ctx.getGroupsController().getGroupOrThrow(groupID);
			ctx.getGroupsController().enforceUserAdminLevel(g, actx.self.user, Group.AdminLevel.ADMIN);
		}
		return Map.of("upload_url", ApiUtils.getUploadURL(actx, "uploadAvatar", Map.of("oid", groupID==0 ? actx.self.id : -groupID)));
	}

	public static Object getAlbums(ApplicationContext ctx, ApiCallContext actx){
		Actor owner;
		if(actx.self==null || actx.hasParam("owner_id")){
			int ownerID=actx.requireParamIntNonZero("owner_id");
			if(ownerID>0)
				owner=ctx.getUsersController().getUserOrThrow(ownerID);
			else
				owner=ctx.getGroupsController().getGroupOrThrow(-ownerID);
		}else{
			owner=actx.self.user;
		}

		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbums(owner, actx.hasPermission(ClientAppPermission.PHOTOS_READ)
				? actx.self.user : null, actx.booleanParam("need_system"), false);
		int total=albums.size();
		int offset=actx.getOffset();
		int count=actx.getCount(albums.size()-offset, albums.size()-offset);
		if(offset>0 || count<albums.size()){
			if(offset>=albums.size())
				return new ApiPaginatedList<>(total, List.of());
			albums=albums.subList(offset, offset+count);
		}

		Map<Long, Photo> coverPhotos;
		if(actx.booleanParam("need_covers")){
			coverPhotos=ctx.getPhotosController().getPhotosIgnoringPrivacy(albums.stream().map(a->a.coverID).filter(id->id!=0).toList());
		}else{
			coverPhotos=null;
		}

		return new ApiPaginatedList<>(total, albums.stream().map(a->new ApiPhotoAlbum(a, actx, owner, coverPhotos)).toList());
	}

	public static Object getAlbumsById(ApplicationContext ctx, ApiCallContext actx){
		List<Long> ids=actx.requireCommaSeparatedStringList("album_ids")
				.stream()
				.limit(100)
				.filter(XTEA::isValidObjectID)
				.map(id->XTEA.decodeObjectID(id, ObfuscatedObjectIDType.PHOTO_ALBUM))
				.toList();

		Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbums(ids, actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null);
		if(albums.isEmpty())
			return List.of();

		HashMap<Integer, Actor> owners=new HashMap<>();
		Set<Integer> needUsers=albums.values().stream().map(a->a.ownerID).filter(id->id>0).collect(Collectors.toSet());
		Set<Integer> needGroups=albums.values().stream().map(a->a.ownerID).filter(id->id<0).map(id->-id).collect(Collectors.toSet());
		if(!needUsers.isEmpty()){
			owners.putAll(ctx.getUsersController().getUsers(needUsers));
		}
		if(!needGroups.isEmpty()){
			for(Group g:ctx.getGroupsController().getGroupsByIdAsList(needGroups))
				owners.put(-g.id, g);
		}

		Map<Long, Photo> coverPhotos;
		if(actx.booleanParam("need_covers")){
			coverPhotos=ctx.getPhotosController().getPhotosIgnoringPrivacy(albums.values().stream().map(a->a.coverID).filter(id->id!=0).toList());
		}else{
			coverPhotos=null;
		}

		return ids.stream()
				.map(albums::get)
				.filter(Objects::nonNull)
				.map(a->new ApiPhotoAlbum(a, actx, owners.get(a.ownerID), coverPhotos))
				.toList();
	}

	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		String albumID=actx.requireParamString("album_id");
		PhotoAlbum album=switch(albumID){
			case "profile" -> ctx.getPhotosController().getSystemAlbum(ApiUtils.getOwnerOrSelf(ctx, actx, "owner_id"), PhotoAlbum.SystemAlbumType.AVATARS);
			case "saved" -> ctx.getPhotosController().getSystemAlbum(ApiUtils.getOwnerOrSelf(ctx, actx, "owner_id"), PhotoAlbum.SystemAlbumType.SAVED);
			default -> {
				if(!XTEA.isValidObjectID(albumID))
					throw new ObjectNotFoundException();
				yield ctx.getPhotosController().getAlbum(XTEA.decodeObjectID(albumID, ObfuscatedObjectIDType.PHOTO_ALBUM),
						actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null);
			}
		};

		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null,
				album, actx.getOffset(), actx.getCount(50, 1000), actx.booleanParam("rev"));
		return new ApiPaginatedList<>(photos.total, ApiUtils.getPhotos(ctx, actx, photos.list));
	}
	
	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		List<Long> ids=actx.requireCommaSeparatedStringList("photo_ids")
				.stream()
				.limit(1000)
				.filter(XTEA::isValidObjectID)
				.map(id->XTEA.decodeObjectID(id, ObfuscatedObjectIDType.PHOTO))
				.toList();

		Map<Long, Photo> allPhotos=ctx.getPhotosController().getPhotosIgnoringPrivacy(ids);
		if(allPhotos.isEmpty())
			return List.of();

		Set<Long> needAlbums=allPhotos.values().stream().map(p->p.albumID).collect(Collectors.toSet());

		// The presence of an album in this map indicates that the user can view it.
		Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbums(needAlbums, actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null);
		if(albums.isEmpty())
			return List.of();

		List<Photo> photos=ids.stream()
				.map(allPhotos::get)
				.filter(p->p!=null && albums.containsKey(p.albumID))
				.toList();
		return ApiUtils.getPhotos(ctx, actx, photos);
	}

	public static Object getAll(ApplicationContext ctx, ApiCallContext actx){
		Actor owner=ApiUtils.getOwnerOrSelf(ctx, actx, "owner_id");
		PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotos(owner, actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null,
				actx.getOffset(), actx.getCount(50, 1000));
		return new ApiPaginatedList<>(photos.total, ApiUtils.getPhotos(ctx, actx, photos.list));
	}

	public static Object getUserPhotos(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");
		PaginatedList<Photo> photos=ctx.getPhotosController().getUserTaggedPhotos(actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null,
				user, actx.getOffset(), actx.getCount(50, 1000), actx.booleanParam("rev"));
		return new ApiPaginatedList<>(photos.total, ApiUtils.getPhotos(ctx, actx, photos.list));
	}

	public static Object getNewTags(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<Photo> photos=ctx.getPhotosController().getUserUnapprovedTaggedPhotos(actx.self.user, actx.getOffset(), actx.getCount(50, 100));
		List<ApiPhoto> apiPhotos=ApiUtils.getPhotos(ctx, actx, photos.list);
		Map<Long, List<PhotoTag>> allTags=ctx.getPhotosController().getTagsForPhotos(photos.list.stream().map(p->p.id).collect(Collectors.toSet()));
		int selfID=actx.self.user.id;
		for(ApiPhoto p:apiPhotos){
			List<PhotoTag> tags=allTags.get(p.rawID);
			if(tags!=null){
				for(PhotoTag t:tags){
					if(t.userID()==selfID){
						p.placerId=t.placerID();
						p.tagId=t.id();
						p.tagCreated=t.createdAt().getEpochSecond();
						break;
					}
				}
			}
		}
		return new ApiPaginatedList<>(photos.total, apiPhotos);
	}

	public static Object createAlbum(ApplicationContext ctx, ApiCallContext actx){
		String title=actx.requireParamString("title");
		if(title.length()<2)
			throw actx.error(ApiErrorType.PARAM_INVALID, "title must be at least 2 characters long");
		String description=actx.optParamString("description", "");
		try{
			long albumID;
			Actor owner;
			if(actx.hasParam("group_id")){
				Group group=ctx.getGroupsController().getGroupOrThrow(actx.requireParamIntPositive("group_id"));
				albumID=ctx.getPhotosController().createAlbum(actx.self.user, group, title, description, actx.booleanParam("comments_disabled"), actx.booleanParam("upload_by_admins_only"));
				owner=group;
			}else{
				ApiPrivacySetting viewPrivacy=actx.optParamJsonObject("privacy_view", ApiPrivacySetting.class);
				ApiPrivacySetting commentPrivacy=actx.optParamJsonObject("privacy_comment", ApiPrivacySetting.class);
				albumID=ctx.getPhotosController().createAlbum(actx.self.user, title, description,
						viewPrivacy==null ? PrivacySetting.DEFAULT : viewPrivacy.toNativePrivacySetting(),
						commentPrivacy==null ? PrivacySetting.DEFAULT : commentPrivacy.toNativePrivacySetting());
				owner=actx.self.user;
			}
			return new ApiPhotoAlbum(ctx.getPhotosController().getAlbumIgnoringPrivacy(albumID), actx, owner, null);
		}catch(UserErrorException x){
			if("err_too_many_photo_albums".equals(x.getMessage()))
				throw actx.error(ApiErrorType.TOO_MANY_PHOTO_ALBUMS);
			else
				throw x;
		}
	}

	public static Object editAlbum(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("album_id"), ObfuscatedObjectIDType.PHOTO_ALBUM);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(id, actx.self.user);
		String title=actx.optParamString("title", album.title);
		if(title.length()<2)
			throw actx.error(ApiErrorType.PARAM_INVALID, "title must be at least 2 characters long");
		String description=actx.optParamString("description", album.description);
		if(album.ownerID>0){
			PrivacySetting viewPrivacy=actx.hasParam("privacy_view") ? actx.optParamJsonObject("privacy_view", ApiPrivacySetting.class).toNativePrivacySetting() : album.viewPrivacy;
			PrivacySetting commentPrivacy=actx.hasParam("privacy_comment") ? actx.optParamJsonObject("privacy_comment", ApiPrivacySetting.class).toNativePrivacySetting() : album.commentPrivacy;
			ctx.getPhotosController().updateUserAlbum(actx.self.user, album, title, description, viewPrivacy, commentPrivacy);
		}else{
			boolean disableCommenting=actx.hasParam("comments_disabled") ? actx.booleanParam("comments_disabled") : album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
			boolean restrictUploads=actx.hasParam("upload_by_admin_only") ? actx.booleanParam("upload_by_admin_only") : album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
			ctx.getPhotosController().updateGroupAlbum(actx.self.user, album, title, description, disableCommenting, restrictUploads);
		}
		return true;
	}

	public static Object deleteAlbum(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("album_id"), ObfuscatedObjectIDType.PHOTO_ALBUM);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(id, actx.self.user);
		ctx.getPhotosController().deleteAlbum(actx.self.user, album);
		return true;
	}

	public static Object edit(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, photo);
		String caption=actx.optParamString("caption", "");
		FormattedTextFormat textFormat=ApiUtils.getTextFormat(actx);
		ctx.getPhotosController().updatePhotoDescription(actx.self.user, photo, caption, textFormat);
		return true;
	}

	public static Object makeCover(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		long albumID=XTEA.decodeObjectID(actx.requireParamString("album_id"), ObfuscatedObjectIDType.PHOTO_ALBUM);
		if(photo.albumID!=albumID)
			throw new ObjectNotFoundException();
		PhotoAlbum album=ctx.getPhotosController().getAlbum(albumID, actx.self.user);
		ctx.getPhotosController().setPhotoAsAlbumCover(actx.self.user, album, photo);
		return true;
	}

	public static Object delete(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, photo);
		ctx.getPhotosController().deletePhoto(actx.self.user, photo);
		return true;
	}

	public static Object copy(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, photo);
		Photo copy=ctx.getPhotosController().savePhotoToAlbum(actx.self.user, photo);
		return copy.getIdString();
	}

	public static Object getTags(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null, photo);
		List<PhotoTag> tags=ctx.getPhotosController().getTagsForPhoto(photo.id);
		if(tags.isEmpty())
			return List.of();
		Map<Integer, User> users=ctx.getUsersController().getUsers(tags.stream().map(PhotoTag::userID).filter(id->id>0).collect(Collectors.toSet()));

		record ApiPhotoTag(@Nullable Integer userId, long id, int placerId, @NotNull String name, float x1, float y1, float x2, float y2, long date, @Nullable Boolean confirmed){
		}
		return tags.stream()
				.map(pt->new ApiPhotoTag(pt.approved() ? pt.userID() : null, pt.id(), pt.placerID(),
						pt.approved() ? (users.containsKey(pt.userID()) ? users.get(pt.userID()).getFullName() : "DELETED") : pt.name(),
						pt.rect().x1(), pt.rect().y1(), pt.rect().x2(), pt.rect().y2(), pt.createdAt().getEpochSecond(), pt.userID()>0 ? pt.approved() : null))
				.toList();
	}

	public static Object putTag(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, photo);

		int userID=actx.optParamIntPositive("user_id");
		User user=null;
		String name;
		if(userID==0){
			name=actx.requireParamString("name");
		}else{
			user=ctx.getUsersController().getUserOrThrow(userID);
			if(ctx.getFriendsController().getSimpleFriendshipStatus(actx.self.user, user)!=FriendshipStatus.FRIENDS)
				throw actx.error(ApiErrorType.NO_PERMISSION, "this user is not a friend of the current user");
			name=user.getFullName();
		}
		float x1=actx.requireParamFloatInRange("x1", 0, 1);
		float y1=actx.requireParamFloatInRange("y1", 0, 1);
		float x2=actx.requireParamFloatInRange("x2", 0, 1);
		float y2=actx.requireParamFloatInRange("y2", 0, 1);
		if(x1>=x2 || y1>=y2)
			throw actx.paramError("invalid tag rect (x1 >= x2 || y1 >= y2)");
		ImageRect rect=new ImageRect(x1, y1, x2, y2);
		return ctx.getPhotosController().createPhotoTag(actx.self.user, photo, user, name, rect);
	}

	public static Object removeTag(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		// Not enforcing privacy to allow deleting current user's tags from photos that they can't access
		long tagID=actx.requireParamIntPositive("tag_id");
		ctx.getPhotosController().deletePhotoTag(actx.self.user, photo, tagID);
		return true;
	}

	public static Object confirmTag(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		long tagID=actx.requireParamIntPositive("tag_id");
		ctx.getPhotosController().approvePhotoTag(actx.self.user, photo, tagID);
		return true;
	}

	public static Object getComments(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceObjectPrivacy(actx.hasPermission(ClientAppPermission.PHOTOS_READ) ? actx.self.user : null, photo);

		return ApiUtils.getObjectComments(ctx, actx, photo);
	}

	public static Object createComment(ApplicationContext ctx, ApiCallContext actx){
		String photoID=actx.requireParamString("photo_id");
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		return ApiUtils.createComment(ctx, actx, photo);
	}

	public static Object deleteComment(ApplicationContext ctx, ApiCallContext actx){
		String commentID=actx.requireParamString("comment_id");
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(commentID, ObfuscatedObjectIDType.COMMENT));
		if(comment.parentObjectID.type()!=CommentableObjectType.PHOTO)
			throw new ObjectNotFoundException();
		ctx.getCommentsController().deleteComment(actx.self.user, comment);
		return true;
	}

	public static Object getCommentEditSource(ApplicationContext ctx, ApiCallContext actx){
		return ApiUtils.getCommentEditSource(ctx, actx, CommentableObjectType.PHOTO);
	}

	public static Object editComment(ApplicationContext ctx, ApiCallContext actx){
		String commentID=actx.requireParamString("comment_id");
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(commentID, ObfuscatedObjectIDType.COMMENT));
		if(comment.parentObjectID.type()!=CommentableObjectType.PHOTO)
			throw new ObjectNotFoundException();
		return ApiUtils.editComment(ctx, actx, comment);
	}

	public static Object getFeedEntry(ApplicationContext ctx, ApiCallContext actx){
		String listID=actx.requireParamString("list_id");
		String[] listParts=listID.split("/");
		if(listParts.length!=5)
			throw actx.paramError("list_id is invalid");

		String feedType=listParts[0];
		int id=safeParseInt(listParts[1]);
		int expectedTypeOrdinal=parseIntOrDefault(listParts[2], -1);
		int expectedAuthorID=safeParseInt(listParts[3]);
		long rawExpectedTimestamp=safeParseLong(listParts[4]);
		if(rawExpectedTimestamp==0)
			throw actx.paramError("list_id is invalid");
		Instant expectedTimestamp=Instant.ofEpochSecond(rawExpectedTimestamp);
		NewsfeedEntry.Type expectedType=expectedTypeOrdinal>=0 && expectedTypeOrdinal<NewsfeedEntry.Type.values().length ? NewsfeedEntry.Type.values()[expectedTypeOrdinal] : null;
		if(expectedType!=NewsfeedEntry.Type.ADD_PHOTO && expectedType!=NewsfeedEntry.Type.PHOTO_TAG)
			throw actx.paramError("list_id is invalid");
		int offset=actx.getOffset();
		int count=actx.getCount(50, 1000);
		PaginatedList<NewsfeedEntry> feed=switch(feedType){
			case "friends" -> ctx.getNewsfeedController().getFriendsGroupedEntries(actx.self.user, expectedType, ctx.getUsersController().getUserOrThrow(expectedAuthorID),
					id, expectedTimestamp, actx.self.prefs.timeZone, offset, count);
			case "groups" -> ctx.getNewsfeedController().getGroupsGroupedEntries(actx.self.user, expectedType, ctx.getGroupsController().getGroupOrThrow(-expectedAuthorID),
					id, expectedTimestamp, actx.self.prefs.timeZone, offset, count);
			default -> throw actx.paramError("list_id is invalid");
		};
		if(feed.list.isEmpty())
			throw new ObjectNotFoundException();

		List<Long> photoIDs=feed.list.stream().map(e->e.objectID).toList();
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs);
		return new ApiPaginatedList<>(feed.total, ApiUtils.getPhotos(ctx, actx, photoIDs.stream().map(photos::get).toList()));
	}

	public static Object save(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("album_id"), ObfuscatedObjectIDType.PHOTO_ALBUM);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(id, actx.self.user);
		if(album.systemType!=null || !actx.permissions.canUploadToPhotoAlbum(album))
			throw new UserActionNotAllowedException();
		long fileID=XTEA.decodeObjectID(actx.requireParamString("id"), ObfuscatedObjectIDType.MEDIA_FILE);
		LocalImage img=MediaStorageUtils.getLocalImage(fileID, actx.requireParamString("hash"), MediaFileUploadPurpose.ALBUM_PHOTO, album.ownerID);
		if(img==null)
			throw actx.paramError("invalid file");
		String mutexName="albumFile"+fileID;
		try{
			// Prevent a possible race condition when someone tries to call photos.save multiple times in parallel for the same file.
			// If I don't do this, someone will report me a bug that "oh hey I can upload a file once and make duplicate photos sometimes" sooner or later.
			uploadMutex.acquire(mutexName);

			long photoID=ctx.getPhotosController().createPhoto(actx.self.user, album, fileID, actx.optParamString("caption"), ApiUtils.getTextFormat(actx));
			return new ApiPhoto(ctx.getPhotosController().getPhotoIgnoringPrivacy(photoID), actx, null, null);
		}finally{
			uploadMutex.release(mutexName);
		}
	}

	public static Object saveOwnerPhoto(ApplicationContext ctx, ApiCallContext actx){
		int groupID=actx.optParamIntPositive("group_id");
		Group group;
		if(groupID!=0){
			actx.requirePermission(ClientAppPermission.GROUPS_WRITE);
			group=ctx.getGroupsController().getGroupOrThrow(groupID);
			ctx.getGroupsController().enforceUserAdminLevel(group, actx.self.user, Group.AdminLevel.ADMIN);
		}else{
			group=null;
		}
		long fileID=XTEA.decodeObjectID(actx.requireParamString("id"), ObfuscatedObjectIDType.MEDIA_FILE);
		LocalImage img=MediaStorageUtils.getLocalImage(fileID, actx.requireParamString("hash"), MediaFileUploadPurpose.ALBUM_PHOTO, groupID==0 ? actx.self.user.id : -groupID);
		if(img==null)
			throw actx.paramError("invalid file");
		String mutexName="avatarFile"+fileID;
		AvatarCropRects cropRects=null;
		if(actx.hasParam("crop_x1") && actx.hasParam("crop_y1") && actx.hasParam("crop_x2") && actx.hasParam("crop_y2")
				&& actx.hasParam("square_x1") && actx.hasParam("square_y1") && actx.hasParam("square_x2") && actx.hasParam("square_y2")){
			cropRects=new AvatarCropRects(
					new ImageRect(
							actx.requireParamFloatInRange("crop_x1", 0, 1),
							actx.requireParamFloatInRange("crop_y1", 0, 1),
							actx.requireParamFloatInRange("crop_x2", 0, 1),
							actx.requireParamFloatInRange("crop_y2", 0, 1)
					),
					new ImageRect(
							actx.requireParamFloatInRange("square_x1", 0, 1),
							actx.requireParamFloatInRange("square_y1", 0, 1),
							actx.requireParamFloatInRange("square_x2", 0, 1),
							actx.requireParamFloatInRange("square_y2", 0, 1)
					)
			);
		}
		try{
			uploadMutex.acquire(mutexName);
			long photoID=ctx.getPhotosController().updateAvatar(actx.self, group, img, cropRects, ctx.getAppsController().getAppByID(actx.token.appID()));
			return new ApiPhoto(ctx.getPhotosController().getPhotoIgnoringPrivacy(photoID), actx, null, null);
		}finally{
			uploadMutex.release(mutexName);
		}
	}
}
