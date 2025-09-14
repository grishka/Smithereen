package smithereen.controllers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubLink;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.jsonld.JLD;
import smithereen.model.LikedObjectID;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserDataExport;
import smithereen.model.comments.Comment;
import smithereen.model.friends.FriendList;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.media.MediaFileType;
import smithereen.model.media.UserDataArchiveMetadata;
import smithereen.model.notifications.RealtimeNotification;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.CommentStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

public class UserDataExportWorker{
	private static final Logger LOG=LoggerFactory.getLogger(UserDataExportWorker.class);

	private final ApplicationContext context;
	private ExecutorService executor=new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
			Thread.ofPlatform().name("UserDataExportWorker-", 0).factory());

	public UserDataExportWorker(ApplicationContext context){
		this.context=context;
	}

	public void startExport(User user){
		try{
			long id=UserStorage.createUserDataExport(user.id);
			executor.submit(()->doExport(user, id));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void expireExports(){
		try{
			for(UserDataExport export:UserStorage.getUserDataExportsToExpire()){
				MediaStorage.deleteMediaFileReference(export.id, MediaFileReferenceType.USER_EXPORT_ARCHIVE, export.fileID);
				UserStorage.updateUserDataExport(export.id, 0, UserDataExport.State.EXPIRED, export.size);
			}
		}catch(Throwable x){
			LOG.error("Failed to expire user data exports", x);
		}
	}

	private void doExport(User user, long id){
		LOG.debug("Starting data export {} for user {}", id, user.id);
		try{
			File archive=File.createTempFile("SmithereenUserExport", ".zip");
			HashSet<MediaFileID> filesToInclude=new HashSet<>();
			try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(archive), StandardCharsets.UTF_8)){

				// Actor itself
				writeActivityPubObject(user, filesToInclude, zos, "actor");

				// Friend lists
				List<FriendList> friendLists=context.getFriendsController().getFriendLists(user);
				writeJsonElement(friendLists.stream().map(fl->new JsonObjectBuilder().add("id", fl.id()).add("name", fl.name()).build()).collect(JsonArrayBuilder.COLLECTOR),
						zos, "friendLists");

				// Friends
				writeLocalObjectRemoteLinkCollections(offset->{
					PaginatedList<User> users=context.getFriendsController().getFriends(user, offset, 500, FriendsController.SortOrder.HINTS);
					Map<Integer, BitSet> lists=context.getFriendsController().getFriendListsForUsers(user, user, users.list.stream().map(u->u.id).toList());
					List<ActivityPubFriendLink> links=users.list.stream().map(u->{
						ActivityPubFriendLink fl=new ActivityPubFriendLink();
						fl.href=u.activityPubID;
						BitSet userLists=lists.get(u.id);
						fl.lists=userLists==null ? List.of() : userLists.stream().map(i->i+1).boxed().toList();
						return fl;
					}).toList();
					return new PaginatedList<>(users, links);
				}, filesToInclude, zos, user.getFriendsURL(), "friends", true);

				// Followers
				writeLinkCollections(offset->context.getFriendsController().getFollowers(user, offset, 500), filesToInclude, zos, user.getFollowersURL(), "followers", false);

				// Following
				writeLinkCollections(offset->context.getFriendsController().getFollows(user, offset, 500), filesToInclude, zos, user.getFollowersURL(), "follows", false);

				// Groups
				writeLinkCollections(offset->context.getGroupsController().getUserGroups(user, user, offset, 500), filesToInclude, zos, user.getGroupsURL(), "groups", true);

				// Events
				writeLinkCollections(offset->context.getGroupsController().getUserEvents(user, GroupsController.EventsType.ALL, offset, 500), filesToInclude, zos, user.getGroupsURL(), "events", false);

				// Bookmarked users
				writeLinkCollections(offset->{
					PaginatedList<Integer> ids=context.getBookmarksController().getBookmarkedUsers(user, offset, 500);
					Map<Integer, User> users=context.getUsersController().getUsers(ids.list);
					return new PaginatedList<>(ids, ids.list.stream().map(users::get).filter(Objects::nonNull).toList());
				}, filesToInclude, zos, null, "bookmarkedUsers", false);

				// Bookmarked groups
				writeLinkCollections(offset->{
					PaginatedList<Integer> ids=context.getBookmarksController().getBookmarkedGroups(user, offset, 500);
					return new PaginatedList<>(ids, context.getGroupsController().getGroupsByIdAsList(ids.list));
				}, filesToInclude, zos, null, "bookmarkedGroups", false);

				// Blocked users
				writeLinkCollections(offset->{
					List<User> users=context.getPrivacyController().getBlockedUsers(user);
					return new PaginatedList<>(users, users.size(), 0, users.size());
				}, filesToInclude, zos, null, "blockedUsers", false);

				// Blocked domains
				writeJsonElement(context.getPrivacyController().getBlockedDomains(user).stream().collect(JsonArrayBuilder.COLLECTOR), zos, "blockedDomains");

				// Word filters
				writeJsonElement(context.getNewsfeedController().getWordFilters(user, true).stream().map(wf->{
					return new JsonObjectBuilder()
							.add("id", wf.id)
							.add("name", wf.name)
							.add("contexts", wf.contexts.stream().map(c->c.toString().toLowerCase()).collect(JsonArrayBuilder.COLLECTOR))
							.add("words", wf.words.stream().collect(JsonArrayBuilder.COLLECTOR))
							.add("expires", wf.expiresAt.getEpochSecond())
							.build();
				}).collect(JsonArrayBuilder.COLLECTOR), zos, "wordFilters");

				// Wall
				writeLocalObjectRemoteLinkCollections(offset->{
					PaginatedList<Post> posts=context.getWallController().getWallPosts(user, user, false, offset, 100);
					return new PaginatedList<>(posts, posts.list.stream().map(p->NoteOrQuestion.fromNativePost(p, context)).toList());
				}, filesToInclude, zos, user.getWallURL(), "wall", false);

				// Wall comments
				writeLocalObjectRemoteLinkCollections(offset->{
					try{
						PaginatedList<Post> posts=PostStorage.getWallComments(user.id, offset, 100);
						return new PaginatedList<>(posts, posts.list.stream().map(p->{
							try{
								return NoteOrQuestion.fromNativePost(p, context);
							}catch(ObjectNotFoundException x){
								return null;
							}
						}).filter(Objects::nonNull).toList());
					}catch(SQLException x){
						throw new InternalServerErrorException(x);
					}
				}, filesToInclude, zos, user.getWallCommentsURL(), "wallComments", false);

				// Photo albums
				writeLocalObjectRemoteLinkCollections(offset->{
					List<PhotoAlbum> albums=context.getPhotosController().getAllAlbumsIgnoringPrivacy(user);
					for(PhotoAlbum album:albums){
						writeLocalObjectRemoteLinkCollections(albumOffset->{
							PaginatedList<Photo> photos=context.getPhotosController().getAlbumPhotos(user, album, albumOffset, 100, false);
							return new PaginatedList<>(photos, photos.list.stream().map(p->ActivityPubPhoto.fromNativePhoto(p, album, context)).toList());
						}, filesToInclude, zos, album.getActivityPubID(), "photoAlbum"+album.getIdString()+"_", true);
						writeLocalObjectRemoteLinkCollections(commentsOffset->{
							PaginatedList<Comment> comments=context.getCommentsController().getPhotoAlbumComments(album, commentsOffset, 100);
							return new PaginatedList<>(comments, comments.list.stream().map(p->NoteOrQuestion.fromNativeComment(p, context)).toList());
						}, filesToInclude, zos, album.activityPubComments==null ? new UriBuilder(album.getActivityPubID()).appendPath("comments").build() : album.activityPubComments,
								"photoAlbum"+album.getIdString()+"_comments", false);
					}
					return new PaginatedList<>(albums.stream().map(pa->ActivityPubPhotoAlbum.fromNativeAlbum(pa, context)).toList(), albums.size(), 0, albums.size());
				}, filesToInclude, zos, user.getPhotoAlbumsURL(), "photoAlbums", false);

				// Tagged photos
				writeLocalObjectRemoteLinkCollections(offset->{
					PaginatedList<Photo> photos=context.getPhotosController().getUserTaggedPhotos(user, user, offset, 100);
					Set<Long> needAlbums=photos.list.stream().map(p->p.albumID).collect(Collectors.toSet());
					Map<Long, PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums);
					return new PaginatedList<>(photos, photos.list.stream().map(p->ActivityPubPhoto.fromNativePhoto(p, albums.get(p.albumID), context)).toList());
				}, filesToInclude, zos, user.getTaggedPhotosURL(), "taggedPhotos", false);

				// Likes
				writeLinkCollections(offset->{
					PaginatedList<LikedObjectID> ids=context.getUserInteractionsController().getLikedObjects(user, offset, 500);
					Set<Integer> needPosts=ids.list.stream().filter(lid->lid.type()==Like.ObjectType.POST).map(lid->(int)lid.id()).collect(Collectors.toSet());
					Set<Long> needComments=ids.list.stream().filter(lid->lid.type()==Like.ObjectType.COMMENT).map(LikedObjectID::id).collect(Collectors.toSet());
					Set<Long> needPhotos=ids.list.stream().filter(lid->lid.type()==Like.ObjectType.PHOTO).map(LikedObjectID::id).collect(Collectors.toSet());
					try{
						Map<Integer, URI> postIDs;
						if(needPosts.isEmpty())
							postIDs=Map.of();
						else
							postIDs=PostStorage.getActivityPubIDsByLocalIDs(needPosts);

						Map<Long, URI> commentIDs;
						if(needComments.isEmpty())
							commentIDs=Map.of();
						else
							commentIDs=CommentStorage.getCommentActivityPubIDsByLocalIDs(needComments);

						Map<Long, URI> photoIDs;
						if(needPhotos.isEmpty())
							photoIDs=Map.of();
						else
							photoIDs=PhotoStorage.getPhotoActivityPubIDsByLocalIDs(needPhotos);

						List<ActivityPubLink> links=ids.list.stream().map(lid->{
							ActivityPubLink l=new ActivityPubLink();
							l.activityPubID=switch(lid.type()){
								case POST -> postIDs.get((int)lid.id());
								case COMMENT -> commentIDs.get(lid.id());
								case PHOTO -> photoIDs.get(lid.id());
							};
							return l;
						}).filter(l->l.activityPubID!=null).toList();
						return new PaginatedList<>(ids, links);
					}catch(SQLException x){
						throw new InternalServerErrorException(x);
					}
				}, filesToInclude, zos, null, "likes", false);

				for(MediaFileID fid:filesToInclude){
					String fileName=Base64.getUrlEncoder().withoutPadding().encodeToString(fid.randomID())+"_"+
							Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(fid.id(), ObfuscatedObjectIDType.MEDIA_FILE)))+
							"."+fid.type().getFileExtension();
					try(InputStream in=MediaFileStorageDriver.getInstance().openStream(fid)){
						zos.putNextEntry(new ZipEntry("media/"+fileName));
						Utils.copyBytes(in, zos);
						zos.closeEntry();
					}catch(IOException x){
						LOG.warn("Failed to copy file {} to export {} for user {}", fid, id, user.id, x);
					}
				}
			}
			MediaFileRecord record=MediaStorage.createMediaFileRecord(MediaFileType.USER_EXPORT_ARCHIVE, archive.length(), user.id, new UserDataArchiveMetadata());
			MediaFileStorageDriver.getInstance().storeFile(archive, record.id(), false);
			UserStorage.updateUserDataExport(id, record.id().id(), UserDataExport.State.READY, record.size());
			MediaStorage.createMediaFileReference(record.id().id(), id, MediaFileReferenceType.USER_EXPORT_ARCHIVE, user.id);
			context.getNotificationsController().sendRealtimeNotifications(user, "exportReady"+System.currentTimeMillis(), RealtimeNotification.Type.EXPORT_READY, UserStorage.getUserDataExport(id), null, null);
			LOG.debug("Data export {} for user {} finished", id, user.id);
		}catch(Throwable x){
			LOG.error("Failed to generate data export {} for user {}", id, user.id, x);
			try{
				UserStorage.updateUserDataExport(id, 0, UserDataExport.State.FAILED, 0);
			}catch(SQLException xx){
				LOG.error("Failed to update data export {} status", id, xx);
			}
		}
	}

	private void writeJsonElement(JsonElement el, ZipOutputStream zos, String name) throws IOException{
		zos.putNextEntry(new ZipEntry(name+".json"));
		zos.write(el.toString().getBytes(StandardCharsets.UTF_8));
		zos.closeEntry();
	}

	private void writeActivityPubObject(ActivityPubObject obj, Set<MediaFileID> filesToInclude, ZipOutputStream zos, String name) throws IOException{
		ExportSerializerContext serializerContext=new ExportSerializerContext(context);
		writeJsonElement(obj.asRootActivityPubObject(serializerContext), zos, name);
		filesToInclude.addAll(serializerContext.filesToInclude);
	}

	private <T extends ActivityPubObject> void writeLinkCollections(PaginatedListGetter<T> listGetter, Set<MediaFileID> filesToInclude,
																	ZipOutputStream zos, URI baseCollectionID, String name, boolean ordered) throws IOException{
		writeCollections(listGetter, obj->new LinkOrObject(obj.activityPubID), filesToInclude, zos, baseCollectionID, name, ordered);
	}

	private <T extends ActivityPubObject> void writeLocalObjectRemoteLinkCollections(PaginatedListGetter<T> listGetter, Set<MediaFileID> filesToInclude,
																					 ZipOutputStream zos, URI baseCollectionID, String name, boolean ordered) throws IOException{
		writeCollections(listGetter, obj->{
			if(obj.activityPubID==null || Config.isLocal(obj.activityPubID))
				return new LinkOrObject(obj);
			else
				return new LinkOrObject(obj.activityPubID);
		}, filesToInclude, zos, baseCollectionID, name, ordered);
	}

	private <T extends ActivityPubObject> void writeCollections(PaginatedListGetter<T> listGetter, Function<T, LinkOrObject> converter,
																Set<MediaFileID> filesToInclude, ZipOutputStream zos, URI baseCollectionID, String name, boolean ordered) throws IOException{
		int offset=0, total, i=0;
		do{
			PaginatedList<T> list=listGetter.get(offset);
			total=list.total;
			offset+=list.perPage;

			CollectionPage apPage=new CollectionPage(ordered);
			apPage.partOf=baseCollectionID;
			apPage.activityPubID=URI.create(name+i+".json");
			if(offset<total){
				apPage.next=URI.create(name+(i+1)+".json");
			}
			apPage.totalItems=list.total;
			apPage.items=new ArrayList<>();
			for(T item:list.list){
				apPage.items.add(converter.apply(item));
			}
			writeActivityPubObject(apPage, filesToInclude, zos, name+i);

			i++;
		}while(offset<total);
	}

	private interface PaginatedListGetter<T extends ActivityPubObject>{
		PaginatedList<T> get(int offset) throws IOException;
	}

	public static class ExportSerializerContext extends SerializerContext{
		public Set<MediaFileID> filesToInclude=new HashSet<>();

		public ExportSerializerContext(ApplicationContext appContext){
			super(appContext, (String)null);
		}
	}

	private static class ActivityPubFriendLink extends ActivityPubLink{
		public List<Integer> lists;

		@Override
		public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
			super.asActivityPubObject(obj, serializerContext);
			if(!lists.isEmpty()){
				obj.add("lists", lists.stream().collect(JsonArrayBuilder.COLLECTOR));
				serializerContext.addSmAlias("lists");
			}
			return obj;
		}
	}
}
