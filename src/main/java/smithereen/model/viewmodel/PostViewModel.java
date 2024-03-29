package smithereen.model.viewmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import smithereen.model.PaginatedList;
import smithereen.model.Post;

public class PostViewModel{
	public Post post;
	public List<PostViewModel> repliesObjects=new ArrayList<>();
	public int totalTopLevelComments;
	private boolean loadedRepliesCountKnown;
	private int loadedRepliesCount;
	public boolean canComment=true;

	public PostViewModel(Post post){
		this.post=post;
	}

	public int getMissingRepliesCount(){
		return post.replyCount-getLoadedRepliesCount();
	}

	public int getLoadedRepliesCount(){
		if(loadedRepliesCountKnown)
			return loadedRepliesCount;
		loadedRepliesCount=0;
		for(PostViewModel reply:repliesObjects){
			loadedRepliesCount+=reply.getLoadedRepliesCount()+1;
		}
		loadedRepliesCountKnown=true;
		return loadedRepliesCount;
	}

	public int getLoadableRepliesCount(){
		int count=post.replyCount;
		for(PostViewModel reply:repliesObjects){
			count-=reply.post.replyCount+1;
		}
		return count;
	}

	public void getAllReplies(List<PostViewModel> replies){
		replies.addAll(repliesObjects);
		for(PostViewModel reply:repliesObjects){
			reply.getAllReplies(replies);
		}
	}

	public void getAllReplyIDs(Collection<Integer> out){
		for(PostViewModel reply:repliesObjects){
			out.add(reply.post.id);
			reply.getAllReplyIDs(out);
		}
	}

	public static PaginatedList<PostViewModel> wrap(PaginatedList<Post> list){
		return new PaginatedList<>(list.list.stream().map(PostViewModel::new).toList(), list.total, list.offset, list.perPage);
	}

	public static void collectActorIDs(Collection<PostViewModel> posts, Set<Integer> userIDs, Set<Integer> groupIDs){
		for(PostViewModel pvm:posts){
			userIDs.add(pvm.post.authorID);
			if(pvm.post.ownerID>0)
				userIDs.add(pvm.post.ownerID);
			else
				groupIDs.add(-pvm.post.ownerID);
			collectActorIDs(pvm.repliesObjects, userIDs, groupIDs);
		}
	}
}
