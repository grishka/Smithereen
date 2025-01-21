package smithereen.model.reports;

import com.google.gson.JsonObject;

import smithereen.model.Post;

public final class ReportedPost extends Post{
	public int reportID;

	@Override
	public String getPhotoListID(){
		return "reports/"+reportID+"/"+super.getPhotoListID();
	}

	@Override
	public void fillFromReport(int reportID, JsonObject jo){
		this.reportID=reportID;
		super.fillFromReport(reportID, jo);
	}
}
