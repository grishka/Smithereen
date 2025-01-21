package smithereen.model.viewmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.model.PaginatedList;
import smithereen.model.comments.Comment;

public class CommentViewModel extends BasePostViewModel<Comment, CommentViewModel>{
	public CommentViewModel(Comment c){
		post=c;
	}

	public static PaginatedList<CommentViewModel> wrap(PaginatedList<Comment> list){
		return new PaginatedList<>(list.list.stream().map(CommentViewModel::new).collect(Collectors.toCollection(ArrayList::new)), list.total, list.offset, list.perPage);
	}

	public static void collectUserIDs(Collection<CommentViewModel> comments, Set<Integer> userIDs){
		for(CommentViewModel cvm:comments){
			userIDs.add(cvm.post.authorID);
			if(cvm.parentAuthorID>0)
				userIDs.add(cvm.parentAuthorID);
			collectUserIDs(cvm.repliesObjects, userIDs);
		}
	}
}
