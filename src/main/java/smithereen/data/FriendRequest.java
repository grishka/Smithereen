package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FriendRequest{
	public User from;
	public String message;

	public static FriendRequest fromResultSet(ResultSet res) throws SQLException{
		FriendRequest req=new FriendRequest();
		req.message=res.getString("message");
		if(res.getString("domain").length()>0)
			req.from=ForeignUser.fromResultSet(res);
		else
			req.from=User.fromResultSet(res);
		return req;
	}
}
