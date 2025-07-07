package smithereen.model.board;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
import smithereen.storage.DatabaseUtils;
import smithereen.util.XTEA;

public final class BoardTopic implements ActivityPubRepresentable, CommentableContentObject{
	public long id;
	public int groupID;
	public String title;
	public Instant createdAt;
	public Instant updatedAt;
	public boolean isPinned;
	public int authorID;
	public int lastCommentAuthorID;
	public int numComments;
	public boolean isClosed;
	public URI apID;
	public URI apURL;
	public long firstCommentID;
	public Instant pinnedAt;

	public static BoardTopic fromResultSet(ResultSet res) throws SQLException{
		BoardTopic t=new BoardTopic();
		t.id=res.getLong("id");
		t.title=res.getString("title");
		t.groupID=res.getInt("group_id");
		t.authorID=res.getInt("author_id");
		t.createdAt=DatabaseUtils.getInstant(res, "created_at");
		t.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		t.pinnedAt=DatabaseUtils.getInstant(res, "pinned_at");
		t.isPinned=t.pinnedAt!=null;
		t.numComments=res.getInt("num_comments");
		t.lastCommentAuthorID=res.getInt("last_comment_author_id");
		t.isClosed=res.getBoolean("is_closed");
		t.firstCommentID=res.getLong("first_comment_id");
		String apID=res.getString("ap_id");
		if(apID!=null){
			t.apID=URI.create(apID);
			String apURL=res.getString("ap_url");
			if(apURL!=null)
				t.apURL=URI.create(apURL);
		}
		return t;
	}

	@Override
	public CommentParentObjectID getCommentParentID(){
		return new CommentParentObjectID(CommentableObjectType.BOARD_TOPIC, id);
	}

	@Override
	public String getURL(){
		return "/topics/"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.BOARD_TOPIC);
	}

	@Override
	public URI getActivityPubID(){
		if(apID!=null)
			return apID;
		return Config.localURI("/topics/"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.BOARD_TOPIC));
	}

	@Override
	public URI getCommentCollectionID(ApplicationContext context){
		return getActivityPubID();
	}

	@Override
	public URI getActivityPubURL(){
		if(apURL!=null)
			return apURL;
		return getActivityPubID();
	}

	@Override
	public int getOwnerID(){
		return -groupID;
	}

	@Override
	public int getAuthorID(){
		return authorID;
	}

	@Override
	public long getObjectID(){
		return id;
	}

	public String getIdString(){
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.BOARD_TOPIC);
	}
}
