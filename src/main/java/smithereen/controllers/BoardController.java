package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.groups.GroupFeatureState;
import smithereen.storage.BoardStorage;
import smithereen.text.FormattedTextFormat;

public class BoardController{
	private final ApplicationContext context;

	public BoardController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<BoardTopic> getTopicsIgnoringPrivacy(Group group, int offset, int count){
		try{
			return BoardStorage.getGroupTopics(group.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<BoardTopic> getPinnedTopicsIgnoringPrivacy(Group group, int offset, int count){
		try{
			return BoardStorage.getGroupPinnedTopics(group.id, offset, count);
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

			context.getNewsfeedController().putFriendsFeedEntry(self, id, NewsfeedEntry.Type.BOARD_TOPIC);
			context.getNewsfeedController().putGroupsFeedEntry(group, id, NewsfeedEntry.Type.BOARD_TOPIC);

			// TODO federate

			return topic;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long getTopicIDByActivityPubID(URI apID){
		try{
			return BoardStorage.getTopicIDByActivityPubID(apID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long putForeignTopic(BoardTopic topic, NoteOrQuestion firstComment){
		if(topic.id>0)
			throw new IllegalArgumentException("This topic is already stored locally");
		try{
			topic.id=BoardStorage.putForeignTopic(topic);
			Comment comment=firstComment.asNativeComment(context);
			context.getCommentsController().putOrUpdateForeignComment(comment);
			if(!comment.parentObjectID.equals(topic.getCommentParentID()))
				throw new FederationException("Comment doesn't belong to this topic");
			BoardStorage.setTopicFirstCommentID(topic.id, comment.id);
			return topic.id;
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
		context.getNewsfeedController().deleteFriendsFeedEntriesForObject(topic.id, NewsfeedEntry.Type.BOARD_TOPIC);
		context.getNewsfeedController().deleteGroupsFeedEntriesForObject(topic.id, NewsfeedEntry.Type.BOARD_TOPIC);
		// TODO federate
	}

	public void renameTopic(User self, BoardTopic topic, String title){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		if(topic.authorID!=self.id){
			context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}else if(context.getPrivacyController().isUserBlocked(self, group)){
			throw new UserActionNotAllowedException();
		}
		renameTopic(topic, title);
		// TODO federate
	}

	void renameTopic(BoardTopic topic, String title){
		try{
			BoardStorage.renameTopic(topic.id, title);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setTopicClosed(User self, BoardTopic topic, boolean closed){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		setTopicClosed(topic, closed);
		// TODO federate
	}

	void setTopicClosed(BoardTopic topic, boolean closed){
		try{
			BoardStorage.setTopicClosed(topic.id, closed);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setTopicPinned(User self, BoardTopic topic, boolean pinned){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		setTopicPinned(topic, pinned);
		// TODO federate
	}

	void setTopicPinned(BoardTopic topic, boolean pinned){
		try{
			BoardStorage.setTopicPinned(topic.id, pinned);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	void setTopicPinned(BoardTopic topic, Instant pinnedAt){
		try{
			BoardStorage.setTopicPinned(topic.id, pinnedAt);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
