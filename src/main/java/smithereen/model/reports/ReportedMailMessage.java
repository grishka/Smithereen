package smithereen.model.reports;

import com.google.gson.JsonObject;

import smithereen.model.MailMessage;

public final class ReportedMailMessage extends MailMessage{
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
