package smithereen.storage;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.CachedRemoteImage;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.PrivacySetting;
import smithereen.model.SizedImage;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoMetadata;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;

public class PhotoStorage{
	public static List<PhotoAlbum> getAllAlbums(int ownerID) throws SQLException{
		String ownerField=ownerID>0 ? "owner_user_id" : "owner_group_id";
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.allColumns()
				.where(ownerField+"=?", Math.abs(ownerID))
				.orderBy("display_order ASC")
				.executeAsStream(PhotoAlbum::fromResultSet)
				.toList();
	}

	public static PhotoAlbum getAlbum(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.allColumns()
				.where("id=?", id)
				.executeAndGetSingleObject(PhotoAlbum::fromResultSet);
	}

	public static Map<Long, PhotoAlbum> getAlbums(Collection<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.allColumns()
				.whereIn("id", ids)
				.executeAsStream(PhotoAlbum::fromResultSet)
				.collect(Collectors.toMap(a->a.id, Function.identity()));
	}

	public static long createUserAlbum(int userID, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("photo_albums")
					.selectExpr("IFNULL(MAX(display_order), 0)+1")
					.where("owner_user_id=?", userID)
					.executeAndGetInt();
			return new SQLQueryBuilder(conn)
					.insertInto("photo_albums")
					.value("owner_user_id", userID)
					.value("title", title)
					.value("description", description)
					.value("privacy", Utils.gson.toJson(Map.of(
							"view", viewPrivacy,
							"comment", commentPrivacy
					)))
					.value("display_order", displayOrder)
					.executeAndGetIDLong();
		}
	}

	public static long createGroupAlbum(int groupID, String title, String description, boolean disableComments, boolean restrictUploads) throws SQLException{
		EnumSet<PhotoAlbum.Flag> flags=EnumSet.noneOf(PhotoAlbum.Flag.class);
		if(disableComments)
			flags.add(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
		if(restrictUploads)
			flags.add(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("photo_albums")
					.selectExpr("IFNULL(MAX(display_order), 0)+1")
					.where("owner_group_id=?", groupID)
					.executeAndGetInt();
			return new SQLQueryBuilder(conn)
					.insertInto("photo_albums")
					.value("owner_group_id", groupID)
					.value("title", title)
					.value("description", description)
					.value("flags", Utils.serializeEnumSet(flags))
					.value("display_order", displayOrder)
					.executeAndGetIDLong();
		}
	}

	public static void deleteAlbum(long id, int ownerID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			if(ownerID>0){
				new SQLQueryBuilder(conn)
						.deleteFrom("newsfeed")
						.where("type=? AND object_id IN (SELECT id FROM photos WHERE album_id=?)", NewsfeedEntry.Type.ADD_PHOTO, id)
						.executeNoResult();
				// TODO tags
			}
			new SQLQueryBuilder(conn)
					.deleteFrom("likes")
					.where("object_type=? AND object_id IN (SELECT id FROM photos WHERE album_id=?)", Like.ObjectType.PHOTO, id)
					.executeNoResult();
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("photo_albums")
					.columns("display_order")
					.where("id=?", id)
					.executeAndGetInt();
			new SQLQueryBuilder(conn)
					.update("photo_albums")
					.valueExpr("display_order", "display_order-1")
					.where((ownerID>0 ? "owner_user_id" : "owner_group_id")+"=? AND display_order>?", Math.abs(ownerID), displayOrder)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("photo_albums")
					.where("id=?", id)
					.executeNoResult();
		}
	}

	public static void updateUserAlbum(long id, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy) throws SQLException{
		new SQLQueryBuilder()
				.update("photo_albums")
				.value("title", title)
				.value("description", description)
				.value("privacy", Utils.gson.toJson(Map.of(
						"view", viewPrivacy,
						"comment", commentPrivacy
				)))
				.where("id=?", id)
				.executeNoResult();
	}

	public static void updateGroupAlbum(long id, String title, String description, EnumSet<PhotoAlbum.Flag> flags) throws SQLException{
		new SQLQueryBuilder()
				.update("photo_albums")
				.value("title", title)
				.value("description", description)
				.value("flags", Utils.serializeEnumSet(flags))
				.where("id=?", id)
				.executeNoResult();
	}

	public static int getOwnerAlbumCount(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.count()
				.where(id>0 ? "owner_user_id=?" : "owner_group_id=?", Math.abs(id))
				.executeAndGetInt();
	}

	public static long createLocalPhoto(int ownerID, int authorID, long albumID, long fileID, String description, String descriptionSrc, FormattedTextFormat descriptionFormat, PhotoMetadata metadata) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("photos")
					.selectExpr("IFNULL(MAX(display_order), 0)+1")
					.where("album_id=?", albumID)
					.executeAndGetInt();
			long id=new SQLQueryBuilder(conn)
					.insertInto("photos")
					.value("owner_id", ownerID)
					.value("author_id", authorID)
					.value("album_id", albumID)
					.value("local_file_id", fileID)
					.value("description", description)
					.value("description_source", descriptionSrc)
					.value("description_source_format", descriptionFormat)
					.value("metadata", metadata==null ? null : Utils.gson.toJson(metadata))
					.value("display_order", displayOrder)
					.executeAndGetIDLong();
			new SQLQueryBuilder(conn)
					.update("photo_albums")
					.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
					.valueExpr("num_photos", "num_photos+1")
					.where("id=?", albumID)
					.executeNoResult();
			return id;
		}
	}

	public static void deletePhoto(long id, long albumID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("photos")
					.columns("display_order")
					.where("id=?", id)
					.executeAndGetInt();
			new SQLQueryBuilder(conn)
					.update("photos")
					.where("album_id=? AND display_order>?", albumID, displayOrder)
					.valueExpr("display_order", "display_order-1")
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("photos")
					.where("id=?", id)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.update("photo_albums")
					.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
					.valueExpr("num_photos", "num_photos-1")
					.where("id=?", albumID)
					.executeNoResult();
		}
	}

	public static int getAlbumSize(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.columns("num_photos")
				.where("id=?", id)
				.executeAndGetInt();
	}

	public static Set<Long> getLocalPhotoIDsForAlbum(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id")
				.where("album_id=? AND local_file_id IS NOT NULL", id)
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static List<Photo> getAlbumPhotos(long id, int offset, int count) throws SQLException{
		List<Photo> photos=new SQLQueryBuilder()
				.selectFrom("photos")
				.where("album_id=?", id)
				.orderBy("display_order ASC")
				.limit(count, offset)
				.executeAsStream(Photo::fromResultSet)
				.toList();
		postprocessPhotos(photos);
		return photos;
	}

	public static void setAlbumCover(long albumID, long coverID) throws SQLException{
		new SQLQueryBuilder()
				.update("photo_albums")
				.where("id=?", albumID)
				.value("cover_id", coverID)
				.executeNoResult();
	}

	public static long getLastAlbumPhoto(long albumID) throws SQLException{
		long id=new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id")
				.where("album_id=?", albumID)
				.orderBy("display_order DESC")
				.limit(1, 0)
				.executeAndGetLong();
		return id==-1 ? 0 : id;
	}

	public static void updateAlbumFlags(long albumID, EnumSet<PhotoAlbum.Flag> flags) throws SQLException{
		new SQLQueryBuilder()
				.update("photo_albums")
				.value("flags", Utils.serializeEnumSet(flags))
				.where("id=?", albumID)
				.executeNoResult();
	}

	public static Map<Long, Photo> getPhotos(Collection<Long> ids) throws SQLException{
		Map<Long, Photo> photos=new SQLQueryBuilder()
				.selectFrom("photos")
				.whereIn("id", ids)
				.executeAsStream(Photo::fromResultSet)
				.collect(Collectors.toMap(p->p.id, Function.identity()));
		postprocessPhotos(photos.values());
		return photos;
	}

	private static void postprocessPhotos(Collection<Photo> photos) throws SQLException{
		HashSet<Long> needFileIDs=new HashSet<>();
		HashSet<URI> needCacheItems=new HashSet<>();
		ArrayList<Photo> localPhotos=new ArrayList<>();
		ArrayList<Photo> remotePhotos=new ArrayList<>();
		for(Photo p:photos){
			if(p.localFileID!=0){
				needFileIDs.add(p.localFileID);
				localPhotos.add(p);
			}else{
				needCacheItems.add(p.remoteSrc);
				remotePhotos.add(p);
			}
		}
		if(!needFileIDs.isEmpty()){
			Map<Long, MediaFileRecord> mediaFiles=MediaStorage.getMediaFileRecords(needFileIDs);
			for(Photo p:localPhotos){
				MediaFileRecord mfr=mediaFiles.get(p.localFileID);
				LocalImage li=new LocalImage();
				li.fileID=p.localFileID;
				if(mfr!=null){
					li.fillIn(mfr);
				}
				p.image=li;
			}
		}
		if(!needCacheItems.isEmpty()){
			Map<URI, MediaCache.Item> items=MediaCache.getInstance().get(needCacheItems);
			for(Photo p:remotePhotos){
				MediaCache.Item item=items.get(p.remoteSrc);
				if(item instanceof MediaCache.PhotoItem pi){
					p.image=new CachedRemoteImage(pi, p.remoteSrc);
				}else{
					p.image=new NonCachedRemoteImage(new NonCachedRemoteImage.AlbumPhotoArgs(p), new SizedImage.Dimensions(p.metadata.width, p.metadata.height), p.remoteSrc);
				}
			}
		}
	}

	public static void updatePhotoDescription(long id, String source, String parsed, FormattedTextFormat format) throws SQLException{
		new SQLQueryBuilder()
				.update("photos")
				.where("id=?", id)
				.value("description_source", source)
				.value("description", parsed)
				.value("description_source_format", format)
				.executeNoResult();
	}

	public static Map<Long, FormattedTextSource> getDescriptionSources(Collection<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id", "description_source", "description_source_format")
				.whereIn("id", ids)
				.andWhere("description_source IS NOT NULL")
				.executeAsStream(r->new Pair<>(r.getLong("id"), new FormattedTextSource(r.getString("description_source"), FormattedTextFormat.values()[r.getInt("description_source_format")])))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static long getAlbumIdByActivityPubId(URI activityPubID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photo_albums")
				.columns("id")
				.where("ap_id=?", activityPubID.toString())
				.executeAndGetLong();
	}

	public static void putOrUpdateForeignAlbum(PhotoAlbum album) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			if(album.id==0){
				SQLQueryBuilder qb=new SQLQueryBuilder(conn)
						.insertInto("photo_albums")
						.value(album.ownerID>0 ? "owner_user_id" : "owner_group_id", Math.abs(album.ownerID))
						.value("title", album.title)
						.value("description", album.description)
						.value("created_at", album.createdAt)
						.value("updated_at", album.updatedAt)
						.value("flags", Utils.serializeEnumSet(album.flags))
						.value("ap_id", album.activityPubID.toASCIIString())
						.value("ap_url", album.activityPubURL.toASCIIString())
						.value("ap_comments", album.activityPubComments==null ? null : album.activityPubComments.toASCIIString())
						.value("display_order", album.displayOrder)
						.value("cover_id", album.coverID>0 ? album.coverID : null);
				if(album.ownerID>0){
					qb.value("privacy", Utils.gson.toJson(Map.of("view", album.viewPrivacy, "comment", album.commentPrivacy)));
				}
				album.id=qb.executeAndGetIDLong();
			}else{
				SQLQueryBuilder qb=new SQLQueryBuilder(conn)
						.update("photo_albums")
						.where("id=?", album.id)
						.value("title", album.title)
						.value("description", album.description)
						.value("updated_at", album.updatedAt)
						.value("display_order", album.displayOrder)
						.value("cover_id", album.coverID>0 ? album.coverID : null)
						.value("flags", Utils.serializeEnumSet(album.flags));
				if(album.ownerID>0){
					qb.value("privacy", Utils.gson.toJson(Map.of("view", album.viewPrivacy, "comment", album.commentPrivacy)));
				}
				qb.executeNoResult();
			}
		}
	}

	public static long getPhotoIdByActivityPubId(URI activityPubID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id")
				.where("ap_id=?", activityPubID.toString())
				.executeAndGetLong();
	}

	public static void putOrUpdateForeignPhoto(Photo photo) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			if(photo.id==0){
				photo.id=new SQLQueryBuilder(conn)
						.insertInto("photos")
						.value("owner_id", photo.ownerID)
						.value("author_id", photo.authorID)
						.value("album_id", photo.albumID)
						.value("remote_src", photo.remoteSrc)
						.value("description", photo.description)
						.value("created_at", photo.createdAt)
						.value("metadata", Utils.gson.toJson(photo.metadata))
						.value("ap_id", photo.apID.toString())
						.value("display_order", photo.displayOrder)
						.executeAndGetIDLong();
				new SQLQueryBuilder(conn)
						.update("photo_albums")
						.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
						.valueExpr("num_photos", "num_photos+1")
						.where("id=?", photo.albumID)
						.executeNoResult();
			}else{
				long currentAlbum=new SQLQueryBuilder(conn)
						.selectFrom("photos")
						.columns("album_id")
						.where("id=?", photo.id)
						.executeAndGetLong();
				new SQLQueryBuilder(conn)
						.update("photos")
						.value("remote_src", photo.remoteSrc)
						.value("description", photo.description)
						.value("metadata", Utils.gson.toJson(photo.metadata))
						.value("display_order", photo.displayOrder)
						.where("id=?", photo.id)
						.executeNoResult();
				if(currentAlbum!=photo.albumID){
					new SQLQueryBuilder(conn)
							.update("photo_albums")
							.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
							.valueExpr("num_photos", "num_photos-1")
							.where("id=?", currentAlbum)
							.executeNoResult();
					new SQLQueryBuilder(conn)
							.update("photo_albums")
							.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
							.valueExpr("num_photos", "num_photos+1")
							.where("id=?", photo.albumID)
							.executeNoResult();
				}
			}
		}
	}

	public static Set<Long> getAlbumPhotosNotInSet(long albumID, Set<Long> photoIDs) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id")
				.whereNotIn("id", photoIDs)
				.andWhere("album_id=?", albumID)
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static Set<Long> getLocalPhotoIDsIn(Set<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id")
				.whereIn("id", ids)
				.where("local_file_id IS NOT NULL")
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static void deletePhotos(long albumID, Set<Long> ids) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			new SQLQueryBuilder(conn)
					.deleteFrom("photos")
					.whereIn("id", ids)
					.andWhere("album_id=?", albumID)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.update("photo_albums")
					.where("id=?", albumID)
					.valueExpr("num_photos", "(SELECT COUNT(*) FROM photos WHERE album_id=?)", albumID)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("likes")
					.whereIn("object_id", ids)
					.where("object_type=?", Like.ObjectType.PHOTO)
					.executeNoResult();
		}
	}

	public static int getPhotoIndexInAlbum(long albumID, long photoID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT id, ROW_NUMBER() OVER(ORDER BY display_order ASC) AS rownum FROM `photos` WHERE album_id=? ORDER BY (id=?) DESC LIMIT 1", albumID, photoID);
			try(ResultSet res=stmt.executeQuery()){
				if(res.next() && res.getLong("id")==photoID){
					return res.getInt("rownum")-1;
				}
			}
			return -1;
		}
	}

	public static Map<Long, Long> getAlbumIDsForPhotos(Collection<Long> photoIDs) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.columns("id", "album_id")
				.whereIn("id", photoIDs)
				.executeAsStream(res->new Pair<>(res.getLong(1), res.getLong(2)))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}
}
