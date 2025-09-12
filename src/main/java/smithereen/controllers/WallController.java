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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static smithereen.Utils.ensureUserNotBlocked;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.CommentViewType;
import smithereen.model.ForeignUser;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.Group;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.PostLikeObject;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPermissions;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.admin.UserRole;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.notifications.Notification;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

public class WallController{
	private static final Logger LOG=LoggerFactory.getLogger(WallController.class);

	private final ApplicationContext context;

	public WallController(ApplicationContext context){
		this.context=context;
	}

	public void loadAndPreprocessRemotePostMentions(PostLikeObject post, NoteOrQuestion apSource){
		if(apSource.tag!=null){
			HashMap<Integer, User> mentionedUsers=new HashMap<>();
			for(ActivityPubObject tag:apSource.tag){
				if(tag instanceof Mention mention){
					URI uri=mention.href;
					try{
						User mentionedUser=context.getObjectLinkResolver().resolve(uri, User.class, true, true, false);
						mentionedUsers.put(mentionedUser.id, mentionedUser);
					}catch(Exception x){
						LOG.warn("Error resolving mention for URI {}", uri, x);
					}
				}
			}
			if(!mentionedUsers.isEmpty() && StringUtils.isNotEmpty(post.text)){
				post.text=TextProcessor.preprocessRemotePostMentions(post.text, mentionedUsers);
			}
		}
	}

	/**
	 * Create a new wall post or comment.
	 *
	 * @param author         Post author.
	 * @param wallOwner      Wall owner (user or group). Ignored for comments.
	 * @param inReplyTo      Post this is in reply to, null for a top-level post.
	 * @param textSource     HTML post text, as entered by the user.
	 * @param contentWarning Content warning (null for none).
	 * @param attachmentIDs  IDs (hashes) of previously uploaded photo attachments.
	 * @param poll           Poll to attach.
	 * @param action
	 * @return The newly created post.
	 */
	public Post createWallPost(@NotNull User author, int authorAccountID, @NotNull Actor wallOwner, Post inReplyTo,
							   @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs,
							   @Nullable Poll poll, @Nullable Post repost, @NotNull Map<String, String> attachAltTexts, Post.@Nullable Action action){
		try{
			if(wallOwner instanceof Group group){
				context.getPrivacyController().enforceUserAccessToGroupContent(author, group);
				if(group.wallState==GroupFeatureState.DISABLED)
					throw new UserActionNotAllowedException();
				if(inReplyTo==null){
					ensureUserNotBlocked(author, group);
					if(group.wallState==GroupFeatureState.ENABLED_CLOSED)
						context.getGroupsController().enforceUserAdminLevel(group, author, Group.AdminLevel.MODERATOR);
				}
			}else if(wallOwner instanceof User user){
				if(inReplyTo==null){
					ensureUserNotBlocked(author, user);
					if(user.id!=author.id){
						context.getPrivacyController().enforceUserPrivacy(author, user, UserPrivacySettingKey.WALL_POSTING);
						context.getPrivacyController().enforceUserPrivacy(author, user, UserPrivacySettingKey.WALL_OTHERS_POSTS);
					}
				}
			}
			if(poll!=null && (StringUtils.isEmpty(poll.question) || poll.options.size()<2)){
				LOG.warn("Invalid poll object passed to createWallPost: {}", poll);
				poll=null;
			}

			if(textSource.trim().isEmpty() && attachmentIDs.isEmpty() && poll==null && repost==null)
				throw new BadRequestException("Empty post");

			if(!wallOwner.hasWall() && inReplyTo==null)
				throw new BadRequestException("This actor doesn't support wall posts");

			if(inReplyTo!=null){
				if(inReplyTo.isMastodonStyleRepost()){
					inReplyTo=getPostOrThrow(inReplyTo.repostOf);
				}
				context.getPrivacyController().enforcePostPrivacy(author, inReplyTo);
			}
			if(inReplyTo!=null || wallOwner.getLocalID()!=author.id)
				repost=null;

			if(repost!=null){
				// If we're reposting a repost, use the original post if it's an Announce or there's no comment
				if(repost.isMastodonStyleRepost() || (repost.repostOf!=0 && TextProcessor.stripHTML(repost.text, false).trim().isEmpty() && (repost.attachments==null || repost.attachments.isEmpty()) && repost.poll==null)){
					repost=getPostOrThrow(repost.repostOf);
				}
				// Can't repost wall posts
				if(repost.ownerID!=repost.authorID && repost.getReplyLevel()==0)
					throw new UserActionNotAllowedException();
				// Can't repost comments on them either
				if(repost.getReplyLevel()>0){
					Post topLevel=getPostOrThrow(repost.replyKey.getFirst());
					if(topLevel.ownerID!=topLevel.authorID)
						throw new UserActionNotAllowedException();
				}
				// Reposted post must be public
				context.getPrivacyController().enforcePostPrivacy(null, repost);
				// Author must not be blocked by reposted post author or OP
				User repostAuthor=context.getUsersController().getUserOrThrow(repost.authorID);
				ensureUserNotBlocked(author, repostAuthor);
				if(repost.authorID!=repost.ownerID)
					ensureUserNotBlocked(author, context.getUsersController().getUserOrThrow(repost.ownerID));
				context.getFriendsController().incrementHintsRank(author, repostAuthor, 5);
			}

			final HashSet<User> mentionedUsers=new HashSet<>();
			String text=preparePostText(textSource, mentionedUsers, inReplyTo!=null && inReplyTo.getReplyLevel()>0 ? inReplyTo.authorID : 0, sourceFormat);
			int userID=author.id;
			int postID;
			int pollID=0;

			if(poll!=null){
				List<String> opts=poll.options.stream().map(o->o.text).collect(Collectors.toList());
				if(opts.size()>=2){
					pollID=PostStorage.createPoll(wallOwner.getOwnerID(), poll.question, opts, poll.anonymous, poll.multipleChoice, poll.endTime);
				}
			}

			int maxAttachments=inReplyTo!=null ? 2 : 10;
			int attachmentCount=pollID!=0 ? 1 : 0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();
				MediaStorageUtils.fillAttachmentObjects(context, author, attachObjects, attachmentIDs, attachAltTexts, attachmentCount, maxAttachments);
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.getFirst()).toString();
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
				if(contentWarning.isEmpty())
					contentWarning=null;
			}


			if(text.isEmpty() && StringUtils.isEmpty(attachments) && pollID==0 && repost==null)
				throw new BadRequestException("Empty post");

			EnumSet<Post.Flag> flags=EnumSet.noneOf(Post.Flag.class);

			int ownerUserID=wallOwner instanceof User u ? u.id : 0;
			int ownerGroupID=wallOwner instanceof Group g ? g.id : 0;
			boolean isTopLevelPostOwn=true;
			List<Integer> replyKey;
			if(inReplyTo!=null){
				replyKey=inReplyTo.getReplyKeyForReplies();
				Post topLevel;
				if(!inReplyTo.replyKey.isEmpty()){
					topLevel=getPostOrThrow(inReplyTo.replyKey.getFirst());
				}else{
					topLevel=inReplyTo;
				}
				context.getPrivacyController().enforcePostPrivacy(author, topLevel);

				OwnerAndAuthor topLevelOwnership=getContentAuthorAndOwner(topLevel);
				Actor topLevelOwner=topLevelOwnership.owner();
				User topLevelAuthor=topLevelOwnership.author();

				mentionedUsers.add(topLevelAuthor);
				if(topLevel.isGroupOwner()){
					Group group=(Group) topLevelOwner;
					if(group.wallState==GroupFeatureState.ENABLED_CLOSED || group.wallState==GroupFeatureState.DISABLED)
						throw new UserActionNotAllowedException();
					ownerGroupID=-topLevel.ownerID;
					ownerUserID=0;
					ensureUserNotBlocked(author, topLevelOwner);
					isTopLevelPostOwn=false;
				}else{
					ownerGroupID=0;
					ownerUserID=topLevel.ownerID;
					ensureUserNotBlocked(author, topLevelOwner);
					isTopLevelPostOwn=ownerUserID==topLevel.authorID;
					context.getPrivacyController().enforceUserPrivacy(author, (User)topLevelOwner, UserPrivacySettingKey.WALL_COMMENTING);
				}
				if(inReplyTo!=topLevel){
					User parentAuthor=context.getUsersController().getUserOrThrow(inReplyTo.authorID);
					context.getFriendsController().incrementHintsRank(author, parentAuthor, 3);
					context.getPrivacyController().enforcePostPrivacy(author, inReplyTo);
				}
				if(topLevelAuthor.id!=inReplyTo.authorID)
					context.getFriendsController().incrementHintsRank(author, topLevelAuthor, 5);

				if(topLevel.ownerID!=topLevel.authorID)
					flags.add(Post.Flag.TOP_IS_WALL_TO_WALL);
			}else{
				replyKey=null;
			}
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, textSource, sourceFormat, replyKey, mentionedUsers, attachments, contentWarning, pollID, repost!=null ? repost.id : 0, action, flags);
			if(ownerUserID==userID && replyKey==null){
				context.getNewsfeedController().putFriendsFeedEntry(author, postID, NewsfeedEntry.Type.POST);
			}else if(wallOwner instanceof Group g && replyKey==null){
				context.getNewsfeedController().putGroupsFeedEntry(g, postID, NewsfeedEntry.Type.POST);
			}

			if(wallOwner instanceof User u && u.id!=author.id && (inReplyTo==null || inReplyTo.authorID!=u.id))
				context.getFriendsController().incrementHintsRank(author, u, 3);
			else if(wallOwner instanceof Group g)
				context.getGroupsController().incrementHintsRank(author, g, 5);

			Post post=PostStorage.getPostByID(postID, false);
			if(post==null)
				throw new IllegalStateException("?!");

			if(post.attachments!=null){
				for(ActivityPubObject att:post.attachments){
					if(att instanceof LocalImage li){
						MediaStorage.createMediaFileReference(li.fileID, post.id, MediaFileReferenceType.WALL_ATTACHMENT, post.getOwnerID());
					}
				}
			}

			// Add{Note} is sent for any wall posts & comments on them, for local wall owners.
			// Create{Note} is sent for anything else.
			if(post.ownerID!=post.authorID && (post.getReplyLevel()==0 || (post.getReplyLevel()>0 && !isTopLevelPostOwn)) && !(wallOwner instanceof ForeignActor)){
				context.getActivityPubWorker().sendAddPostToWallActivity(post);
			}else{
				context.getActivityPubWorker().sendCreatePostActivity(post);
			}
			context.getNotificationsController().createNotificationsForObject(post);

			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public String preparePostText(String textSource, final Set<User> mentionedUsers, int parentAuthorID, @NotNull FormattedTextFormat format) throws SQLException{
		String text=TextProcessor.preprocessPostText(textSource, new TextProcessor.MentionCallback(){
			@Override
			public User resolveMention(String username, String domain){
				try{
					if(domain==null){
						User user=UserStorage.getByUsername(username);
						if(user!=null)
							mentionedUsers.add(user);
						return user;
					}
					User user=UserStorage.getByUsername(username+"@"+domain);
					if(user!=null){
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
						mentionedUsers.add(user);
						return user;
					}
				}catch(Exception x){
					LOG.warn("Can't resolve {}", uri, x);
				}
				return null;
			}
		}, format);

		if(parentAuthorID>0){
			// comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
			User parentAuthor=context.getUsersController().getUserOrThrow(parentAuthorID);
			String nameForReply=TextProcessor.escapeHTML(parentAuthor.getNameForReply());
			if(text.startsWith("<p>"+nameForReply+",")){
				text="<p><a href=\""+TextProcessor.escapeHTML(parentAuthor.url.toString())+"\" class=\"mention\" data-user-id=\""+parentAuthor.id+"\">"
						+nameForReply+"</a>"+text.substring(nameForReply.length()+3);
			}
			mentionedUsers.add(parentAuthor);
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
		return getPostOrThrow(id, false);
	}

	/**
	 * Get a post by ID.
	 *
	 * @param id          Post ID
	 * @param wantDeleted whether to return the post even if it was deleted
	 * @return The post
	 * @throws ObjectNotFoundException if the post does not exist or was deleted
	 */
	@NotNull
	public Post getPostOrThrow(int id, boolean wantDeleted){
		if(id<=0)
			throw new ObjectNotFoundException("err_post_not_found");
		try{
			Post post=PostStorage.getPostByID(id, wantDeleted);
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
		if(!post.isLocal())
			throw new ObjectNotFoundException("err_post_not_found");
		return post;
	}

	@NotNull
	public Post getPostOrThrow(URI apID){
		try{
			Post post=PostStorage.getPostByID(apID);
			if(post==null)
				throw new ObjectNotFoundException();
			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	@NotNull
	public Post editPost(@NotNull User self, @NotNull UserPermissions permissions, int id, @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning,
						 @NotNull List<String> attachmentIDs, @Nullable Poll poll, @NotNull Map<String, String> attachAltTexts){
		try{
			Post post=getPostOrThrow(id);
			if(!permissions.canEditPost(post))
				throw new UserActionNotAllowedException();
			context.getPrivacyController().enforceObjectPrivacy(self, post);
			if(textSource.isEmpty() && attachmentIDs.isEmpty() && poll==null && post.repostOf==0)
				throw new BadRequestException("Empty post");

			HashSet<User> mentionedUsers=new HashSet<>();
			Post parent=post.getReplyLevel()>0 ? getPostOrThrow(post.getReplyChainElement(post.getReplyLevel()-1)) : null;
			String text=preparePostText(textSource, mentionedUsers, parent!=null && parent.getReplyLevel()>1 ? parent.authorID : 0, sourceFormat);

			int pollID=0;
			if(poll!=null && !Objects.equals(post.poll, poll)){
				List<String> opts=poll.options.stream().map(o->o.text).collect(Collectors.toList());
				if(opts.size()>=2){
					pollID=PostStorage.createPoll(post.ownerID, poll.question, opts, poll.anonymous, poll.multipleChoice, poll.endTime);
				}
			}else if(post.poll!=null){
				pollID=post.poll.id;
			}
			if(post.poll!=null && pollID==0){
				PostStorage.deletePoll(post.poll.id);
			}

			int maxAttachments=parent!=null ? 2 : 10;
			int attachmentCount=pollID!=0 ? 1 : 0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();

				ArrayList<String> newlyAddedAttachments=new ArrayList<>(attachmentIDs);
				if(post.attachments!=null){
					for(ActivityPubObject att:post.attachments){
						if(att instanceof LocalImage li){
							String localID=li.getLocalID();
							if(!newlyAddedAttachments.remove(localID)){
								LOG.debug("Deleting attachment: {}", localID);
								MediaStorage.deleteMediaFileReference(post.id, MediaFileReferenceType.WALL_ATTACHMENT, li.fileID);
							}else{
								li.name=attachAltTexts.get(li.getLocalID());
								attachObjects.add(li);
							}
						}else{
							attachObjects.add(att);
						}
					}
				}

				if(!newlyAddedAttachments.isEmpty()){
					MediaStorageUtils.fillAttachmentObjects(context, self, attachObjects, newlyAddedAttachments, attachAltTexts, attachmentCount, maxAttachments);
					for(ActivityPubObject att:attachObjects){
						if(att instanceof LocalImage li && newlyAddedAttachments.contains(li.fileRecord.id().getIDForClient())){
							MediaStorage.createMediaFileReference(li.fileID, post.id, MediaFileReferenceType.WALL_ATTACHMENT, post.ownerID);
						}
					}
				}
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.getFirst()).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}

			PostStorage.updateWallPost(id, text, textSource, sourceFormat, mentionedUsers, attachments, contentWarning, pollID);
			if(post.ownerID>0 && post.ownerID==post.authorID){
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
	public PaginatedList<Post> getWallPosts(@Nullable User self, @NotNull Actor owner, boolean ownOnly, int offset, int count){
		try{
			int[] postCount={0};
			Set<Post.Privacy> allowedPrivacy;
			if(self!=null && owner instanceof User ownerUser){
				if(self.id==owner.getOwnerID()){
					allowedPrivacy=EnumSet.allOf(Post.Privacy.class);
				}else{
					FriendshipStatus status=context.getFriendsController().getSimpleFriendshipStatus(self, ownerUser);
					allowedPrivacy=switch(status){
						case FOLLOWING -> EnumSet.of(Post.Privacy.PUBLIC, Post.Privacy.FOLLOWERS_ONLY, Post.Privacy.FOLLOWERS_AND_MENTIONED);
						case FRIENDS -> EnumSet.of(Post.Privacy.PUBLIC, Post.Privacy.FOLLOWERS_ONLY, Post.Privacy.FOLLOWERS_AND_MENTIONED, Post.Privacy.FRIENDS_ONLY);
						default -> EnumSet.of(Post.Privacy.PUBLIC);
					};
				}
			}else{
				allowedPrivacy=EnumSet.of(Post.Privacy.PUBLIC);
			}
			List<Post> wall=PostStorage.getWallPosts(owner.getLocalID(), owner instanceof Group, 0, 0, offset, count, postCount, ownOnly, allowedPrivacy);
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
	public PaginatedList<Post> getWallToWallPosts(@Nullable User self, @NotNull User user, @NotNull User otherUser, int offset, int count){
		try{
			context.getPrivacyController().enforceUserPrivacy(self, user, UserPrivacySettingKey.WALL_OTHERS_POSTS);
			context.getPrivacyController().enforceUserPrivacy(self, otherUser, UserPrivacySettingKey.WALL_OTHERS_POSTS);
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
	public void populateCommentPreviews(@Nullable User self, @NotNull List<PostViewModel> posts, CommentViewType viewType){
		try{
			Set<List<Integer>> postIDs=posts.stream().map(PostViewModel::getReplyKeyForInteractions).collect(Collectors.toSet());
			Map<Integer, PaginatedList<Post>> allComments=PostStorage.getRepliesForFeed(postIDs, viewType==CommentViewType.FLAT);
			List<PostViewModel> commentsThatNeedAuthors=null;
			if(viewType==CommentViewType.FLAT){
				commentsThatNeedAuthors=new ArrayList<>();
			}
			for(PostViewModel post:posts){
				PaginatedList<Post> comments=allComments.get(post.post.getIDForInteractions());
				if(comments!=null){
					context.getPrivacyController().filterPosts(self, comments.list);
					if(!comments.list.isEmpty()){
						post.repliesObjects=comments.list.stream().map(PostViewModel::new).toList();
						post.totalTopLevelComments=comments.total;
						if(viewType==CommentViewType.FLAT){
							commentsThatNeedAuthors.addAll(post.repliesObjects);
						}
					}
				}
			}
			if(viewType==CommentViewType.FLAT){
				fillInParentAuthors(commentsThatNeedAuthors, null);
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
	public Map<Integer, UserInteractions> getUserInteractions(@NotNull List<PostViewModel> posts, @Nullable User self){
		try{
			Set<Integer> postIDs=posts.stream().map(p->p.post.getIDForInteractions()).collect(Collectors.toSet());
			Set<Integer> ownerUserIDs=new HashSet<>(), ownerGroupIDs=new HashSet<>();
			for(PostViewModel p:posts){
				p.getAllReplyIDs(postIDs);
				if(!p.post.isMastodonStyleRepost()){
					if(p.post.ownerID>0)
						ownerUserIDs.add(p.post.ownerID);
					else if(p.post.ownerID<0)
						ownerGroupIDs.add(-p.post.ownerID);
				}else if(p.repost!=null && p.repost.post()!=null){
					Post repost=p.repost.post().post;
					if(repost.ownerID>0)
						ownerUserIDs.add(repost.ownerID);
				}
			}
			Map<Integer, Boolean> canComment=context.getUsersController().getUsers(ownerUserIDs)
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e->context.getPrivacyController().checkUserPrivacy(self, e.getValue(), UserPrivacySettingKey.WALL_COMMENTING)));

			if(!ownerGroupIDs.isEmpty()){
				canComment=new HashMap<>(canComment);
				for(Group group:context.getGroupsController().getGroupsByIdAsList(ownerGroupIDs)){
					if(group==null)
						continue;
					canComment.put(-group.id, group.wallState!=GroupFeatureState.ENABLED_CLOSED);
				}
			}

			List<PostViewModel> allPosts=posts.stream().flatMap(pvm->{
				ArrayList<PostViewModel> replies=new ArrayList<>();
				replies.add(pvm);
				pvm.getAllReplies(replies);
				return replies.stream();
			}).toList();
			Map<Integer, PostViewModel> postsByID=allPosts.stream().collect(Collectors.toMap(pvm->pvm.post.id, Function.identity(), (p1, p2)->p1));
			Map<Integer, UserInteractions> interactions=PostStorage.getPostInteractions(postIDs, self!=null ? self.id : 0);

			for(PostViewModel post:allPosts){
				UserInteractions ui=interactions.get(post.post.getIDForInteractions());
				int ownerID;
				if(post.post.isMastodonStyleRepost()){
					if(post.repost!=null && post.repost.post()!=null){
						ownerID=post.repost.post().post.ownerID;
					}else{
						ownerID=0;
					}
				}else{
					ownerID=post.post.ownerID;
				}
				// Can't comment on posts or in threads that don't exist
				if(post.post.isMastodonStyleRepost() && post.repost!=null && (post.repost.post()==null || (post.repost.post().post.getReplyLevel()>0 && post.repost.topLevel()==null)))
					ui.canComment=false;
				else
					ui.canComment=canComment.getOrDefault(ownerID, true);

				ui.canRepost=true;
				if(post.post.privacy!=Post.Privacy.PUBLIC){
					ui.canRepost=false;
				}else if(post.post.ownerID!=post.post.authorID){
					if(post.post.getReplyLevel()==0){
						ui.canRepost=false; // Wall-to-wall post
					}else if(postsByID.get(post.post.replyKey.getFirst()) instanceof PostViewModel p && p.post.ownerID!=p.post.authorID){
						ui.canRepost=false; // Comment on a wall-to-wall post
					}
				}
			}
			return interactions;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendUpdateQuestionIfNeeded(Post post){
		if(post.poll==null)
			throw new IllegalArgumentException("Post must have a poll");
		if(!post.isLocal())
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

	public Map<URI, Integer> getPostLocalIDsByActivityPubIDs(@NotNull Collection<URI> ids, Actor owner, boolean comments){
		try{
			return PostStorage.getPostLocalIDsByActivityPubIDs(ids, owner instanceof User u ? u.id : 0, owner instanceof Group g ? g.id : 0, comments);
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

	public PostSource getPostSource(Post post){
		try{
			return PostStorage.getPostSource(post.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PostViewModel> getReplies(@Nullable User self, List<Integer> key, int primaryOffset, int primaryCount, int secondaryCount, CommentViewType type, boolean reversed){
		try{
			LOG.trace("Getting post replies: priOffset={}, priCount={}, secCount={}, key={}, type={}", primaryOffset, primaryCount, secondaryCount, key, type);
			Post threadParent=PostStorage.getPostByID(key.getLast(), true);
			if(threadParent==null)
				throw new ObjectNotFoundException();

			// For two-level and flat, if we're getting replies to a comment, always treat them as a flat list
			if((type==CommentViewType.TWO_LEVEL && threadParent.getReplyLevel()>0) || type==CommentViewType.FLAT){
				PaginatedList<PostViewModel> posts=PostViewModel.wrap(PostStorage.getRepliesFlat(key, primaryOffset, primaryCount, reversed));
				context.getPrivacyController().filterPostViewModels(self, posts.list);
				fillInParentAuthors(posts.list, threadParent);
				return posts;
			}
			PostStorage.ThreadedReplies tr=PostStorage.getRepliesThreaded(key, primaryOffset, primaryCount, secondaryCount, type==CommentViewType.TWO_LEVEL, reversed);

			List<PostViewModel> posts=tr.posts().stream().filter(p->context.getPrivacyController().checkPostPrivacy(self, p)).map(PostViewModel::new).toList();
			List<PostViewModel> replies=tr.replies().stream().filter(p->context.getPrivacyController().checkPostPrivacy(self, p)).map(PostViewModel::new).toList();
			Map<Integer, PostViewModel> postMap=Stream.of(posts, replies).flatMap(List::stream).collect(Collectors.toMap(p->p.post.id, Function.identity()));

			for(PostViewModel post:replies){
				if(post.post.getReplyLevel()>key.size()){
					PostViewModel parent=switch(type){
						case THREADED -> postMap.get(post.post.replyKey.getLast());
						case TWO_LEVEL -> postMap.get(post.post.replyKey.get(1));
						case FLAT -> throw new IllegalArgumentException();
					};
					if(parent!=null){
						PostViewModel realParent=postMap.get(post.post.replyKey.getLast());
						if(realParent!=null)
							post.parentAuthorID=realParent.post.authorID;
						parent.repliesObjects.add(post);
					}
				}
			}
			for(PostViewModel post:posts){
				post.parentAuthorID=threadParent.authorID;
			}

			return new PaginatedList<>(posts, tr.total(), primaryOffset, primaryCount);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PostViewModel> getRepliesFlat(@Nullable User self, List<Integer> key, int maxID, int count){
		try{
			PaginatedList<PostViewModel> posts=PostViewModel.wrap(PostStorage.getRepliesFlatWithMaxID(key, maxID, count));
			context.getPrivacyController().filterPostViewModels(self, posts.list);
			fillInParentAuthors(posts.list, null);
			return posts;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void fillInParentAuthors(List<PostViewModel> posts, Post threadParent) throws SQLException{
		HashMap<Integer, Integer> authorIDs=new HashMap<>();
		if(threadParent!=null)
			authorIDs.put(threadParent.id, threadParent.authorID);
		for(PostViewModel p:posts){
			authorIDs.put(p.post.id, p.post.authorID);
		}
		HashSet<Integer> needAuthors=new HashSet<>();
		for(PostViewModel p:posts){
			int author=authorIDs.getOrDefault(p.post.replyKey.getLast(), 0);
			if(author!=0){
				p.parentAuthorID=author;
			}else{
				needAuthors.add(p.post.replyKey.getLast());
			}
		}
		if(!needAuthors.isEmpty()){
			Map<Integer, Integer> moreAuthorIDs=PostStorage.getPostAuthors(needAuthors);
			for(PostViewModel p:posts){
				if(p.parentAuthorID==0){
					p.parentAuthorID=moreAuthorIDs.getOrDefault(p.post.replyKey.getLast(), 0);
				}
			}
		}
	}

	public PaginatedList<Post> getRepliesExact(@Nullable User self, List<Integer> key, int maxID, int count){
		try{
			PaginatedList<Post> posts=PostStorage.getRepliesExact(key.stream().mapToInt(Integer::intValue).toArray(), maxID, count);
			posts.list=new ArrayList<>(posts.list);
			context.getPrivacyController().filterPosts(self, posts.list);
			return posts;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deletePost(@NotNull User self, Post post){
		deletePostInternal(self, post, false);
	}

	public void deletePostAsServerModerator(@NotNull User self, Post post){
		deletePostInternal(self, post, true);
	}

	private void deletePostInternal(@NotNull User self, Post post, boolean ignorePermissions){
		try{
			OwnerAndAuthor oaa;
			try{
				oaa=getContentAuthorAndOwner(post);
			}catch(ObjectNotFoundException x){
				oaa=new OwnerAndAuthor(null, null);
			}
			if(!ignorePermissions){
				context.getPrivacyController().enforceObjectPrivacy(self, post);
				if(post.ownerID!=self.id && post.authorID!=self.id){ // Can always delete own posts and others' posts on own wall
					if(post.ownerID>0) // Can't delete posts not made of owned oneself
						throw new UserActionNotAllowedException();
					else // Must be at least a moderator to delete others' posts in groups
						context.getGroupsController().enforceUserAdminLevel((Group)oaa.owner(), self, Group.AdminLevel.MODERATOR);
				}
			}
			PostStorage.deletePost(post.id);
			NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
			context.getNewsfeedController().clearFriendsFeedCache();
			if(post.ownerID<0 && post.getReplyLevel()==0){
				context.getNewsfeedController().clearGroupsFeedCache();
			}
			User deleteActor=self;
			// if the current user is a moderator, and the post isn't made or owned by them, send the deletion as if the author deleted the post themselves
			if(ignorePermissions && post.authorID!=self.id && !post.isGroupOwner() && post.ownerID!=self.id && !(oaa.author() instanceof ForeignUser)){
				deleteActor=oaa.author();
			}
			if(oaa.author()!=null && Config.isLocal(post.getActivityPubID())){
				context.getActivityPubWorker().sendDeletePostActivity(post, deleteActor);
			}

			if(post.isLocal() && post.attachments!=null){
				MediaStorage.deleteMediaFileReferences(post.id, MediaFileReferenceType.WALL_ATTACHMENT);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Integer> getPollOptionVoters(PollOption option, int offset, int count){
		try{
			return PostStorage.getPollOptionVoters(option.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setPostCWAsModerator(@NotNull UserPermissions permissions, Post post, String cw){
		try{
			if(!permissions.hasPermission(UserRole.Permission.MANAGE_REPORTS))
				throw new UserActionNotAllowedException();

			PostStorage.updateWallPostCW(post.id, cw);

			if(!post.isGroupOwner() && post.ownerID==post.authorID){
				context.getNewsfeedController().clearFriendsFeedCache();
			}

			if(post.isLocal()){
				post=getPostOrThrow(post.id);
				context.getActivityPubWorker().sendUpdatePostActivity(post);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public OwnerAndAuthor getContentAuthorAndOwner(OwnedContentObject post){
		int ownerID=post.getOwnerID();
		int authorID=post.getAuthorID();
		Actor owner;
		User author;
		if(ownerID<0)
			owner=context.getGroupsController().getGroupOrThrow(-ownerID);
		else
			owner=context.getUsersController().getUserOrThrow(ownerID);
		if(authorID!=0){
			try{
				author=context.getUsersController().getUserOrThrow(authorID);
			}catch(ObjectNotFoundException x){
				author=null;
			}
		}else{
			author=null;
		}
		return new OwnerAndAuthor(owner, author);
	}

	public int getPostIDByActivityPubID(URI id){
		try{
			int lid=PostStorage.getLocalIDByActivityPubID(id);
			return Math.max(lid, 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void populateReposts(User self, Collection<PostViewModel> posts, int maxDepth){
		HashMap<Integer, PostViewModel> knownPosts=posts.stream().collect(Collectors.toMap(p->p.post.id, Function.identity(), (a, b)->b, HashMap::new));
		HashSet<Integer> needPosts=new HashSet<>();
		HashSet<PostViewModel> reposts=new HashSet<>(), nextReposts=new HashSet<>();
		for(PostViewModel post:posts){
			if(post.post.repostOf!=0){
				reposts.add(post);
			}
		}
		for(int i=0;i<maxDepth;i++){
			needPosts.clear();
			nextReposts.clear();
			for(PostViewModel post:reposts){
				if(!knownPosts.containsKey(post.post.repostOf)){
					needPosts.add(post.post.repostOf);
				}
			}
			if(!needPosts.isEmpty()){
				Map<Integer, Post> newPosts=getPosts(needPosts);
				needPosts.clear();
				for(Post post:newPosts.values()){
					knownPosts.put(post.id, new PostViewModel(post));
					if(post.getReplyLevel()>0 && !knownPosts.containsKey(post.replyKey.getFirst())){
						needPosts.add(post.replyKey.getFirst());
					}
				}
				// For comments, get their top-level posts
				if(!needPosts.isEmpty()){
					for(Post post:getPosts(needPosts).values()){
						knownPosts.put(post.id, new PostViewModel(post));
					}
				}
			}
			for(PostViewModel post:reposts){
				PostViewModel repost=knownPosts.get(post.post.repostOf);
				if(repost!=null){
					PostViewModel topLevel=repost.post.getReplyLevel()>0 ? knownPosts.get(repost.post.replyKey.getFirst()) : null;
					post.repost=new PostViewModel.Repost(repost, topLevel);
					nextReposts.add(repost);
				}
			}
			HashSet<PostViewModel> tmp=reposts;
			reposts=nextReposts;
			nextReposts=tmp;
		}
	}

	public PaginatedList<Post> getPostReposts(Post post, int offset, int count){
		try{
			return PostStorage.getPostReposts(post.getIDForInteractions(), offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, PostViewModel> getUserReplies(User self, Collection<List<Integer>> replyKeys){
		if(replyKeys.isEmpty())
			return Map.of();
		try{
			List<Post> posts=PostStorage.getUserReplies(self.id, replyKeys);
			return posts.stream().collect(Collectors.toMap(p->p.replyKey.getLast(), PostViewModel::new, (p1, p2)->p1.post.id>p2.post.id ? p1 : p2));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<PostViewModel> getAllPostsByAuthor(User user, int offset, int count){
		try{
			PaginatedList<Post> posts=PostStorage.getAllPostsByAuthor(user.id, offset, count);
			return PostViewModel.wrap(posts);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// region Pinned posts

	public List<Post> getPinnedPosts(User self, User owner){
		try{
			List<Post> posts=PostStorage.getPinnedPosts(owner.id);
			boolean hasPrivate=false;
			for(Post p:posts){
				if(p.privacy!=Post.Privacy.PUBLIC){
					hasPrivate=true;
					break;
				}
			}
			if(hasPrivate){
				posts=new ArrayList<>(posts);
				context.getPrivacyController().filterPosts(self, posts);
			}
			return posts;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean isPostPinned(Post post){
		try{
			return PostStorage.isPostPinned(post.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void pinPost(Post post, boolean keepPrevious){
		try{
			PostStorage.pinPost(post.authorID, post.id, keepPrevious);
			// TODO federate
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void unpinPost(Post post){
		try{
			PostStorage.unpinPost(post.authorID, post.id);
			// TODO federate
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
}
