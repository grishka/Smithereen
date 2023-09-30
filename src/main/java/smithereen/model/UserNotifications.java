package smithereen.model;

public class UserNotifications{
	private int newFriendRequests=0;
	private int newNotifications=0;
	private int newGroupInvitations;
	private int newEventInvitations;
	private int unreadMailCount;

	public synchronized int getNewFriendRequestCount(){
		return newFriendRequests;
	}

	public synchronized void incNewFriendRequestCount(int amount){
		newFriendRequests+=amount;
	}

	public synchronized int getNewNotificationsCount(){
		return newNotifications;
	}

	public synchronized void incNewNotificationsCount(int amount){
		newNotifications+=amount;
	}

	public synchronized void setNotificationsViewed(){
		newNotifications=0;
	}

	public synchronized void incNewGroupInvitationsCount(int amount){
		newGroupInvitations+=amount;
	}

	public synchronized int getNewGroupInvitationsCount(){
		return newGroupInvitations;
	}

	public synchronized void incNewEventInvitationsCount(int amount){
		newEventInvitations+=amount;
	}

	public synchronized int getNewEventInvitationsCount(){
		return newEventInvitations;
	}

	public synchronized void incUnreadMailCount(int amount){
		unreadMailCount+=amount;
	}

	public synchronized int getUnreadMailCount(){
		return unreadMailCount;
	}
}
