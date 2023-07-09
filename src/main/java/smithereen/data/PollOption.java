package smithereen.data;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PollOption{
	public int id;
	public URI activityPubID;
	public String text;
	public int numVotes;

	public static PollOption fromResultSet(ResultSet res, URI pollApID, Poll poll) throws SQLException{
		PollOption opt=new PollOption();
		opt.id=res.getInt("id");
		String _id=res.getString("ap_id");
		if(_id!=null)
			opt.activityPubID=URI.create(_id);
		opt.text=res.getString("text");
		opt.numVotes=res.getInt("num_votes");
		return opt;
	}

	public URI getActivityPubID(URI pollID){
		if(activityPubID!=null)
			return activityPubID;
		return new UriBuilder(pollID).fragment("options/"+id).build();
	}
}
