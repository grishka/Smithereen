package smithereen.model.reports;

import com.google.gson.JsonObject;

import smithereen.model.photos.Photo;

public final class ReportedPhoto extends Photo{
	public int reportID;

	@Override
	public String getSinglePhotoListID(){
		return "reports/"+reportID+"/photos/"+getIdString();
	}

	@Override
	public void fillFromReport(int reportID, JsonObject jo){
		this.reportID=reportID;
		super.fillFromReport(reportID, jo);
	}
}
