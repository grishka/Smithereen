package smithereen.controllers;

import org.jetbrains.annotations.NotNull;

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

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.CommentStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PhotoStorage;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import smithereen.text.TextProcessor;
import spark.Request;

public class PhotosController{
	public static final int MAX_ALBUMS_PER_OWNER=70;
	public static final int MAX_PHOTOS_PER_ALBUM=5000;

	private final ApplicationContext context;
	private final Object albumCacheLock=new Object();
	private final Object albumCreationLock=new Object();
	private final Object photoCreationLock=new Object();
	private final LruCache<Integer, List<PhotoAlbum>> albumListCache=new LruCache<>(500);
	private final LruCache<Long, PhotoAlbum> albumCache=new LruCache<>(10_000);

	public PhotosController(ApplicationContext context){
		this.context=context;
	}

	public List<PhotoAlbum> getAllAlbums(Actor owner, User self){
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
			return switch(owner){
				case User user when self!=null && user.id==self.id -> albums;
				case User user -> albums.stream().filter(a->context.getPrivacyController().checkUserPrivacy(self, user, a.viewPrivacy)).toList();
				default -> albums;
			};
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
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
			return albums;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PhotoAlbum> getRandomAlbumsForProfile(Actor owner, User self, int count){
		List<PhotoAlbum> filteredAlbums=new ArrayList<>(getAllAlbums(owner, self));
		Collections.shuffle(filteredAlbums);
		return new PaginatedList<>(filteredAlbums.subList(0, Math.min(filteredAlbums.size(), count)), filteredAlbums.size());
	}

	public PaginatedList<PhotoAlbum> getMostRecentAlbums(Actor owner, User self, int count, boolean onlyHavingCover){
		List<PhotoAlbum> filteredAlbums=new ArrayList<>(getAllAlbums(owner, self));
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
		try{
			MediaStorage.deleteMediaFileReferences(PhotoStorage.getLocalPhotoIDsForAlbum(album.id), MediaFileReferenceType.ALBUM_PHOTO);
			deleteCommentsForAlbum(album.id);
			PhotoStorage.deleteAlbum(album.id, album.ownerID);
			synchronized(this){
				albumListCache.remove(album.ownerID);
			}
			albumCache.remove(album.id);
			if(album.ownerID>0){
				context.getNewsfeedController().clearFriendsFeedCache();
			}
			// TODO groups newsfeed
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateUserAlbum(User self, @NotNull PhotoAlbum album, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy){
		if(album.ownerID<0)
			throw new IllegalArgumentException();
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
			context.getActivityPubWorker().sendUpdatePhotoAlbum(context.getGroupsController().getGroupOrThrow(-album.ownerID), album);
			// TODO groups newsfeed
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

		String parsedDescription=descriptionSource==null ? "" : TextProcessor.preprocessPostText(descriptionSource, null, descriptionFormat);
		try{
			long id;
			boolean needUpdateCover=!album.flags.contains(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
			synchronized(photoCreationLock){
				if(PhotoStorage.getAlbumSize(album.id)>=MAX_PHOTOS_PER_ALBUM)
					throw new UserErrorException("err_too_many_photos_in_album");
				id=PhotoStorage.createLocalPhoto(album.ownerID, self.id, album.id, fileID, parsedDescription, descriptionSource, descriptionFormat, null);
				if(needUpdateCover)
					PhotoStorage.setAlbumCover(album.id, id);
			}
			MediaStorage.createMediaFileReference(fileID, id, MediaFileReferenceType.ALBUM_PHOTO, album.ownerID);
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(album.ownerID);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.numPhotos++;
							if(needUpdateCover)
								a.coverID=id;
							break;
						}
					}
				}
			}
			if(album.ownerID>0){
				context.getNewsfeedController().putFriendsFeedEntry(self, id, NewsfeedEntry.Type.ADD_PHOTO);
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
			albumCache.put(album.id, album);
			// TODO groups newsfeed
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
		synchronized(albumCacheLock){
			List<PhotoAlbum> albums=albumListCache.get(photo.ownerID);
			if(albums!=null){
				for(PhotoAlbum a:albums){
					if(a.id==photo.albumID){
						a.numPhotos--;
						if(needUpdateCover){
							a.coverID=newCoverID;
							a.flags.remove(PhotoAlbum.Flag.COVER_SET_EXPLICITLY);
						}
						break;
					}
				}
			}
		}
		LikeStorage.deleteAllLikesForObject(photo.id, Like.ObjectType.PHOTO);
		context.getCommentsController().deleteCommentsForObject(photo);
		NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.PHOTO, photo.id);
		if(album.ownerID>0){
			context.getNewsfeedController().clearFriendsFeedCache();
			context.getNewsfeedController().deleteFriendsFeedEntry(context.getUsersController().getUserOrThrow(album.ownerID), photo.id, NewsfeedEntry.Type.ADD_PHOTO);
			// TODO delete tags
		}
		albumCache.put(album.id, album);
		// TODO groups newsfeed
	}

	public PaginatedList<Photo> getAlbumPhotos(User self, PhotoAlbum album, int offset, int count){
		try{
			return new PaginatedList<>(PhotoStorage.getAlbumPhotos(album.id, offset, count), album.numPhotos, offset, count);
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

	public void putOrUpdateForeignAlbum(PhotoAlbum album){
		try{
			PhotoStorage.putOrUpdateForeignAlbum(album);
			synchronized(albumCacheLock){
				albumListCache.remove(album.ownerID);
			}
			if(album.ownerID>0)
				context.getNewsfeedController().clearFriendsFeedCache();
			albumCache.put(album.id, album);
			// TODO groups newsfeed
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putOrUpdateForeignPhoto(Photo photo){
		try{
			boolean isNew=photo.id==0;
			if(photo.ownerID<0){
				User self=context.getUsersController().getUserOrThrow(photo.authorID);
				Group group=context.getGroupsController().getGroupOrThrow(-photo.ownerID);
				if(context.getPrivacyController().isUserBlocked(self, group))
					throw new UserActionNotAllowedException();
			}

			PhotoStorage.putOrUpdateForeignPhoto(photo);
			if(isNew){
				synchronized(albumCacheLock){
					albumListCache.remove(photo.ownerID);
				}
				if(photo.ownerID>0){
					User owner=context.getUsersController().getUserOrThrow(photo.ownerID);
					context.getNewsfeedController().putFriendsFeedEntry(owner, photo.id, NewsfeedEntry.Type.ADD_PHOTO);
				}
				// TODO groups newsfeed
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
				synchronized(this){
					albumListCache.remove(owner.getOwnerID());
				}
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

			synchronized(this){
				albumListCache.remove(album.ownerID);
			}
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
}
