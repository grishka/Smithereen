package smithereen.model.photos;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public record PhotoTag(long id, long photoID, int placerID, int userID, String name, Instant createdAt, boolean approved, ImageRect rect, URI apID){
	public static PhotoTag fromResultSet(ResultSet res) throws SQLException{
		String apID=res.getString("ap_id");
		return new PhotoTag(
				res.getLong("id"),
				res.getLong("photo_id"),
				res.getInt("placer_id"),
				res.getInt("user_id"),
				res.getString("name"),
				DatabaseUtils.getInstant(res, "created_at"),
				res.getBoolean("approved"),
				new ImageRect(
						res.getFloat("x1"),
						res.getFloat("y1"),
						res.getFloat("x2"),
						res.getFloat("y2")
				),
				apID==null ? null : URI.create(apID)
		);
	}
}
