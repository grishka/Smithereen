package smithereen.model;

import java.util.List;

/**
 * This holds information about user interactions (e.g. likes) with an object (e.g. post)
 */
public class UserInteractions{
	public int likeCount;
	public boolean isLiked;
	public int commentCount;
	public boolean canComment;
	public List<Integer> pollChoices;
	public int repostCount;
	public boolean canRepost;

	@Override
	public String toString(){
		return "UserInteractions{"+
				"likeCount="+likeCount+
				", isLiked="+isLiked+
				", commentCount="+commentCount+
				", canComment="+canComment+
				", pollChoices="+pollChoices+
				", repostCount="+repostCount+
				", canRepost="+canRepost+
				'}';
	}
}
