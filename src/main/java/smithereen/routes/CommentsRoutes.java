package smithereen.routes;

import com.google.gson.reflect.TypeToken;

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

import static smithereen.Utils.*;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.LikeableContentObject;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.PostSource;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.storage.utils.Pair;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.FormattedTextFormat;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

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
			case "topic" -> ctx.getBoardController().getTopic(self==null ? null : self.user, XTEA.decodeObjectID(parentID, ObfuscatedObjectIDType.BOARD_TOPIC));
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
		Map<String, String> attachmentAltTexts;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
			String altTextsJson=req.queryParams("attachAltTexts");
			if(StringUtils.isNotEmpty(altTextsJson)){
				try{
					attachmentAltTexts=gson.fromJson(altTextsJson, new TypeToken<>(){});
				}catch(Exception x){
					attachmentAltTexts=Map.of();
				}
			}else{
				attachmentAltTexts=Map.of();
			}
		}else{
			attachments=Collections.emptyList();
			attachmentAltTexts=Map.of();
		}

		Comment inReplyTo=replyTo!=0 ? ctx.getCommentsController().getCommentIgnoringPrivacy(replyTo) : null;
		Comment comment=ctx.getCommentsController().createComment(self.user, parent, inReplyTo, text, self.prefs.textFormat, contentWarning, attachments, attachmentAltTexts);

		if(isAjax(req)){
			String rid=req.queryParams("rid");
			String ridSuffix="";
			if(StringUtils.isNotEmpty(rid))
				ridSuffix="_"+rid;
			boolean fromNotifications="notifications".equals(req.queryParams("from"));
			boolean fromFeed="feed".equals(req.queryParams("from"));
			String formID=req.queryParams("formID");
			CommentViewModel pvm=new CommentViewModel(comment);
			ArrayList<LikeableContentObject> needInteractions=new ArrayList<>();
			needInteractions.add(comment);
			if(inReplyTo!=null)
				pvm.parentAuthorID=inReplyTo.authorID;
			RenderedTemplateResponse model;
			if(fromNotifications){
				model=new RenderedTemplateResponse("wall_reply_notifications", req)
						.with("post", pvm)
						.with("parentType", switch(parent.getCommentParentID().type()){
							case PHOTO -> Notification.ObjectType.PHOTO;
							case BOARD_TOPIC -> Notification.ObjectType.BOARD_TOPIC;
						})
						.with("postType", Notification.ObjectType.COMMENT);
			}else{
				model=new RenderedTemplateResponse("comment", req).with("post", pvm);
			}
			CommentViewType viewType=parent instanceof BoardTopic ? CommentViewType.FLAT : self.prefs.commentViewType;

			Map<Long, UserInteractions> commentsInteractions=ctx.getUserInteractionsController().getUserInteractions(needInteractions, self.user);
			model.with("commentInteractions", commentsInteractions);

			model.with("replyFormID", "wallPostForm_"+(viewType==CommentViewType.FLAT ? "comment_" : "commentReply_")+parent.getCommentParentID().getHtmlElementID());
			Map<Integer, User> users=new HashMap<>();
			users.put(self.user.id, self.user);
			if(inReplyTo!=null && inReplyTo.authorID!=self.user.id){
				try{
					users.put(inReplyTo.authorID, ctx.getUsersController().getUserOrThrow(inReplyTo.authorID));
				}catch(ObjectNotFoundException ignore){}
			}
			model.with("users", users);
			model.with("comments", Map.of(comment.id, comment));
			model.with("commentViewType", viewType);
			model.with("canComment", true).with("parentObject", parent);
			if(parent instanceof BoardTopic){
				model.with("board", !fromFeed);
				model.with("maxReplyDepth", 0);
			}else{
				model.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1);
			}
			String postHTML=model.renderToString();
			WebDeltaResponse rb;
			if(fromNotifications){
				rb=new WebDeltaResponse(resp)
						.remove("notificationsOwnReply"+ridSuffix)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "wallPostForm_"+formID, postHTML)
						.addClass("wallPostForm_"+formID, "collapsed");
			}else if(replyTo==0 || viewType==CommentViewType.FLAT){
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "comments_"+parent.getCommentParentID().getHtmlElementID(), postHTML);
			}else{
				rb=new WebDeltaResponse(resp).insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "commentReplies"+switch(viewType){
					case THREADED -> XTEA.encodeObjectID(replyTo, ObfuscatedObjectIDType.COMMENT);
					case TWO_LEVEL -> XTEA.encodeObjectID(comment.replyKey.get(Math.min(comment.getReplyLevel()-1, 0)), ObfuscatedObjectIDType.COMMENT);
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

	private static void prepareCommentList(ApplicationContext ctx, List<CommentViewModel> wall, RenderedTemplateResponse model, Account self, CommentableContentObject parent){
		HashSet<Integer> needUsers=new HashSet<>();
		CommentViewModel.collectUserIDs(wall, needUsers);
		model.with("users", ctx.getUsersController().getUsers(needUsers, true))
				.with("maxReplyDepth", parent instanceof BoardTopic ? 0 : PostRoutes.getMaxReplyDepth(self)-1);
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
		if(parent instanceof BoardTopic)
			viewType=CommentViewType.FLAT;
		PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getCommentsWithMaxID(parent, maxID, 100, viewType==CommentViewType.FLAT);
		RenderedTemplateResponse model=new RenderedTemplateResponse("comment_list", req);
		model.with("comments", comments.list).with("baseReplyLevel", 0);
		prepareCommentList(ctx, comments.list, model, self, parent);
		Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(comments.list.stream().map(cvm->cvm.post).toList(), self!=null ? self.user : null);
		model.with("commentInteractions", interactions);

		boolean canComment=switch(parent){
			case Photo p -> ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(p.ownerID), ctx.getPhotosController().getAlbumIgnoringPrivacy(p.albumID).commentPrivacy);
			case BoardTopic bt -> self!=null && ctx.getBoardController().canPostInTopic(self.user, bt);
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
		Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(allReplies.stream().map(cvm->cvm.post).toList(), self!=null ? self.user : null);
		model.with("commentInteractions", interactions);
		prepareCommentList(ctx, comments.list, model, self, parent);
		boolean canComment=switch(parent){
			case Photo p -> ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, ctx.getUsersController().getUserOrThrow(p.ownerID), ctx.getPhotosController().getAlbumIgnoringPrivacy(p.albumID).commentPrivacy);
			case BoardTopic bt -> self!=null && ctx.getBoardController().canPostInTopic(self.user, bt);
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
			if(req.queryParams("elid")!=null){
				return new WebDeltaResponse(resp).remove(req.queryParams("elid"));
			}
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
			model.with("attachAltTexts", comment.attachments.stream()
					.map(att->att instanceof LocalImage li && li.photoID==0 ? new Pair<>(li.fileRecord.id().getIDForClient(), li.name) : null)
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::first, Pair::second))
			);
			Map<String, PhotoViewerInlineData> pvData=comment.attachments.stream()
					.map(att->att instanceof LocalImage li && li.photoID==0 ? new Pair<>(li.getLocalID(), new PhotoViewerInlineData(0, "rawFile/"+li.getLocalID(), li.getURLsForPhotoViewer())) : null)
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::first, Pair::second, (a, b)->b));
			model.with("attachPvData", pvData);
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
		Map<String, String> attachmentAltTexts;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
			String altTextsJson=req.queryParams("attachAltTexts");
			if(StringUtils.isNotEmpty(altTextsJson)){
				try{
					attachmentAltTexts=gson.fromJson(altTextsJson, new TypeToken<>(){});
				}catch(Exception x){
					attachmentAltTexts=Map.of();
				}
			}else{
				attachmentAltTexts=Map.of();
			}
		}else{
			attachments=Collections.emptyList();
			attachmentAltTexts=Map.of();
		}

		comment=ctx.getCommentsController().editComment(self.user, comment, text, enumValue(req.queryParams("format"), FormattedTextFormat.class), contentWarning, attachments, attachmentAltTexts);
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
			model.with("commentInteractions", ctx.getUserInteractionsController().getUserInteractions(List.of(cvm.post), self.user));
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
		RenderedTemplateResponse model=new RenderedTemplateResponse("comment_hover_card", req)
				.with("post", pvm)
				.with("users", ctx.getUsersController().getUsers(needUsers, true));
		if(comment.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC){
//			model.with("board", true);
			model.with("maxReplyDepth", 0);
		}else{
			model.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1);
		}
		return model;
	}

	private static Comment getCommentForRequest(Request req){
		User self=null;
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			self=info.account.user;
		}
		ApplicationContext ctx=context(req);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		ctx.getPrivacyController().enforceObjectPrivacy(self, comment);
		return comment;
	}

	public static Object like(Request req, Response resp){
		return UserInteractionsRoutes.like(req, resp, getCommentForRequest(req));
	}

	public static Object unlike(Request req, Response resp, Account self, ApplicationContext ctx){
		return UserInteractionsRoutes.setLiked(req, resp, self, ctx, getCommentForRequest(req), false);
	}

	public static Object likePopover(Request req, Response resp){
		return UserInteractionsRoutes.likePopover(req, resp, getCommentForRequest(req));
	}

	public static Object likeList(Request req, Response resp){
		return UserInteractionsRoutes.likeList(req, resp, getCommentForRequest(req));
	}

	public static Object comment(Request req, Response resp){
		User self=null;
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			self=info.account.user;
		}
		ApplicationContext ctx=context(req);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		CommentableContentObject parent=context(req).getCommentsController().getCommentParent(self, comment);
		if(parent instanceof BoardTopic){
			int index=ctx.getCommentsController().getCommentIndexForFlatView(comment);
			resp.redirect(parent.getURL()+"?offset="+(index-(index%20))+"#comment"+comment.getIDString());
		}else{
			// TODO do this more correctly to make sure the right page of a long comment thread is shown
			resp.redirect(parent.getURL()+"#comment"+comment.getIDString());
		}
		return "";
	}
}
