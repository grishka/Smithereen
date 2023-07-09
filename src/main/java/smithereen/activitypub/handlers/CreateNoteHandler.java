package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Create;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.OwnerAndAuthor;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.PostStorage;
import spark.utils.StringUtils;

public class CreateNoteHandler extends ActivityTypeHandler<ForeignUser, Create, NoteOrQuestion>{
	private static final Logger LOG=LoggerFactory.getLogger(CreateNoteHandler.class);

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Create activity, NoteOrQuestion post) throws SQLException{
		if(!post.attributedTo.equals(actor.activityPubID))
			throw new BadRequestException("object.attributedTo and actor.id must match");
		if(PostStorage.getPostByID(post.activityPubID)!=null){
			// Already exists. Ignore and return 200 OK.
			return;
		}

		if(post.attributedTo!=null && !post.attributedTo.equals(actor.activityPubID))
			throw new BadRequestException("attributedTo must match the actor ID");

		Actor owner=null;
		if(post.target!=null){
			if(post.target.attributedTo!=null){
				owner=context.appContext.getObjectLinkResolver().resolve(post.target.attributedTo, Actor.class, true, true, false);
				if(!Objects.equals(owner.getWallURL(), post.target.activityPubID)){
					// Unknown target collection
					return;
				}
			}
			if(owner==null)
				throw new BadRequestException("Unknown wall owner (from target, which must be a link to sm:wall if present - see FEP-400e)");
		}else{
			owner=actor;
		}
		if(post.inReplyTo==null)
			checkNotBlocked(owner, actor, false);

		// Special handling for poll votes because using a separate activity type would've been too easy.
		if((post.attachment==null || post.attachment.isEmpty()) && StringUtils.isEmpty(post.content) && post.inReplyTo!=null && post.name!=null){
//			Post parent=context.appContext.getObjectLinkResolver().resolve(post.inReplyTo, Post.class, false, false, false, (JsonObject) null, true);
			Post parent=context.appContext.getWallController().getPostOrThrow(post.inReplyTo);
			if(parent.poll!=null){
				int optionID=0;
				if(post.context!=null){
					for(PollOption opt:parent.poll.options){
						if(post.context.equals(opt.activityPubID)){
							optionID=opt.id;
							break;
						}
					}
				}else{
					for(PollOption opt:parent.poll.options){
						if(post.name.equals(opt.text)){
							optionID=opt.id;
							break;
						}
					}
				}
				if(optionID!=0){
					if(!parent.poll.isExpired()){
						PostStorage.voteInPoll(actor.id, parent.poll.id, optionID, post.activityPubID, parent.poll.multipleChoice);
						context.appContext.getWallController().sendUpdateQuestionIfNeeded(parent);
					}
					context.appContext.getNewsfeedController().clearFriendsFeedCache();
					return;
				}
			}
		}

		boolean isPublic=false;
		if(post.to==null || post.to.isEmpty()){
			if(post.cc==null || post.cc.isEmpty()){
				throw new BadRequestException("to or cc are both empty");
			}else{
				for(LinkOrObject cc:post.cc){
					if(cc.link==null)
						throw new BadRequestException("post.cc must only contain links");
					if(ActivityPub.isPublic(cc.link)){
						isPublic=true;
						break;
					}
				}
			}
		}else{
			for(LinkOrObject to:post.to){
				if(to.link==null)
					throw new BadRequestException("post.to must only contain links");
				if(ActivityPub.isPublic(to.link)){
					isPublic=true;
					break;
				}
			}
			if(!isPublic && post.cc!=null){
				for(LinkOrObject cc:post.cc){
					if(cc.link==null)
						throw new BadRequestException("post.cc must only contain links");
					if(ActivityPub.isPublic(cc.link)){
						isPublic=true;
						break;
					}
				}
			}
		}
		if(!isPublic)
			throw new BadRequestException("Only public posts are supported");

		if(actor.activityPubID.equals(owner.activityPubID) && post.inReplyTo==null){
			URI followers=actor.getFollowersURL();
			boolean addressesAnyFollowers=false;
			for(LinkOrObject l:post.to){
				if(followers.equals(l.link)){
					addressesAnyFollowers=true;
					break;
				}
			}
			if(!addressesAnyFollowers){
				for(LinkOrObject l:post.cc){
					if(followers.equals(l.link)){
						addressesAnyFollowers=true;
						break;
					}
				}
			}
			if(!addressesAnyFollowers){
				LOG.warn("Dropping this post {} because it's public but doesn't address any followers", post.activityPubID);
				return;
			}
		}
		if(post.inReplyTo!=null){
			if(post.inReplyTo.equals(post.activityPubID))
				throw new BadRequestException("Post can't be a reply to itself. This makes no sense.");
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				Post nativePost=post.asNativePost(context.appContext);
				context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
//				post.setParent(parent);
				Post topLevel=context.appContext.getWallController().getPostOrThrow(parent.getReplyLevel()>0 ? parent.replyKey.get(0) : parent.id);
				OwnerAndAuthor oaa=context.appContext.getWallController().getPostAuthorAndOwner(topLevel);
				owner=oaa.owner();
//				nativePost.replyKey=parent.getReplyKeyForReplies();
//				nativePost.ownerID=topLevel.ownerID;

				checkNotBlocked(owner, actor, true);

				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
				NotificationUtils.putNotificationsForPost(nativePost, parent);
				if(topLevel.isLocal()){
					if(!Objects.equals(owner.activityPubID, oaa.author().activityPubID)){
						context.appContext.getActivityPubWorker().sendAddPostToWallActivity(nativePost);
					}else{
						if(context.ldSignatureOwner!=null)
							context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(topLevel), oaa.author());
					}
				}
			}else{
				LOG.info("Don't have parent post {} for {}", post.inReplyTo, post.activityPubID);
				boolean mentionsLocalUsers=false;
				if(post.tag!=null){
					for(ActivityPubObject tag:post.tag){
						if(tag instanceof Mention mention){
							if(Config.isLocal(mention.href)){
								mentionsLocalUsers=true;
								break;
							}
						}
					}
				}
				if(!mentionsLocalUsers){
					LOG.warn("Dropping post {} because its parent isn't known and it doesn't mention local users.", post.activityPubID);
					return;
				}
				context.appContext.getActivityPubWorker().fetchReplyThread(post);
			}
		}else{
			Post nativePost=post.asNativePost(context.appContext);
			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);

			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
			NotificationUtils.putNotificationsForPost(nativePost, null);
			if(nativePost.ownerID!=nativePost.authorID){
				context.appContext.getActivityPubWorker().sendAddPostToWallActivity(nativePost);
			}else{
				context.appContext.getNewsfeedController().clearFriendsFeedCache();
			}
		}
	}

	private void checkNotBlocked(Actor owner, ForeignUser actor, boolean isReply) throws SQLException{
		if(owner instanceof User user && !Objects.equals(owner.activityPubID, actor.activityPubID)){
			Utils.ensureUserNotBlocked(actor, user);
			if(owner instanceof ForeignActor && !isReply)
				throw new BadRequestException("Create{Note} can't be used to notify about posts on foreign actors' walls. Wall owner must send an Add{Note} instead.");
		}
		if(owner instanceof Group group)
			Utils.ensureUserNotBlocked(actor, group);
	}
}
