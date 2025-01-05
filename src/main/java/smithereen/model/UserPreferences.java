package smithereen.model;

import com.google.gson.annotations.SerializedName;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Locale;

import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
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
	@SerializedName("cfeed")
	public EnumSet<CommentsNewsfeedObjectType> commentsFeedFilter;
}
