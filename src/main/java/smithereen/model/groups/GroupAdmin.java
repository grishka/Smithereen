package smithereen.model.groups;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.model.Group;
import smithereen.model.User;
import smithereen.storage.UserStorage;

public class GroupAdmin{
	public int userID;
	public Group.AdminLevel level;
	public String title;
	public int displayOrder;

	public URI activityPubUserID;

	public static GroupAdmin fromResultSet(ResultSet res) throws SQLException{
		GroupAdmin admin=new GroupAdmin();
		admin.level=Group.AdminLevel.values()[res.getInt("level")];
		admin.userID=res.getInt("user_id");
		admin.title=res.getString("title");
		admin.displayOrder=res.getInt("display_order");
		return admin;
	}
}
