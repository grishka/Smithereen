package smithereen.routes;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.PostSource;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.FormattedTextFormat;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class CommentsRoutes{
	private static CommentableContentObject getParentObject(Request req, ApplicationContext ctx){
		requireQueryParams(req, "parentType", "parentID");
		String parentID=req.queryParams("parentID");
		Account self=sessionInfo(req) instanceof SessionInfo si ? si.account : null;
		return switch(req.queryParams("parentType")){
			case "photo" -> {
				Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(parentID, ObfuscatedObjectIDType.PHOTO));
				ctx.getPrivacyController().enforceObjectPrivacy(self!=null ? self.user : null, photo);
				yield photo;
			}
			default -> throw new BadRequestException();
		};
	}

	public static Object createComment(Request req, Response resp, Account self, ApplicationContext ctx){
		CommentableContentObject parent=getParentObject(req, ctx);
		String text=req.queryParams("text");
		long replyTo=0;
		if(StringUtils.isNotEmpty(req.queryParams("replyTo"))){
			replyTo=XTEA.decodeObjectID(req.queryParams("replyTo"), ObfuscatedObjectIDType.COMMENT);
		}
		String contentWarning=req.queryParams("contentWarning");
		List<String> attachments;
		if(StringUtils.isNotEmpty(req.queryParams("attachments")))
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
		else
			attachments=List.of();

		Comment inReplyTo=replyTo!=0 ? ctx.getCommentsController().getCommentIgnoringPrivacy(replyTo) : null;
		Comment comment=ctx.getCommentsController().createComment(self.user, parent, inReplyTo, text, self.prefs.textFormat, contentWarning, attachments);

		if(isAjax(req)){
			String formID=req.queryParams("formID");
			CommentViewModel pvm=new CommentViewModel(comment);
			ArrayList<CommentViewModel> needInteractions=new ArrayList<>();
			needInteractions.add(pvm);
			if(inReplyTo!=null)
				pvm.parentAuthorID=inReplyTo.authorID;
			RenderedTemplateResponse model=new RenderedTemplateResponse("comment", req).with("post", pvm);
//			if(replyTo!=0){
			//CommentViewModel topLevel=new CommentViewModel(context(req).getCommentsController().getCommentIgnoringPrivacy(comment.replyKey.getFirst()));

			model.with("replyFormID", "wallPostForm_commentReply_"+parent.getCommentParentID().getHtmlElementID());
//				model.with("topLevel", topLevel);
//				needInteractions.add(topLevel);
//			}
//			Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(needInteractions, self.user);
//			model.with("postInteractions", interactions);
			Map<Integer, User> users=new HashMap<>();
			users.put(self.user.id, self.user);
			if(inReplyTo!=null && inReplyTo.authorID!=self.user.id){
				try{
					users.put(inReplyTo.authorID, ctx.getUsersController().getUserOrThrow(inReplyTo.authorID));
				}catch(ObjectNotFoundException ignore){}
			}
			model.with("users", users);
			model.with("comments", Map.of(comment.id, comment));
			model.with("commentViewType", self.prefs.commentViewType).with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1);
			model.with("canComment", true).with("parentObject", parent);
			String postHTML=model.renderToString();
//			if(req.attribute("mobile")!=null && replyTo==0){
//				postHTML="<div class=\"card\">"+postHTML+"</div>";
//			}else if(replyTo==0){
//				// TODO correctly handle day headers in feed
//				String cl="feed".equals(formID) ? "feedRow" : "wallRow";
//				postHTML="<div class=\""+cl+"\">"+postHTML+"</div>";
//			}
			WebDeltaResponse rb;
			if(replyTo==0 || self.prefs.commentViewType==CommentViewType.FLAT){
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "comments_"+parent.getCommentParentID().getHtmlElementID(), postHTML);
			}else{
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "commentReplies"+switch(self.prefs.commentViewType){
					case THREADED -> XTEA.encodeObjectID(replyTo, ObfuscatedObjectIDType.COMMENT);
					case TWO_LEVEL -> XTEA.encodeObjectID(comment.replyKey.get(Math.min(comment.getReplyLevel()-1, 1)), ObfuscatedObjectIDType.COMMENT);
					case FLAT -> throw new IllegalStateException();
				}, postHTML).show("commentReplies"+replyTo);
			}
			if(req.attribute("mobile")==null && replyTo==0){
				rb.runScript("updatePostForms();");
			}
			return rb.setInputValue("postFormText_"+formID, "").setContent("postFormAttachments_"+formID, "");
		}
		resp.redirect(back(req));
		return "";
	}

	public static void prepareCommentList(ApplicationContext ctx, List<CommentViewModel> wall, RenderedTemplateResponse model, Account self){
		HashSet<Integer> needUsers=new HashSet<>();
		CommentViewModel.collectUserIDs(wall, needUsers);
		model.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1);
	}

	public static Object ajaxCommentPreview(Request req, Response resp){
		ApplicationContext ctx=context(req);
		CommentableContentObject parent=getParentObject(req, ctx);

		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		long maxID=XTEA.decodeObjectID(req.queryParams("firstID"), ObfuscatedObjectIDType.COMMENT);
		if(maxID==0)
			throw new BadRequestException();

		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getCommentsWithMaxID(parent, maxID, 100, viewType==CommentViewType.FLAT);
		RenderedTemplateResponse model=new RenderedTemplateResponse("comment_list", req);
		model.with("comments", comments.list).with("baseReplyLevel", 0);
		prepareCommentList(ctx, comments.list, model, self);
//		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(topLevel), comments.list).flatMap(List::stream).toList(), self!=null ? self.user : null);
//		model.with("postInteractions", interactions)

		boolean canComment=switch(parent){
			case Photo p -> ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(p.ownerID), ctx.getPhotosController().getAlbumIgnoringPrivacy(p.albumID).commentPrivacy);
		};

		String elementID=parent.getCommentParentID().getHtmlElementID();

		model.with("preview", true)
				.with("canComment", canComment)
				.with("replyFormID", "wallPostForm_commentReply_"+elementID)
				.with("commentViewType", viewType)
				.with("parentObject", parent);
		WebDeltaResponse rb=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_BEGIN, "comments_"+elementID, model.renderToString())
				.hide("prevLoader_"+elementID);
		if(comments.total>comments.list.size()){
			rb.show("loadPrevBtn_"+elementID).setAttribute("loadPrevBtn_"+elementID, "data-first-id", String.valueOf(comments.list.getFirst().post.id));
			if(viewType==CommentViewType.FLAT){
				rb.setContent("loadPrevBtn_"+elementID, lang(req).get("comments_show_X_more_comments", Map.of("count", comments.total-comments.list.size())));
			}
		}else{
			rb.remove("prevLoader_"+elementID, "loadPrevBtn_"+elementID);
		}
		return rb;
	}

	public static Object ajaxCommentBranch(Request req, Response resp){
		ApplicationContext ctx=context(req);
		CommentableContentObject parent=getParentObject(req, ctx);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		if(!Objects.equals(comment.parentObjectID, parent.getCommentParentID()))
			throw new ObjectNotFoundException();

		String commentID=comment.getIDString();

		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		int offset=offset(req);

		CommentViewType viewType=info!=null && info.account!=null ? info.account.prefs.commentViewType : CommentViewType.THREADED;
		PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getComments(parent, comment.getReplyKeyForReplies(), offset, 100, 50, viewType);
		RenderedTemplateResponse model=new RenderedTemplateResponse("comment_list", req);
		model.with("comments", comments.list)
				.with("parentObject", parent)
				.with("replyFormID", "wallPostForm_commentReply_"+parent.getCommentParentID().getHtmlElementID());
		ArrayList<CommentViewModel> allReplies=new ArrayList<>();
		for(CommentViewModel c:comments.list){
			allReplies.add(c);
			c.getAllReplies(allReplies);
		}
//		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(Stream.of(List.of(topLevel), allReplies).flatMap(List::stream).toList(), self!=null ? self.user : null);
//		model.with("postInteractions", interactions);
		prepareCommentList(ctx, comments.list, model, self);
		boolean canComment=switch(parent){
			case Photo p -> ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(p.ownerID), ctx.getPhotosController().getAlbumIgnoringPrivacy(p.albumID).commentPrivacy);
		};
		model.with("commentViewType", viewType).with("canComment", canComment);
		WebDeltaResponse wdr=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "loadRepliesContainer"+commentID, model.renderToString());
		if(comments.list.size()+offset==comments.total){
			wdr.remove("loadRepliesContainer"+commentID);
		}else{
			wdr.hide("repliesLoader"+commentID)
					.show("loadRepliesLink"+commentID)
					.setAttribute("loadRepliesLink"+commentID, "data-offset", String.valueOf(offset+comments.list.size()))
					.setContent("loadRepliesLink"+commentID, lang(req).get("comments_show_X_more_replies", Map.of("count", comments.total-comments.list.size()-offset)));
		}
		return wdr;
	}

	public static Object confirmDeleteComment(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete_reply"), l.get("delete_reply_confirm"), "/comments/"+req.params(":id")+"/delete");
	}

	public static Object deleteComment(Request req, Response resp, Account self, ApplicationContext ctx){
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		ctx.getCommentsController().deleteComment(self.user, comment);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).remove("comment"+comment.getIDString());
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object editCommentForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		if(!sessionInfo(req).permissions.canEditPost(comment))
			throw new UserActionNotAllowedException();
		ctx.getCommentsController().getCommentParent(self.user, comment); // also enforces privacy
		RenderedTemplateResponse model;
		if(isAjax(req)){
			model=new RenderedTemplateResponse("wall_post_form", req);
		}else{
			model=new RenderedTemplateResponse("content_wrap", req).with("contentTemplate", "wall_post_form");
		}
		String id=comment.getIDString();
		model.with("addClasses", "editing nonCollapsible").with("isEditing", true).with("id", "edit"+id).with("editingPostID", id);
		PostSource source=ctx.getCommentsController().getCommentSource(comment);
		model.with("prefilledPostText", source.text()).with("sourceFormat", source.format()).with("action", comment.getInternalURL()+"/edit").with("isComment", true);
		if(comment.hasContentWarning())
			model.with("contentWarning", comment.contentWarning);
		if(comment.attachments!=null && !comment.attachments.isEmpty()){
			model.with("draftAttachments", comment.attachments);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.hide("postInner"+id)
					.hide("postFloatingActions"+id)
					.hide("inReplyTo"+id)
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postInner"+id, model.renderToString())
					.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "postAuthor"+id, "<span class=\"grayText lowercase\" id=\"postEditingLabel"+id+"\">&nbsp;-&nbsp;"+lang(req).get("editing_comment")+"</span>")
					.runScript("updatePostForms(); ge(\"postFormText_edit"+id+"\").focus();");
		}
		return model.pageTitle(lang(req).get("editing_comment"));
	}

	public static Object editComment(Request req, Response resp, Account self, ApplicationContext ctx){
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		if(!sessionInfo(req).permissions.canEditPost(comment))
			throw new UserActionNotAllowedException();
		CommentableContentObject parent=ctx.getCommentsController().getCommentParent(self.user, comment);
		String text=req.queryParams("text");
		String contentWarning=req.queryParams("contentWarning");
		List<String> attachments;
		if(StringUtils.isNotEmpty(req.queryParams("attachments")))
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
		else
			attachments=Collections.emptyList();

		comment=ctx.getCommentsController().editComment(self.user, comment, text, enumValue(req.queryParams("format"), FormattedTextFormat.class), contentWarning, attachments);
		if(isAjax(req)){
			if(req.attribute("mobile")!=null)
				return new WebDeltaResponse(resp).replaceLocation(parent.getURL());

			CommentViewModel cvm=new CommentViewModel(comment);
			RenderedTemplateResponse model=new RenderedTemplateResponse("comment", req)
					.with("post", cvm)
					.with("parentObject", parent)
					.with("replyFormID", "wallPostForm_commentReply_"+parent.getCommentParentID().getHtmlElementID())
					.with("canComment", true);
			model.with("users", Map.of(self.user.id, self.user));
			ArrayList<PostViewModel> needInteractions=new ArrayList<>();
//			needInteractions.add(cvm);
//			Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(needInteractions, self.user);
//			model.with("postInteractions", interactions);
			String id=comment.getIDString();
			return new WebDeltaResponse(resp).setContent("postInner"+id, model.renderBlock("postInner"))
					.show("postInner"+id)
					.show("postFloatingActions"+id)
					.show("inReplyTo"+id)
					.remove("wallPostForm_edit"+id, "postEditingLabel"+id);
		}
		resp.redirect(parent.getURL());
		return "";
	}

	public static Object commentHoverCard(Request req, Response resp){
		if(isMobile(req) || !isAjax(req))
			return "";
		ApplicationContext ctx=context(req);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		Account self=null;
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			self=info.account;
		}
		ctx.getCommentsController().getCommentParent(self==null ? null : self.user, comment); // also enforces privacy
		CommentViewModel pvm=new CommentViewModel(comment);
		if(comment.getReplyLevel()>0)
			pvm.parentAuthorID=ctx.getCommentsController().getCommentIgnoringPrivacy(comment.replyKey.getLast()).authorID;
		HashSet<Integer> needUsers=new HashSet<>();
		CommentViewModel.collectUserIDs(Set.of(pvm), needUsers);
		return new RenderedTemplateResponse("comment_hover_card", req)
				.with("post", pvm)
				.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1)
				.with("users", ctx.getUsersController().getUsers(needUsers));
	}
}
