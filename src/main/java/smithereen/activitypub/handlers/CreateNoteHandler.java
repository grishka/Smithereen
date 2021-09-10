package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import smithereen.Config;
import smithereen.ObjectLinkResolver;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.data.Group;
import smithereen.data.PollOption;
import smithereen.exceptions.BadRequestException;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.activities.Create;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class CreateNoteHandler extends ActivityTypeHandler<ForeignUser, Create, Post>{
	private static final Logger LOG=LoggerFactory.getLogger(CreateNoteHandler.class);

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Create activity, Post post) throws SQLException{
		if(!post.attributedTo.equals(actor.activityPubID))
			throw new BadRequestException("object.attributedTo and actor.id must match");
		if(PostStorage.getPostByID(post.activityPubID)!=null){
			// Already exists. Ignore and return 200 OK.
			return;
		}
		post.resolveDependencies(true, false);
		if(post.user==null || post.user.id!=actor.id)
			throw new BadRequestException("Can only create posts for self");
		if(post.owner==null)
			throw new BadRequestException("Unknown wall owner (from target, which must be a link to sm:wall if present - see FEP-400e)");
		if(post.owner instanceof User && !Objects.equals(post.owner.activityPubID, post.user.activityPubID)){
			Utils.ensureUserNotBlocked(actor, (User) post.owner);
			if(post.owner instanceof ForeignActor)
				throw new BadRequestException("Create{Note} can't be used to notify about posts on foreign actors' walls. Wall owner must send an Add{Note} instead.");
		}
		if(post.owner instanceof Group)
			Utils.ensureUserNotBlocked(actor, (Group) post.owner);

		// Special handling for poll votes because using a separate activity type would've been too easy.
		if((post.attachment==null || post.attachment.isEmpty()) && StringUtils.isEmpty(post.content) && post.inReplyTo!=null && post.name!=null){
			Post parent=ObjectLinkResolver.resolve(post.inReplyTo, Post.class, false, false, false);
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
						if(post.name.equals(opt.name)){
							optionID=opt.id;
							break;
						}
					}
				}
				if(optionID!=0){
					if(!parent.poll.isExpired())
						PostStorage.voteInPoll(actor.id, parent.poll.id, optionID, post.activityPubID, parent.poll.multipleChoice);
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
		if(post.user==post.owner && post.inReplyTo==null){
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
		Utils.loadAndPreprocessRemotePostMentions(post);
		if(post.inReplyTo!=null){
			if(post.inReplyTo.equals(post.activityPubID))
				throw new BadRequestException("Post can't be a reply to itself. This makes no sense.");
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				post.setParent(parent);
				ObjectLinkResolver.storeOrUpdateRemoteObject(post);
				NotificationUtils.putNotificationsForPost(post, parent);
				Post topLevel=PostStorage.getPostByID(post.replyKey[0], false);
				if(topLevel!=null && topLevel.local){
					if(context.ldSignatureOwner!=null)
						context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(topLevel), topLevel.user);
				}
			}else{
				LOG.info("Don't have parent post {} for {}", post.inReplyTo, post.activityPubID);
				boolean mentionsLocalUsers=false;
				for(ActivityPubObject tag:post.tag){
					if(tag instanceof Mention){
						if(Config.isLocal(((Mention) tag).href)){
							mentionsLocalUsers=true;
							break;
						}
					}
				}
				if(!mentionsLocalUsers){
					LOG.warn("Dropping post {} because its parent isn't known and it doesn't mention local users.", post.activityPubID);
					return;
				}
				ActivityPubWorker.getInstance().fetchReplyThread(post);
			}
		}else{
			ObjectLinkResolver.storeOrUpdateRemoteObject(post);
			NotificationUtils.putNotificationsForPost(post, null);
			if(!Objects.equals(post.owner.activityPubID, post.user.activityPubID)){
				ActivityPubWorker.getInstance().sendAddPostToWallActivity(post);
			}
		}
	}
}
