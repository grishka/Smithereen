package smithereen.routes;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.LikeableContentObject;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.comments.Comment;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class UserInteractionsRoutes{
	public static Object setLiked(Request req, Response resp, Account self, ApplicationContext ctx, LikeableContentObject obj, boolean liked){
		req.attribute("noHistory", true);
		ctx.getUserInteractionsController().setObjectLiked(obj, liked, self.user);
		if(isAjax(req)){
			UserInteractions interactions=obj instanceof Post post ?
					ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self.user).get(post.getIDForInteractions())
					: ctx.getUserInteractionsController().getUserInteractions(List.of(obj), self.user).get(obj.getObjectID());
			String elementID=switch(obj){
				case Post post -> "Post"+post.id;
				case Photo photo -> "Photo"+photo.getIdString();
				case Comment comment -> "Comment"+comment.getIDString();
			};
			String url=switch(obj){
				case Post post -> post.getInternalURL().toString();
				case Photo photo -> photo.getURL();
				case Comment comment -> comment.getInternalURL().toString();
			};

			String rid=req.queryParams("rid");
			if(StringUtils.isNotEmpty(rid)){
				elementID+="_"+rid;
			}

			String urlPath=liked ? "unlike" : "like";
			WebDeltaResponse b=new WebDeltaResponse(resp)
					.setContent("likeCounter"+elementID, String.valueOf(interactions.likeCount))
					.setAttribute("likeButton"+elementID, "href", url+"/"+urlPath+"?csrf="+requireSession(req).csrfToken);
			if(interactions.likeCount==0)
				b.hide("likeCounter"+elementID);
			else
				b.show("likeCounter"+elementID);
			return b;
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object likePopover(Request req, Response resp, LikeableContentObject obj){
		ApplicationContext ctx=context(req);
		SessionInfo info=sessionInfo(req);
		User self=info!=null && info.account!=null ? info.account.user : null;

		context(req).getPrivacyController().enforceObjectPrivacy(self, obj);
		List<User> users=ctx.getUserInteractionsController().getLikesForObject(obj, self, 0, 6).list;
		String _content=new RenderedTemplateResponse("like_popover", req).with("users", users).renderToString();
		UserInteractions interactions=obj instanceof Post post ?
				ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self).get(post.getIDForInteractions())
				: ctx.getUserInteractionsController().getUserInteractions(List.of(obj), self).get(obj.getObjectID());
		String elementID=switch(obj){
			case Post post -> "Post"+post.id;
			case Photo photo -> "Photo"+photo.getIdString();
			case Comment comment -> "Comment"+comment.getIDString();
		};
		String url=switch(obj){
			case Post post -> post.getInternalURL().toString();
			case Photo photo -> photo.getURL();
			case Comment comment -> comment.getInternalURL().toString();
		};

		String rid=req.queryParams("rid");
		if(StringUtils.isNotEmpty(rid)){
			elementID+="_"+rid;
		}

		WebDeltaResponse b=new WebDeltaResponse(resp)
				.setContent("likeCounter"+elementID, String.valueOf(interactions.likeCount));
		if(info!=null && info.account!=null){
			b.setAttribute("likeButton"+elementID, "href", url+"/"+(interactions.isLiked ? "un" : "")+"like?csrf="+info.csrfToken);
		}
		if(interactions.likeCount==0)
			b.hide("likeCounter"+elementID);
		else
			b.show("likeCounter"+elementID);

		LikePopoverResponse o=new LikePopoverResponse();
		o.content=_content;
		o.title=lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount));
		o.altTitle=self==null ? null : lang(req).get("liked_by_X_people", Map.of("count", interactions.likeCount+(interactions.isLiked ? -1 : 1)));
		o.actions=b.commands();
		o.show=interactions.likeCount>0;
		o.fullURL=url+"/likes";
		return gson.toJson(o);
	}

	public static Object likeList(Request req, Response resp, LikeableContentObject obj){
		ApplicationContext ctx=context(req);
		SessionInfo info=sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, obj);
		UserInteractions interactions=obj instanceof Post post ?
				ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), self!=null ? self.user : null).get(post.getIDForInteractions())
				: ctx.getUserInteractionsController().getUserInteractions(List.of(obj), self!=null ? self.user : null).get(obj.getObjectID());
		int offset=offset(req);
		PaginatedList<User> likes=ctx.getUserInteractionsController().getLikesForObject(obj, null, offset, 100);
		RenderedTemplateResponse model;
		if(isMobile(req)){
			model=new RenderedTemplateResponse("content_interactions_likes", req);
		}else{
			model=new RenderedTemplateResponse(isAjax(req) ? "content_interactions_box" : "content_wrap", req);
		}

		String elementID=switch(obj){
			case Post post -> "Post"+post.id;
			case Photo photo -> "Photo"+photo.getIdString();
			case Comment comment -> "Comment"+comment.getIDString();
		};
		String url=switch(obj){
			case Post post -> post.getInternalURL().toString();
			case Photo photo -> photo.getURL();
			case Comment comment -> comment.getInternalURL().toString();
		};

		model.paginate(likes)
				.with("emptyMessage", lang(req).get("likes_empty"))
				.with("interactions", interactions)
				.with("object", obj)
				.with("tab", "likes")
				.with("url", url)
				.with("elementID", elementID);

		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(likes.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}

		if(isMobile(req))
			return model.pageTitle(lang(req).get("likes_title"));

		if(isAjax(req)){
			String paginationID=req.queryParams("pagination");
			boolean fromTab=req.queryParams("fromTab")!=null;
			if(fromTab){
				return model.renderBlock("likes");
			}else if(paginationID!=null){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("likesInner"));
				if(offset+likes.list.size()<likes.total){
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", url+"/likes?offset="+(offset+likes.perPage));
				}else{
					r.remove("ajaxPagination_"+paginationID);
				}
				return r;
			}else{
				return new WebDeltaResponse(resp)
						.box(lang(req).get("likes_title"), model.renderToString(), "likesList", 620)
						.runScript("initTabbedBox(ge(\"interactionsTabs"+elementID+"\"), ge(\"interactionsContent"+elementID+"\")); initDynamicControls(ge(\"likesList\"));");
			}
		}
		model.with("contentTemplate", "content_interactions_box").with("title", lang(req).get("likes_title"));
		return model;
	}

	public static Object like(Request req, Response resp, LikeableContentObject obj){
		ApplicationContext ctx=context(req);
		if(requireAccount(req, null) && verifyCSRF(req, resp)){
			return setLiked(req, resp, sessionInfo(req).account, ctx, obj, true);
		}
		if(!(obj instanceof ActivityPubRepresentable apr))
			throw new UserErrorException("not implemented yet");

		Lang l=lang(req);
		String url=apr.getActivityPubURL().toString();
		String title=switch(obj){
			case Post post -> l.get(post.getReplyLevel()>0 ? "remote_like_comment_title" : "remote_like_post_title");
			case Photo photo -> l.get("remote_like_photo_title");
			case Comment comment -> l.get("remote_like_comment_title");
		};
		return remoteInteraction(req, resp, url, title, null, !(obj instanceof Post));
	}

	static Object remoteInteraction(Request req, Response resp, String url, String title, Post postToEmbed, boolean hideWorksWith){
		RenderedTemplateResponse model;
		if(isAjax(req)){
			if(isMobile(req)){
				model=new RenderedTemplateResponse("remote_interaction", req);
			}else{
				model=new RenderedTemplateResponse("layer_with_title", req).with("contentTemplate", "remote_interaction");
			}
		}else{
			model=new RenderedTemplateResponse("content_wrap", req).with("contentTemplate", "remote_interaction");
		}
		model.with("contentURL", url).with("serverSignupMode", Config.signupMode).with("hideWorksWith", hideWorksWith);
		model.pageTitle(title);
		if(!isMobile(req) && postToEmbed!=null && postToEmbed.isLocal())
			model.with("postToEmbed", postToEmbed);
		if(isAjax(req)){
			if(isMobile(req)){
				return new WebDeltaResponse(resp)
						.box(title, model.renderToString(), null, false)
						.runScript("restoreRemoteInteractionDomain();");
			}else{
				return new WebDeltaResponse(resp)
						.layer(model.renderToString(), null)
						.runScript("restoreRemoteInteractionDomain();");
			}
		}else{
			return model;
		}
	}

	public static class LikePopoverResponse{
		public String content;
		public String title;
		public String altTitle;
		public String fullURL;
		public List<WebDeltaResponse.Command> actions;
		public boolean show;
	}
}
