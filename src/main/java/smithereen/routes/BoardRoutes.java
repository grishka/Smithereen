package smithereen.routes;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.board.BoardTopic;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class BoardRoutes{
	private static Group getGroup(Request req){
		return context(req).getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
	}

	private static BoardTopic getTopic(Request req){
		Account self=currentUserAccount(req);
		return context(req).getBoardController().getTopic(self==null ? null : self.user, XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.BOARD_TOPIC));
	}

	public static Object createTopicForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		ctx.getBoardController().enforceTopicCreationPermission(self.user, group);
		Templates.addJsLangForNewPostForm(req);
		return new RenderedTemplateResponse("board_create_topic_form", req)
				.pageTitle(group.name+" | "+lang(req).get("board_new_topic_title"))
				.headerBack(group)
				.with("group", group);
	}

	public static Object createTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "title");
		String title=req.queryParams("title");
		Group group=getGroup(req);
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
		BoardTopic topic=ctx.getBoardController().createTopic(self.user, group, title, text, self.prefs.textFormat, contentWarning, attachments, attachmentAltTexts);
		return ajaxAwareRedirect(req, resp, "/topics/"+XTEA.encodeObjectID(topic.id, ObfuscatedObjectIDType.BOARD_TOPIC));
	}

	public static Object topic(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Account self=currentUserAccount(req);

		BoardTopic topic=getTopic(req);
		Group group=ctx.getGroupsController().getGroupOrThrow(topic.groupID);
		int offset;
		if(req.queryParams("last")!=null){
			offset=topic.numComments-(topic.numComments%20);
		}else{
			offset=offset(req);
		}
		PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getComments(topic, List.of(), offset, 20, 0, CommentViewType.FLAT);
		HashSet<Integer> needUsers=new HashSet<>();
		needUsers.add(topic.authorID);
		CommentViewModel.collectUserIDs(comments.list, needUsers);
		List<CommentViewModel> allComments=new ArrayList<>();
		for(CommentViewModel cvm:comments.list){
			allComments.add(cvm);
			cvm.getAllReplies(allComments);
		}
		Map<Long, UserInteractions> commentsInteractions=ctx.getUserInteractionsController().getUserInteractions(allComments.stream().map(vm->vm.post).toList(), self!=null ? self.user : null);
		Group.AdminLevel adminLevel=self==null ? Group.AdminLevel.REGULAR : ctx.getGroupsController().getMemberAdminLevel(group, self.user);

		RenderedTemplateResponse model=new RenderedTemplateResponse("board_topic", req)
				.with("users", ctx.getUsersController().getUsers(needUsers))
				.paginate(comments)
				.with("commentInteractions", commentsInteractions)
				.with("commentViewType", CommentViewType.FLAT)
				.with("maxReplyDepth", 0)
				.with("topic", topic)
				.with("group", group)
				.with("canComment", self!=null && ctx.getBoardController().canPostInTopic(self.user, topic))
				.with("canEditTopic", self!=null && (adminLevel.isAtLeast(Group.AdminLevel.MODERATOR) || topic.authorID==self.user.id))
				.headerBack(group)
				.pageTitle(topic.title+" | "+group.name);

		String msg=req.session().attribute("boardTopicMessage"+topic.id);
		if(msg!=null){
			req.session().removeAttribute("boardTopicMessage"+topic.id);
			Lang l=lang(req);
			model.with("message", l.get("board_topic_"+msg+"_title"))
					.with("messageSubtitle", l.get("board_topic_"+msg+"_subtitle"));
		}

		return model;
	}

	public static Object groupTopics(Request req, Response resp){
		Group group=getGroup(req);
		ApplicationContext ctx=context(req);
		PaginatedList<BoardTopic> topics=ctx.getBoardController().getTopics(group, offset(req), 40);
		Account self=currentUserAccount(req);
		Group.AdminLevel adminLevel=Group.AdminLevel.REGULAR;
		if(self!=null)
			adminLevel=ctx.getGroupsController().getMemberAdminLevel(group, self.user);
		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("board_topic_list", req)
				.pageTitle(l.get("group_board")+" | "+group.name)
				.mobileToolbarTitle(l.get("group_board"))
				.headerBack(group)
				.paginate(topics)
				.with("group", group)
				.with("users", ctx.getUsersController().getUsers(topics.list.stream().map(t->t.lastCommentAuthorID).collect(Collectors.toSet())))
				.with("canCreateTopics", self!=null && (group.boardState==GroupFeatureState.ENABLED_OPEN || (group.boardState==GroupFeatureState.ENABLED_RESTRICTED && adminLevel.isAtLeast(Group.AdminLevel.MODERATOR))));
		if(self!=null){
			String msg=req.session().attribute("groupBoardMessage"+group.id);
			if(msg!=null){
				req.session().removeAttribute("groupBoardMessage"+group.id);
				if(msg.equals("topicDeleted")){
					model.with("message", l.get("board_topic_deleted")).with("messageSubtitle", l.get("board_topic_deleted_subtitle"));
				}
			}
		}
		return model;
	}

	public static Object deleteTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		BoardTopic topic=getTopic(req);
		Group group=ctx.getGroupsController().getGroupOrThrow(topic.groupID);
		ctx.getBoardController().deleteTopic(self.user, topic);
		req.session().attribute("groupBoardMessage"+group.id, "topicDeleted");
		return ajaxAwareRedirect(req, resp, "/groups/"+group.id+"/board");
	}
	
	public static Object renameTopicForm(Request req, Response resp, Account self, ApplicationContext ctx){
		BoardTopic topic=getTopic(req);
		return wrapForm(req, resp, "board_rename_topic_form", "/topics/"+topic.getIdString()+"/rename", lang(req).get("board_editing_topic"), "save", "renameTopic", List.of("title"), k->topic.title, null);
	}

	public static Object renameTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "title");
		BoardTopic topic=getTopic(req);

		ctx.getBoardController().renameTopic(self.user, topic, req.queryParams("title"));

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	private static Object setTopicClosed(Request req, Response resp, Account self, ApplicationContext ctx, boolean closed){
		BoardTopic topic=getTopic(req);

		ctx.getBoardController().setTopicClosed(self.user, topic, closed);
		req.session().attribute("boardTopicMessage"+topic.id, closed ? "locked" : "unlocked");

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object closeTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		return setTopicClosed(req, resp, self, ctx, true);
	}

	public static Object openTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		return setTopicClosed(req, resp, self, ctx, false);
	}

	private static Object setTopicPinned(Request req, Response resp, Account self, ApplicationContext ctx, boolean pinned){
		BoardTopic topic=getTopic(req);

		ctx.getBoardController().setTopicPinned(self.user, topic, pinned);
		req.session().attribute("boardTopicMessage"+topic.id, pinned ? "pinned" : "unpinned");

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object pinTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		return setTopicPinned(req, resp, self, ctx, true);
	}

	public static Object unpinTopic(Request req, Response resp, Account self, ApplicationContext ctx){
		return setTopicPinned(req, resp, self, ctx, false);
	}
}
