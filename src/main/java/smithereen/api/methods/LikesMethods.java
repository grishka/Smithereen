package smithereen.api.methods;

import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.LikeableContentObject;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.comments.Comment;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.util.XTEA;

public class LikesMethods{
	public static Object add(ApplicationContext ctx, ApiCallContext actx){
		return setLiked(ctx, actx, true);
	}

	public static Object delete(ApplicationContext ctx, ApiCallContext actx){
		return setLiked(ctx, actx, false);
	}

	private static Object setLiked(ApplicationContext ctx, ApiCallContext actx, boolean liked){
		LikeableContentObject obj=getObject(ctx, actx);
		ctx.getUserInteractionsController().setObjectLiked(obj, liked, actx.self.user);
		return Map.of("likes", ctx.getUserInteractionsController().getLikeCount(obj));
	}

	public static Object isLiked(ApplicationContext ctx, ApiCallContext actx){
		LikeableContentObject obj=getObject(ctx, actx);
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self==null ? null : actx.self.user, obj);
		User user=ApiUtils.getUserOrSelf(ctx, actx, "user_id");

		record IsLikedResponse(boolean liked, Boolean reposted){}
		if(obj instanceof Post post){
			UserInteractions interactions=ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), user).get(post.id);
			return new IsLikedResponse(interactions.isLiked, interactions.isReposted);
		}else{
			return new IsLikedResponse(ctx.getUserInteractionsController().isLiked(obj, user), null);
		}
	}

	public static Object getList(ApplicationContext ctx, ApiCallContext actx){
		LikeableContentObject obj=getObject(ctx, actx);
		ctx.getPrivacyController().enforceObjectPrivacy(actx.self==null || !actx.hasPermission(ClientAppPermission.LIKES_READ) ? null : actx.self.user, obj);
		boolean friendsOnly=actx.booleanParam("friends_only") && actx.self!=null;
		boolean skipOwn=actx.booleanParam("skip_own");
		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		PaginatedList<Integer> userIDs=ctx.getUserInteractionsController().getLikesForObject(obj, actx.self==null ? null : actx.self.user, offset, count, skipOwn, friendsOnly);
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(userIDs.total, ApiUtils.getUsers(userIDs.list, ctx, actx));
		}else{
			return new ApiPaginatedList<>(userIDs);
		}
	}

	private static LikeableContentObject getObject(ApplicationContext ctx, ApiCallContext actx){
		String type=actx.requireParamString("type");
		return switch(type){
			case "post" -> ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("item_id"));
			case "photo" -> ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(actx.requireParamString("item_id"), ObfuscatedObjectIDType.PHOTO));
			case "photo_comment", "topic_comment" -> {
				Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(actx.requireParamString("item_id"), ObfuscatedObjectIDType.COMMENT));
				if(!type.equals(switch(comment.parentObjectID.type()){
					case PHOTO -> "photo_comment";
					case BOARD_TOPIC -> "topic_comment";
				})){
					throw new ObjectNotFoundException();
				}
				yield comment;
			}
			default -> throw actx.paramError("type must be one of post, photo, photo_comment, topic_comment");
		};
	}
}
