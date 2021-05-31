package smithereen.data;

import com.google.gson.JsonParseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.UserStorage;

public class Account{
	public int id;
	public String email;
	public User user;
	public AccessLevel accessLevel;
	public UserPreferences prefs;
	public Timestamp createdAt;
	public Timestamp lastActive;
	public BanInfo banInfo;

	public User invitedBy; // used in admin UIs

	@Override
	public String toString(){
		return "Account{"+
				"id="+id+
				", email='"+email+'\''+
				", user="+user+
				", accessLevel="+accessLevel+
				", prefs="+prefs+
				", createdAt="+createdAt+
				", lastActive="+lastActive+
				", invitedBy="+invitedBy+
				'}';
	}

	public static Account fromResultSet(ResultSet res) throws SQLException{
		Account acc=new Account();
		acc.id=res.getInt("id");
		acc.email=res.getString("email");
		acc.accessLevel=AccessLevel.values()[res.getInt("access_level")];
		acc.user=UserStorage.getById(res.getInt("user_id"));
		acc.createdAt=res.getTimestamp("created_at");
		acc.lastActive=res.getTimestamp("last_active");
		String ban=res.getString("ban_info");
		if(ban!=null)
			acc.banInfo=Utils.gson.fromJson(ban, BanInfo.class);
		String prefs=res.getString("preferences");
		if(prefs==null){
			acc.prefs=new UserPreferences();
		}else{
			try{
				acc.prefs=Utils.gson.fromJson(prefs, UserPreferences.class);
			}catch(JsonParseException x){
				acc.prefs=new UserPreferences();
			}
		}
		return acc;
	}

	public enum AccessLevel{
		BANNED,
		REGULAR,
		MODERATOR,
		ADMIN
	}

	public static class BanInfo{
		public int adminUserId;
		public String reason;
		public Instant when;
	}
}
