package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.jsonld.JLD;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class Post extends ActivityPubObject{
	public int id;
	public User user;
	public User owner;

	public String userLink;
	public String userLinkAttrs="";

	public int[] replyKey={};

	public List<Post> replies=new ArrayList<>();
	public boolean local;

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
				local=true;
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

		byte[] rk=res.getBytes("reply_key");
		if(rk!=null){
			replyKey=new int[rk.length/4];
			try{
				DataInputStream in=new DataInputStream(new ByteArrayInputStream(rk));
				for(int i=0;i<rk.length/4;i++){
					replyKey[i]=in.readInt();
				}
			}catch(IOException ignore){}
		}

		// If this is a reply, we want to notify the author of the top-level post as well as the author of the comment this reply is to
		// TODO optimize
		if(replyKey.length>0){
			int topLevelPostID=replyKey[0];
			int topLevelOwnerID=PostStorage.getOwnerForPost(topLevelPostID);
			if(tag==null)
				tag=new ArrayList<>();
			if(topLevelOwnerID!=0){
				User user=UserStorage.getById(topLevelOwnerID);
				addToCC(user.activityPubID);
				Mention mention=new Mention();
				mention.href=user.activityPubID;
				tag.add(mention);
			}
			if(replyKey.length>1){
				int replyOwnerID=PostStorage.getOwnerForPost(replyKey[replyKey.length-1]);
				if(replyOwnerID!=0 && replyOwnerID!=topLevelOwnerID){
					User user=UserStorage.getById(replyOwnerID);
					addToCC(user.activityPubID);
					Mention mention=new Mention();
					mention.href=user.activityPubID;
					tag.add(mention);
				}
			}
			inReplyTo=PostStorage.getActivityPubID(replyKey[replyKey.length-1]);
		}
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
		user=UserStorage.getUserByActivityPubID(attributedTo);
		if(inReplyTo!=null){
			owner=user;
		}else{

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

	public URI getInternalURL(){
		return Config.localURI("/"+owner.getFullUsername()+"/posts/"+id);
	}

	public void setParent(Post parent){
		replyKey=new int[parent.replyKey.length+1];
		System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
		replyKey[replyKey.length-1]=parent.id;
		inReplyTo=parent.activityPubID;
		if(tag==null)
			tag=new ArrayList<>();
		else if(!(tag instanceof ArrayList))
			tag=new ArrayList<>(tag);
		Mention mention=new Mention();
		mention.href=parent.user.activityPubID;
		tag.add(mention);
	}

	public int getReplyLevel(){
		return replyKey.length;
	}

	public void addToCC(URI uri){
		LinkOrObject l=new LinkOrObject(uri);
		if(!cc.contains(l)){
			if(!(cc instanceof ArrayList)){
				cc=new ArrayList<>(cc);
			}
			cc.add(l);
		}
	}
}
