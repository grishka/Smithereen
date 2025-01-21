package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record Server(int id, String host, String software, String version, Instant lastUpdated, LocalDate lastErrorDay, int errorDayCount, boolean isUp, FederationRestriction restriction, EnumSet<Feature> features){

	public static Server fromResultSet(ResultSet res) throws SQLException{
		String restriction=res.getString("restriction");
		EnumSet<Feature> features=EnumSet.noneOf(Feature.class);
		Utils.deserializeEnumSet(features, Feature.class, res.getLong("features"));
		return new Server(
				res.getInt("id"),
				res.getString("host"),
				res.getString("software"),
				res.getString("version"),
				DatabaseUtils.getInstant(res, "last_updated"),
				DatabaseUtils.getLocalDate(res, "last_error_day"),
				res.getInt("error_day_count"),
				res.getBoolean("is_up"),
				restriction!=null ? Utils.gson.fromJson(restriction, FederationRestriction.class) : null,
				features
		);
	}

	public Availability getAvailability(){
		if(lastErrorDay==null)
			return Availability.UP;
		return isUp ? Availability.FAILING : Availability.DOWN;
	}

	public enum Availability{
		UP,
		FAILING,
		DOWN
	}

	public enum Feature{
		WALL_POSTS,
		PHOTO_ALBUMS
	}
}
