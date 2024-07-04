package smithereen.storage;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.model.PrivacySetting;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoMetadata;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.text.FormattedTextFormat;

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

	public static long createUserAlbum(int userID, String title, String description, PrivacySetting viewPrivacy, PrivacySetting commentPrivacy) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("photo_albums")
				.value("owner_user_id", userID)
				.value("title", title)
				.value("description", description)
				.value("privacy", Utils.gson.toJson(Map.of(
						"view", viewPrivacy,
						"comment", commentPrivacy
				)))
				.valueExpr("display_order", "(SELECT IFNULL(MAX(display_order), 0)+1 FROM photo_albums WHERE owner_user_id=?)", userID)
				.executeAndGetIDLong();
	}

	public static long createGroupAlbum(int groupID, String title, String description, boolean disableComments, boolean restrictUploads) throws SQLException{
		EnumSet<PhotoAlbum.Flag> flags=EnumSet.noneOf(PhotoAlbum.Flag.class);
		if(disableComments)
			flags.add(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
		if(restrictUploads)
			flags.add(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
		return new SQLQueryBuilder()
				.insertInto("photo_albums")
				.value("owner_group_id", groupID)
				.value("title", title)
				.value("description", description)
				.value("flags", Utils.serializeEnumSet(flags))
				.valueExpr("display_order", "(SELECT IFNULL(MAX(display_order), 0)+1 FROM photo_albums WHERE owner_group_id=?)", groupID)
				.executeAndGetIDLong();
	}

	public static void deleteAlbum(long id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("photo_albums")
				.where("id=?", id)
				.executeNoResult();
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
				.where("owner_id=?", id)
				.executeAndGetInt();
	}

	public static long createLocalPhoto(int ownerID, int authorID, long albumID, long fileID, String description, String descriptionSrc, FormattedTextFormat descriptionFormat, PhotoMetadata metadata) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
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
					.valueExpr("display_order", "(SELECT IFNULL(MAX(display_order), 0)+1 FROM photos WHERE album_id=?)", albumID)
					.executeAndGetIDLong();
			new SQLQueryBuilder(conn)
					.update("photo_albums")
					.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
					.valueExpr("num_photos", "num_photos+1")
					.executeNoResult();
			return id;
		}
	}

	public static void deletePhoto(long id, long albumID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			new SQLQueryBuilder(conn)
					.update("photos")
					.where("album_id=? AND display_order>(SELECT display_order FROM photos WHERE id=?)", albumID, id)
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
		return new SQLQueryBuilder()
				.selectFrom("photos")
				.where("album_id=?", id)
				.orderBy("display_order ASC")
				.limit(count, offset)
				.executeAsStream(Photo::fromResultSet)
				.toList();
	}
}
