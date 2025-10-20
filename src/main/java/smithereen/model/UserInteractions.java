package smithereen.model;

import java.util.List;

/**
 * This holds information about user interactions (e.g. likes) with an object (e.g. post)
 */
public class UserInteractions{
	public int likeCount;
	public boolean isLiked;
	public boolean canLike;
	public int commentCount;
	public boolean canComment;
	public List<Integer> pollChoices;
	public int repostCount;
	public boolean canRepost;
	public boolean isReposted;

	@Override
	public String toString(){
		return "UserInteractions{"+
				"likeCount="+likeCount+
				", isLiked="+isLiked+
				", canLike="+canLike+
				", commentCount="+commentCount+
				", canComment="+canComment+
				", pollChoices="+pollChoices+
				", repostCount="+repostCount+
				", canRepost="+canRepost+
				", isReposted="+isReposted+
				'}';
	}
}
