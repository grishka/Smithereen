package smithereen.storage;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.LruCache;
import smithereen.Utils;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileMetadata;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.media.MediaFileType;
import smithereen.storage.sql.SQLQueryBuilder;

public class MediaStorage{
	private static final LruCache<Long, MediaFileRecord> recordCache=new LruCache<>(5000);

	public static synchronized Map<Long, MediaFileRecord> getMediaFileRecords(Collection<Long> ids) throws SQLException{
		if(ids.isEmpty())
			return Map.of();
		HashMap<Long, MediaFileRecord> res=new HashMap<>();
		Set<Long> needIDs=new HashSet<>();
		for(long id:ids){
			MediaFileRecord r=recordCache.get(id);
			if(r==null)
				needIDs.add(id);
			else
				res.put(id, r);
		}
		if(needIDs.isEmpty())
			return res;
		new SQLQueryBuilder()
				.selectFrom("media_files")
				.columns("id", "random_id", "size", "type", "created_at", "metadata", "original_owner_id")
				.whereIn("id", needIDs)
				.executeAsStream(MediaFileRecord::fromResultSet)
				.forEach(r->{
					res.put(r.id().id(), r);
					recordCache.put(r.id().id(), r);
				});
		return res;
	}

	public static synchronized MediaFileRecord getMediaFileRecord(long id) throws SQLException{
		MediaFileRecord r=recordCache.get(id);
		if(r!=null)
			return r;
		r=new SQLQueryBuilder()
				.selectFrom("media_files")
				.columns("id", "random_id", "size", "type", "created_at", "metadata", "original_owner_id")
				.where("id=?", id)
				.executeAndGetSingleObject(MediaFileRecord::fromResultSet);
		if(r!=null)
			recordCache.put(id, r);
		return r;
	}

	public static MediaFileRecord createMediaFileRecord(MediaFileType type, long fileSize, int ownerID, MediaFileMetadata metadata) throws SQLException{
		byte[] randomID=Utils.randomBytes(18);
		long id=new SQLQueryBuilder()
				.insertInto("media_files")
				.value("random_id", randomID)
				.value("size", fileSize)
				.value("type", type)
				.value("metadata", Utils.gson.toJson(metadata))
				.value("original_owner_id", ownerID)
				.executeAndGetIDLong();
		MediaFileRecord mfr=new MediaFileRecord(
				new MediaFileID(id, randomID, ownerID, type),
				fileSize, Instant.now(), metadata
		);
		synchronized(MediaStorage.class){
			recordCache.put(id, mfr);
		}
		return mfr;
	}

	public static void createMediaFileReference(long fileID, long objectID, MediaFileReferenceType type, int ownerID) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("media_file_refs")
				.value("file_id", fileID)
				.value("object_id", objectID)
				.value("object_type", type)
				.value(ownerID<0 ? "owner_group_id" : "owner_user_id", ownerID!=0 ? Math.abs(ownerID) : null)
				.executeNoResult();
	}

	public static void deleteMediaFileReferences(long objectID, MediaFileReferenceType type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("media_file_refs")
				.where("object_id=? AND object_type=?", objectID, type)
				.executeNoResult();
	}

	public static void deleteMediaFileReference(long objectID, MediaFileReferenceType type, long fileID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("media_file_refs")
				.where("object_id=? AND object_type=? AND file_id=?", objectID, type, fileID)
				.executeNoResult();
	}

	public static List<MediaFileRecord> getUnreferencedMediaFileRecords() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("media_files")
				.where("ref_count=0 AND created_at<(CURRENT_TIMESTAMP()-INTERVAL 1 DAY)")
				.executeAsStream(MediaFileRecord::fromResultSet)
				.toList();
	}

	public static void deleteMediaFileRecords(Collection<Long> ids) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("media_files")
				.whereIn("id", ids)
				.executeNoResult();
	}
}
