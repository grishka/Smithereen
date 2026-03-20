package smithereen.api.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.api.ApiCallContext;
import smithereen.model.Post;
import smithereen.model.UserInteractions;
import smithereen.model.apps.ClientApp;
import smithereen.model.attachments.Attachment;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.PostViewModel;

public class ApiWallPost{
	public int id;
	public int ownerId;
	public int fromId;
	public String apId, url;
	public long date;
	public String text;
	public String privacy;
	public Likes likes;
	public Reposts reposts;
	public PostSource postSource;
	public List<ApiAttachment> attachments;
	public String contentWarning;
	public boolean canDelete, canEdit;
	public List<Integer> mentionedUsers;

	// Top-level only
	public Comments comments;
	public List<ApiWallPost> repostHistory;
	public Boolean isMastodonStyleRepost;
	public Boolean canPin, isPinned;

	// Comments only
	public List<Integer> parentStack;
	public Integer replyToComment, replyToUser;
	public CommentThread thread;

	public record Likes(int count, boolean canLike, boolean userLikes){}
	public record Reposts(int count, boolean canRepost, boolean userReposted){}
	public record Comments(int count, boolean canComment){}
	public record CommentThread(int count, int replyCount, List<ApiWallPost> items){}
	public record PostSourceApp(long id, String apId, String name){}
	public record PostSource(PostSourceApp app, String action){}

	public ApiWallPost(PostViewModel post, ApiCallContext actx, Map<Integer, UserInteractions> interactions,
					   Map<Integer, List<Integer>> pinnedIDs, Map<Long, Photo> photos, Map<Long, ClientApp> apps){
		this(post, actx, interactions, pinnedIDs, photos, apps, true);
	}

	private ApiWallPost(PostViewModel post, ApiCallContext actx, Map<Integer, UserInteractions> interactions,
						Map<Integer, List<Integer>> pinnedIDs, Map<Long, Photo> photos, Map<Long, ClientApp> apps, boolean fillReposts){
		Post p=post.post;
		id=p.id;
		ownerId=p.ownerID;
		fromId=p.authorID;
		apId=p.getActivityPubID().toString();
		url=p.getActivityPubURL().toString();
		date=p.createdAt.getEpochSecond();
		text=p.text;
		privacy=switch(p.privacy){
			case PUBLIC -> null;
			case FOLLOWERS_ONLY -> "followers";
			case FOLLOWERS_AND_MENTIONED -> "followers_and_mentioned";
			case FRIENDS_ONLY -> "friends";
		};
		if(interactions!=null){
			UserInteractions postInteractions=interactions.get(id);
			if(postInteractions!=null){
				likes=new Likes(postInteractions.likeCount, postInteractions.canLike, postInteractions.isLiked);
				reposts=new Reposts(postInteractions.repostCount, postInteractions.canRepost && actx.self!=null, postInteractions.isReposted);
				if(p.getReplyLevel()==0){
					comments=new Comments(postInteractions.commentCount, postInteractions.canComment);
				}
			}
		}

		contentWarning=p.contentWarning;
		canDelete=actx.permissions!=null && actx.permissions.canDeletePost(p);
		canEdit=actx.permissions!=null && actx.permissions.canEditPost(p);
		mentionedUsers=new ArrayList<>(p.mentionedUserIDs);

		if(p.attachments!=null && !p.attachments.isEmpty()){
			List<Attachment> atts=p.getProcessedAttachments();
			attachments=new ArrayList<>();
			for(Attachment att:atts){
				attachments.add(new ApiAttachment(att, actx, p, photos));
			}
		}
		if(p.poll!=null){
			if(attachments==null)
				attachments=new ArrayList<>();
			ApiAttachment pollAtt=new ApiAttachment();
			pollAtt.type="poll";
			pollAtt.poll=new ApiPoll(p.poll, p, interactions, actx);
			attachments.add(pollAtt);
		}

		ClientApp app=p.appID==0 ? null : apps.get(p.appID);
		if(app!=null || p.action!=null){
			postSource=new PostSource(app==null ? null : new PostSourceApp(app.id, app.getActivityPubID().toString(), app.name), switch(p.action){
				case AVATAR_UPDATE -> "profile_picture_update";
				case null -> null;
			});
		}

		if(p.getReplyLevel()==0){
			isMastodonStyleRepost=p.isMastodonStyleRepost();
			if(post.repost!=null && fillReposts){
				repostHistory=new ArrayList<>();
				fillReposts(post.repost, actx, interactions, pinnedIDs, photos, apps, new HashSet<>());
			}
			canPin=actx.self!=null && actx.self.user.id==p.ownerID;
			if(p.ownerID>0){
				List<Integer> ownerPinnedIDs=pinnedIDs.get(p.ownerID);
				if(ownerPinnedIDs!=null)
					isPinned=ownerPinnedIDs.contains(id);
			}
		}else{
			if(p.getReplyLevel()>1){
				parentStack=p.replyKey.subList(1, p.replyKey.size());
				replyToComment=p.replyKey.getLast();
				replyToUser=post.parentAuthorID;
			}
			List<ApiWallPost> replies=post.repliesObjects.stream().map(reply->new ApiWallPost(reply, actx, interactions, pinnedIDs, photos, apps)).toList();
			thread=new CommentThread(p.replyCount, p.immediateReplyCount, replies);
		}
	}

	private void fillReposts(PostViewModel.Repost repost, ApiCallContext actx, Map<Integer, UserInteractions> interactions,
							 Map<Integer, List<Integer>> pinnedIDs, Map<Long, Photo> photos, Map<Long, ClientApp> apps, Set<Integer> seenPostIDs){
		repostHistory.add(new ApiWallPost(repost.post(), actx, interactions, pinnedIDs, photos, apps, false));
		seenPostIDs.add(repost.post().post.id);
		if(repost.post().repost!=null && !seenPostIDs.contains(repost.post().repost.post().post.id))
			fillReposts(repost.post().repost, actx, interactions, pinnedIDs, photos, apps, seenPostIDs);
	}
}
