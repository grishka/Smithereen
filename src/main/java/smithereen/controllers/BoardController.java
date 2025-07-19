package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.LocalActivityPubBoardTopic;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
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
	private static final Logger LOG=LoggerFactory.getLogger(BoardController.class);
	private final ApplicationContext context;

	private final LruCache<Long, BoardTopic> topicCache=new LruCache<>(1000);

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
		BoardTopic topic=topicCache.get(id);
		if(topic!=null)
			return topic;
		try{
			topic=BoardStorage.getTopic(id);
			if(topic==null)
				throw new ObjectNotFoundException();
			topicCache.put(id, topic);
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
		if(ids.size()==1){
			long id=ids.iterator().next();
			return Map.of(id, getTopicIgnoringPrivacy(id));
		}
		HashMap<Long, BoardTopic> topics=new HashMap<>();
		Set<Long> remainingIDs=new HashSet<>(ids);
		for(long id:ids){
			BoardTopic topic=topicCache.get(id);
			if(topic!=null){
				topics.put(id, topic);
				remainingIDs.remove(id);
			}
		}
		if(remainingIDs.isEmpty())
			return topics;
		try{
			Map<Long, BoardTopic> moreTopics=BoardStorage.getTopics(remainingIDs);
			topics.putAll(moreTopics);
			for(BoardTopic topic:moreTopics.values()){
				topicCache.put(topic.id, topic);
			}
			return topics;
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

			if(group instanceof ForeignGroup fg){
				LOG.debug("Creating topic remotely in {} on behalf of {}", fg.activityPubID, self.activityPubID);
				try{
					context.getActivityPubWorker().sendCreateBoardTopicRequest(self, fg, topic, comment);
				}catch(FederationException x){
					LOG.debug("Failed to create topic. Deleting the local one", x);
					BoardStorage.deleteTopic(topic.id);
					context.getCommentsController().deleteCommentsForObject(topic);
					throw new UserErrorException(x.getMessage(), x);
				}
				topicCache.remove(id);
				topic=getTopicIgnoringPrivacy(id); // To make sure all fields are set after Accept{TopicCreationRequest}
			}else{
				context.getActivityPubWorker().sendCreateBoardTopic(group, topic, comment);
			}

			topicCache.put(id, topic);
			return topic;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public BoardTopic createTopicWithExistingFirstComment(ForeignUser self, Group group, String title, NoteOrQuestion comment){
		enforceTopicCreationPermission(self, group);
		try{
			long id=BoardStorage.createTopic(group.id, title, self.id, null, null);
			BoardTopic topic=BoardStorage.getTopic(id);

			ActivityPubBoardTopic target=new LocalActivityPubBoardTopic(topic);
			target.activityPubID=topic.getActivityPubID();
			target.attributedTo=group.activityPubID;
			comment.target=target;
			Comment nativeComment=comment.asNativeComment(context);
			context.getCommentsController().putOrUpdateForeignComment(nativeComment);
			BoardStorage.setTopicFirstCommentID(id, nativeComment.id);
			topic.firstCommentID=nativeComment.id;
			topic.numComments=1;

			context.getNewsfeedController().putFriendsFeedEntry(self, id, NewsfeedEntry.Type.BOARD_TOPIC);
			context.getNewsfeedController().putGroupsFeedEntry(group, id, NewsfeedEntry.Type.BOARD_TOPIC);

			return topic;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long getTopicIDByActivityPubID(URI apID){
		if(Config.isLocal(apID)){
			ObjectLinkResolver.ObjectTypeAndID tid=ObjectLinkResolver.getObjectIdFromLocalURL(apID);
			if(tid!=null && tid.type()==ObjectLinkResolver.ObjectType.BOARD_TOPIC)
				return tid.id();
			return -1;
		}
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
			topicCache.put(topic.id, topic);
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
		deleteTopicWithFederation(group, topic);
	}

	void deleteTopicWithFederation(Group group, BoardTopic topic){
		if(group instanceof ForeignGroup){
			Comment firstComment=context.getCommentsController().getCommentIgnoringPrivacy(topic.firstCommentID);
			User author=context.getUsersController().getUserOrThrow(firstComment.authorID);
			if(!(author instanceof ForeignUser)){
				context.getActivityPubWorker().sendDeleteComment(author, firstComment, topic);
			}
		}else{
			context.getActivityPubWorker().sendDeleteBoardTopic(group, topic);
		}
		deleteTopic(topic);
	}

	public void deleteTopic(BoardTopic topic){
		topicCache.remove(topic.id);
		try{
			BoardStorage.deleteTopic(topic.id);
			context.getCommentsController().deleteCommentsForObject(topic);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		context.getNewsfeedController().deleteFriendsFeedEntriesForObject(topic.id, NewsfeedEntry.Type.BOARD_TOPIC);
		context.getNewsfeedController().deleteGroupsFeedEntriesForObject(topic.id, NewsfeedEntry.Type.BOARD_TOPIC);
	}

	public void renameTopic(User self, BoardTopic topic, String title){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		if(topic.authorID!=self.id){
			context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		}else if(context.getPrivacyController().isUserBlocked(self, group)){
			throw new UserActionNotAllowedException();
		}
		title=title.trim();
		String oldTitle=topic.title;
		renameTopic(topic, title);
		topic.title=title;
		if(group instanceof ForeignGroup fg){
			try{
				context.getActivityPubWorker().sendRenameBoardTopicRequest(self, fg, topic, title);
			}catch(FederationException x){
				LOG.debug("Failed to rename topic {}, reverting", topic.id, x);
				renameTopic(topic, oldTitle);
				topic.title=oldTitle;
				throw new UserErrorException(x.getMessage(), x);
			}
		}else{
			context.getActivityPubWorker().sendUpdateBoardTopic(group, topic);
		}
	}

	void renameTopic(BoardTopic topic, String title){
		try{
			BoardStorage.renameTopic(topic.id, title);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		topicCache.put(topic.id, topic);
	}

	public void setTopicClosed(User self, BoardTopic topic, boolean closed){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		setTopicClosed(topic, closed);
		topic.isClosed=closed;
		if(!(group instanceof ForeignGroup))
			context.getActivityPubWorker().sendUpdateBoardTopic(group, topic);
	}

	void setTopicClosed(BoardTopic topic, boolean closed){
		try{
			BoardStorage.setTopicClosed(topic.id, closed);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		topicCache.put(topic.id, topic);
	}

	public void setTopicPinned(User self, BoardTopic topic, boolean pinned){
		Group group=context.getGroupsController().getGroupOrThrow(topic.groupID);
		context.getGroupsController().enforceUserAdminLevel(group, self, Group.AdminLevel.MODERATOR);
		setTopicPinned(topic, pinned);
		topic.isPinned=pinned;
		topic.pinnedAt=pinned ? Instant.now() : null;
		if(!(group instanceof ForeignGroup))
			context.getActivityPubWorker().sendUpdateBoardTopic(group, topic);
	}

	void setTopicPinned(BoardTopic topic, boolean pinned){
		try{
			BoardStorage.setTopicPinned(topic.id, pinned);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		topicCache.put(topic.id, topic);
	}

	void setTopicPinned(BoardTopic topic, Instant pinnedAt){
		try{
			BoardStorage.setTopicPinned(topic.id, pinnedAt);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		topicCache.put(topic.id, topic);
	}

	public void setTopicActivityPubID(BoardTopic topic, URI apID, URI apURL){
		try{
			BoardStorage.setTopicActivityPubID(topic.id, apID, apURL);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		topicCache.remove(topic.id);
	}
}
