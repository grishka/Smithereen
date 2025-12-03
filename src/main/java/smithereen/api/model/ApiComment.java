package smithereen.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import smithereen.api.ApiCallContext;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.Post;
import smithereen.model.UserInteractions;
import smithereen.model.attachments.Attachment;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.util.XTEA;

public class ApiComment{
	public String id;
	public int ownerId;
	public int fromId;
	public String apId, url;
	public long date;
	public String text;
	public Likes likes;
	public List<ApiAttachment> attachments;
	public String contentWarning;
	public boolean canDelete, canEdit;
	public List<Integer> mentionedUsers;
	public List<String> parentStack;
	public String replyToComment;
	public Integer replyToUser;
	public CommentThread thread;

	public transient long rawID;

	public record Likes(int count, boolean canLike, boolean userLikes){}
	public record CommentThread(int count, int replyCount, List<ApiComment> items){}

	public ApiComment(CommentViewModel comment, ApiCallContext actx, Map<Long, UserInteractions> interactions, Map<Long, Photo> photos){
		Comment c=comment.post;
		rawID=c.id;
		id=c.getIDString();
		ownerId=c.ownerID;
		fromId=c.authorID;
		apId=c.getActivityPubID().toString();
		url=c.getActivityPubURL().toString();
		date=c.createdAt.getEpochSecond();
		text=c.text;
		if(interactions!=null){
			UserInteractions postInteractions=interactions.get(c.id);
			if(postInteractions!=null){
				likes=new Likes(postInteractions.likeCount, postInteractions.canLike, postInteractions.isLiked);
			}
		}

		contentWarning=c.contentWarning;
		canDelete=actx.permissions!=null && actx.permissions.canDeletePost(c);
		canEdit=actx.permissions!=null && actx.permissions.canEditPost(c);
		mentionedUsers=new ArrayList<>(c.mentionedUserIDs);

		if(c.attachments!=null && !c.attachments.isEmpty()){
			List<Attachment> atts=c.getProcessedAttachments();
			attachments=new ArrayList<>();
			for(Attachment att:atts){
				attachments.add(new ApiAttachment(att, actx, c, photos));
			}
		}

		if(c.getReplyLevel()>0){
			parentStack=c.replyKey.stream().map(id->XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT)).toList();
			replyToComment=XTEA.encodeObjectID(c.replyKey.getLast(), ObfuscatedObjectIDType.COMMENT);
			replyToUser=comment.parentAuthorID;
		}
		List<ApiComment> replies=comment.repliesObjects.stream().map(reply->new ApiComment(reply, actx, interactions, photos)).toList();
		thread=new CommentThread(c.replyCount, c.immediateReplyCount, replies);
	}
}
