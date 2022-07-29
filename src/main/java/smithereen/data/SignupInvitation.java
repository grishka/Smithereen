package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public class SignupInvitation{
	public int id;
	public String code;
	public Instant createdAt;
	public String email;
	public int signupsRemaining;
	public int ownerID;

	public boolean noAddFriend;
	public String firstName, lastName;

	public static SignupInvitation fromResultSet(ResultSet res) throws SQLException{
		SignupInvitation inv=new SignupInvitation();
		inv.id=res.getInt("id");
		inv.code=Utils.byteArrayToHexString(res.getBytes("code"));
		inv.createdAt=DatabaseUtils.getInstant(res, "created");
		inv.email=res.getString("email");
		inv.signupsRemaining=res.getInt("signups_remaining");
		inv.ownerID=res.getInt("owner_id");
		String _extra=res.getString("extra");
		if(_extra!=null){
			ExtraInfo extra=Utils.gson.fromJson(_extra, ExtraInfo.class);
			inv.noAddFriend=extra.noAddFriend;
			inv.firstName=extra.firstName;
			inv.lastName=extra.lastName;
		}
		return inv;
	}

	@Override
	public String toString(){
		return "SignupInvitation{"+
				"id="+id+
				", code='"+code+'\''+
				", createdAt="+createdAt+
				", email='"+email+'\''+
				", signupsRemaining="+signupsRemaining+
				", noAddFriend="+noAddFriend+
				", firstName='"+firstName+'\''+
				", lastName='"+lastName+'\''+
				'}';
	}

	public static String getExtra(boolean noAddFriend, String firstName, String lastName){
		return Utils.gson.toJson(new ExtraInfo(noAddFriend, firstName, lastName));
	}

	private static class ExtraInfo{
		public boolean noAddFriend;
		public String firstName;
		public String lastName;

		public ExtraInfo(){

		}

		public ExtraInfo(boolean noAddFriend, String firstName, String lastName){
			this.noAddFriend=noAddFriend;
			this.firstName=firstName;
			this.lastName=lastName;
		}
	}
}
