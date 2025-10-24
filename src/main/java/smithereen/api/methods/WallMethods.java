package smithereen.api.methods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.controllers.WallController;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.text.FormattedTextFormat;
import spark.utils.StringUtils;

public class WallMethods{
	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		int ownerID=actx.requireParamIntNonZero("owner_id");
		Actor owner=ownerID>0 ? ctx.getUsersController().getUserOrThrow(ownerID) : ctx.getGroupsController().getGroupOrThrow(-ownerID);
		User self=actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null;
		boolean canSeeAllPosts;
		if(owner instanceof User u){
			ctx.getPrivacyController().enforceUserProfileAccess(self, u);
			canSeeAllPosts=ctx.getPrivacyController().checkUserPrivacy(self, u, UserPrivacySettingKey.WALL_OTHERS_POSTS);
		}else if(owner instanceof Group g){
			ctx.getPrivacyController().enforceUserAccessToGroupContent(self, g);
			canSeeAllPosts=true;
		}else{
			throw new InternalServerErrorException();
		}

		String filter=actx.optParamString("filter");
		WallController.WallMode mode;
		if(canSeeAllPosts){
			mode=switch(filter){
				case "others" -> WallController.WallMode.OTHERS;
				case "all" -> WallController.WallMode.ALL;
				case null, default -> WallController.WallMode.OWNER;
			};
		}else{
			mode=WallController.WallMode.OWNER;
		}

		int offset=actx.getOffset();
		int count=actx.getCount(20, 100);
		PaginatedList<PostViewModel> posts=PostViewModel.wrap(ctx.getWallController().getWallPosts(self, owner, mode, offset, count));
		ApiPaginatedListWithActors<ApiWallPost> res=new ApiPaginatedListWithActors<>(posts.total, ApiUtils.getPosts(posts.list, ctx, actx, true, true));
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(posts.list, needUsers, needGroups);
			res.profiles=ApiUtils.getUsers(needUsers, ctx, actx);
			res.groups=ApiUtils.getGroups(needGroups, ctx, actx);
		}
		return res;
	}

	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		List<Integer> ids=actx.requireCommaSeparatedStringList("posts")
				.stream()
				.map(i->Utils.safeParseInt(i.contains("_") ? i.substring(i.indexOf('_')+1) : i))
				.filter(i->i>0)
				.limit(100)
				.toList();

		User self=actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null;

		Map<Integer, Post> postsByID=ctx.getWallController().getPosts(ids);
		ArrayList<PostViewModel> posts=new ArrayList<>();
		for(int id:ids){
			Post post=postsByID.get(id);
			if(post==null)
				continue;
			posts.add(new PostViewModel(post));
		}
		ctx.getPrivacyController().filterPostViewModels(self, posts);

		List<ApiWallPost> res=ApiUtils.getPosts(posts, ctx, actx, true, true);
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(posts, needUsers, needGroups);

			record PostsWithActors(List<ApiWallPost> items, List<ApiUser> profiles, List<ApiGroup> groups){}

			return new PostsWithActors(res, ApiUtils.getUsers(needUsers, ctx, actx), ApiUtils.getGroups(needGroups, ctx, actx));
		}else{
			return res;
		}
	}

	public static Object getComments(ApplicationContext ctx, ApiCallContext actx){
		int postID=actx.requireParamIntPositive("post_id");
		CommentViewType viewType=switch(actx.optParamString("view_type")){
			case "threaded" -> CommentViewType.THREADED;
			case "two_level" -> CommentViewType.TWO_LEVEL;
			case "flat" -> CommentViewType.FLAT;
			case null, default -> actx.self==null ? CommentViewType.FLAT : actx.self.prefs.commentViewType;
		};
		int offset=actx.getOffset();
		int count=actx.getCount(20, 100);
		int secondaryCount=Math.min(100, actx.optParamIntPositive("secondary_count", 20));
		int commentID=actx.optParamIntPositive("comment_id");
		boolean reversed="desc".equals(actx.optParamString("sort"));
		boolean needLikes=actx.booleanParam("need_likes");

		User self=actx.self!=null && actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null;

		Post post=ctx.getWallController().getPostOrThrow(postID);
		ctx.getPrivacyController().enforcePostPrivacy(self, post);
		Post threadParent;
		if(commentID>0){
			threadParent=ctx.getWallController().getPostOrThrow(commentID, true);
			if(threadParent.getReplyLevel()==0 || threadParent.replyKey.getFirst()!=postID)
				throw actx.error(ApiErrorType.NOT_FOUND);
			if(viewType==CommentViewType.TWO_LEVEL)
				viewType=CommentViewType.FLAT;
		}else{
			threadParent=post;
		}

		PaginatedList<PostViewModel> comments=ctx.getWallController().getReplies(self, threadParent.getReplyKeyForReplies(), offset, count, secondaryCount, viewType, reversed);

		record CommentsResponse(int count, List<ApiWallPost> items, String viewType, List<ApiUser> profiles, List<ApiGroup> groups){}

		List<ApiWallPost> apiComments=ApiUtils.getPosts(comments.list, ctx, actx, needLikes, false);
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(comments.list, needUsers, needGroups);
			return new CommentsResponse(comments.total, apiComments, viewType.name().toLowerCase(), ApiUtils.getUsers(needUsers, ctx, actx), ApiUtils.getGroups(needGroups, ctx, actx));
		}
		return new CommentsResponse(comments.total, apiComments, viewType.name().toLowerCase(), null, null);
	}

	public static Object getReposts(ApplicationContext ctx, ApiCallContext actx){
		int postID=actx.requireParamIntPositive("post_id");
		User self=actx.self!=null && actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null;
		Post post=ctx.getWallController().getPostOrThrow(postID);
		ctx.getPrivacyController().enforcePostPrivacy(self, post);

		PaginatedList<PostViewModel> posts=PostViewModel.wrap(ctx.getWallController().getPostReposts(post, actx.getOffset(), actx.getCount(20, 100)));
		ApiPaginatedListWithActors<ApiWallPost> res=new ApiPaginatedListWithActors<>(posts.total, ApiUtils.getPosts(posts.list, ctx, actx, true, true));
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(posts.list, needUsers, needGroups);
			res.profiles=ApiUtils.getUsers(needUsers, ctx, actx);
			res.groups=ApiUtils.getGroups(needGroups, ctx, actx);
		}
		return res;
	}
	
	public static Object pin(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(post.ownerID!=actx.self.user.id)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can only pin posts on own wall");
		if(post.ownerID!=post.authorID)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can only pin own posts");
		if(post.getReplyLevel()>0)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can't pin comments");

		ctx.getWallController().pinPost(post, false);

		return true;
	}

	public static Object unpin(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(post.ownerID!=actx.self.user.id)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can only pin posts on own wall");
		if(post.ownerID!=post.authorID)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can only pin own posts");
		if(post.getReplyLevel()>0)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "can't pin comments");

		ctx.getWallController().unpinPost(post);

		return true;
	}

	public static Object delete(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(!actx.permissions.canDeletePost(post))
			throw actx.error(ApiErrorType.ACCESS_DENIED, "no access to delete this post");
		ctx.getWallController().deletePost(actx.self.user, post);
		return true;
	}

	public static Object post(ApplicationContext ctx, ApiCallContext actx){
		int ownerID=actx.requireParamIntNonZero("owner_id");
		String message=actx.optParamString("message");
		JsonArray attachments=actx.optParamJsonArray("attachments");
		String cw=actx.optParamString("content_warning");
		FormattedTextFormat textFormat=switch(actx.optParamString("text_format")){
			case "markdown" -> FormattedTextFormat.MARKDOWN;
			case "html" -> FormattedTextFormat.HTML;
			case "plain" -> FormattedTextFormat.PLAIN;
			case null -> actx.self.prefs.textFormat;
			default -> throw actx.paramError("text_format must be one of markdown, html, plain");
		};
		String guid=actx.optParamString("guid");
		if(StringUtils.isNotEmpty(guid)){
			guid=actx.token.getEncodedID()+"|"+guid;
		}

		Poll poll=null;
		List<String> attachmentIDs;
		Map<String, String> attachmentAltTexts;
		if(attachments!=null){
			attachmentIDs=new ArrayList<>();
			attachmentAltTexts=new HashMap<>();
			int i=0;
			for(JsonElement el:attachments){
				if(!(el instanceof JsonObject obj))
					throw actx.paramError("attachments["+i+"] is not an object");
				if(!(obj.get("type") instanceof JsonPrimitive jType && jType.isString()))
					throw actx.paramError("attachments["+i+"].type is not a string");
				String type=jType.getAsString();
				switch(type){
					case "image" -> {
						if(!(obj.get("file_id") instanceof JsonPrimitive jId))
							throw actx.paramError("attachments["+i+"].file_id is undefined");
						if(!(obj.get("file_hash") instanceof JsonPrimitive jHash))
							throw actx.paramError("attachments["+i+"].file_hash is undefined");

						String id=jId.getAsString()+":"+jHash.getAsString();
						attachmentIDs.add(id);

						if(obj.get("text") instanceof JsonPrimitive jText && jText.isString())
							attachmentAltTexts.put(id, jText.getAsString());
					}
					case "photo" -> {
						if(!(obj.get("photo_id") instanceof JsonPrimitive jId))
							throw actx.paramError("attachments["+i+"].photo_id is undefined");
						String photoID=jId.getAsString();
						attachmentIDs.add("photo:"+photoID);
					}
					case "poll" -> {
						if(!(obj.get("poll_id") instanceof JsonPrimitive jId))
							throw actx.paramError("attachments["+i+"].poll_id is undefined");
						int id=jId.getAsInt();
						// TODO
					}
					default -> throw actx.paramError("attachments["+i+"].type must be one of image, photo, poll");
				}
				i++;
			}
		}else{
			attachmentIDs=List.of();
			attachmentAltTexts=Map.of();
		}

		Actor owner;
		if(ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-ownerID);

		return ctx.getWallController().createWallPost(actx.self.user, owner, null, message, textFormat, cw, attachmentIDs, poll, null, attachmentAltTexts, null, ctx.getAppsController().getAppByID(actx.token.appID()), guid).id;
	}
}
