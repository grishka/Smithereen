package smithereen.data;

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
	PRIVATE_MESSAGES;

	public String getDescriptionLangKey(){
		return switch(this){
			case WALL_POSTING -> "privacy_wall_posting";
			case WALL_OTHERS_POSTS -> "privacy_wall_others_posts";
			case WALL_COMMENTING -> "privacy_wall_commenting";
			case GROUP_INVITE -> "privacy_group_invites";
			case PRIVATE_MESSAGES -> "privacy_mail";
		};
	}

	// What is shown in the UI when "NONE" is selected: "only me" or "no one"
	public boolean isOnlyMe(){
		return switch(this){
			case WALL_OTHERS_POSTS, WALL_COMMENTING -> true;
			default -> false;
		};
	}
}
