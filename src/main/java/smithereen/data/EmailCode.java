package smithereen.data;

import org.json.JSONObject;

import java.sql.Timestamp;

public class EmailCode{

	public static final long VALIDITY_MS=24*60*60*1000;

	public int accountID;
	public JSONObject extra;
	public Type type;
	public Timestamp createdAt;

	public boolean isExpired(){
		return System.currentTimeMillis()-createdAt.getTime()>VALIDITY_MS;
	}

	public enum Type{
		NEW_ACCOUNT_CONFIRM,
		PASSWORD_RESET,
		EMAIL_CHANGE
	}
}
