package smithereen.data;

import java.util.ArrayList;

import smithereen.data.notifications.Notification;

public class UserNotifications{
	private int newFriendRequests=0;
	private int newNotifications=0;

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
}
