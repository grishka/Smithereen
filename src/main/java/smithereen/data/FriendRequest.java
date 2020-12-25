package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.storage.UserStorage;

public class FriendRequest{
	public User from;
	public String message;

	public static FriendRequest fromResultSet(ResultSet res) throws SQLException{
		FriendRequest req=new FriendRequest();
		req.message=res.getString("message");
		req.from=UserStorage.getById(res.getInt("from_user_id"));
		return req;
	}
}
