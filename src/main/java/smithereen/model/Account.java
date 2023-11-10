package smithereen.model;

import com.google.gson.JsonParseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Config;
import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.UserStorage;

public class Account{
	public int id;
	public String email;
	public User user;
	public UserPreferences prefs;
	public Instant createdAt;
	public Instant lastActive;
	public BanInfo banInfo;
	public ActivationInfo activationInfo;
	public int roleID;
	public int promotedBy;

	public User invitedBy; // used in admin UIs

	@Override
	public String toString(){
		return "Account{"+
				"id="+id+
				", email='"+email+'\''+
				", user="+user+
				", prefs="+prefs+
				", createdAt="+createdAt+
				", lastActive="+lastActive+
				", banInfo="+banInfo+
				", activationInfo="+activationInfo+
				", invitedBy="+invitedBy+
				'}';
	}

	public static Account fromResultSet(ResultSet res) throws SQLException{
		Account acc=new Account();
		acc.id=res.getInt("id");
		acc.email=res.getString("email");
		acc.user=UserStorage.getById(res.getInt("user_id"));
		acc.createdAt=DatabaseUtils.getInstant(res, "created_at");
		acc.lastActive=DatabaseUtils.getInstant(res, "last_active");
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
		String activation=res.getString("activation_info");
		if(activation!=null)
			acc.activationInfo=Utils.gson.fromJson(activation, ActivationInfo.class);
		acc.roleID=res.getInt("role");
		acc.promotedBy=res.getInt("promoted_by");
		return acc;
	}

	public String getUnconfirmedEmail(){
		if(activationInfo==null)
			return null;
		return switch(activationInfo.emailState){
			case NOT_CONFIRMED -> email;
			case CHANGE_PENDING -> activationInfo.newEmail;
		};
	}

	public String getCurrentEmailMasked(){
		String[] parts=email.split("@", 2);
		if(parts.length!=2)
			return email;
		String user=parts[0];
		int count=user.length()<5 ? 1 : 2;
		return user.substring(0, count)+"*".repeat(user.length()-count)+"@"+parts[1];
	}

	public static class BanInfo{
		public int adminUserId;
		public String reason;
		public Instant when;
	}

	public static class ActivationInfo{
		public String emailConfirmationKey;
		public String newEmail;
		public EmailConfirmationState emailState;

		public enum EmailConfirmationState{
			NOT_CONFIRMED,
			CHANGE_PENDING
		}
	}
}
