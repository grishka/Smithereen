package smithereen.controllers;

import java.sql.SQLException;

import smithereen.ApplicationContext;
import smithereen.data.Account;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.SessionStorage;

public class NotificationsController{
	private final ApplicationContext context;

	public NotificationsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<Notification> getNotifications(User user, int offset, int count){
		try{
			return NotificationsStorage.getNotifications(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setNotificationsSeen(Account self, int lastSeenID){
		try{
			if(lastSeenID>self.prefs.lastSeenNotificationID){
				self.prefs.lastSeenNotificationID=lastSeenID;
				SessionStorage.updatePreferences(self.id, self.prefs);
			}
			NotificationsStorage.getNotificationsForUser(self.user.id, self.prefs.lastSeenNotificationID).setNotificationsViewed();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
