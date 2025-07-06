package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.groups.GroupFeatureState;
import smithereen.storage.BoardStorage;
import smithereen.text.FormattedTextFormat;

public class BoardController{
	private final ApplicationContext context;

	public BoardController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<BoardTopic> getTopics(Group group, int offset, int count){
		try{
			return BoardStorage.getGroupTopics(group.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public BoardTopic getTopicIgnoringPrivacy(long id){
		try{
			BoardTopic topic=BoardStorage.getTopic(id);
			if(topic==null)
				throw new ObjectNotFoundException();
			return topic;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public BoardTopic getTopic(User self, long id){
		BoardTopic topic=getTopicIgnoringPrivacy(id);
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getPrivacyController().enforceUserAccessToGroupContent(self, group);
		if(group.boardState==GroupFeatureState.DISABLED)
			throw new UserActionNotAllowedException("err_access_content");
		return topic;
	}

	public Map<Long, BoardTopic> getTopicsIgnoringPrivacy(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return BoardStorage.getTopics(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean canCreateTopics(User self, Group group){
		return context.getPrivacyController().canUserAccessGroupContent(self, group) && group.boardState!=GroupFeatureState.DISABLED
				&& !(group.boardState==GroupFeatureState.ENABLED_RESTRICTED && !context.getGroupsController().getMemberAdminLevel(group, self).isAtLeast(Group.AdminLevel.MODERATOR))
				&& !context.getPrivacyController().isUserBlocked(self, group);
	}

	public void enforceTopicCreationPermission(User self, Group group){
		if(!canCreateTopics(self, group))
			throw new UserActionNotAllowedException();
	}

	public boolean canPostInTopic(User self, BoardTopic topic){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		return group.boardState!=GroupFeatureState.DISABLED
				&& !topic.isClosed
				&& context.getPrivacyController().canUserAccessGroupContent(self, group)
				&& !context.getPrivacyController().isUserBlocked(self, group);
	}

	public BoardTopic createTopic(User self, Group group, String title, @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat,
								  @Nullable String contentWarning, @NotNull List<String> attachmentIDs, @NotNull Map<String, String> attachAltTexts){
		enforceTopicCreationPermission(self, group);
		try{
			if(textSource.trim().isEmpty() && attachmentIDs.isEmpty())
				throw new BadRequestException("Empty comment");
			long id=BoardStorage.createTopic(group.id, title, self.id, null, null);
			BoardTopic topic=BoardStorage.getTopic(id);

			Comment comment=context.getCommentsController().createComment(self, topic, null, textSource, sourceFormat, contentWarning, attachmentIDs, attachAltTexts, true);
			topic.numComments=1;
			topic.firstCommentID=comment.id;
			BoardStorage.setTopicFirstCommentID(id, comment.id);

			// TODO federate

			return topic;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteTopic(User self, BoardTopic topic){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		if(topic.authorID!=self.id){
			context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}else if(context.getPrivacyController().isUserBlocked(self, group)){
			throw new UserActionNotAllowedException();
		}
		try{
			BoardStorage.deleteTopic(topic.id);
			context.getCommentsController().deleteCommentsForObject(topic);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		// TODO federate
	}

	public void renameTopic(User self, BoardTopic topic, String title){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		if(topic.authorID!=self.id){
			context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}else if(context.getPrivacyController().isUserBlocked(self, group)){
			throw new UserActionNotAllowedException();
		}
		try{
			BoardStorage.renameTopic(topic.id, title);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		// TODO federate
	}

	public void setTopicClosed(User self, BoardTopic topic, boolean closed){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			BoardStorage.setTopicClosed(topic.id, closed);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		// TODO federate
	}

	public void setTopicPinned(User self, BoardTopic topic, boolean pinned){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		try{
			BoardStorage.setTopicPinned(topic.id, pinned);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		// TODO federate
	}
}
