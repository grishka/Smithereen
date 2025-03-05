package smithereen.model.notifications;

import java.util.ArrayList;
import java.util.List;

public final class GroupedNotification implements NotificationWrapper{
	public List<Notification> notifications=new ArrayList<>();

	@Override
	public String toString(){
		return "GroupedNotification{"+
				"notifications="+notifications+
				'}';
	}

	@Override
	public Notification getLatestNotification(){
		return notifications.getFirst();
	}
}
