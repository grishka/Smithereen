package smithereen.model.photos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

import smithereen.storage.DatabaseUtils;

public record PhotoTag(long id, long photoID, int placerID, int userID, @NotNull String name, @NotNull Instant createdAt, boolean approved, @NotNull ImageRect rect, @Nullable URI apID){
	public static PhotoTag fromResultSet(ResultSet res) throws SQLException{
		String apID=res.getString("ap_id");
		return new PhotoTag(
				res.getLong("id"),
				res.getLong("photo_id"),
				res.getInt("placer_id"),
				res.getInt("user_id"),
				res.getString("name"),
				Objects.requireNonNull(DatabaseUtils.getInstant(res, "created_at"), "created_at must not be null"),
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
