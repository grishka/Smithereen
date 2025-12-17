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
import smithereen.activitypub.objects.LocalImage;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.controllers.WallController;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.Post;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.media.MediaFileUploadPurpose;
import smithereen.model.media.MediaFileUploadTokens;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.text.FormattedTextFormat;
import smithereen.util.XTEA;
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
		ApiPaginatedListWithActors<ApiWallPost> res=new ApiPaginatedListWithActors<>(posts.total, ApiUtils.getPosts(posts.list, ctx, actx, true, true, true));
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

		List<ApiWallPost> res=ApiUtils.getPosts(posts, ctx, actx, true, true, true);
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

		List<ApiWallPost> apiComments=ApiUtils.getPosts(comments.list, ctx, actx, needLikes, false, true);
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
		ApiPaginatedListWithActors<ApiWallPost> res=new ApiPaginatedListWithActors<>(posts.total, ApiUtils.getPosts(posts.list, ctx, actx, true, true, true));
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
		Actor owner;
		if(ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-ownerID);

		return createOrUpdatePost(ctx, actx, owner, null, null, null);
	}

	public static Object repost(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		return createOrUpdatePost(ctx, actx, actx.self.user, post, null, null);
	}

	public static Object createComment(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(post.getReplyLevel()!=0)
			throw new ObjectNotFoundException();
		Post replyTo;
		if(actx.hasParam("reply_to_comment")){
			Post comment=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("reply_to_comment"));
			if(comment.getReplyLevel()==0 || comment.replyKey.getFirst()!=post.id)
				throw new ObjectNotFoundException();
			replyTo=comment;
		}else{
			replyTo=post;
		}
		return createOrUpdatePost(ctx, actx, ctx.getWallController().getContentAuthorAndOwner(post).owner(), null, replyTo, null);
	}

	private static Object createOrUpdatePost(ApplicationContext ctx, ApiCallContext actx, Actor owner, Post repost, Post replyTo, Post edit){
		String message=actx.optParamString("message");
		String cw=actx.optParamString("content_warning");
		FormattedTextFormat textFormat=ApiUtils.getTextFormat(actx);
		boolean isReply=replyTo!=null || (edit!=null && edit.getReplyLevel()>0);

		ApiUtils.InputAttachments attachments=ApiUtils.parseAttachments(ctx, actx, isReply);

		if(edit!=null){
			ctx.getWallController().editPost(actx.self.user, actx.permissions, edit.id, message, textFormat, cw, attachments.ids(), attachments.poll(), attachments.altTexts());
			return edit.id;
		}

		String guid=actx.optParamString("guid");
		if(StringUtils.isNotEmpty(guid)){
			guid=actx.token.getEncodedID()+"|"+guid;
		}
		return ctx.getWallController().createWallPost(actx.self.user, owner, replyTo, message, textFormat, cw, attachments.ids(), attachments.poll(), repost, attachments.altTexts(), null, ctx.getAppsController().getAppByID(actx.token.appID()), guid).id;
	}

	public static Object getEditSource(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(!actx.permissions.canEditPost(post))
			throw actx.error(ApiErrorType.ACCESS_DENIED, "no access to edit this post");
		PostSource source=ctx.getWallController().getPostSource(post);
		List<Map<String, Object>> attachments=new ArrayList<>();
		for(Attachment att:post.getProcessedAttachments()){
			if(att instanceof PhotoAttachment pa){
				if(pa.photoID!=0){
					attachments.add(Map.of(
							"type", "photo",
							"photo_id", XTEA.encodeObjectID(pa.photoID, ObfuscatedObjectIDType.PHOTO)
					));
				}else if(pa.image instanceof LocalImage li && li.fileRecord!=null){
					Map<String, Object> sa=new HashMap<>();
					sa.put("type", "image");
					sa.put("file_id", XTEA.encodeObjectID(li.fileID, ObfuscatedObjectIDType.MEDIA_FILE));
					sa.put("file_hash", MediaFileUploadTokens.getToken(li.fileRecord.id(), MediaFileUploadPurpose.ATTACHMENT, actx.self.user.id));
					sa.put("text", li.name);
					attachments.add(sa);
				}
			}
		}
		if(post.poll!=null){
			attachments.add(Map.of(
					"type", "poll",
					"poll_id", post.poll.id
			));
		}

		record SourceResponse(String text, String format, List<Map<String, Object>> attachments){}
		return new SourceResponse(source.text(), source.format().name().toLowerCase(), attachments);
	}

	public static Object edit(ApplicationContext ctx, ApiCallContext actx){
		Post post=ctx.getWallController().getPostOrThrow(actx.requireParamIntPositive("post_id"));
		if(!actx.permissions.canEditPost(post))
			throw actx.error(ApiErrorType.ACCESS_DENIED, "no access to edit this post");
		return createOrUpdatePost(ctx, actx, ctx.getWallController().getContentAuthorAndOwner(post).owner(), null, null, post);
	}
}
