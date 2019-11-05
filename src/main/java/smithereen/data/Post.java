package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.jsonld.JLD;
import smithereen.storage.UserStorage;

public class Post extends ActivityPubObject{
	public int id;
	public User user;
	public User owner;

	public String userLink;
	public String userLinkAttrs="";

	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();
		post.fillFromResultSet(res);
		return post;
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt("id");
		content=res.getString("text");
		published=res.getTimestamp("created_at");
		user=UserStorage.getById(res.getInt("author_id"));
		owner=UserStorage.getById(res.getInt("owner_user_id"));
		summary=res.getString("content_warning");
		attributedTo=user.activityPubID;

		to=Collections.singletonList(new LinkOrObject(tryParseURL(JLD.ACTIVITY_STREAMS+"#Public")));
		if(user.id==owner.id){
			cc=Collections.singletonList(new LinkOrObject(user.getFollowersURL()));
		}else{
			cc=Collections.singletonList(new LinkOrObject(owner.activityPubID));
		}
		String apid=res.getString("ap_id");
		try{
			if(apid==null){
				activityPubID=new URI(owner.activityPubID.getScheme(), owner.activityPubID.getSchemeSpecificPart()+"/posts/"+id, null);
				url=activityPubID;
			}else{
				activityPubID=new URI(apid);
				url=new URI(res.getString("ap_url"));
			}
		}catch(URISyntaxException ignore){}

		String att=res.getString("attachments");
		if(att!=null){
			try{
				attachment=parseSingleObjectOrArray(att.charAt(0)=='[' ? new JSONArray(att) : new JSONObject(att));
			}catch(Exception ignore){}
		}

		userLink=user.url.toString();
	}

	public boolean hasContentWarning(){
		return summary!=null;
	}

	@Override
	public String getType(){
		return "Note";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		JSONObject root=super.asActivityPubObject(obj, contextCollector);
		root.put("sensitive", hasContentWarning());
		contextCollector.addAlias("sensitive", "as:sensitive");

		return root;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception{
		super.parseActivityPubObject(obj);
		Object _content=obj.get("content");
		if(_content instanceof JSONArray){
			content=((JSONArray) _content).getString(0);
		}
		return this;
	}

	public String serializeAttachments(){
		if(attachment==null)
			return null;
		return serializeObjectArrayCompact(attachment, new ContextCollector()).toString();
	}

	public boolean canBeManagedBy(User user){
		return owner.equals(user) || this.user.equals(user);
	}
}
