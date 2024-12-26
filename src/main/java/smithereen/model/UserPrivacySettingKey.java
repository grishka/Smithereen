package smithereen.model;

import com.google.gson.annotations.SerializedName;

public enum UserPrivacySettingKey{
	@SerializedName("wp")
	WALL_POSTING,
	@SerializedName("wop")
	WALL_OTHERS_POSTS,
	@SerializedName("wc")
	WALL_COMMENTING,
	@SerializedName("gi")
	GROUP_INVITE,
	@SerializedName("ms")
	PRIVATE_MESSAGES,
	@SerializedName("pt")
	PHOTO_TAG,
	@SerializedName("ptl")
	PHOTO_TAG_LIST;

	public String getDescriptionLangKey(){
		return switch(this){
			case WALL_POSTING -> "privacy_wall_posting";
			case WALL_OTHERS_POSTS -> "privacy_wall_others_posts";
			case WALL_COMMENTING -> "privacy_wall_commenting";
			case GROUP_INVITE -> "privacy_group_invites";
			case PRIVATE_MESSAGES -> "privacy_mail";
			case PHOTO_TAG -> "privacy_photo_tagging";
			case PHOTO_TAG_LIST -> "privacy_photo_tag_list";
		};
	}

	// What is shown in the UI when "NONE" is selected: "only me" or "no one"
	public boolean isOnlyMe(){
		return switch(this){
			case WALL_OTHERS_POSTS, WALL_COMMENTING, PHOTO_TAG, PHOTO_TAG_LIST -> true;
			default -> false;
		};
	}

	// Is this setting for viewing some content, or is it for an active action?
	public boolean isForViewing(){
		return switch(this){
			case WALL_OTHERS_POSTS, PHOTO_TAG_LIST -> true;
			default -> false;
		};
	}

	public String getActivityPubKey(){
		return switch(this){
			case WALL_POSTING -> "sm:wallPosting";
			case WALL_OTHERS_POSTS -> "sm:wallPostVisibility";
			case WALL_COMMENTING -> "sm:commenting";
			case GROUP_INVITE -> "sm:groupInvitations";
			case PRIVATE_MESSAGES -> "sm:directMessages";
			case PHOTO_TAG -> "sm:photoTagging";
			case PHOTO_TAG_LIST -> "sm:photoTagList";
		};
	}

	public PrivacySetting getDefaultValue(){
		return isFriendsOnly() ? PrivacySetting.DEFAULT_FRIENDS_ONLY : PrivacySetting.DEFAULT;
	}

	// Can this setting be "everyone" or "friends of friends", or does it have to be "friends only" or stricter?
	public boolean isFriendsOnly(){
		return this==PHOTO_TAG;
	}
}
