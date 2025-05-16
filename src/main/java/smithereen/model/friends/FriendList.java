package smithereen.model.friends;

import java.sql.ResultSet;
import java.sql.SQLException;

public record FriendList(int id, String name){
	public static final int FIRST_PUBLIC_LIST_ID=57;

	public static FriendList fromResultSet(ResultSet res) throws SQLException{
		return new FriendList(res.getInt("id"), res.getString("name"));
	}
}
