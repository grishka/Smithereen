package smithereen.model.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record MediaFileRecord(MediaFileID id, long size, Instant createdAt, MediaFileMetadata metadata){
	public static MediaFileRecord fromResultSet(ResultSet res) throws SQLException{
		MediaFileType type=MediaFileType.values()[res.getInt("type")];
		return new MediaFileRecord(
				new MediaFileID(res.getLong("id"), res.getBytes("random_id"), res.getInt("original_owner_id"), type),
				res.getLong("size"),
				DatabaseUtils.getInstant(res, "created_at"),
				Utils.gson.fromJson(res.getString("metadata"), MediaFileMetadata.classForType(type))
		);
	}
}
