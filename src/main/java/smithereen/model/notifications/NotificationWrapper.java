package smithereen.model.notifications;

public sealed interface NotificationWrapper permits Notification, GroupedNotification{
	Notification getLatestNotification();
}
