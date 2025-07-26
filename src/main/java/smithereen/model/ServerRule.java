package smithereen.model;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import smithereen.Utils;

public record ServerRule(int id, int priority, String title, String description, Map<String, Translation> translations, boolean isDeleted){

	public static ServerRule fromResultSet(ResultSet res) throws SQLException{
		return new ServerRule(
				res.getInt("id"),
				res.getInt("priority"),
				res.getString("title"),
				res.getString("description"),
				Utils.gson.fromJson(res.getString("translations"), new TypeToken<>(){}),
				res.getBoolean("is_deleted")
		);
	}

	public record Translation(String title, String description){}
}
