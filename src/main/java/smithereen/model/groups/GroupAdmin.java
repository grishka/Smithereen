package smithereen.model.groups;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.model.Group;
import smithereen.model.User;
import smithereen.storage.UserStorage;

public class GroupAdmin{
	public User user;
	public Group.AdminLevel level;
	public String title;

	public URI activityPubUserID;

	public static GroupAdmin fromResultSet(ResultSet res) throws SQLException{
		GroupAdmin admin=new GroupAdmin();
		admin.level=Group.AdminLevel.values()[res.getInt(1)];
		admin.user=UserStorage.getById(res.getInt(2)); // TODO make this better
		admin.title=res.getString(3);
		return admin;
	}
}
