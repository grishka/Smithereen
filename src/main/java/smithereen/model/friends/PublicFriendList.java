package smithereen.model.friends;

import smithereen.util.TranslatableEnum;

public enum PublicFriendList implements TranslatableEnum<PublicFriendList>{
	BEST_FRIENDS,
	RELATIVES,
	COLLEAGUES,
	UNIVERSITY_FRIENDS,
	SCHOOL_FRIENDS;

	@Override
	public String getLangKey(){
		return "friend_list_"+toString().toLowerCase();
	}
}
