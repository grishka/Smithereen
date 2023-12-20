package smithereen.model.viewmodel;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.model.Account;
import smithereen.storage.DatabaseUtils;

public record AdminUserViewModel(int userID, int accountID, int role, String email, Account.ActivationInfo activationInfo, Instant lastActive, InetAddress lastIP){
//	public record Counters(int posts, int comments, int friends, int followers, int reportsFrom, int reportsOf, int staffComments, int strikes){}
	public static AdminUserViewModel fromResultSet(ResultSet res) throws SQLException{
		String activationInfo=res.getString("activation_info");
		return new AdminUserViewModel(
				res.getInt("user_id"),
				res.getInt("account_id"),
				res.getInt("role"),
				res.getString("email"),
				activationInfo!=null ? Utils.gson.fromJson(activationInfo, Account.ActivationInfo.class) : null,
				DatabaseUtils.getInstant(res, "last_active"),
				Utils.deserializeInetAddress(res.getBytes("last_ip"))
		);
	}

	public String getEmailDomain(){
		if(email==null)
			return null;
		int index=email.indexOf('@');
		if(index==-1)
			return null;
		return email.substring(index+1);
	}
}
