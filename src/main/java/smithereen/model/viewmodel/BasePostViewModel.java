package smithereen.model.viewmodel;

import java.util.ArrayList;
import java.util.List;

import smithereen.model.PostLikeObject;

public abstract class BasePostViewModel<T extends PostLikeObject, S extends BasePostViewModel<T, S>>{
	public T post;
	public List<S> repliesObjects=new ArrayList<>();
	public int totalTopLevelComments;
	protected boolean loadedRepliesCountKnown;
	protected int loadedRepliesCount;
	public int parentAuthorID;

	public int getMissingRepliesCount(){
		return post.replyCount-getLoadedRepliesCount();
	}

	public int getLoadedRepliesCount(){
		if(loadedRepliesCountKnown)
			return loadedRepliesCount;
		loadedRepliesCount=0;
		for(S reply:repliesObjects){
			loadedRepliesCount+=reply.getLoadedRepliesCount()+1;
		}
		loadedRepliesCountKnown=true;
		return loadedRepliesCount;
	}

	public int getLoadableRepliesCount(){
		int count=post.replyCount;
		for(S reply:repliesObjects){
			count-=reply.post.replyCount+1;
		}
		return count;
	}

	public void getAllReplies(List<S> replies){
		replies.addAll(repliesObjects);
		for(S reply:repliesObjects){
			reply.getAllReplies(replies);
		}
	}
}
