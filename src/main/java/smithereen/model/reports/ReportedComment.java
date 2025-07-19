package smithereen.model.reports;

import com.google.gson.JsonObject;

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
}
