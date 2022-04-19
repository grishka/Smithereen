package smithereen;

import smithereen.activitypub.ActivityPubWorker;
import smithereen.controllers.FriendsController;
import smithereen.controllers.GroupsController;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.controllers.PrivacyController;
import smithereen.controllers.UserInteractionsController;
import smithereen.controllers.UsersController;
import smithereen.controllers.WallController;

public class ApplicationContext{
	private final WallController wallController;
	private final GroupsController groupsController;
	private final UsersController usersController;
	private final UserInteractionsController userInteractionsController;
	private final FriendsController friendsController;
	private final ObjectLinkResolver objectLinkResolver;
	private final ActivityPubWorker activityPubWorker;
	private final PrivacyController privacyController;

	public ApplicationContext(){
		wallController=new WallController(this);
		groupsController=new GroupsController(this);
		usersController=new UsersController(this);
		userInteractionsController=new UserInteractionsController(this);
		friendsController=new FriendsController(this);
		objectLinkResolver=new ObjectLinkResolver(this);
		activityPubWorker=new ActivityPubWorker(this);
		privacyController=new PrivacyController(this);
	}

	public WallController getWallController(){
		return wallController;
	}

	public GroupsController getGroupsController(){
		return groupsController;
	}

	public UsersController getUsersController(){
		return usersController;
	}

	public UserInteractionsController getUserInteractionsController(){
		return userInteractionsController;
	}

	public FriendsController getFriendsController(){
		return friendsController;
	}

	public ObjectLinkResolver getObjectLinkResolver(){
		return objectLinkResolver;
	}

	public ActivityPubWorker getActivityPubWorker(){
		return activityPubWorker;
	}

	public PrivacyController getPrivacyController(){
		return privacyController;
	}
}
