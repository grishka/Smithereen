package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.storage.UserStorage;

public class Account{
	public static final int ACCESS_LEVEL_BANNED=0;
	public static final int ACCESS_LEVEL_REGULAR=1;
	public static final int ACCESS_LEVEL_MODERATOR=2;
	public static final int ACCESS_LEVEL_ADMIN=3;

	public int id;
	public String email;
	public User user;
	public int accessLevel;

	@Override
	public String toString(){
		return "Account{"+
				"id="+id+
				", email='"+email+'\''+
				", user="+user+
				", accessLevel="+accessLevel+
				'}';
	}

	public static Account fromResultSet(ResultSet res) throws SQLException{
		Account acc=new Account();
		acc.id=res.getInt("id");
		acc.email=res.getString("email");
		acc.accessLevel=res.getInt("access_level");
		acc.user=UserStorage.getById(res.getInt("user_id"));
		return acc;
	}
}
