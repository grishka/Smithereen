package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Move;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;

public class PersonMovePersonHandler extends ActivityTypeHandler<ForeignUser, Move, ForeignUser>{
	private static final HashSet<Integer> movingUsers=new HashSet<>();

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Move activity, ForeignUser object) throws SQLException{
		boolean success=false;
		try{
			if(actor.id!=object.id)
				throw new BadRequestException("Actor and object IDs don't match");
			if(activity.target==null || activity.target.link==null)
				throw new BadRequestException("Move{Person} must have a `target` pointing to the new account");
			URI target=activity.target.link;

			synchronized(movingUsers){
				if(movingUsers.contains(actor.id)){
					LOG.debug("Not moving {} to {} because its previous Move activity is already being processed", actor.activityPubID, target);
					return;
				}
				movingUsers.add(actor.id);
			}

			ForeignUser newUser;
			try{
				newUser=context.appContext.getObjectLinkResolver().resolve(target, ForeignUser.class, true, true, true);
			}catch(ObjectNotFoundException x){
				throw new BadRequestException("Failed to fetch the target account from "+target, x);
			}
			if(!newUser.alsoKnownAs.contains(actor.activityPubID))
				throw new BadRequestException("New actor does not contain old actor's ID in `alsoKnownAs`");

			if(actor.movedAt!=null){
				LOG.debug("Not moving {} to {} because this user has already moved accounts", actor.activityPubID, target);
				return;
			}

			actor.movedTo=newUser.id;
			actor.movedAt=Instant.now();
			newUser.movedFrom=actor.id;
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(actor, actor);
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(newUser, newUser);

			success=true;
			BackgroundTaskRunner.getInstance().submit(()->performMove(context.appContext, actor, newUser));
		}finally{
			if(!success){
				synchronized(movingUsers){
					movingUsers.remove(actor.id);
				}
			}
		}
	}

	private void performMove(ApplicationContext ctx, ForeignUser oldUser, ForeignUser newUser){
		try{
			List<Integer> localFollowers=UserStorage.getUserLocalFollowers(oldUser.id);
			LOG.debug("Started moving {} followers for {} -> {}", localFollowers.size(), oldUser.activityPubID, newUser.activityPubID);
			for(int id:localFollowers){
				try{
					User user=ctx.getUsersController().getUserOrThrow(id);
					ctx.getFriendsController().removeFriend(user, oldUser);
					ctx.getFriendsController().followUser(user, newUser);
				}catch(UserErrorException|ObjectNotFoundException ignore){}
			}
			LOG.debug("Done moving followers for {} -> {}", oldUser.activityPubID, newUser.activityPubID);

			List<User> blockingUsers=UserStorage.getBlockingUsers(oldUser.id);
			LOG.debug("Started moving {} blocks for {} -> {}", blockingUsers.size(), oldUser.activityPubID, newUser.activityPubID);
			for(User user:blockingUsers){
				if(user instanceof ForeignUser)
					continue;
				ctx.getFriendsController().blockUser(user, newUser);
			}
			LOG.debug("Done moving blocks for {} -> {}", oldUser.activityPubID, newUser.activityPubID);
		}catch(Exception x){
			LOG.error("Failed to move {} to {}", oldUser.activityPubID, newUser.activityPubID, x);
		}finally{
			synchronized(movingUsers){
				movingUsers.remove(oldUser.id);
			}
		}
	}
}
