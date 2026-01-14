package smithereen.model.notifications;

import org.jetbrains.annotations.NotNull;

public sealed interface NotificationWrapper permits Notification, GroupedNotification{
	@NotNull
	Notification getLatestNotification();
}
