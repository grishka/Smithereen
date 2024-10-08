package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import smithereen.model.reports.ReportableContentObject;
import smithereen.model.reports.ReportedComment;
import smithereen.model.reports.ReportedMailMessage;
import smithereen.model.reports.ReportedPhoto;
import smithereen.model.reports.ReportedPost;
import smithereen.storage.DatabaseUtils;
import spark.utils.StringUtils;

public class ViolationReport{

	public int id;
	public int reporterID;
	public int targetID;
	public String comment;
	public int moderatorID;
	public Instant time;
	public String serverDomain;
	public State state;
	public List<ReportableContentObject> content=List.of();

	public static ViolationReport fromResultSet(ResultSet res) throws SQLException{
		ViolationReport r=new ViolationReport();
		r.id=res.getInt("id");
		r.reporterID=res.getInt("reporter_id");
		r.targetID=res.getInt("target_id");
		r.comment=res.getString("comment");
		r.moderatorID=res.getInt("moderator_id");
		r.time=DatabaseUtils.getInstant(res, "time");
		r.serverDomain=res.getString("server_domain");
		r.state=State.values()[res.getInt("state")];
		String content=res.getString("content");
		if(StringUtils.isNotEmpty(content)){
			JsonArray ja=JsonParser.parseString(content).getAsJsonArray();
			r.content=new ArrayList<>();
			for(JsonElement e:ja){
				r.content.add(deserializeContentObject(r.id, e.getAsJsonObject()));
			}
		}
		return r;
	}

	private static ReportableContentObject deserializeContentObject(int id, JsonObject jo){
		String type=jo.get("type").getAsString();
		ReportableContentObject obj=switch(type){
			case "post" -> new ReportedPost();
			case "message" -> new ReportedMailMessage();
			case "photo" -> new ReportedPhoto();
			case "comment" -> new ReportedComment();
			default -> throw new IllegalStateException("Unexpected value: " + type);
		};
		obj.fillFromReport(id, jo);
		return obj;
	}

	public enum State{
		OPEN,
		CLOSED_REJECTED,
		CLOSED_ACTION_TAKEN
	}
}
