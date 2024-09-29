package smithereen.model.comments;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.Utils;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PostLikeObject;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

public non-sealed class Comment extends PostLikeObject implements ActivityPubRepresentable, CommentReplyParent{
	public long id;
	public CommentParentObjectID parentObjectID;
	public List<Long> replyKey=List.of();

	public static Comment fromResultSet(ResultSet res) throws SQLException{
		Comment c=new Comment();
		c.fillFromResultSet(res);
		return c;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getLong("id");
		parentObjectID=new CommentParentObjectID(CommentableObjectType.values()[res.getInt("parent_object_type")], res.getLong("parent_object_id"));
		byte[] rk=res.getBytes("reply_key");
		if(rk!=null){
			replyKey=new ArrayList<>(rk.length/8);
			Utils.deserializeLongCollection(rk, replyKey);
		}

		super.fillFromResultSet(res);
	}

	@Override
	public int getReplyLevel(){
		return replyKey.size();
	}

	@Override
	public URI getInternalURL(){
		return UriBuilder.local().path("comments", getIDString()).build();
	}

	@Override
	public long getObjectID(){
		return id;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return null;
	}

	@Override
	public String getPhotoListID(){
		return "comments/"+getIDString();
	}

	public String getReplyParentID(){
		if(replyKey==null || replyKey.isEmpty())
			return null;
		return XTEA.encodeObjectID(replyKey.getLast(), ObfuscatedObjectIDType.COMMENT);
	}

	public String getIDString(){
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT);
	}

	public List<Long> getReplyKeyForReplies(){
		ArrayList<Long> rk=new ArrayList<>(replyKey);
		rk.add(id);
		return rk;
	}

	public URI getActivityPubID(){
		return activityPubID!=null ? activityPubID : getInternalURL();
	}

	public URI getActivityPubURL(){
		return activityPubURL!=null ? activityPubURL : getInternalURL();
	}
}
