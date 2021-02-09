package smithereen.activitypub.handlers;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;

import smithereen.exceptions.ObjectNotFoundException;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class UpdateNoteHandler extends ActivityTypeHandler<ForeignUser, Update, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, Post post) throws SQLException{
		if(post.user.id==actor.id){
			Post existing=PostStorage.getPostByID(post.activityPubID);
			if(existing==null){
				throw new ObjectNotFoundException("Existing post not found");
			}
			if(post.user.id!=existing.user.id){
				throw new IllegalArgumentException("Post author doesn't match existing");
			}
			if(!post.owner.activityPubID.equals(existing.owner.activityPubID)){
				throw new IllegalArgumentException("Post owner doesn't match existing");
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
				if(!post.mentionedUsers.isEmpty() && StringUtils.isNotEmpty(post.content)){
					post.content=Utils.preprocessRemotePostMentions(post.content, post.mentionedUsers);
				}
			}
			PostStorage.putForeignWallPost(post);
		}else{
			throw new IllegalArgumentException("No access to update this post");
		}
	}
}
