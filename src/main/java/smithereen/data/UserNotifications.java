package smithereen.data;

import java.util.ArrayList;

public class UserNotifications{
	private int newFriendRequests=0;
	private ArrayList<Notification> notifications=new ArrayList<>();

	public synchronized int getNewFriendRequestCount(){
		return newFriendRequests;
	}

	public synchronized void incNewFriendRequestCount(int amount){
		newFriendRequests+=amount;
	}
}
