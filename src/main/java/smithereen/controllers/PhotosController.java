package smithereen.controllers;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.MediaStorage;
import smithereen.storage.PhotoStorage;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;

public class PhotosController{
	private static final int MAX_ALBUMS_PER_OWNER=70;
	private static final int MAX_PHOTOS_PER_ALBUM=5000;

	private final ApplicationContext context;
	private final Object albumCacheLock=new Object();
	private final Object albumCreationLock=new Object();
	private final Object photoCreationLock=new Object();
	private final LruCache<Integer, List<PhotoAlbum>> albumListCache=new LruCache<>(500);

	public PhotosController(ApplicationContext context){
		this.context=context;
	}

	public List<PhotoAlbum> getAllAlbums(Actor owner, User self){
		try{
			if(owner instanceof Group group){
				context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			}
			List<PhotoAlbum> albums=albumListCache.get(owner.getLocalID());
			if(albums==null){
				albums=PhotoStorage.getAllAlbums(owner.getLocalID());
				albumListCache.put(owner.getLocalID(), albums);
			}
			return switch(owner){
				case User user when user.id==self.id -> albums;
				case User user -> albums.stream().filter(a->context.getPrivacyController().checkUserPrivacy(self, user, a.viewPrivacy)).toList();
				default -> albums;
			};
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PhotoAlbum> getRandomAlbumsForProfile(Actor owner, User self, int count){
		List<PhotoAlbum> filteredAlbums=new ArrayList<>(getAllAlbums(owner, self));
		Collections.shuffle(filteredAlbums);
		return new PaginatedList<>(filteredAlbums.subList(0, Math.min(filteredAlbums.size(), count)), filteredAlbums.size());
	}

	public PhotoAlbum getAlbum(long id, User self){
		try{
			PhotoAlbum album=PhotoStorage.getAlbum(id);
			if(album==null)
				throw new ObjectNotFoundException();
			if(album.ownerID>0){
				User owner=context.getUsersController().getUserOrThrow(album.ownerID);
				context.getPrivacyController().enforceUserPrivacy(self, owner, album.viewPrivacy, true);
			}else{
				Group owner=context.getGroupsController().getGroupOrThrow(-album.ownerID);
				context.getPrivacyController().enforceUserAccessToGroupContent(self, owner);
			}
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
			// TODO federate
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
			// TODO federate
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean canManageAlbum(User self, @NotNull PhotoAlbum album){
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

	public void deleteAlbum(User self, PhotoAlbum album){
		enforceAlbumManagementPermission(self, album);
		try{
			MediaStorage.deleteMediaFileReferences(PhotoStorage.getLocalPhotoIDsForAlbum(album.id), MediaFileReferenceType.ALBUM_PHOTO);
			PhotoStorage.deleteAlbum(album.id);
			albumListCache.remove(album.ownerID);
			// TODO federate
			// TODO newsfeed
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
			// TODO federate
			// TODO newsfeed
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
			// TODO federate
			// TODO groups newsfeed
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long createPhoto(User self, @NotNull PhotoAlbum album, long fileID, String descriptionSource, FormattedTextFormat descriptionFormat){
		if(album.ownerID>0){
			if(album.ownerID!=self.id)
				throw new UserActionNotAllowedException();
		}else{
			Group group=context.getGroupsController().getGroupOrThrow(-album.ownerID);
			context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			if(album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS))
				context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}

		String parsedDescription=TextProcessor.preprocessPostText(descriptionSource, null, descriptionFormat);
		try{
			long id;
			synchronized(photoCreationLock){
				if(PhotoStorage.getAlbumSize(album.id)>=MAX_PHOTOS_PER_ALBUM)
					throw new UserErrorException("err_too_many_photos_in_album");
				id=PhotoStorage.createLocalPhoto(album.ownerID, self.id, album.id, fileID, parsedDescription, descriptionSource, descriptionFormat, null);
			}
			MediaStorage.createMediaFileReference(fileID, id, MediaFileReferenceType.ALBUM_PHOTO, album.ownerID);
			synchronized(albumCacheLock){
				List<PhotoAlbum> albums=albumListCache.get(album.ownerID);
				if(albums!=null){
					for(PhotoAlbum a:albums){
						if(a.id==album.id){
							a.numPhotos++;
							break;
						}
					}
				}
			}
			// TODO federate
			// TODO newsfeed
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deletePhoto(User self, Photo photo){
		if(photo.ownerID>0){
			if(photo.ownerID!=self.id)
				throw new UserActionNotAllowedException();
		}else{
			Group group=context.getGroupsController().getGroupOrThrow(-photo.ownerID);
			context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
			if(photo.authorID!=self.id)
				context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}
		try{
			synchronized(photoCreationLock){
				PhotoStorage.deletePhoto(photo.id, photo.albumID);
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
							break;
						}
					}
				}
			}
			// TODO delete likes
			// TODO delete comments
			// TODO federate
			// TODO newsfeed
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Photo> getAlbumPhotos(User self, PhotoAlbum album, int offset, int count){
		try{
			return new PaginatedList<>(PhotoStorage.getAlbumPhotos(album.id, offset, count), album.numPhotos, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
