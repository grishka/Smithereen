package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;
import spark.utils.StringUtils;

public class SignupRequest{
	public int id;
	public String email, firstName, lastName, reason;
	public Instant createdAt;

	public static SignupRequest fromResultSet(ResultSet res) throws SQLException{
		SignupRequest req=new SignupRequest();
		req.id=res.getInt("id");
		req.email=res.getString("email");
		req.firstName=res.getString("first_name");
		req.lastName=res.getString("last_name");
		req.reason=res.getString("reason");
		req.createdAt=DatabaseUtils.getInstant(res, "created_at");
		return req;
	}

	public String getFullName(){
		return StringUtils.isEmpty(lastName) ? firstName : (firstName+" "+lastName);
	}
}
