package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public class UserDataExport{
	public static final int COOLDOWN_DAYS=7;
	public static final int RETENTION_DAYS=2;

	public long id;
	public int userID;
	public State state;
	public long size;
	public long fileID;
	public Instant requestedAt;

	public static UserDataExport fromResultSet(ResultSet res) throws SQLException{
		UserDataExport e=new UserDataExport();
		e.id=res.getLong("id");
		e.userID=res.getInt("user_id");
		e.state=State.values()[res.getInt("state")];
		e.size=res.getInt("size");
		e.fileID=res.getLong("file_id");
		e.requestedAt=DatabaseUtils.getInstant(res, "requested_at");
		return e;
	}

	public enum State{
		PREPARING,
		READY,
		EXPIRED,
		FAILED
	}
}
