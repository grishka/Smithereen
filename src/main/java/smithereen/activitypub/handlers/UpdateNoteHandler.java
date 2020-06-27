package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.storage.PostStorage;
import spark.utils.StringUtils;

public class UpdateNoteHandler extends ActivityTypeHandler<ForeignUser, Update, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, Post post) throws SQLException{
		if(post.user.id==actor.id){
			PostStorage.putForeignWallPost(post);
		}else{
			throw new IllegalArgumentException("No access to update this post");
		}
	}
}
