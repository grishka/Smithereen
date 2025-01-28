package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubTaggedPerson;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionQueryResult;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.Account;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.FriendshipStatus;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.PrivacySetting;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserNotifications;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.AbsoluteImageRect;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.ImageRect;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoMetadata;
import smithereen.model.photos.PhotoTag;
import smithereen.model.photos.TaggedPhotosPseudoAlbum;
import smithereen.storage.CommentStorage;
import smithereen.storage.GroupStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.UserStorage;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import smithereen.text.TextProcessor;
import spark.Request;
import spark.utils.StringUtils;

public class PhotosController{
	private static final Logger LOG=LoggerFactory.getLogger(PhotosController.class);

	public static final int MAX_ALBUMS_PER_OWNER=70;
	public static final int MAX_PHOTOS_PER_ALBUM=5000;
	public static final int MAX_TAGS_PER_PHOTO=50;

	private final ApplicationContext context;
	private final Object albumCacheLock=new Object();
	private final Object albumCreationLock=new Object();
	private final Object photoCreationLock=new Object();
	private final LruCache<Integer, List<PhotoAlbum>> albumListCache=new LruCache<>(500);
	private final LruCache<Long, PhotoAlbum> albumCache=new LruCache<>(10_000);

	public PhotosController(ApplicationContext context){
		this.context=context;
	}

	public List<PhotoAlbum> getAllAlbums(Actor owner, User self, boolean needSystem, boolean needTagged){
		try{
			if(owner instanceof Group group){
				context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			}
			List<PhotoAlbum> albums=albumListCache.get(owner.getOwnerID());
			if(albums==null){
				albums=PhotoStorage.getAllAlbums(owner.getOwnerID());
				albumListCache.put(owner.getOwnerID(), albums);
			}
			for(PhotoAlbum album:albums){
				albumCache.put(album.id, album);
			}
			albums=new ArrayList<>(albums);
			if(needTagged && owner instanceof User user){
				try{
					PhotoAlbum album=getUserTaggedPhotosPseudoAlbum(self, user);
					if(album!=null)
						albums.addFirst(album);
				}catch(UserActionNotAllowedException ignore){}
			}
			if(needSystem){
				List<PhotoAlbum> systemAlbums=PhotoStorage.getSystemAlbums(owner.getOwnerID());
				if(!systemAlbums.isEmpty()){
					albums.addAll(0, systemAlbums);
				}
			}
			return switch(owner){
				case User user when self!=null && user.id==self.id -> albums;
				case User user -> albums.stream().filter(a->context.getPrivacyController().checkUserPrivacy(self, user, a.viewPrivacy)).toList();
				default -> albums;
			};
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PhotoAlbum getUserTaggedPhotosPseudoAlbum(User self, User user){
		PaginatedList<Photo> taggedPhotos=getUserTaggedPhotos(self, user, 0, 1);
		if(taggedPhotos.total>0){
			PhotoAlbum pseudoAlbum=new TaggedPhotosPseudoAlbum();
			pseudoAlbum.ownerID=user.id;
			pseudoAlbum.systemType=PhotoAlbum.SystemAlbumType.TAGGED;
			pseudoAlbum.coverID=taggedPhotos.list.getFirst().id;
			pseudoAlbum.numPhotos=taggedPhotos.total;
			pseudoAlbum.title="Tagged photos";
			pseudoAlbum.viewPrivacy=PrivacySetting.DEFAULT;
			return pseudoAlbum;
		}
		return null;
	}

	public List<PhotoAlbum> getAllAlbumsIgnoringPrivacy(Actor owner){
		try{
			List<PhotoAlbum> albums=albumListCache.get(owner.getOwnerID());
			if(albums==null){
				albums=PhotoStorage.getAllAlbums(owner.getOwnerID());
				albumListCache.put(owner.getOwnerID(), albums);
			}
			for(PhotoAlbum album:albums){
				albumCache.put(album.id, album);
			}
			return albums;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<PhotoAlbum> getAllAlbumsForActivityPub(Actor owner, Request req){
		try{
			if(owner instanceof Group group){
				context.getPrivacyController().enforceGroupContentAccess(req, group);
			}
			List<PhotoAlbum> albums=albumListCache.get(owner.getOwnerID());
			if(albums==null){
				albums=PhotoStorage.getAllAlbums(owner.getOwnerID());
				albumListCache.put(owner.getOwnerID(), albums);
			}
			if(owner instanceof User user){
				String domain=ActivityPub.getRequesterDomain(req);
				albums=albums.stream().filter(a->context.getPrivacyController().checkUserPrivacyForRemoteServer(domain, user, a.viewPrivacy)).toList();
			}
			List<PhotoAlbum> systemAlbums=PhotoStorage.getSystemAlbums(owner.getOwnerID());
			if(!systemAlbums.isEmpty()){
				albums=new ArrayList<>(albums);
				albums.addAll(0, systemAlbums);
			}
			return albums;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PhotoAlbum> getRandomAlbumsForProfile(Actor owner, User self, int count){
		List<PhotoAlbum> filteredAlbums=new ArrayList<>(getAllAlbums(owner, self, false, false));
		Collections.shuffle(filteredAlbums);
		return new PaginatedList<>(filteredAlbums.subList(0, Math.min(filteredAlbums.size(), count)), filteredAlbums.size());
	}

	public PaginatedList<PhotoAlbum> getMostRecentAlbums(Actor owner, User self, int count, boolean onlyHavingCover){
		List<PhotoAlbum> filteredAlbums=new ArrayList<>(getAllAlbums(owner, self, false, false));
		int size=filteredAlbums.size();
		if(onlyHavingCover)
			filteredAlbums.removeIf(a->a.coverID==0);
		filteredAlbums.sort(Comparator.comparing(a->a.updatedAt));
		return new PaginatedList<>(filteredAlbums.subList(0, Math.min(filteredAlbums.size(), count)), size);
	}

	public PhotoAlbum getAlbum(long id, User self){
		PhotoAlbum album=getAlbumIgnoringPrivacy(id);
		if(album.ownerID>0){
			User owner=context.getUsersController().getUserOrThrow(album.ownerID);
			context.getPrivacyController().enforceUserPrivacy(self, owner, album.viewPrivacy, true);
		}else{
			Group owner=context.getGroupsController().getGroupOrThrow(-album.ownerID);
			context.getPrivacyController().enforceUserAccessToGroupContent(self, owner);
		}
		return album;
	}

	public PhotoAlbum getAlbumForActivityPub(long id, Request req){
		PhotoAlbum album=getAlbumIgnoringPrivacy(id);
		context.getPrivacyController().enforceContentPrivacyForActivityPub(req, album);
		return album;
	}

	public Map<Long, PhotoAlbum> getAlbumsIgnoringPrivacy(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		if(ids.size()==1){
			long id=ids.iterator().next();
			try{
				return Map.of(id, getAlbumIgnoringPrivacy(id));
			}catch(ObjectNotFoundException x){
				return Map.of();
			}
		}
		try{
			HashMap<Long, PhotoAlbum> result=new HashMap<>();
			HashSet<Long> remainingIDs=new HashSet<>();
			for(long id:ids){
				PhotoAlbum album=albumCache.get(id);
				if(album==null)
					remainingIDs.add(id);
				else
					result.put(id, album);
			}
			if(!remainingIDs.isEmpty()){
				Map<Long, PhotoAlbum> extraAlbums=PhotoStorage.getAlbums(remainingIDs);
				for(PhotoAlbum album:extraAlbums.values())
					albumCache.put(album.id, album);
				result.putAll(extraAlbums);
			}
			return result;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PhotoAlbum getAlbumIgnoringPrivacy(long id){
		try{
			PhotoAlbum album=albumCache.get(id);
			if(album==null){
				album=PhotoStorage.getAlbum(id);
				if(album!=null)
					albumCache.put(id, album);
			}
			if(album==null)
				throw new ObjectNotFoundException();
			return album;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, PhotoAlbum> getAlbums(Collection<Long> ids, User self){
		Map<Long, PhotoAlbum> albums=getAlbumsIgnoringPrivacy(ids);
		if(albums.isEmpty())
			return Map.of();
		Map<Integer, User> users=context.getUsersController().getUsers(albums.values().stream().filter(a->a.ownerID>0).map(a->a.ownerID).collect(Collectors.toSet()));
		Map<Integer, Group> groups=context.getGroupsController().getGroupsByIdAsMap(albums.values().stream().filter(a->a.ownerID<0).map(a->-a.ownerID).collect(Collectors.toSet()));
		return albums.values().stream()
				.filter(a->{
					if(a.ownerID>0){
						return context.getPrivacyController().checkUserPrivacy(self, users.get(a.ownerID), a.viewPrivacy);
					}else{
						return context.getPrivacyController().canUserAccessGroupContent(self, groups.get(-a.ownerID));
					}
				})
				.collect(Collectors.toMap(a->a.id, Function.identity()));
	}

	public long createAlbum(User self, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy){
		try{
			long id;
			synchronized(albumCreationLock){
				if(PhotoStorage.getOwnerAlbumCount(Objects.requireNonNull(self).id)>=MAX_ALBUMS_PER_OWNER)
					throw new UserErrorException("err_too_many_photo_albums");
				id=PhotoStorage.createUserAlbum(self.id, Objects.requireNonNull(title), description, Objects.requireNonNull(viewPrivacy), Objects.requireNonNull(commentPrivacy));
			}
			albumListCache.remove(self.id);
			if(!viewPrivacy.isFullyPrivate())
				context.getActivityPubWorker().sendCreatePhotoAlbum(self, getAlbumIgnoringPrivacy(id));
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long createAlbum(User self, Group owner, String title, String description, boolean disableCommenting, boolean restrictUploads){
		try{
			context.getGroupsController().enforceUserAdminLevel(owner, self, Group.AdminLevel.MODERATOR);
			long id;
			synchronized(albumCreationLock){
				if(PhotoStorage.getOwnerAlbumCount(-owner.id)>=MAX_ALBUMS_PER_OWNER)
					throw new UserErrorException("err_too_many_photo_albums");
				id=PhotoStorage.createGroupAlbum(owner.id, title, description, disableCommenting, restrictUploads);
			}
			albumListCache.remove(-owner.id);
			context.getActivityPubWorker().sendCreatePhotoAlbum(owner, getAlbumIgnoringPrivacy(id));
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PhotoAlbum getSystemAlbum(Actor owner, PhotoAlbum.SystemAlbumType type){
		List<PhotoAlbum> cachedList=albumListCache.get(owner.getOwnerID());
		if(cachedList!=null){
			for(PhotoAlbum pa:cachedList){
				if(pa.systemType==type)
					return pa;
			}
		}
		try{
			PhotoAlbum pa=PhotoStorage.getSystemAlbum(owner.getOwnerID(), type);
			if(pa==null)
				throw new ObjectNotFoundException();
			return pa;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PhotoAlbum getOrCreateSystemAlbum(Actor owner, PhotoAlbum.SystemAlbumType type){
		try{
			return getSystemAlbum(owner, type);
		}catch(ObjectNotFoundException x){
			try{
				long id;
				synchronized(albumCreationLock){
					id=PhotoStorage.createSystemAlbum(owner.getOwnerID(), type);
				}
				albumListCache.remove(owner.getOwnerID());
				PhotoAlbum album=getAlbumIgnoringPrivacy(id);
				context.getActivityPubWorker().sendCreatePhotoAlbum(owner, album);
				return album;
			}catch(SQLException xx){
				throw new InternalServerErrorException(xx);
			}
		}
	}

	public boolean canManageAlbum(User self, @NotNull PhotoAlbum album){
		if(self==null)
			return false;
		if(album.ownerID>0){
			return self.id==album.ownerID;
		}else{
			Group group=context.getGroupsController().getGroupOrThrow(-album.ownerID);
			return context.getGroupsController().getMemberAdminLevel(group, self).isAtLeast(Group.AdminLevel.MODERATOR);
		}
	}

	public void enforceAlbumManagementPermission(User self, PhotoAlbum album){
		if(!canManageAlbum(self, album))
			throw new UserActionNotAllowedException();
	}

	public boolean canManagePhoto(User self, @NotNull Photo photo){
		if(self==null)
			return false;
		if(photo.ownerID>0){
			return photo.ownerID==self.id;
		}else{
			Group group=context.getGroupsController().getGroupOrThrow(-photo.ownerID);
			context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			return photo.authorID==self.id || context.getGroupsController().getMemberAdminLevel(group, self).isAtLeast(Group.AdminLevel.MODERATOR);
		}
	}

	public void enforcePhotoManagementPermission(User self, Photo photo){
		if(!canManagePhoto(self, photo))
			throw new UserActionNotAllowedException();
	}

	public void deleteAlbum(User self, PhotoAlbum album){
		enforceAlbumManagementPermission(self, album);
		deletePhotoAlbumInternal(album);
		if(!(self instanceof ForeignUser)){
			context.getActivityPubWorker().sendDeletePhotoAlbum(album.ownerID>0 ? self : context.getGroupsController().getGroupOrThrow(-album.ownerID), album);
		}
	}

	public void deleteAlbum(ForeignGroup self, PhotoAlbum album){
		if(album.ownerID!=-self.id)
			throw new UserActionNotAllowedException();
		deletePhotoAlbumInternal(album);
	}

	private void deletePhotoAlbumInternal(PhotoAlbum album){
		if(album.systemType!=null)
			throw new UserActionNotAllowedException();
		try{
			MediaStorage.deleteMediaFileReferences(PhotoStorage.getLocalPhotoIDsForAlbum(album.id), MediaFileReferenceType.ALBUM_PHOTO);
			deleteCommentsForAlbum(album.id);
			PhotoStorage.deleteAlbum(album.id, album.ownerID);
			albumListCache.remove(album.ownerID);
			albumCache.remove(album.id);
			if(album.ownerID>0){
				context.getNewsfeedController().clearFriendsFeedCache();
			}else{
				context.getNewsfeedController().clearGroupsFeedCache();
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateUserAlbum(User self, @NotNull PhotoAlbum album, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy){
		if(album.ownerID<0)
			throw new IllegalArgumentException();
		if(album.systemType!=null)
			throw new UserActionNotAllowedException();
		enforceAlbumManagementPermission(self, album);
		if(Objects.equals(album.title, title) && Objects.equals(album.description, description)
				&& Objects.equals(album.viewPrivacy, viewPrivacy) && Objects.equals(album.commentPrivacy, commentPrivacy))
			return;
		try{
			PhotoStorage.updateUserAlbum(album.id, title, description, viewPrivacy, commentPrivacy);
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(self.id);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.title=title;
							a.description=description;
							a.viewPrivacy=viewPrivacy;
							a.commentPrivacy=commentPrivacy;
							break;
						}
					}
				}
			}
			album.title=title;
			album.description=description;
			album.viewPrivacy=viewPrivacy;
			album.commentPrivacy=commentPrivacy;
			albumCache.put(album.id, album);
			context.getNewsfeedController().clearFriendsFeedCache();
			context.getActivityPubWorker().sendUpdatePhotoAlbum(self, album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateGroupAlbum(User self, @NotNull PhotoAlbum album, String title, String description, boolean disableCommenting, boolean restrictUploads){
		if(album.ownerID>0)
			throw new IllegalArgumentException();
		if(album.systemType!=null)
			throw new UserActionNotAllowedException();
		enforceAlbumManagementPermission(self, album);
		if(Objects.equals(album.title, title) && Objects.equals(album.description, description)
				&& album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING)==disableCommenting
				&& album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS)==restrictUploads)
			return;

		EnumSet<PhotoAlbum.Flag> newFlags=EnumSet.copyOf(album.flags);
		if(disableCommenting)
			newFlags.add(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
		else
			newFlags.remove(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
		if(restrictUploads)
			newFlags.add(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
		else
			newFlags.remove(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);

		try{
			PhotoStorage.updateGroupAlbum(album.id, title, description, newFlags);
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(self.id);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.title=title;
							a.description=description;
							a.flags=newFlags;
							break;
						}
					}
				}
			}
			album.title=title;
			album.description=description;
			album.flags=newFlags;
			albumCache.put(album.id, album);
			context.getNewsfeedController().clearGroupsFeedCache();
			context.getActivityPubWorker().sendUpdatePhotoAlbum(context.getGroupsController().getGroupOrThrow(-album.ownerID), album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long createPhoto(User self, @NotNull PhotoAlbum album, long fileID, String descriptionSource, FormattedTextFormat descriptionFormat){
		self.ensureLocal();
		Actor owner;
		if(album.ownerID>0){
			if(album.ownerID!=self.id)
				throw new UserActionNotAllowedException();
			owner=self;
		}else{
			Group group=context.getGroupsController().getGroupOrThrow(-album.ownerID);
			if(context.getPrivacyController().isUserBlocked(self, group))
				throw new UserActionNotAllowedException();
			context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			if(album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS))
				context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
			owner=group;
		}
		return createPhotoInternal(self, owner, album, fileID, descriptionSource, descriptionFormat, null);
	}

	private long createPhotoInternal(User self, Actor owner, @NotNull PhotoAlbum album, long fileID, String descriptionSource, FormattedTextFormat descriptionFormat, PhotoMetadata metadata){
		String parsedDescription=descriptionSource==null ? "" : TextProcessor.preprocessPostText(descriptionSource, null, descriptionFormat);
		try{
			long id;
			boolean needUpdateCover=!album.flags.contains(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
			synchronized(photoCreationLock){
				if(PhotoStorage.getAlbumSize(album.id)>=MAX_PHOTOS_PER_ALBUM)
					throw new UserErrorException("err_too_many_photos_in_album");
				id=PhotoStorage.createLocalPhoto(album.ownerID, self.id, album.id, fileID, parsedDescription, descriptionSource, descriptionFormat, metadata);
				if(needUpdateCover)
					PhotoStorage.setAlbumCover(album.id, id);
			}
			MediaStorage.createMediaFileReference(fileID, id, MediaFileReferenceType.ALBUM_PHOTO, album.ownerID);
			int numPhotos=0;
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(album.ownerID);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.numPhotos++;
							numPhotos=a.numPhotos;
							if(needUpdateCover)
								a.coverID=id;
							break;
						}
					}
				}
			}
			if(album.systemType==null){
				if(album.ownerID>0){
					context.getNewsfeedController().putFriendsFeedEntry(self, id, NewsfeedEntry.Type.ADD_PHOTO);
				}else{
					context.getNewsfeedController().putGroupsFeedEntry((Group) owner, id, NewsfeedEntry.Type.ADD_PHOTO);
				}
			}

			if(album.activityPubID!=null){
				Photo photo=getPhotoIgnoringPrivacy(id);
				context.getActivityPubWorker().sendCreateAlbumPhoto(self, photo, album);
			}else if(album.ownerID<0 || !album.viewPrivacy.isFullyPrivate()){
				Photo photo=getPhotoIgnoringPrivacy(id);
				context.getActivityPubWorker().sendAddPhotoToAlbum(owner, photo, album);
			}

			if(needUpdateCover){
				album.coverID=id;
				context.getActivityPubWorker().sendUpdatePhotoAlbum(owner, album);
			}
			if(numPhotos!=0)
				album.numPhotos=numPhotos;
			else
				album.numPhotos++;
			albumCache.put(album.id, album);
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deletePhoto(User self, Photo photo){
		enforcePhotoManagementPermission(self, photo);
		PhotoAlbum album=getAlbumIgnoringPrivacy(photo.albumID);
		try{
			deletePhotoInternal(photo, album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		if(photo.apID==null){
			if(photo.ownerID==self.id){ // Deleted by author
				context.getActivityPubWorker().sendDeleteAlbumPhoto(self, photo, album);
			}else if(photo.ownerID<0){ // Removed by group moderators
				context.getActivityPubWorker().sendRemoveAlbumPhoto(context.getGroupsController().getGroupOrThrow(-photo.ownerID), photo, album);
			}else{
				throw new IllegalStateException();
			}
		}
	}

	public void deletePhoto(Group self, Photo photo){
		if(photo.ownerID!=self.getOwnerID())
			throw new UserActionNotAllowedException();
		PhotoAlbum album=getAlbumIgnoringPrivacy(photo.albumID);
		try{
			deletePhotoInternal(photo, album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void deletePhotoInternal(Photo photo, PhotoAlbum album) throws SQLException{
		Actor owner=context.getWallController().getContentAuthorAndOwner(photo).owner();
		if(photo.metadata!=null && photo.metadata.correspondingPostID!=0){
			try{
				Post post=context.getWallController().getPostOrThrow(photo.metadata.correspondingPostID);
				context.getWallController().deletePost((User) owner, post);
			}catch(ObjectNotFoundException ignore){}
		}
		boolean needUpdateCover=album.coverID==photo.id;
		long newCoverID=0;
		synchronized(photoCreationLock){
			PhotoStorage.deletePhoto(photo.id, photo.albumID);
			if(needUpdateCover){
				newCoverID=PhotoStorage.getLastAlbumPhoto(photo.albumID);
				PhotoStorage.setAlbumCover(photo.albumID, newCoverID);
				if(album.flags.contains(PhotoAlbum.Flag.COVER_SET_EXPLICITLY)){
					EnumSet<PhotoAlbum.Flag> flags=EnumSet.copyOf(album.flags);
					flags.remove(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
					PhotoStorage.updateAlbumFlags(photo.albumID, flags);
				}
			}
		}
		if(photo.localFileID!=0){
			MediaStorage.deleteMediaFileReference(photo.id, MediaFileReferenceType.ALBUM_PHOTO, photo.localFileID);
		}
		int numPhotos=Integer.MAX_VALUE;
		synchronized(albumCacheLock){
			List<PhotoAlbum> albums=albumListCache.get(photo.ownerID);
			if(albums!=null){
				for(PhotoAlbum a:albums){
					if(a.id==photo.albumID){
						a.numPhotos--;
						numPhotos=a.numPhotos;
						if(needUpdateCover){
							a.coverID=newCoverID;
							a.flags.remove(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
						}
						break;
					}
				}
			}
		}
		if(numPhotos!=Integer.MAX_VALUE)
			album.numPhotos=numPhotos;
		else
			album.numPhotos--;
		LikeStorage.deleteAllLikesForObject(photo.id, Like.ObjectType.PHOTO);
		context.getCommentsController().deleteCommentsForObject(photo);
		NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.PHOTO, photo.id);
		if(album.ownerID>0){
			context.getNewsfeedController().deleteFriendsFeedEntry(context.getUsersController().getUserOrThrow(album.ownerID), photo.id, NewsfeedEntry.Type.ADD_PHOTO);
			context.getNewsfeedController().deleteFriendsFeedEntriesForObject(photo.id, NewsfeedEntry.Type.PHOTO_TAG);
		}else{
			context.getNewsfeedController().deleteGroupsFeedEntry(context.getGroupsController().getGroupOrThrow(-album.ownerID), photo.id, NewsfeedEntry.Type.ADD_PHOTO);
		}
		albumCache.put(album.id, album);

		// If they deleted their current avatar, find the newest photo in the avatars album and set it as the avatar
		if(owner.icon!=null && !owner.icon.isEmpty() && owner.icon.getFirst() instanceof LocalImage li && li.photoID==photo.id){
			if(album.systemType!=PhotoAlbum.SystemAlbumType.AVATARS)
				throw new InternalServerErrorException("Huh?!");
			try{
				if(album.numPhotos>0){
					Photo newAva=getPhotoIgnoringPrivacy(PhotoStorage.getLastAlbumPhoto(album.id));
					setPhotoToAvatar(owner, newAva);
				}else{
					deleteAvatar(owner);
				}
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
	}

	public PaginatedList<Photo> getAlbumPhotos(User self, PhotoAlbum album, int offset, int count, boolean reverseOrder){
		try{
			if(album.systemType==PhotoAlbum.SystemAlbumType.AVATARS)
				reverseOrder=!reverseOrder; // Avatars are always sorted newest to oldest by default
			return new PaginatedList<>(PhotoStorage.getAlbumPhotos(album.id, offset, count, reverseOrder), album.numPhotos, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, Photo> getPhotosIgnoringPrivacy(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return PhotoStorage.getPhotos(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Photo getPhotoIgnoringPrivacy(long id){
		try{
			Map<Long, Photo> m=PhotoStorage.getPhotos(List.of(id));
			Photo p=m.get(id);
			if(p==null)
				throw new ObjectNotFoundException();
			return p;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updatePhotoDescription(User self, Photo photo, String descriptionSource, FormattedTextFormat format){
		enforcePhotoManagementPermission(self, photo);
		try{
			String parsedDescription=TextProcessor.preprocessPostText(descriptionSource, null, format);
			PhotoStorage.updatePhotoDescription(photo.id, descriptionSource, parsedDescription, format);
			photo.description=parsedDescription;
			context.getActivityPubWorker().sendUpdateAlbumPhoto(self, photo, getAlbum(photo.albumID, self));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, FormattedTextSource> getPhotoDescriptionSources(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return PhotoStorage.getDescriptionSources(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setPhotoAsAlbumCover(User self, PhotoAlbum album, Photo photo){
		if(photo.albumID!=album.id)
			throw new UserErrorException("Photo is not in this album");
		if(album.systemType!=null)
			throw new UserActionNotAllowedException();
		enforceAlbumManagementPermission(self, album);
		try{
			PhotoStorage.setAlbumCover(album.id, photo.id);
			if(!album.flags.contains(PhotoAlbum.Flag.COVER_SET_EXPLICITLY)){
				album.flags.add(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
				PhotoStorage.updateAlbumFlags(album.id, album.flags);
			}
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(photo.ownerID);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==photo.albumID){
							a.coverID=photo.id;
							a.flags.add(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
							break;
						}
					}
				}
			}
			albumCache.put(album.id, album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long getAlbumIdByActivityPubId(URI activityPubID){
		try{
			long id=PhotoStorage.getAlbumIdByActivityPubId(activityPubID);
			return id==-1 ? 0 : id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long getPhotoIdByActivityPubId(URI activityPubID){
		try{
			long id=PhotoStorage.getPhotoIdByActivityPubId(activityPubID);
			return id==-1 ? 0 : id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<URI, Long> getPhotoIdsByActivityPubIds(Collection<URI> apIDs){
		if(apIDs.isEmpty())
			return Map.of();
		try{
			return PhotoStorage.getPhotoIdsByActivityPubIds(apIDs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putOrUpdateForeignAlbum(PhotoAlbum album){
		try{
			PhotoStorage.putOrUpdateForeignAlbum(album);
			synchronized(albumCacheLock){
				albumListCache.remove(album.ownerID);
			}
			if(album.ownerID>0)
				context.getNewsfeedController().clearFriendsFeedCache();
			else
				context.getNewsfeedController().clearGroupsFeedCache();
			album.numPhotos=PhotoStorage.getAlbumSize(album.id);
			albumCache.put(album.id, album);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putOrUpdateForeignPhoto(Photo photo, ActivityPubPhoto origObj){
		putOrUpdateForeignPhotos(List.of(new Pair<>(photo, origObj)));
	}

	public void putOrUpdateForeignPhotos(List<Pair<Photo, ActivityPubPhoto>> photos){
		try{
			record PhotoTagToQuery(long id, long photoID, URI photoApID){}
			record PendingUserCollectionQuery(ForeignUser user, ArrayList<PhotoTagToQuery> queries){}

			HashMap<URI, PendingUserCollectionQuery> userCollectionQueries=new HashMap<>();
			for(Pair<Photo, ActivityPubPhoto> p:photos){
				Photo photo=p.first();
				ActivityPubPhoto origObj=p.second();

				boolean isNew=photo.id==0;
				if(photo.ownerID<0){
					User self=context.getUsersController().getUserOrThrow(photo.authorID);
					Group group=context.getGroupsController().getGroupOrThrow(-photo.ownerID);
					if(context.getPrivacyController().isUserBlocked(self, group))
						throw new UserActionNotAllowedException();
				}

				PhotoStorage.putOrUpdateForeignPhoto(photo);
				List<PhotoTag> existingTags;
				if(isNew){
					existingTags=List.of();
					synchronized(albumCacheLock){
						albumListCache.remove(photo.ownerID);
						albumCache.remove(photo.albumID);
					}
					if(photo.ownerID>0){
						User owner=context.getUsersController().getUserOrThrow(photo.ownerID);
						context.getNewsfeedController().putFriendsFeedEntry(owner, photo.id, NewsfeedEntry.Type.ADD_PHOTO);
					}else{
						Group owner=context.getGroupsController().getGroupOrThrow(-photo.ownerID);
						context.getNewsfeedController().putGroupsFeedEntry(owner, photo.id, NewsfeedEntry.Type.ADD_PHOTO);
					}
				}else{
					existingTags=PhotoStorage.getPhotoTags(photo.id);
				}

				Set<URI> existingTagIDs=existingTags.stream().map(PhotoTag::apID).collect(Collectors.toSet());
				List<ActivityPubObject> apTags=origObj.tag==null ? List.of() : origObj.tag;
				HashSet<URI> newTagIDs=new HashSet<>();
				for(ActivityPubObject apTag: apTags){
					if(apTag instanceof ActivityPubTaggedPerson tp){
						if(tp.activityPubID==null)
							continue;
						newTagIDs.add(tp.activityPubID);
					}
				}
				List<PhotoTag> tagsToDelete=existingTags.stream().filter(t->!newTagIDs.contains(t.apID())).toList();
				if(!tagsToDelete.isEmpty()){
					PhotoStorage.deletePhotoTags(photo.id, tagsToDelete.stream().map(PhotoTag::id).collect(Collectors.toSet()));
					for(PhotoTag tag:tagsToDelete){
						if(tag.userID()!=0){
							if(!tag.approved()){
								UserNotifications un=NotificationsStorage.getNotificationsFromCache(tag.userID());
								if(un!=null)
									un.incNewPhotoTagCount(-1);
								try{
									User user=context.getUsersController().getUserOrThrow(tag.userID());
									context.getNotificationsController().sendRealtimeCountersUpdates(user);
								}catch(ObjectNotFoundException ignore){}
							}else{
								try{
									User user=context.getUsersController().getUserOrThrow(tag.userID());
									context.getNewsfeedController().deleteFriendsFeedEntry(user, photo.id, NewsfeedEntry.Type.PHOTO_TAG);
								}catch(ObjectNotFoundException ignore){}
							}
						}
					}
				}
				List<ActivityPubTaggedPerson> tagsToCreate=apTags.stream()
						.map(t->t instanceof ActivityPubTaggedPerson tp ? tp : null)
						.filter(t->t!=null && !existingTagIDs.contains(t.activityPubID))
						.toList();
				for(ActivityPubTaggedPerson tag:tagsToCreate){
					if(tag.rect==null || StringUtils.isEmpty(tag.name) || tag.attributedTo==null)
						continue;
					User placer;
					try{
						placer=context.getObjectLinkResolver().resolveLocally(tag.attributedTo, User.class);
					}catch(ObjectNotFoundException x){
						continue;
					}
					User user=null;
					try{
						if(tag.href!=null && Utils.uriHostMatches(placer.activityPubID, tag.href)){
							user=context.getObjectLinkResolver().resolve(tag.href, User.class, true, true, false);
							if(!context.getPrivacyController().checkUserPrivacy(placer, user, UserPrivacySettingKey.PHOTO_TAG))
								user=null;
						}
					}catch(ObjectNotFoundException|IllegalStateException ignore){}
					boolean actuallyApproved=tag.approved;
					if(tag.approved){
						if(user instanceof ForeignUser fu){
							if(fu.collectionQueryEndpoint!=null && !Utils.uriHostMatches(fu.activityPubID, photo.apID)){
								actuallyApproved=false;
							}
						}else{
							// Tags for local users always start non-approved
							actuallyApproved=false;
						}
					}
					long tagID=PhotoStorage.createPhotoTag(photo.id, placer.id, user==null ? 0 : user.id, tag.name, actuallyApproved, tag.rect, tag.activityPubID);
					if(tag.approved && !actuallyApproved && user instanceof ForeignUser fu && fu.collectionQueryEndpoint!=null && fu.getTaggedPhotosURL()!=null){
						userCollectionQueries.computeIfAbsent(user.activityPubID, id->new PendingUserCollectionQuery(fu, new ArrayList<>()))
								.queries.add(new PhotoTagToQuery(tagID, photo.id, photo.getActivityPubID()));
					}
					if(user!=null && !(user instanceof ForeignUser)){
						UserNotifications un=NotificationsStorage.getNotificationsFromCache(user.id);
						if(un!=null)
							un.incNewPhotoTagCount(1);
						context.getNotificationsController().sendRealtimeCountersUpdates(user);
					}
				}
				List<ActivityPubTaggedPerson> tagsToMaybeUpdate=apTags.stream()
						.map(t->t instanceof ActivityPubTaggedPerson tp ? tp : null)
						.filter(t->t!=null && existingTagIDs.contains(t.activityPubID))
						.toList();
				Map<URI, PhotoTag> existingTagsByID=existingTags.stream().collect(Collectors.toMap(PhotoTag::apID, Function.identity()));
				for(ActivityPubTaggedPerson tag:tagsToMaybeUpdate){
					PhotoTag existing=existingTagsByID.get(tag.activityPubID);
					if(!tag.rect.equals(existing.rect())){
						PhotoStorage.updatePhotoTagsRects(photo.id, Map.of(existing.id(), tag.rect));
					}
					if(tag.approved && !existing.approved() && existing.userID()!=0){
						try{
							User user=context.getUsersController().getUserOrThrow(existing.userID());
							if(user instanceof ForeignUser fu){
								if(!Utils.uriHostMatches(fu.activityPubID, photo.apID) && fu.collectionQueryEndpoint!=null && fu.getTaggedPhotosURL()!=null){
									userCollectionQueries.computeIfAbsent(user.activityPubID, id->new PendingUserCollectionQuery(fu, new ArrayList<>()))
											.queries.add(new PhotoTagToQuery(existing.id(), photo.id, photo.getActivityPubID()));
								}else{
									PhotoStorage.approvePhotoTag(photo.id, existing.id());
								}
							}
						}catch(ObjectNotFoundException ignore){}
					}
				}
			}
			for(PendingUserCollectionQuery query:userCollectionQueries.values()){
				Map<URI, PhotoTagToQuery> queriesByPhotoIDs=query.queries.stream().map(q->new Pair<>(q.photoApID, q)).collect(Collectors.toMap(Pair::first, Pair::second));
				CollectionQueryResult res=ActivityPub.performCollectionQuery(query.user, query.user.getTaggedPhotosURL(), query.queries.stream().map(PhotoTagToQuery::photoApID).toList());
				for(LinkOrObject item:res.items){
					if(item.link!=null && queriesByPhotoIDs.get(item.link) instanceof PhotoTagToQuery(long id, long photoID, URI photoApID)){
						PhotoStorage.approvePhotoTag(photoID, id);
					}
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteRemoteAlbumsNotInSet(Actor owner, Set<Long> ids){
		owner.ensureRemote();
		try{
			List<PhotoAlbum> albums=getAllAlbumsIgnoringPrivacy(owner)
					.stream()
					.filter(a->!ids.contains(a.id))
					.toList();
			if(!albums.isEmpty()){
				for(PhotoAlbum album:albums){
					MediaStorage.deleteMediaFileReferences(PhotoStorage.getLocalPhotoIDsForAlbum(album.id), MediaFileReferenceType.ALBUM_PHOTO);
					deleteCommentsForAlbum(album.id);
					PhotoStorage.deleteAlbum(album.id, album.ownerID);
					albumCache.remove(album.id);
				}
				albumListCache.remove(owner.getOwnerID());
				if(owner instanceof User){
					context.getNewsfeedController().clearFriendsFeedCache();
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteRemotePhotosNotInSet(PhotoAlbum album, Set<Long> ids){
		if(album.activityPubID==null)
			throw new IllegalArgumentException("Must be remote");
		try{
			Set<Long> idsToDelete=PhotoStorage.getAlbumPhotosNotInSet(album.id, ids);
			if(idsToDelete.isEmpty())
				return;

			MediaStorage.deleteMediaFileReferences(PhotoStorage.getLocalPhotoIDsIn(idsToDelete), MediaFileReferenceType.ALBUM_PHOTO);
			PhotoStorage.deletePhotos(album.id, idsToDelete);

			albumListCache.remove(album.ownerID);
			if(album.ownerID>0){
				context.getNewsfeedController().clearFriendsFeedCache();
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getPhotoIndexInAlbum(PhotoAlbum album, Photo photo){
		try{
			int index=PhotoStorage.getPhotoIndexInAlbum(album.id, photo.id);
			if(index==-1)
				throw new ObjectNotFoundException();
			return index;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void deleteCommentsForAlbum(long albumID) throws SQLException{
		for(Set<Long> commentIDs=CommentStorage.getPhotoAlbumCommentIDsForDeletion(albumID);!commentIDs.isEmpty();commentIDs=CommentStorage.getPhotoAlbumCommentIDsForDeletion(albumID)){
			MediaStorage.deleteMediaFileReferences(commentIDs, MediaFileReferenceType.COMMENT_ATTACHMENT);
			LikeStorage.deleteAllLikesForObjects(commentIDs, Like.ObjectType.COMMENT);
			CommentStorage.deleteComments(commentIDs);
		}
		CommentStorage.deleteCommentBookmarksForPhotoAlbum(albumID);
	}

	public Photo savePhotoToAlbum(User self, Photo photo){
		if(photo.ownerID==self.id)
			throw new UserActionNotAllowedException();
		long fileID;
		if(photo.apID!=null){
			try{
				fileID=MediaStorageUtils.copyRemoteImageToLocalStorage(self, photo.image).fileID;
			}catch(SQLException | IOException x){
				throw new InternalServerErrorException(x);
			}
		}else{
			fileID=photo.localFileID;
		}
		return saveFileToAlbum(self, fileID);
	}

	public Photo saveImageToAlbum(User self, SizedImage image){
		long fileID;
		if(image instanceof LocalImage li){
			fileID=li.fileID;
		}else{
			try{
				fileID=MediaStorageUtils.copyRemoteImageToLocalStorage(self, image).fileID;
			}catch(SQLException | IOException x){
				throw new InternalServerErrorException(x);
			}
		}
		return saveFileToAlbum(self, fileID);
	}

	private Photo saveFileToAlbum(User self, long fileID){
		PhotoAlbum album=getOrCreateSystemAlbum(self, PhotoAlbum.SystemAlbumType.SAVED);
		long id;
		try{
			id=PhotoStorage.createLocalPhoto(self.id, self.id, album.id, fileID, "", null, null, null);
			PhotoStorage.setAlbumCover(album.id, id);
			MediaStorage.createMediaFileReference(fileID, id, MediaFileReferenceType.ALBUM_PHOTO, album.ownerID);
			int numPhotos=0;
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(album.ownerID);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.numPhotos++;
							numPhotos=a.numPhotos;
							a.coverID=id;
							break;
						}
					}
				}
			}
			album.coverID=id;
			if(numPhotos!=0)
				album.numPhotos=numPhotos;
			else
				album.numPhotos++;
			albumCache.put(album.id, album);
			Photo newPhoto=getPhotoIgnoringPrivacy(id);
			context.getActivityPubWorker().sendAddPhotoToAlbum(self, newPhoto, album);
			context.getActivityPubWorker().sendUpdatePhotoAlbum(self, album);
			return newPhoto;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Photo> getAllPhotos(Actor owner, @Nullable User self, int offset, int count){
		List<Long> albumIDs=getAllAlbums(owner, self, true, false).stream()
				.filter(a->a.systemType!=PhotoAlbum.SystemAlbumType.SAVED)
				.map(a->a.id)
				.toList();
		if(albumIDs.isEmpty())
			return PaginatedList.emptyList(count);
		try{
			return PhotoStorage.getAllPhotosInAlbums(albumIDs, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setPhotoRotation(User self, Photo photo, SizedImage.Rotation rotation){
		enforcePhotoManagementPermission(self, photo);
		if(photo.apID!=null)
			throw new IllegalArgumentException("Can only rotate local photos");
		try{
			if(photo.metadata==null)
				photo.metadata=new PhotoMetadata();
			SizedImage.Rotation prevRotation=photo.metadata.rotation;
			if(prevRotation==null)
				prevRotation=SizedImage.Rotation._0;
			if(rotation==prevRotation)
				return;

			int diffDeg=rotation.value()-prevRotation.value();
			if(diffDeg<0)
				diffDeg=360+diffDeg;
			SizedImage.Rotation diff=SizedImage.Rotation.valueOf(diffDeg);
			if(photo.metadata.cropRects!=null){
				AbsoluteImageRect profileCrop=photo.metadata.cropRects.profile().makeAbsolute(photo.getWidth(), photo.getHeight());
				AbsoluteImageRect thumbCrop=photo.metadata.cropRects.thumb().makeAbsolute(profileCrop.getWidth(), profileCrop.getHeight());
				photo.metadata.cropRects=new AvatarCropRects(
						profileCrop.rotate(diff).makeRelative(),
						thumbCrop.rotate(diff).makeRelative()
				);
			}

			List<PhotoTag> tags=PhotoStorage.getPhotoTags(photo.id);
			if(!tags.isEmpty()){
				Map<Long, ImageRect> rotatedRects=tags.stream()
						.map(t->new Pair<>(t.id(), t.rect().makeAbsolute(photo.getWidth(), photo.getHeight()).rotate(diff).makeRelative()))
						.collect(Collectors.toMap(Pair::first, Pair::second));
				PhotoStorage.updatePhotoTagsRects(photo.id, rotatedRects);
			}

			photo.metadata.rotation=rotation;
			if(photo.image instanceof LocalImage li)
				li.rotation=rotation;
			PhotoStorage.updatePhotoMetadata(photo.id, photo.metadata);
			context.getActivityPubWorker().sendUpdateAlbumPhoto(self, photo, getAlbum(photo.albumID, self));

			Actor owner=context.getWallController().getContentAuthorAndOwner(photo).owner();
			if(owner.getAvatar() instanceof LocalImage li && li.photoID==photo.id){
				setPhotoToAvatar(owner, photo);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateAvatar(Account self, @Nullable Group group, LocalImage img, AvatarCropRects crop){
		if(group!=null)
			context.getGroupsController().enforceUserAdminLevel(group, self.user, Group.AdminLevel.ADMIN);

		Actor owner=group==null ? self.user : group;
		int imgW, imgH;
		if(img.rotation==SizedImage.Rotation._90 || img.rotation==SizedImage.Rotation._270){
			imgW=img.height;
			imgH=img.width;
		}else{
			imgW=img.width;
			imgH=img.height;
		}

		if(crop==null){
			AbsoluteImageRect thumbRect;
			if(imgW>imgH){ // Pick the square out of the center
				thumbRect=new AbsoluteImageRect(imgW/2-imgH/2, 0, imgW/2+imgH/2, imgH, imgW, imgH);
			}else{ // Pick the square form the top
				thumbRect=new AbsoluteImageRect(0, 0, imgW, imgW, imgW, imgH);
			}
			crop=new AvatarCropRects(new ImageRect(0, 0, 1, 1), thumbRect.makeRelative());
		}

		AbsoluteImageRect profileCrop=crop.profile().makeAbsolute(imgW, imgH);
		float ratio=profileCrop.getWidth()/(float)profileCrop.getHeight();
		if(ratio>2.5f){ // too wide
			crop=new AvatarCropRects(new AbsoluteImageRect(profileCrop.x1(), profileCrop.y1(), Math.round(profileCrop.x1()+profileCrop.getHeight()*2.5f), profileCrop.y2(), imgW, imgH).makeRelative(), crop.thumb());
		}else if(ratio<0.25f){ // too tall
			crop=new AvatarCropRects(new AbsoluteImageRect(profileCrop.x1(), profileCrop.y1(), profileCrop.x2(), Math.round(profileCrop.y1()+profileCrop.getWidth()/0.25f), imgW, imgH).makeRelative(), crop.thumb());
		}

		PhotoAlbum album=getOrCreateSystemAlbum(owner, PhotoAlbum.SystemAlbumType.AVATARS);
		PhotoMetadata meta=new PhotoMetadata();
		meta.cropRects=crop;
		meta.rotation=img.rotation;
		long id=createPhotoInternal(self.user, owner, album, img.fileID, null, null, meta);
		Photo photo=getPhotoIgnoringPrivacy(id);
		setPhotoToAvatar(owner, photo);

		if(group==null){
			Post post=context.getWallController().createWallPost(self.user, self.id, self.user, null, "", self.prefs.textFormat, null, List.of("photo:"+photo.getIdString()), null, null, Map.of(), Post.Action.AVATAR_UPDATE);
			photo.metadata.correspondingPostID=post.id;
			try{
				PhotoStorage.updatePhotoMetadata(photo.id, photo.metadata);
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
	}

	public void setPhotoToAvatar(Actor owner, Photo photo){
		try{
			if(!(photo.image instanceof LocalImage li))
				throw new IllegalArgumentException();
			LocalImage ava=new LocalImage();
			ava.fileID=li.fileID;
			ava.fillIn(li.fileRecord);
			ava.rotation=li.rotation;
			ava.photoID=photo.id;
			ava.avaCropRects=photo.metadata.cropRects;
			String serializedAva=MediaStorageUtils.serializeAttachment(ava).toString();

			MediaStorage.deleteMediaFileReferences(owner.getLocalID(), owner instanceof Group ? MediaFileReferenceType.GROUP_AVATAR : MediaFileReferenceType.USER_AVATAR);

			if(owner instanceof User user){
				UserStorage.updateProfilePicture(user, serializedAva);
				user=UserStorage.getById(user.id);
				context.getActivityPubWorker().sendUpdateUserActivity(user);
			}else if(owner instanceof Group group){
				GroupStorage.updateProfilePicture(group, serializedAva);
				group=GroupStorage.getById(group.id);
				context.getActivityPubWorker().sendUpdateGroupActivity(group);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteAvatar(Actor owner){
		try{
			MediaStorage.deleteMediaFileReferences(owner.getLocalID(), owner instanceof Group ? MediaFileReferenceType.GROUP_AVATAR : MediaFileReferenceType.USER_AVATAR);
			if(owner instanceof User user){
				UserStorage.updateProfilePicture(user, null);
				user=UserStorage.getById(user.id);
				context.getActivityPubWorker().sendUpdateUserActivity(user);
			}else if(owner instanceof Group group){
				GroupStorage.updateProfilePicture(group, null);
				group=GroupStorage.getById(group.id);
				context.getActivityPubWorker().sendUpdateGroupActivity(group);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateAvatarCrop(User self, Photo photo, AvatarCropRects crop, SizedImage.Rotation rotation){
		enforcePhotoManagementPermission(self, photo);
		PhotoAlbum album=getAlbumIgnoringPrivacy(photo.albumID);
		if(album.systemType!=PhotoAlbum.SystemAlbumType.AVATARS)
			throw new IllegalArgumentException();

		PhotoMetadata meta=photo.metadata==null ? new PhotoMetadata() : photo.metadata;
		rotation=SizedImage.Rotation.valueOf(((meta.rotation==null ? SizedImage.Rotation._0 : meta.rotation).value()+rotation.value())%360);
		meta.cropRects=crop;
		meta.rotation=rotation;
		if(photo.image instanceof LocalImage li)
			li.rotation=rotation;
		photo.metadata=meta;
		try{
			PhotoStorage.updatePhotoMetadata(photo.id, meta);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		Actor owner=context.getWallController().getContentAuthorAndOwner(photo).owner();
		if(owner.getAvatar() instanceof LocalImage li && li.photoID==photo.id)
			setPhotoToAvatar(owner, photo);
	}

	public long createPhotoTag(User self, Photo photo, User user, String name, ImageRect rect){
		// maybe reconsider this in the future (VK allows one's friends to add tags to their photos)
		enforcePhotoManagementPermission(self, photo);

		try{
			if(user!=null){
				if(user.id!=self.id){
					if(context.getFriendsController().getSimpleFriendshipStatus(self, user)!=FriendshipStatus.FRIENDS)
						throw new UserActionNotAllowedException();
					context.getPrivacyController().enforceUserPrivacy(self, user, UserPrivacySettingKey.PHOTO_TAG);
				}
				List<PhotoTag> existingTags=PhotoStorage.getPhotoTags(photo.id);
				if(existingTags.size()>=MAX_TAGS_PER_PHOTO)
					throw new UserErrorException("photo_err_too_many_tags");
				for(PhotoTag tag:existingTags){
					if(tag.userID()==user.id)
						throw new UserErrorException("photo_err_user_already_tagged");
				}
				name=user.getFullName();
			}
			long id=PhotoStorage.createPhotoTag(photo.id, self.id, user!=null ? user.id : 0, name, user!=null && user.id==self.id, rect, null);
			if(user!=null && !(user instanceof ForeignUser) && user.id!=self.id){
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(user.id);
				if(un!=null)
					un.incNewPhotoTagCount(1);
				context.getNotificationsController().sendRealtimeCountersUpdates(user);
			}
			context.getActivityPubWorker().sendUpdateAlbumPhoto(context.getWallController().getContentAuthorAndOwner(photo).author(), photo, getAlbumIgnoringPrivacy(photo.albumID));
			if(user!=null && user.id==self.id){
				context.getNewsfeedController().putFriendsFeedEntry(self, photo.id, NewsfeedEntry.Type.PHOTO_TAG);
			}
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<PhotoTag> getTagsForPhoto(long id){
		try{
			return PhotoStorage.getPhotoTags(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, List<PhotoTag>> getTagsForPhotos(Collection<Long> ids){
		try{
			return PhotoStorage.getPhotoTags(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deletePhotoTag(User self, Photo photo, long tagID){
		try{
			PhotoTag tag=PhotoStorage.getPhotoTag(photo.id, tagID);
			if(tag==null)
				throw new ObjectNotFoundException();
			if(tag.placerID()!=self.id && tag.userID()!=self.id)
				enforcePhotoManagementPermission(self, photo);
			PhotoStorage.deletePhotoTag(photo.id, tagID);
			if(tag.userID()!=0 && !tag.approved()){
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(tag.userID());
				if(un!=null)
					un.incNewPhotoTagCount(-1);
				try{
					User user=context.getUsersController().getUserOrThrow(tag.userID());
					context.getNotificationsController().sendRealtimeCountersUpdates(user);
				}catch(ObjectNotFoundException ignore){}
			}
			User placer=context.getUsersController().getUserOrThrow(tag.placerID());
			if(tag.approved()){
				// Tell followers that this photo was removed from the tagged photos collection
				context.getActivityPubWorker().sendDeletePhotoTag(self, photo, getAlbumIgnoringPrivacy(photo.albumID), tag, placer, context.getWallController().getContentAuthorAndOwner(photo).owner());
			}else{
				// This photo wasn't in the tagged photos collection, so just tell the owner and placer that we rejected the tag
				if(photo.apID!=null)
					context.getActivityPubWorker().sendRejectPhotoTag(self, photo, getAlbumIgnoringPrivacy(photo.albumID), tag, placer, context.getWallController().getContentAuthorAndOwner(photo).owner());
			}
			if(tag.userID()!=0 && tag.approved()){
				try{
					User user=context.getUsersController().getUserOrThrow(tag.userID());
					context.getNewsfeedController().deleteFriendsFeedEntry(user, photo.id, NewsfeedEntry.Type.PHOTO_TAG);
				}catch(ObjectNotFoundException ignore){}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Photo> getUserTaggedPhotos(User self, User user, int offset, int count){
		context.getPrivacyController().enforceUserPrivacy(self, user, UserPrivacySettingKey.PHOTO_TAG_LIST);
		return getUserTaggedPhotosIgnoringPrivacy(user, offset, count);
	}

	public PaginatedList<Photo> getUserTaggedPhotosIgnoringPrivacy(User user, int offset, int count){
		try{
			PaginatedList<Long> ids=PhotoStorage.getUserTaggedPhotos(user.id, offset, count, true);
			Map<Long, Photo> photos=getPhotosIgnoringPrivacy(ids.list);
			return new PaginatedList<>(ids, ids.list.stream().map(photos::get).toList());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Set<URI> getUserTaggedPhotosActivityPubIDs(User user, Collection<URI> ids){
		Map<Long, URI> photoIDs=new HashMap<>();
		ids.stream()
				.filter(Config::isLocal)
				.map(id->new Pair<>(id, ObjectLinkResolver.getObjectIdFromLocalURL(id)))
				.filter(oi->oi.second()!=null && oi.second().type()==ObjectLinkResolver.ObjectType.PHOTO)
				.forEach(p->photoIDs.put(p.second().id(), p.first()));
		Set<URI> foreignPhotoIDs=ids.stream()
				.filter(u->!Config.isLocal(u))
				.collect(Collectors.toSet());
		if(!foreignPhotoIDs.isEmpty()){
			getPhotoIdsByActivityPubIds(foreignPhotoIDs)
					.forEach((key, value)->photoIDs.put(value, key));
		}
		if(photoIDs.isEmpty())
			return Set.of();
		try{
			return PhotoStorage.getUserTaggedPhotosInSet(user.id, photoIDs.keySet())
					.stream()
					.map(photoIDs::get)
					.collect(Collectors.toSet());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Photo> getUserUnapprovedTaggedPhotos(User self, int offset, int count){
		try{
			PaginatedList<Long> ids=PhotoStorage.getUserTaggedPhotos(self.id, offset, count, false);
			Map<Long, Photo> photos=getPhotosIgnoringPrivacy(ids.list);
			return new PaginatedList<>(ids, ids.list.stream().map(photos::get).toList());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void approvePhotoTag(User self, Photo photo, long tagID){
		try{
			PhotoTag tag=PhotoStorage.getPhotoTag(photo.id, tagID);
			if(tag==null)
				throw new ObjectNotFoundException();
			if(tag.userID()!=self.id || tag.approved())
				throw new UserActionNotAllowedException();
			PhotoStorage.approvePhotoTag(photo.id, tagID);
			UserNotifications un=NotificationsStorage.getNotificationsFromCache(self.id);
			if(un!=null)
				un.incNewPhotoTagCount(-1);
			context.getNotificationsController().sendRealtimeCountersUpdates(self);
			User placer=context.getUsersController().getUserOrThrow(tag.placerID());
			if(photo.apID!=null)
				context.getActivityPubWorker().sendApprovePhotoTag(self, photo, getAlbumIgnoringPrivacy(photo.albumID), tag, placer, context.getWallController().getContentAuthorAndOwner(photo).owner());
			context.getNewsfeedController().putFriendsFeedEntry(self, photo.id, NewsfeedEntry.Type.PHOTO_TAG);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Set<URI> getAlbumPhotosActivityPubIDs(PhotoAlbum album, Collection<URI> ids){
		Map<Long, URI> photoIDs=new HashMap<>();
		ids.stream()
				.filter(Config::isLocal)
				.map(id->new Pair<>(id, ObjectLinkResolver.getObjectIdFromLocalURL(id)))
				.filter(oi->oi.second()!=null && oi.second().type()==ObjectLinkResolver.ObjectType.PHOTO)
				.forEach(p->photoIDs.put(p.second().id(), p.first()));
		Set<URI> foreignPhotoIDs=ids.stream()
				.filter(u->!Config.isLocal(u))
				.collect(Collectors.toSet());
		if(!foreignPhotoIDs.isEmpty()){
			getPhotoIdsByActivityPubIds(foreignPhotoIDs)
					.forEach((key, value)->photoIDs.put(value, key));
		}
		if(photoIDs.isEmpty())
			return Set.of();
		try{
			return PhotoStorage.getAlbumPhotosInSet(album.id, photoIDs.keySet())
					.stream()
					.map(photoIDs::get)
					.collect(Collectors.toSet());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, Photo> getUserProfilePhotos(Collection<User> users){
		Set<Long> needPhotos=users.stream()
				.map(u->u.getAvatarImage() instanceof LocalImage li && li.photoID!=0 ? li.photoID : null)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<URI> needForeignPhotos=users.stream()
				.map(u->u.getAvatarImage() instanceof Image img && !(img instanceof LocalImage) && img.photoApID!=null ? img.photoApID : null)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if(!needForeignPhotos.isEmpty()){
			needPhotos=new HashSet<>(needPhotos);
			needPhotos.addAll(getPhotoIdsByActivityPubIds(needForeignPhotos).values());
		}
		if(needPhotos.isEmpty())
			return Map.of();

		return getPhotosIgnoringPrivacy(needPhotos).values().stream().collect(Collectors.toMap(ph->ph.ownerID, Function.identity()));
	}
}
