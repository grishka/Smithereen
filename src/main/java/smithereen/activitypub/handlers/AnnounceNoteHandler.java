package smithereen.activitypub.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

import smithereen.BadRequestException;
import smithereen.Config;
import smithereen.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class AnnounceNoteHandler extends ActivityTypeHandler<ForeignUser, Announce, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Announce activity, Post post) throws SQLException{
		if(post.user==null){
			ActivityPubObject _author;
			try{
				_author=ActivityPub.fetchRemoteObject(post.attributedTo.toString());
			}catch(IOException x){
				throw new BadRequestException(x.toString());
			}
			if(!(_author instanceof ForeignUser)){
				throw new IllegalArgumentException("Post author isn't a user");
			}
			ForeignUser author=(ForeignUser) _author;
			UserStorage.putOrUpdateForeignUser(author);
			post.owner=post.user=author;
		}
		PostStorage.putForeignWallPost(post);
		long time=activity.published==null ? System.currentTimeMillis() : activity.published.getTime();
		NewsfeedStorage.putRetoot(actor.id, post.id, new Timestamp(time));

		if(!(post.user instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.RETOOT;
			n.actorID=actor.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.user.id, n);
		}
	}
}
