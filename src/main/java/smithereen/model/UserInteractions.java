package smithereen.model;

import java.util.List;

/**
 * This holds information about user interactions (e.g. likes) with an object (e.g. post)
 */
public class UserInteractions{
	public int likeCount;
	public boolean isLiked;
	public int commentCount;
	public List<Integer> pollChoices;
	public int repostCount;
}
