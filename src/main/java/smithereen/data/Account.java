package smithereen.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import smithereen.storage.UserStorage;

public class Account{
	public int id;
	public String email;
	public User user;
	public AccessLevel accessLevel;
	public UserPreferences prefs;
	public Timestamp createdAt;

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
				'}';
	}

	public static Account fromResultSet(ResultSet res) throws SQLException{
		Account acc=new Account();
		acc.id=res.getInt("id");
		acc.email=res.getString("email");
		acc.accessLevel=AccessLevel.values()[res.getInt("access_level")];
		acc.user=UserStorage.getById(res.getInt("user_id"));
		acc.createdAt=res.getTimestamp("created_at");
		String prefs=res.getString("preferences");
		if(prefs==null){
			acc.prefs=new UserPreferences();
		}else{
			try{
				acc.prefs=UserPreferences.fromJSON(new JSONObject(prefs));
			}catch(JSONException x){
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
}
