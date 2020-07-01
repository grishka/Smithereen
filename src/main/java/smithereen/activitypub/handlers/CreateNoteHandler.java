package smithereen.activitypub.handlers;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;

import smithereen.BadRequestException;
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

public class CreateNoteHandler extends ActivityTypeHandler<ForeignUser, Create, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Create activity, Post post) throws SQLException{
		if(!post.attributedTo.equals(actor.activityPubID))
			throw new BadRequestException("object.attributedTo and actor.id must match");
		if(PostStorage.getPostByID(post.activityPubID)!=null){
			// Already exists. Ignore and return 200 OK.
			return;
		}
		if(post.user==null || post.user.id!=actor.id)
			throw new BadRequestException("Can only create posts for self");
		if(post.owner==null)
			throw new BadRequestException("Unknown wall owner (from partOf, which must be an outbox URI if present)");
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
				System.out.println("Dropping this post because it's public but doesn't address any followers");
				return;
			}
		}
		if(post.tag!=null){
			for(ActivityPubObject tag:post.tag){
				if(tag instanceof Mention){
					URI uri=((Mention) tag).href;
					User mentionedUser=UserStorage.getUserByActivityPubID(uri);
					if(mentionedUser==null){
						try{
							ActivityPubObject _user=ActivityPub.fetchRemoteObject(uri.toString());
							if(_user instanceof ForeignUser){
								ForeignUser u=(ForeignUser) _user;
								UserStorage.putOrUpdateForeignUser(u);
								mentionedUser=u;
							}
						}catch(IOException ignore){}
					}
					if(mentionedUser!=null){
						if(post.mentionedUsers.isEmpty())
							post.mentionedUsers=new ArrayList<>();
						if(!post.mentionedUsers.contains(mentionedUser))
							post.mentionedUsers.add(mentionedUser);
					}
				}
			}
		}
		if(post.inReplyTo!=null){
			if(post.inReplyTo.equals(post.activityPubID))
				throw new BadRequestException("Post can't be a reply to itself. This makes no sense.");
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				post.setParent(parent);
				PostStorage.putForeignWallPost(post);
				NotificationUtils.putNotificationsForPost(post, parent);
				Post topLevel=PostStorage.getPostByID(post.replyKey[0]);
				if(topLevel!=null && topLevel.local){
					if(context.ldSignatureOwner!=null)
						context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(topLevel), topLevel.user);
				}
			}else{
				System.out.println("Don't have parent post "+post.inReplyTo+" for "+post.activityPubID);
				ActivityPubWorker.getInstance().fetchReplyThread(post);
			}
		}else{
			PostStorage.putForeignWallPost(post);
			NotificationUtils.putNotificationsForPost(post, null);
		}
	}
}
