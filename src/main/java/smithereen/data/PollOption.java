package smithereen.data;

import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import smithereen.Config;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;

public class PollOption extends ActivityPubObject{
	public int id;

	@Override
	public String getType(){
		return "Note";
	}

	@Override
	public ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		return super.parseActivityPubObject(obj, parserContext);
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("PollOption{");
		sb.append(super.toString());
		sb.append("id=");
		sb.append(id);
		sb.append('}');
		return sb.toString();
	}

	public int getNumVotes(){
		if(replies!=null && replies.object instanceof ActivityPubCollection){
			ActivityPubCollection replies=(ActivityPubCollection) this.replies.object;
			return replies.totalItems;
		}
		return 0;
	}

	public void addVotes(int n){
		if(replies==null || !(replies.object instanceof ActivityPubCollection))
			throw new IllegalStateException("replies is null");
		ActivityPubCollection replies=(ActivityPubCollection) this.replies.object;
		replies.totalItems+=n;
	}

	public void setNumVotes(int n){
		if(replies==null || !(replies.object instanceof ActivityPubCollection))
			throw new IllegalStateException("replies is null");
		ActivityPubCollection replies=(ActivityPubCollection) this.replies.object;
		replies.totalItems=n;
	}

	public static PollOption fromResultSet(ResultSet res, URI pollApID, Poll poll) throws SQLException{
		PollOption opt=new PollOption();
		opt.id=res.getInt("id");
		String _id=res.getString("ap_id");
		if(_id==null){
			opt.activityPubID=pollApID!=null && Config.isLocal(pollApID) ? new UriBuilder(pollApID).fragment("options/"+opt.id).build() : null;
		}else{
			opt.activityPubID=URI.create(_id);
		}
		opt.name=res.getString("text");
		ActivityPubCollection replies=new ActivityPubCollection(false);
		replies.totalItems=res.getInt("num_votes");
		if(!poll.anonymous && (_id==null || (pollApID!=null && Config.isLocal(pollApID)))){
			replies.items=Collections.emptyList();
			replies.activityPubID=Config.localURI("/activitypub/objects/polls/"+poll.id+"/options/"+opt.id+"/votes");
		}
		opt.replies=new LinkOrObject(replies);
		return opt;
	}
}
