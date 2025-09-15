package smithereen.model.reports;

import com.google.gson.JsonObject;

import java.util.Set;

import smithereen.model.Post;
import smithereen.model.comments.Comment;

public final class ReportedComment extends Comment{
	public int reportID;
	public String topicTitle;
	public boolean isFirstInTopic;

	@Override
	public String getPhotoListID(){
		return "reports/"+reportID+"/"+super.getPhotoListID();
	}

	@Override
	public void fillFromReport(int reportID, JsonObject jo){
		this.reportID=reportID;
		if(jo.has("topicTitle")){
			topicTitle=jo.get("topicTitle").getAsString();
			isFirstInTopic=jo.has("isFirst") && jo.get("isFirst").getAsBoolean();
		}
		super.fillFromReport(reportID, jo);
	}

	@Override
	public JsonObject serializeForReport(int targetID, Set<Long> outFileIDs){
		JsonObject obj=super.serializeForReport(targetID, outFileIDs);
		if(topicTitle!=null){
			obj.addProperty("topicTitle", topicTitle);
			obj.addProperty("isFirst", isFirstInTopic);
		}
		return obj;
	}
}
