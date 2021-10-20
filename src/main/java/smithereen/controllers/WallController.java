package smithereen.controllers;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

import static smithereen.Utils.ensureUserNotBlocked;
import static smithereen.Utils.escapeHTML;
import static smithereen.Utils.parseIntOrDefault;
import static smithereen.Utils.preprocessPostHTML;

public class WallController{
	private static final Logger LOG=LoggerFactory.getLogger(WallController.class);

	private ApplicationContext context;

	public WallController(ApplicationContext context){
		this.context=context;
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
	public Post createWallPost(@NotNull User author, @NotNull Actor wallOwner, int inReplyToID,
							   @NotNull String textSource, @Nullable String contentWarning, @NotNull List<String> attachmentIDs,
							   @Nullable Poll poll){
		try{
			if(poll!=null && (StringUtils.isEmpty(poll.question) || poll.options.size()<2)){
				LOG.warn("Invalid poll object passed to createWallPost: {}", poll);
				poll=null;
			}

			if(textSource.length()==0 && attachmentIDs.isEmpty() && poll==null)
				throw new BadRequestException("Empty post");

			if(!wallOwner.hasWall())
				throw new BadRequestException("This actor doesn't support wall posts");

			final ArrayList<User> mentionedUsers=new ArrayList<>();
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
						ActivityPubObject obj=ActivityPub.fetchRemoteObject(uri);
						if(obj instanceof ForeignUser){
							ForeignUser _user=(ForeignUser)obj;
							UserStorage.putOrUpdateForeignUser(_user);
							if(!mentionedUsers.contains(_user))
								mentionedUsers.add(_user);
							return _user;
						}
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
			int userID=author.id;
			int postID;
			int pollID=0;

			if(poll!=null){
				List<String> opts=poll.options.stream().map(o->o.name).collect(Collectors.toList());
				if(opts.size()>=2){
					pollID=PostStorage.createPoll(author.id, poll.question, opts, poll.anonymous, poll.multipleChoice, poll.endTime);
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
					ActivityPubObject obj=MediaCache.getAndDeleteDraftAttachment(id, author.id);
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

			Post parent=null;
			int ownerUserID=wallOwner instanceof User ? ((User) wallOwner).id : 0;
			int ownerGroupID=wallOwner instanceof Group ? ((Group) wallOwner).id : 0;
			int[] replyKey;
			if(inReplyToID!=0){
				parent=getPostOrThrow(inReplyToID);
				replyKey=new int[parent.replyKey.length+1];
				System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
				replyKey[replyKey.length-1]=parent.id;
				// comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
				if(parent.replyKey.length>0 && text.startsWith("<p>"+escapeHTML(parent.user.getNameForReply())+", ")){
					text="<p><a href=\""+escapeHTML(parent.user.url.toString())+"\" class=\"mention\" data-user-id=\""+parent.user.id+"\">"+escapeHTML(parent.user.getNameForReply())+"</a>"+text.substring(parent.user.getNameForReply().length()+3);
				}
				if(!mentionedUsers.contains(parent.user))
					mentionedUsers.add(parent.user);
				Post topLevel;
				if(parent.replyKey.length>1){
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
					}else{
						ownerGroupID=0;
						ownerUserID=((User) topLevel.owner).id;
						ensureUserNotBlocked(author, (User)topLevel.owner);
					}
				}
			}else{
				replyKey=null;
			}
			postID=PostStorage.createWallPost(userID, ownerUserID, ownerGroupID, text, textSource, replyKey, mentionedUsers, attachments, contentWarning, pollID);

			Post post=PostStorage.getPostByID(postID, false);
			if(post==null)
				throw new IllegalStateException("?!");
			if(inReplyToID==0 && (ownerGroupID!=0 || ownerUserID!=userID) && !(wallOwner instanceof ForeignActor)){
				ActivityPubWorker.getInstance().sendAddPostToWallActivity(post);
			}else{
				ActivityPubWorker.getInstance().sendCreatePostActivity(post);
			}
			NotificationUtils.putNotificationsForPost(post, parent);

			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	/**
	 * Get a post by ID.
	 * @param id Post ID
	 * @return The post
	 * @throws ObjectNotFoundException if the post does not exist or was deleted
	 */
	@NotNull
	public Post getPostOrThrow(int id){
		try{
			Post post=PostStorage.getPostByID(id, false);
			if(post==null)
				throw new ObjectNotFoundException("err_post_not_found");
			return post;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
