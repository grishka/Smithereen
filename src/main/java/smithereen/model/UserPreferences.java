package smithereen.model;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Locale;

import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.feed.GroupsNewsfeedTypeFilter;
import smithereen.model.notifications.EmailNotificationFrequency;
import smithereen.model.notifications.EmailNotificationType;
import smithereen.model.notifications.RealtimeNotificationSettingType;
import smithereen.text.FormattedTextFormat;

public class UserPreferences{
	@SerializedName("lang")
	public Locale locale;
	@SerializedName("tz")
	public ZoneId timeZone;
	@SerializedName("lsntf")
	public int lastSeenNotificationID;
	@SerializedName("tfmt")
	public FormattedTextFormat textFormat=FormattedTextFormat.MARKDOWN;
	@SerializedName("cmv")
	public CommentViewType commentViewType=CommentViewType.THREADED;
	@SerializedName("ffeed")
	public EnumSet<FriendsNewsfeedTypeFilter> friendFeedFilter;
	@SerializedName("gfeed")
	public EnumSet<GroupsNewsfeedTypeFilter> groupFeedFilter;
	@SerializedName("cfeed")
	public EnumSet<CommentsNewsfeedObjectType> commentsFeedFilter;
	@SerializedName("ntft")
	public EnumSet<RealtimeNotificationSettingType> notifierTypes;
	@SerializedName("ntfs")
	public boolean notifierEnableSound=true;
	@SerializedName("ntfmt")
	public boolean notifierShowMessageText=true;
	public boolean countLikesInUnread;
	public EnumSet<EmailNotificationType> emailNotificationTypes=EnumSet.allOf(EmailNotificationType.class);
	public EmailNotificationFrequency emailNotificationFrequency=EmailNotificationFrequency.DAILY;
	public Instant lastEmailNotification;
}
