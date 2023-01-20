package smithereen.controllers;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.Mention;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.UserPermissions;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.notifications.Notification;
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class WallController{
	private static final Logger LOG=LoggerFactory.getLogger(WallController.class);

	private final ApplicationContext context;

	public WallController(ApplicationContext context){
		this.context=context;
	}

	public void loadAndPreprocessRemotePostMentions(Post post){
		if(post.tag!=null){
			for(ActivityPubObject tag:post.tag){
				if(tag instanceof Mention){
					URI uri=((Mention) tag).href;
					try{
						User mentionedUser=context.getObjectLinkResolver().resolve(uri, User.class, true, true, false);
						if(post.mentionedUsers.isEmpty())
							post.mentionedUsers=new HashSet<>();
						post.mentionedUsers.add(mentionedUser);
					}catch(Exception x){
						LOG.warn("Error resolving mention for URI {}", uri, x);
					}
				}
			}
			if(!post.mentionedUsers.isEmpty() && StringUtils.isNotEmpty(post.content)){
				post.content=Utils.preprocessRemotePostMentions(post.content, post.mentionedUsers);
			}
		}
	}

	/**
	 * Create a new wall post or comment.
	 * @param author Post author.
	 * @param wallOwner Wall owner (user or group). Ignored for comments.
	 * @param inReplyToID Post ID this is in reply to, 0 for a top-level post.
	 * @param textSource HTML post text, as entered by the user.
	 * @param contentWarning Content warning (null for none).
	 * @param attachmentIDs IDs (hashes) of previously uploaded photo attachments.
	 * @param poll Poll to attach.
	 * @return The newly created post.
	 */
	public Post createWallPost(@NotNull User author, int authorAccountID, @NotNull Actor wallOwner, int inReplyToID,
							   @NotNull String textSource, @Nullable String contentWarning, @NotNull List<String> attachmentIDs,
							   @Nullable Poll poll){
		try{
			if(wallOwner instanceof Group group){
				context.getPrivacyController().enforceUserAccessToGroupContent(author, group);
			}
			if(poll!=null && (StringUtils.isEmpty(poll.question) || poll.options.size()<2)){
				LOG.warn("Invalid poll object passed to createWallPost: {}", poll);
				poll=null;
			}

			if(textSource.length()==0 && attachmentIDs.isEmpty() && poll==null)
				throw new BadRequestException("Empty post");

			if(!wallOwner.hasWall() && inReplyToID==0)
				throw new BadRequestException("This actor doesn't support wall posts");

			Post parent=inReplyToID!=0 ? getPostOrThrow(inReplyToID) : null;

			final ArrayList<User> mentionedUsers=new ArrayList<>();
			String text=preparePostText(textSource, mentionedUsers, parent);
			int userID=author.id;
			int postID;
			int pollID=0;

			if(poll!=null){
				List<String> opts=poll.options.stream().map(o->o.name).collect(Collectors.toList());
				if(opts.size()>=2){
					pollID=PostStorage.createPoll(wallOwner.getOwnerID(), poll.question, opts, poll.anonymous, poll.multipleChoice, poll.endTime);
				}
			}

			int maxAttachments=inReplyToID!=0 ? 2 : 10;
			int attachmentCount=pollID!=0 ? 1 : 0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();
				for(String id:attachmentIDs){
					if(!id.matches("^[a-fA-F0-9]{32}$"))
						continue;
					ActivityPubObject obj=MediaCache.getAndDeleteDraftAttachment(id, authorAccountID);
					if(obj!=null){
						attachObjects.add(obj);
						attachmentCount++;
					}
					if(attachmentCount==maxAttachments)
						break;
				}
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.get(0)).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}

			if(contentWarning!=null){
				contentWarning=contentWarning.trim();
				if(contentWarning.length()==0)
					contentWarning=null;
			}


			if(text.length()==0 && StringUtils.isEmpty(attachments) && pollID==0)
				throw new BadRequestException("Empty post");

			int ownerUserID=wallOwner instanceof User u ? u.id : 0;
			int ownerGroupID=wallOwner instanceof Group g ? g.id : 0;
			boolean isTopLevelPostOwn=true;
			int[] replyKey;
			if(parent!=null){
				replyKey=new int[parent.replyKey.length+1];
				System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
				replyKey[replyKey.length-1]=parent.id;
				Post topLevel;
				if(parent.replyKey.length>0){
					topLevel=PostStorage.getPostByID(parent.replyKey[0], false);
					if(topLevel!=null && !mentionedUsers.contains(topLevel.user))
						mentionedUsers.add(topLevel.user);
				}else{
					topLevel=parent;
				}
				if(topLevel!=null){
					if(topLevel.isGroupOwner()){
						ownerGroupID=((Group) topLevel.owner).id;
						ownerUserID=0;
						ensureUserNotBlocked(author, (Group) topLevel.owner);
						isTopLevelPostOwn=false;
					}else{
						ownerGroupID=0;
						ownerUserID=((User) topLevel.owner).id;
						ensureUserNotBlocked(author, (User)topLevel.owner);
						isTopLevelPostOwn=ownerUserID==topLevel.user.id;
					}
				}
			}else{
				replyKey=null;
			}
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, textSource, replyKey, mentionedUsers, attachments, contentWarning, pollID);
			if(ownerUserID==userID && replyKey==null){
				context.getNewsfeedController().putFriendsFeedEntry(author, postID, NewsfeedEntry.Type.POST);
			}

			Post post=PostStorage.getPostByID(postID, false);
			if(post==null)
				throw new IllegalStateException("?!");

			// Add{Note} is sent for any wall posts & comments on them, for local wall owners.
			// Create{Note} is sent for anything else.
			if((ownerGroupID!=0 || ownerUserID!=userID) && !isTopLevelPostOwn && !(wallOwner instanceof ForeignActor)){
				context.getActivityPubWorker().sendAddPostToWallActivity(post);
			}else{
				context.getActivityPubWorker().sendCreatePostActivity(post);
			}
			NotificationUtils.putNotificationsForPost(post, parent);

			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private String preparePostText(String textSource, final List<User> mentionedUsers, @Nullable Post parent) throws SQLException{
		String text=preprocessPostHTML(textSource, new Utils.MentionCallback(){
			@Override
			public User resolveMention(String username, String domain){
				try{
					if(domain==null){
						User user=UserStorage.getByUsername(username);
						if(user!=null && !mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
					User user=UserStorage.getByUsername(username+"@"+domain);
					if(user!=null){
						if(!mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
					URI uri=ActivityPub.resolveUsername(username, domain);
					return context.getObjectLinkResolver().resolve(uri, User.class, true, true, false);
				}catch(Exception x){
					LOG.warn("Can't resolve {}@{}", username, domain, x);
				}
				return null;
			}

			@Override
			public User resolveMention(String uri){
				try{
					URI u=new URI(uri);
					if("acct".equalsIgnoreCase(u.getScheme())){
						if(u.getSchemeSpecificPart().contains("@")){
							String[] parts=u.getSchemeSpecificPart().split("@");
							return resolveMention(parts[0], parts[1]);
						}
						return resolveMention(u.getSchemeSpecificPart(), null);
					}
					User user=UserStorage.getUserByActivityPubID(u);
					if(user!=null){
						if(!mentionedUsers.contains(user))
							mentionedUsers.add(user);
						return user;
					}
				}catch(Exception x){
					LOG.warn("Can't resolve {}", uri, x);
				}
				return null;
			}
		});

		if(parent!=null){
			// comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
			if(parent.replyKey.length>0 && text.startsWith("<p>"+escapeHTML(parent.user.getNameForReply())+", ")){
				text="<p><a href=\""+escapeHTML(parent.user.url.toString())+"\" class=\"mention\" data-user-id=\""+parent.user.id+"\">"
						+escapeHTML(parent.user.getNameForReply())+"</a>"+text.substring(parent.user.getNameForReply().length()+3);
			}
			if(!mentionedUsers.contains(parent.user))
				mentionedUsers.add(parent.user);
		}

		return text;
	}

	/**
	 * Get a post by ID.
	 * @param id Post ID
	 * @return The post
	 * @throws ObjectNotFoundException if the post does not exist or was deleted
	 */
	@NotNull
	public Post getPostOrThrow(int id){
		if(id<=0)
			throw new ObjectNotFoundException("err_post_not_found");
		try{
			Post post=PostStorage.getPostByID(id, false);
			if(post==null)
				throw new ObjectNotFoundException("err_post_not_found");
			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	@NotNull
	public Post getLocalPostOrThrow(int id){
		Post post=getPostOrThrow(id);
		if(!post.local)
			throw new ObjectNotFoundException("err_post_not_found");
		return post;
	}

	@NotNull
	public Post editPost(@NotNull User self, @NotNull UserPermissions permissions, int id, @NotNull String textSource, @Nullable String contentWarning, @NotNull List<String> attachmentIDs, @Nullable Poll poll){
		try{
			Post post=getPostOrThrow(id);
			if(!permissions.canEditPost(post))
				throw new UserActionNotAllowedException();
			context.getPrivacyController().enforceObjectPrivacy(self, post);
			if(textSource.length()==0 && attachmentIDs.isEmpty() && poll==null)
				throw new BadRequestException("Empty post");

			ArrayList<User> mentionedUsers=new ArrayList<>();
			Post parent=post.getReplyLevel()>0 ? getPostOrThrow(post.getReplyChainElement(post.getReplyLevel()-1)) : null;
			String text=preparePostText(textSource, mentionedUsers, parent);

			int pollID=0;
			if(poll!=null && !Objects.equals(post.poll, poll)){
				List<String> opts=poll.options.stream().map(o->o.name).collect(Collectors.toList());
				if(opts.size()>=2){
					pollID=PostStorage.createPoll(post.owner.getOwnerID(), poll.question, opts, poll.anonymous, poll.multipleChoice, poll.endTime);
				}
			}
			if(post.poll!=null && pollID==0){
				PostStorage.deletePoll(post.poll.id);
			}

			int maxAttachments=parent!=null ? 2 : 10;
			int attachmentCount=pollID!=0 ? 1 : 0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();

				ArrayList<String> remainingAttachments=new ArrayList<>(attachmentIDs);
				if(post.attachment!=null){
					for(ActivityPubObject att:post.attachment){
						if(att instanceof LocalImage li){
							if(!remainingAttachments.remove(li.localID)){
								LOG.debug("Deleting attachment: {}", li.localID);
								MediaStorageUtils.deleteAttachmentFiles(li);
							}else{
								attachObjects.add(li);
							}
						}else{
							attachObjects.add(att);
						}
					}
				}

				if(!remainingAttachments.isEmpty()){
					for(String aid : remainingAttachments){
						if(!aid.matches("^[a-fA-F0-9]{32}$"))
							continue;
						ActivityPubObject obj=MediaCache.getAndDeleteDraftAttachment(aid, post.user.id);
						if(obj!=null){
							attachObjects.add(obj);
							attachmentCount++;
						}
						if(attachmentCount==maxAttachments)
							break;
					}
				}
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.get(0)).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}

			PostStorage.updateWallPost(id, text, textSource, mentionedUsers, attachments, contentWarning, pollID);
			if(!post.isGroupOwner() && post.owner.getLocalID()==post.user.id){
				context.getNewsfeedController().clearFriendsFeedCache();
			}

			post=getPostOrThrow(id);
			context.getActivityPubWorker().sendUpdatePostActivity(post);
			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	/**
	 * Get posts from a wall.
	 * @param owner Wall owner, either a user or a group
	 * @param ownOnly Whether to return only user's own posts or include other's posts
	 * @param offset Pagination offset
	 * @param count Maximum number of posts to return
	 * @return A reverse-chronologically sorted paginated list of wall posts
	 */
	public PaginatedList<Post> getWallPosts(@NotNull Actor owner, boolean ownOnly, int offset, int count){
		try{
			int[] postCount={0};
			List<Post> wall=PostStorage.getWallPosts(owner.getLocalID(), owner instanceof Group, 0, 0, offset, count, postCount, ownOnly);
			return new PaginatedList<>(wall, postCount[0], offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	/**
	 * Get posts that two users posted on each other's walls.
	 * @param user The first user
	 * @param otherUser The second user
	 * @param offset Pagination offset
	 * @param count Maximum number of posts to return
	 * @return A reverse-chronologically sorted paginated list of wall posts
	 */
	public PaginatedList<Post> getWallToWallPosts(@NotNull User user, @NotNull User otherUser, int offset, int count){
		try{
			int[] postCount={0};
			List<Post> wall=PostStorage.getWallToWall(user.id, otherUser.id, offset, count, postCount);
			return new PaginatedList<>(wall, postCount[0], offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	/**
	 * Add top-level comments to each post.
	 * @param posts List of posts to add comments to
	 */
	public void populateCommentPreviews(@NotNull List<Post> posts){
		try{
			Set<Integer> postIDs=posts.stream().map((Post p)->p.id).collect(Collectors.toSet());
			Map<Integer, PaginatedList<Post>> allComments=PostStorage.getRepliesForFeed(postIDs);
			for(Post post:posts){
				PaginatedList<Post> comments=allComments.get(post.id);
				if(comments!=null){
					post.repliesObjects=comments.list;
					post.totalTopLevelComments=comments.total;
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	/**
	 * Get {@link UserInteractions} for posts.
	 * @param posts List of posts to get user interactions for
	 * @param self Current user to check whether posts are liked
	 * @return A map from a post ID to a {@link UserInteractions} object for each post
	 */
	public Map<Integer, UserInteractions> getUserInteractions(@NotNull List<Post> posts, @Nullable User self){
		try{
			Set<Integer> postIDs=posts.stream().map((Post p)->p.id).collect(Collectors.toSet());
			for(Post p:posts){
				p.getAllReplyIDs(postIDs);
			}
			return PostStorage.getPostInteractions(postIDs, self!=null ? self.id : 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendUpdateQuestionIfNeeded(Post post){
		if(post.poll==null)
			throw new IllegalArgumentException("Post must have a poll");
		if(!post.local)
			return;

		if(post.poll.lastVoteTime.until(Instant.now(), ChronoUnit.MINUTES)>=5){
			BackgroundTaskRunner.getInstance().submitDelayed(()->{
				try{
					// Get post again so the poll inside is up-to-date.
					context.getActivityPubWorker().sendUpdatePostActivity(getPostOrThrow(post.id));
				}catch(Exception x){
				  LOG.warn("Error sending Update{Question}", x);
				}
			}, 5, TimeUnit.MINUTES);
		}
	}

	public Map<URI, Integer> getPostLocalIDsByActivityPubIDs(@NotNull Collection<URI> ids, Actor owner){
		try{
			return PostStorage.getPostLocalIDsByActivityPubIDs(ids, owner instanceof User u ? u.id : 0, owner instanceof Group g ? g.id : 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, Post> getPosts(Collection<Integer> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return PostStorage.getPostsByID(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Post> getReplies(int[] key){
		try{
			return PostStorage.getReplies(key);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Post> getRepliesExact(int[] key, int maxID, int count){
		try{
			return PostStorage.getRepliesExact(key, maxID, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deletePost(@NotNull SessionInfo info, Post post){
		deletePostInternal(info, post, false);
	}

	public void deletePostAsServerModerator(@NotNull SessionInfo info, Post post){
		deletePostInternal(info, post, true);
	}

	private void deletePostInternal(@NotNull SessionInfo info, Post post, boolean ignorePermissions){
		try{
			if(!ignorePermissions){
				context.getPrivacyController().enforceObjectPrivacy(info.account.user, post);
				if(!info.permissions.canDeletePost(post)){
					throw new UserActionNotAllowedException();
				}
			}
			PostStorage.deletePost(post.id);
			NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
			if(Config.isLocal(post.activityPubID) && post.attachment!=null && !post.attachment.isEmpty()){
				MediaStorageUtils.deleteAttachmentFiles(post.attachment);
			}
			context.getNewsfeedController().clearFriendsFeedCache();
			User deleteActor=info.account.user;
			// if the current user is a moderator, and the post isn't made or owned by them, send the deletion as if the author deleted the post themselves
			if(info.account.accessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal() && post.user.id!=info.account.user.id && !post.isGroupOwner() && post.owner.getLocalID()!=info.account.user.id && !(post.user instanceof ForeignUser)){
				deleteActor=post.user;
			}
			context.getActivityPubWorker().sendDeletePostActivity(post, deleteActor);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getPollOptionVoters(PollOption option, int offset, int count){
		try{
			return UserStorage.getByIdAsList(PostStorage.getPollOptionVoters(option.id, offset, count));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setPostCWAsModerator(@NotNull UserPermissions permissions, Post post, String cw){
		try{
			if(permissions.serverAccessLevel.compareTo(Account.AccessLevel.MODERATOR)<0)
				throw new UserActionNotAllowedException();

			PostStorage.updateWallPostCW(post.id, cw);

			if(!post.isGroupOwner() && post.owner.getLocalID()==post.user.id){
				context.getNewsfeedController().clearFriendsFeedCache();
			}

			if(post.local){
				post=getPostOrThrow(post.id);
				context.getActivityPubWorker().sendUpdatePostActivity(post);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
