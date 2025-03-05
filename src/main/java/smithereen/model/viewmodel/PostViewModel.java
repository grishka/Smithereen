package smithereen.model.viewmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.filtering.WordFilter;

public class PostViewModel extends BasePostViewModel<Post, PostViewModel>{
	public Repost repost;
	public WordFilter matchedFilter;

	public PostViewModel(Post post){
		this.post=post;
	}

	public void getAllReplyIDs(Collection<Integer> out){
		for(PostViewModel reply:repliesObjects){
			out.add(reply.post.id);
			reply.getAllReplyIDs(out);
		}
	}

	public List<Integer> getReplyKeyForInteractions(){
		if(post.isMastodonStyleRepost() && repost!=null)
			return repost.post.post.getReplyKeyForReplies();
		return post.getReplyKeyForReplies();
	}

	public static PaginatedList<PostViewModel> wrap(PaginatedList<Post> list){
		return new PaginatedList<>(list.list.stream().map(PostViewModel::new).collect(Collectors.toCollection(ArrayList::new)), list.total, list.offset, list.perPage);
	}

	public static void collectActorIDs(Collection<PostViewModel> posts, Set<Integer> userIDs, Set<Integer> groupIDs){
		for(PostViewModel pvm:posts){
			userIDs.add(pvm.post.authorID);
			if(pvm.post.ownerID>0){
				userIDs.add(pvm.post.ownerID);
			}else{
				if(groupIDs!=null)
					groupIDs.add(-pvm.post.ownerID);
			}
			if(pvm.repost!=null){
				if(pvm.repost.post!=null)
					collectActorIDs(Set.of(pvm.repost.post), userIDs, groupIDs);
				if(pvm.repost.topLevel!=null)
					collectActorIDs(Set.of(pvm.repost.topLevel), userIDs, groupIDs);
			}
			if(pvm.parentAuthorID>0)
				userIDs.add(pvm.parentAuthorID);
			collectActorIDs(pvm.repliesObjects, userIDs, groupIDs);
		}
	}

	public record Repost(PostViewModel post, PostViewModel topLevel){}
}
