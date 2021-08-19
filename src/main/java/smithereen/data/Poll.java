package smithereen.data;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Poll{
	public int id;
	public int ownerID;
	public String question;
	public boolean multipleChoice;
	public boolean anonymous;
	public List<PollOption> options;
	public Date endTime;
	public int numVoters;
	public URI activityPubID;

	public static Poll fromResultSet(ResultSet res) throws SQLException{
		Poll p=new Poll();
		p.id=res.getInt("id");
		p.ownerID=res.getInt("owner_id");
		p.question=res.getString("question");
		p.multipleChoice=res.getBoolean("is_multi_choice");
		p.anonymous=res.getBoolean("is_anonymous");
		Timestamp end=res.getTimestamp("end_time");
		if(end!=null)
			p.endTime=new Date(end.getTime());
		p.numVoters=res.getInt("num_voted_users");
		p.options=new ArrayList<>();
		String apID=res.getString("ap_id");
		if(apID!=null)
			p.activityPubID=URI.create(apID);
		return p;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("Poll{");
		sb.append("id=");
		sb.append(id);
		sb.append(", ownerID=");
		sb.append(ownerID);
		if(question!=null){
			sb.append(", question='");
			sb.append(question);
			sb.append('\'');
		}
		sb.append(", multipleChoice=");
		sb.append(multipleChoice);
		sb.append(", anonymous=");
		sb.append(anonymous);
		if(options!=null){
			sb.append(", options=");
			sb.append(options);
		}
		if(endTime!=null){
			sb.append(", endTime=");
			sb.append(endTime);
		}
		sb.append(", numVoters=");
		sb.append(numVoters);
		sb.append('}');
		return sb.toString();
	}

	public boolean isExpired(){
		return endTime!=null && endTime.getTime()<System.currentTimeMillis();
	}
}
