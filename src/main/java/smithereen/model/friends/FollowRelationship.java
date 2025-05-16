package smithereen.model.friends;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.BitSet;

public record FollowRelationship(int followerID, int followeeID, boolean mutual, boolean accepted, boolean muted, BitSet lists){
	public static FollowRelationship fromResultSet(ResultSet res) throws SQLException{
		return new FollowRelationship(
				res.getInt("follower_id"),
				res.getInt("followee_id"),
				res.getBoolean("mutual"),
				res.getBoolean("accepted"),
				res.getBoolean("muted"),
				BitSet.valueOf(new long[]{res.getLong("lists")})
		);
	}
}
