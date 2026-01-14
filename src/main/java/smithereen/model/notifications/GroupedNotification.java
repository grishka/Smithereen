package smithereen.model.notifications;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GroupedNotification implements NotificationWrapper{
	@NotNull
	public List<Notification> notifications=new ArrayList<>();

	@Override
	public String toString(){
		return "GroupedNotification{"+
				"notifications="+notifications+
				'}';
	}

	@Override
	@NotNull
	public Notification getLatestNotification(){
		return notifications.getFirst();
	}
}
