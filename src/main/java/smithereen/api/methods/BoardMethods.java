package smithereen.api.methods;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiBoardTopic;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiUser;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.board.BoardTopic;
import smithereen.model.board.BoardTopicsSortOrder;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableObjectType;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class BoardMethods{
	private static final Map<String, BoardTopicsSortOrder> ORDER_VALUES=Map.of(
			"updated_desc", BoardTopicsSortOrder.UPDATED_DESC,
			"updated_asc", BoardTopicsSortOrder.UPDATED_ASC,
			"created_desc", BoardTopicsSortOrder.CREATED_DESC,
			"created_asc", BoardTopicsSortOrder.CREATED_ASC
	);

	public static Object getTopics(ApplicationContext ctx, ApiCallContext actx){
		int groupID=actx.requireParamIntPositive("group_id");
		Group group=ctx.getGroupsController().getGroupOrThrow(groupID);
		ctx.getPrivacyController().enforceUserAccessToGroupContent(actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null, group);
		BoardTopicsSortOrder order=actx.optParamEnum("order", ORDER_VALUES, BoardTopicsSortOrder.UPDATED_DESC);
		PaginatedList<BoardTopic> rawTopics=ctx.getBoardController().getTopicsIgnoringPrivacy(group, actx.getOffset(), actx.getCount(40, 100), order);
		List<ApiBoardTopic> topics=rawTopics.list.stream().map(ApiBoardTopic::new).toList();

		populateCommentPreviews(ctx, actx, rawTopics.list, topics);

		if(actx.optParamBoolean("extended")){
			HashSet<Integer> needUsers=new HashSet<>();
			for(BoardTopic t:rawTopics.list){
				needUsers.add(t.authorID);
				needUsers.add(t.lastCommentAuthorID);
			}
			ApiPaginatedListWithActors<ApiBoardTopic> list=new ApiPaginatedListWithActors<>(rawTopics.total, topics);
			list.profiles=ApiUtils.getUsers(needUsers, ctx, actx);
			return list;
		}
		return new ApiPaginatedList<>(rawTopics.total, topics);
	}

	public static Object getTopicsById(ApplicationContext ctx, ApiCallContext actx){
		Set<Long> ids=actx.requireCommaSeparatedStringSet("topic_ids")
				.stream()
				.map(id->XTEA.decodeObjectID(id, ObfuscatedObjectIDType.BOARD_TOPIC))
				.collect(Collectors.toSet());
		List<BoardTopic> rawTopics;
		try{
			rawTopics=new ArrayList<>(ctx.getBoardController().getTopicsIgnoringPrivacy(ids).values());
		}catch(ObjectNotFoundException x){
			rawTopics=List.of();
		}
		boolean extended=actx.optParamBoolean("extended");

		record TopicsResponse(List<ApiBoardTopic> items, List<ApiUser> profiles){}
		if(rawTopics.isEmpty())
			return extended ? new TopicsResponse(List.of(), List.of()) : List.of();

		Set<Integer> needGroups=rawTopics.stream().map(t->t.groupID).collect(Collectors.toSet());
		User self=actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null;
		Set<Integer> inaccessibleGroups=ctx.getGroupsController().getGroupsByIdAsList(needGroups)
				.stream()
				.filter(g->!ctx.getPrivacyController().canUserAccessGroupContent(self, g))
				.map(g->g.id)
				.collect(Collectors.toSet());
		rawTopics.removeIf(t->inaccessibleGroups.contains(t.groupID));

		List<ApiBoardTopic> topics=rawTopics.stream().map(ApiBoardTopic::new).toList();

		populateCommentPreviews(ctx, actx, rawTopics, topics);

		List<ApiUser> users=null;
		if(extended){
			HashSet<Integer> needUsers=new HashSet<>();
			for(BoardTopic t:rawTopics){
				needUsers.add(t.authorID);
				needUsers.add(t.lastCommentAuthorID);
			}
			users=ApiUtils.getUsers(needUsers, ctx, actx);
			return new TopicsResponse(topics, users);
		}
		return topics;
	}

	private static void populateCommentPreviews(ApplicationContext ctx, ApiCallContext actx, List<BoardTopic> rawTopics, List<ApiBoardTopic> topics){
		Map<Long, Comment> comments;
		String previewMode=actx.optParamString("preview");
		if("first".equals(previewMode)){
			comments=ctx.getBoardController().getFirstOrLastComments(rawTopics.stream().map(t->t.id).collect(Collectors.toSet()), true, false);
		}else if("last".equals(previewMode)){
			comments=ctx.getBoardController().getFirstOrLastComments(rawTopics.stream().map(t->t.id).collect(Collectors.toSet()), false, false);
		}else{
			comments=null;
		}
		if(comments!=null){
			int previewLength=actx.optParamIntPositive("preview_length", 90);
			for(ApiBoardTopic topic: topics){
				Comment c=comments.get(topic.rawID);
				if(c==null){
					topic.commentPreview="";
				}else{
					String strippedText=TextProcessor.stripHTML(c.text, true);
					topic.commentPreview=previewLength>0 ? TextProcessor.truncateOnWordBoundary(strippedText, previewLength) : strippedText;
				}
			}
		}
	}

	public static Object getComments(ApplicationContext ctx, ApiCallContext actx){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null, topicID);
		return ApiUtils.getObjectComments(ctx, actx, topic);
	}

	public static Object createComment(ApplicationContext ctx, ApiCallContext actx){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null, topicID);
		return ApiUtils.createComment(ctx, actx, topic);
	}

	public static Object getCommentEditSource(ApplicationContext ctx, ApiCallContext actx){
		return ApiUtils.getCommentEditSource(ctx, actx, CommentableObjectType.BOARD_TOPIC);
	}

	public static Object editComment(ApplicationContext ctx, ApiCallContext actx){
		long commentID=XTEA.decodeObjectID(actx.requireParamString("comment_id"), ObfuscatedObjectIDType.COMMENT);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(commentID);
		if(comment.parentObjectID.type()!=CommentableObjectType.BOARD_TOPIC)
			throw new ObjectNotFoundException();
		return ApiUtils.editComment(ctx, actx, comment);
	}

	public static Object deleteComment(ApplicationContext ctx, ApiCallContext actx){
		String commentID=actx.requireParamString("comment_id");
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(commentID, ObfuscatedObjectIDType.COMMENT));
		if(comment.parentObjectID.type()!=CommentableObjectType.BOARD_TOPIC)
			throw new ObjectNotFoundException();
		ctx.getCommentsController().deleteComment(actx.self.user, comment);
		return true;
	}

	public static Object createTopic(ApplicationContext ctx, ApiCallContext actx){
		int groupID=actx.requireParamIntPositive("group_id");
		Group group=ctx.getGroupsController().getGroupOrThrow(groupID);
		ctx.getPrivacyController().enforceUserAccessToGroupContent(actx.hasPermission(ClientAppPermission.GROUPS_READ) ? actx.self.user : null, group);

		String title=actx.requireParamString("title");
		String message=actx.optParamString("message");
		String cw=actx.optParamString("content_warning");
		FormattedTextFormat textFormat=ApiUtils.getTextFormat(actx);

		ApiUtils.InputAttachments attachments=ApiUtils.parseAttachments(ctx, actx, true, false);
		if(StringUtils.isEmpty(message) && attachments.ids().isEmpty())
			throw actx.paramError("both message and attachments are undefined");
		String guid=actx.optParamString("guid");
		if(StringUtils.isNotEmpty(guid)){
			guid=actx.token.getEncodedID()+"|topic|"+guid;
		}
		BoardTopic topic=ctx.getBoardController().createTopic(actx.self.user, group, title, message, textFormat, cw, attachments.ids(), attachments.altTexts(), guid);
		return topic.getIdString();
	}

	public static Object editTopic(ApplicationContext ctx, ApiCallContext actx){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.self.user, topicID);
		String title=actx.requireParamString("title");
		ctx.getBoardController().renameTopic(actx.self.user, topic, title);
		return true;
	}

	public static Object deleteTopic(ApplicationContext ctx, ApiCallContext actx){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.self.user, topicID);
		ctx.getBoardController().deleteTopic(actx.self.user, topic);
		return true;
	}

	public static Object openTopic(ApplicationContext ctx, ApiCallContext actx){
		return setTopicClosed(ctx, actx, false);
	}

	public static Object closeTopic(ApplicationContext ctx, ApiCallContext actx){
		return setTopicClosed(ctx, actx, true);
	}

	private static Object setTopicClosed(ApplicationContext ctx, ApiCallContext actx, boolean closed){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.self.user, topicID);
		ctx.getBoardController().setTopicClosed(actx.self.user, topic, closed);
		return true;
	}

	public static Object pinTopic(ApplicationContext ctx, ApiCallContext actx){
		return setTopicPinned(ctx, actx, true);
	}

	public static Object unpinTopic(ApplicationContext ctx, ApiCallContext actx){
		return setTopicPinned(ctx, actx, false);
	}

	private static Object setTopicPinned(ApplicationContext ctx, ApiCallContext actx, boolean pinned){
		long topicID=XTEA.decodeObjectID(actx.requireParamString("topic_id"), ObfuscatedObjectIDType.BOARD_TOPIC);
		BoardTopic topic=ctx.getBoardController().getTopic(actx.self.user, topicID);
		ctx.getBoardController().setTopicPinned(actx.self.user, topic, pinned);
		return true;
	}
}
