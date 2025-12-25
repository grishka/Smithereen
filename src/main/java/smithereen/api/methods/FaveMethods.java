package smithereen.api.methods;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.activities.Like;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiWallPost;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.PostViewModel;

public class FaveMethods{
	public static Object addUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		ctx.getBookmarksController().addUserBookmark(actx.self.user, user);
		return true;
	}

	public static Object removeUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ApiUtils.getUser(ctx, actx, "user_id");
		ctx.getBookmarksController().removeUserBookmark(actx.self.user, user);
		return true;
	}

	public static Object addGroup(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getBookmarksController().addGroupBookmark(actx.self.user, group);
		return true;
	}

	public static Object removeGroup(ApplicationContext ctx, ApiCallContext actx){
		Group group=ApiUtils.getGroup(ctx, actx, "group_id");
		ctx.getBookmarksController().removeGroupBookmark(actx.self.user, group);
		return true;
	}

	public static Object getUsers(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<Integer> ids=ctx.getBookmarksController().getBookmarkedUsers(actx.self.user, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(ids.total, ApiUtils.getUsers(ids.list, ctx, actx));
	}

	public static Object getGroups(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<Integer> ids=ctx.getBookmarksController().getBookmarkedGroups(actx.self.user, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(ids.total, ApiUtils.getGroups(ids.list, ctx, actx));
	}

	public static Object getPhotos(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<Long> ids=ctx.getUserInteractionsController().getLikedObjects(actx.self.user, Like.ObjectType.PHOTO, actx.getOffset(), actx.getCount(50, 1000));
		Map<Long, Photo> photoObjects=ctx.getPhotosController().getPhotosIgnoringPrivacy(ids.list);
		List<Photo> photos=ids.list.stream().map(photoObjects::get).toList();
		return new ApiPaginatedList<>(ids.total, ApiUtils.getPhotos(ctx, actx, photos));
	}

	public static Object getPosts(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<PostViewModel> posts=PostViewModel.wrap(ctx.getUserInteractionsController().getLikedPosts(actx.self.user, true, actx.getOffset(), actx.getCount(50, 100)));
		ApiPaginatedListWithActors<ApiWallPost> res=new ApiPaginatedListWithActors<>(posts.total, ApiUtils.getPosts(posts.list, ctx, actx, true, true, true));
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(posts.list, needUsers, needGroups);
			res.profiles=ApiUtils.getUsers(needUsers, ctx, actx);
			res.groups=ApiUtils.getGroups(needGroups, ctx, actx);
		}
		return res;
	}
}
