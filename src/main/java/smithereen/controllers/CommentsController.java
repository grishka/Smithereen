package smithereen.controllers;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.CommentViewType;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.storage.CommentStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class CommentsController{
	private static final Logger LOG=LoggerFactory.getLogger(CommentsController.class);

	private final ApplicationContext context;

	public CommentsController(ApplicationContext context){
		this.context=context;
	}

	private void enforceCommentCreationPrivacy(User self, CommentableContentObject parent, OwnerAndAuthor oaa){
		assert parent.getOwnerID()==oaa.owner().getOwnerID() && parent.getAuthorID()==oaa.author().id;
		switch(parent){
			case Photo photo -> {
				PhotoAlbum album=context.getPhotosController().getAlbum(photo.albumID, self);
				if(oaa.owner() instanceof User ownerUser)
					context.getPrivacyController().enforceUserPrivacy(self, ownerUser, album.commentPrivacy, false);
				else if(album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING))
					throw new UserActionNotAllowedException();
			}
			case BoardTopic topic -> {
				if(!context.getBoardController().canPostInTopic(self, topic))
					throw new UserActionNotAllowedException();
			}
		}
	}

	public Comment createComment(@NotNull User self, @NotNull CommentableContentObject parent, @Nullable Comment inReplyTo,
								 @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs, @NotNull Map<String, String> attachAltTexts){
		return createComment(self, parent, inReplyTo, textSource, sourceFormat, contentWarning, attachmentIDs, attachAltTexts, false);
	}

	Comment createComment(@NotNull User self, @NotNull CommentableContentObject parent, @Nullable Comment inReplyTo,
								 @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs, @NotNull Map<String, String> attachAltTexts,
								 boolean skipFederation){
		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(parent);
		if(context.getPrivacyController().isUserBlocked(self, oaa.owner()))
			throw new UserActionNotAllowedException();
		CommentParentObjectID parentID=parent.getCommentParentID();

		enforceCommentCreationPrivacy(self, parent, oaa);

		if(inReplyTo!=null && !inReplyTo.parentObjectID.equals(parentID))
			inReplyTo=null;
		if(textSource.trim().isEmpty() && attachmentIDs.isEmpty())
			throw new BadRequestException("Empty comment");

		final HashSet<User> mentionedUsers=new HashSet<>();
		try{
			String text=context.getWallController().preparePostText(textSource, mentionedUsers, inReplyTo!=null ? inReplyTo.authorID : 0, sourceFormat);

			int maxAttachments=parentID.type().getMaxAttachments();
			int attachmentCount=0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();
				MediaStorageUtils.fillAttachmentObjects(context, self, attachObjects, attachmentIDs, attachAltTexts, attachmentCount, maxAttachments);
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.getFirst()).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}

			if(contentWarning!=null){
				contentWarning=contentWarning.trim();
				if(contentWarning.isEmpty())
					contentWarning=null;
			}

			if(text.isEmpty() && StringUtils.isEmpty(attachments))
				throw new BadRequestException("Empty comment");

			List<Long> replyKey=inReplyTo==null ? null : inReplyTo.getReplyKeyForReplies();

			long id=CommentStorage.createComment(self.id, parent.getOwnerID(), parentID, text, new FormattedTextSource(textSource, sourceFormat), replyKey,
					mentionedUsers.stream().map(u->u.id).collect(Collectors.toSet()), attachments, contentWarning);
			Comment comment=Objects.requireNonNull(CommentStorage.getComment(id));

			if(comment.attachments!=null){
				for(ActivityPubObject att:comment.attachments){
					if(att instanceof LocalImage li){
						MediaStorage.createMediaFileReference(li.fileID, comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT, comment.getOwnerID());
					}
				}
			}

			context.getNotificationsController().createNotificationsForObject(comment);

			if(inReplyTo!=null){
				User parentAuthor=context.getUsersController().getUserOrThrow(inReplyTo.authorID);
				context.getFriendsController().incrementHintsRank(self, parentAuthor, 5);
			}
			if(oaa.owner() instanceof User u && (inReplyTo==null || inReplyTo.authorID!=u.id)){
				context.getFriendsController().incrementHintsRank(self, u, 3);
			}else if(oaa.owner() instanceof Group g){
				context.getGroupsController().incrementHintsRank(self, g, 3);
			}

			if(!skipFederation){
				if(oaa.owner() instanceof ForeignActor){
					context.getActivityPubWorker().sendCreateComment(self, comment, parent);
				}else{
					context.getActivityPubWorker().sendAddComment(oaa.owner(), comment, parent);
				}
			}

			return comment;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<CommentParentObjectID, PaginatedList<CommentViewModel>> getCommentsForFeed(Collection<CommentParentObjectID> ids, boolean flat, int limit){
		if(ids.isEmpty())
			return Map.of();
		try{
			Map<CommentParentObjectID, PaginatedList<CommentViewModel>> comments=CommentStorage.getCommentsForFeed(ids, flat, limit)
					.entrySet()
					.stream()
					.map(e->new Pair<>(e.getKey(), CommentViewModel.wrap(e.getValue())))
					.collect(Collectors.toMap(Pair::first, Pair::second));
			if(flat)
				fillInParentAuthors(comments.values().stream().flatMap(pl->pl.list.stream()).toList(), null);
			return comments;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Comment getCommentIgnoringPrivacy(long id){
		try{
			Comment comment=CommentStorage.getComment(id);
			if(comment==null)
				throw new ObjectNotFoundException();
			return comment;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, Comment> getCommentsIgnoringPrivacy(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return CommentStorage.getComments(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<CommentViewModel> getCommentsWithMaxID(CommentableContentObject parent, long maxID, int count, boolean flat){
		try{
			PaginatedList<CommentViewModel> comments=CommentViewModel.wrap(CommentStorage.getCommentsWithMaxID(parent.getCommentParentID(), maxID, count, flat));
			if(flat)
				fillInParentAuthors(comments.list, null);
			return comments;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<CommentViewModel> getComments(CommentableContentObject parent, List<Long> key, int primaryOffset, int primaryCount, int secondaryCount, CommentViewType type){
		try{
			Comment threadParent=key.isEmpty() ? null : CommentStorage.getComment(key.getLast());

			// For two-level and flat, if we're getting replies to a comment, always treat them as a flat list
			if((type==CommentViewType.TWO_LEVEL && !key.isEmpty()) || type==CommentViewType.FLAT){
				PaginatedList<CommentViewModel> posts=CommentViewModel.wrap(CommentStorage.getCommentsFlat(parent.getCommentParentID(), key, primaryOffset, primaryCount));
				fillInParentAuthors(posts.list, threadParent);
				return posts;
			}
			CommentStorage.ThreadedReplies tr=CommentStorage.getRepliesThreaded(parent.getCommentParentID(), key, primaryOffset, primaryCount, secondaryCount, type==CommentViewType.TWO_LEVEL);

			List<CommentViewModel> posts=tr.posts().stream().map(CommentViewModel::new).toList();
			List<CommentViewModel> replies=tr.replies().stream().map(CommentViewModel::new).toList();
			Map<Long, CommentViewModel> postMap=Stream.of(posts, replies).flatMap(List::stream).collect(Collectors.toMap(p->p.post.id, Function.identity()));

			for(CommentViewModel post:replies){
				if(post.post.getReplyLevel()>key.size()){
					CommentViewModel parentVM=switch(type){
						case THREADED, TWO_LEVEL -> postMap.get(post.post.replyKey.getLast());
						case FLAT -> throw new IllegalArgumentException();
					};
					if(parentVM!=null){
						CommentViewModel realParent=postMap.get(post.post.replyKey.getLast());
						if(realParent!=null)
							post.parentAuthorID=realParent.post.authorID;
						parentVM.repliesObjects.add(post);
					}
				}
			}
			if(threadParent!=null){
				for(CommentViewModel post: posts){
					post.parentAuthorID=threadParent.authorID;
				}
			}

			return new PaginatedList<>(posts, tr.total(), primaryOffset, primaryCount);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteComment(Actor self, Comment comment){
		if(comment.ownerID>0 && self.getOwnerID()!=comment.ownerID && self.getOwnerID()!=comment.authorID)
			throw new UserActionNotAllowedException();
		if(comment.ownerID<0){
			if(self instanceof User user && comment.authorID!=user.id)
				context.getGroupsController().enforceUserAdminLevel(context.getGroupsController().getGroupOrThrow(-comment.ownerID), user, Group.AdminLevel.MODERATOR);
			else if(self instanceof Group group && group.getOwnerID()!=comment.ownerID)
				throw new UserActionNotAllowedException();
		}
		CommentableContentObject parent=self instanceof User user ? getCommentParent(user, comment) : getCommentParentIgnoringPrivacy(comment);
		if(parent instanceof BoardTopic topic && topic.firstCommentID==comment.id)
			if(self instanceof ForeignUser){
				context.getBoardController().deleteTopic(topic);
				return;
			}else{
				throw new UserActionNotAllowedException("Can't delete first comment in a board topic");
			}
		try{
			CommentStorage.deleteComment(comment);
			if(comment.isLocal() && comment.attachments!=null){
				MediaStorage.deleteMediaFileReferences(comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT);
			}
			LikeStorage.deleteAllLikesForObject(comment.id, Like.ObjectType.COMMENT);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		context.getNotificationsController().deleteNotificationsForObject(comment);

		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(comment);
		if(self.getOwnerID()==comment.authorID){ // User deleted their own comment, send as Delete
			if(!(oaa.author() instanceof ForeignUser))
				context.getActivityPubWorker().sendDeleteComment(oaa.author(), comment, parent);
		}else if(!(oaa.owner() instanceof ForeignActor)){ // Owner deleted someone else's comment, send as Remove
			context.getActivityPubWorker().sendRemoveComment(oaa.owner(), comment, parent);
		}
	}

	public void deleteCommentsForObject(CommentableContentObject parent){
		try{
			CommentParentObjectID parentID=parent.getCommentParentID();
			for(List<Long> commentIDs=CommentStorage.getCommentIDsForDeletion(parentID);!commentIDs.isEmpty();commentIDs=CommentStorage.getCommentIDsForDeletion(parentID)){
				MediaStorage.deleteMediaFileReferences(commentIDs, MediaFileReferenceType.COMMENT_ATTACHMENT);
				LikeStorage.deleteAllLikesForObjects(commentIDs, Like.ObjectType.COMMENT);
				CommentStorage.deleteComments(commentIDs);
			}
			CommentStorage.deleteCommentBookmarks(parentID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public CommentableContentObject getCommentParentIgnoringPrivacy(Comment comment){
		return switch(comment.parentObjectID.type()){
			case PHOTO -> context.getPhotosController().getPhotoIgnoringPrivacy(comment.parentObjectID.id());
			case BOARD_TOPIC -> context.getBoardController().getTopicIgnoringPrivacy(comment.parentObjectID.id());
		};
	}

	public CommentableContentObject getCommentParent(User self, Comment comment){
		CommentableContentObject obj=getCommentParentIgnoringPrivacy(comment);
		context.getPrivacyController().enforceObjectPrivacy(self, obj);
		return obj;
	}

	public PostSource getCommentSource(Comment comment){
		try{
			return CommentStorage.getCommentSource(comment.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	@NotNull
	public Comment editComment(@NotNull User self, Comment comment, @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs, @NotNull Map<String, String> attachAltTexts){
		try{
			CommentableContentObject parent=getCommentParent(self, comment);
			if(textSource.isEmpty() && attachmentIDs.isEmpty())
				throw new BadRequestException("Empty post");

			Comment replyTo=comment.replyKey.isEmpty() ? null : getCommentIgnoringPrivacy(comment.replyKey.getLast());
			HashSet<User> mentionedUsers=new HashSet<>();
			String text=context.getWallController().preparePostText(textSource, mentionedUsers, replyTo!=null ? replyTo.authorID : 0, sourceFormat);

			int maxAttachments=comment.parentObjectID.type().getMaxAttachments();
			int attachmentCount=0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();

				ArrayList<String> newlyAddedAttachments=new ArrayList<>(attachmentIDs);
				if(comment.attachments!=null){
					for(ActivityPubObject att:comment.attachments){
						if(att instanceof LocalImage li){
							String localID=li.fileRecord.id().getIDForClient();
							if(!newlyAddedAttachments.remove(localID)){
								LOG.debug("Deleting attachment: {}", localID);
								MediaStorage.deleteMediaFileReference(comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT, li.fileID);
							}else{
								li.name=attachAltTexts.get(li.getLocalID());
								attachObjects.add(li);
							}
						}else{
							attachObjects.add(att);
						}
					}
				}

				if(!newlyAddedAttachments.isEmpty()){
					MediaStorageUtils.fillAttachmentObjects(context, self, attachObjects, newlyAddedAttachments, attachAltTexts, attachmentCount, maxAttachments);
					for(ActivityPubObject att:attachObjects){
						if(att instanceof LocalImage li && newlyAddedAttachments.contains(li.fileRecord.id().getIDForClient())){
							MediaStorage.createMediaFileReference(li.fileID, comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT, comment.ownerID);
						}
					}
				}
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.getFirst()).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}

			CommentStorage.updateComment(comment.id, text, textSource, sourceFormat, mentionedUsers, attachments, contentWarning);

			comment=getCommentIgnoringPrivacy(comment.id);
			context.getActivityPubWorker().sendUpdateComment(self, comment, parent);

			return comment;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void fillInParentAuthors(List<CommentViewModel> posts, Comment threadParent) throws SQLException{
		HashMap<Long, Integer> authorIDs=new HashMap<>();
		if(threadParent!=null)
			authorIDs.put(threadParent.id, threadParent.authorID);
		for(CommentViewModel p:posts){
			authorIDs.put(p.post.id, p.post.authorID);
		}
		HashSet<Long> needAuthors=new HashSet<>();
		for(CommentViewModel p:posts){
			if(p.post.replyKey==null || p.post.replyKey.isEmpty())
				continue;
			int author=authorIDs.getOrDefault(p.post.replyKey.getLast(), 0);
			if(author!=0){
				p.parentAuthorID=author;
			}else{
				needAuthors.add(p.post.replyKey.getLast());
			}
		}
		if(!needAuthors.isEmpty()){
			Map<Long, Integer> moreAuthorIDs=CommentStorage.getCommentAuthors(needAuthors);
			for(CommentViewModel p:posts){
				if(p.parentAuthorID==0 && p.post.replyKey!=null && !p.post.replyKey.isEmpty()){
					p.parentAuthorID=moreAuthorIDs.getOrDefault(p.post.replyKey.getLast(), 0);
				}
			}
		}
	}

	public PaginatedList<Comment> getPhotoAlbumComments(PhotoAlbum album, int offset, int count){
		try{
			return CommentStorage.getPhotoAlbumComments(album.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Set<URI> getPhotoAlbumCommentIDs(PhotoAlbum album, Collection<URI> filter){
		if(filter.isEmpty())
			return Set.of();
		try{
			Set<URI> foreignIDs=filter.stream().filter(id->!Config.isLocal(id)).collect(Collectors.toSet());
			Set<Long> localIDs=filter.stream()
					.filter(Config::isLocal)
					.map(ObjectLinkResolver::getObjectIdFromLocalURL)
					.filter(id->id!=null && id.type()==ObjectLinkResolver.ObjectType.COMMENT)
					.map(ObjectLinkResolver.ObjectTypeAndID::id)
					.collect(Collectors.toSet());
			HashSet<URI> res=new HashSet<>();
			if(!foreignIDs.isEmpty()){
				res.addAll(CommentStorage.getPhotoAlbumForeignComments(album.id, foreignIDs));
			}
			if(!localIDs.isEmpty()){
				CommentStorage.getPhotoAlbumLocalComments(album.id, localIDs)
						.stream()
						.map(id->Config.localURI("/comments/"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT)))
						.forEach(res::add);
			}
			return res;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Set<URI> getObjectCommentIDs(CommentableContentObject obj, Collection<URI> filter){
		if(filter.isEmpty())
			return Set.of();
		try{
			Set<URI> foreignIDs=filter.stream().filter(id->!Config.isLocal(id)).collect(Collectors.toSet());
			Set<Long> localIDs=filter.stream()
					.filter(Config::isLocal)
					.map(ObjectLinkResolver::getObjectIdFromLocalURL)
					.filter(id->id!=null && id.type()==ObjectLinkResolver.ObjectType.COMMENT)
					.map(ObjectLinkResolver.ObjectTypeAndID::id)
					.collect(Collectors.toSet());
			HashSet<URI> res=new HashSet<>();
			if(!foreignIDs.isEmpty()){
				res.addAll(CommentStorage.getObjectForeignComments(obj.getCommentParentID(), foreignIDs));
			}
			if(!localIDs.isEmpty()){
				CommentStorage.getObjectLocalComments(obj.getCommentParentID(), localIDs)
						.stream()
						.map(id->Config.localURI("/comments/"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT)))
						.forEach(res::add);
			}
			return res;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Comment> getCommentReplies(CommentableContentObject parent, Comment comment, int offset, int count){
		try{
			return CommentStorage.getCommentReplies(parent.getCommentParentID(), comment!=null ? comment.getReplyKeyForReplies() : null, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putOrUpdateForeignComment(Comment comment){
		User author=context.getUsersController().getUserOrThrow(comment.authorID);
		CommentableContentObject parent=getCommentParent(author, comment);
		boolean isNew=false;
		if(comment.id==0){
			enforceCommentCreationPrivacy(author, parent, context.getWallController().getContentAuthorAndOwner(parent));
			isNew=true;
		}
		try{
			CommentStorage.putOrUpdateForeignComment(comment);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		if(isNew)
			context.getNotificationsController().createNotificationsForObject(comment);
	}

	public long getCommentIDByActivityPubID(URI id){
		try{
			long lid=CommentStorage.getCommentIdByActivityPubId(id);
			return Math.max(lid, 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Long, CommentViewModel> getUserReplies(User self, Collection<List<Long>> replyKeys){
		if(replyKeys.isEmpty())
			return Map.of();
		try{
			List<Comment> comments=CommentStorage.getUserReplies(self.id, replyKeys);
			return comments.stream().collect(Collectors.toMap(p->p.replyKey.getLast(), CommentViewModel::new, (p1, p2)->p1.post.id>p2.post.id ? p1 : p2));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getCommentIndexForFlatView(Comment comment){
		try{
			return CommentStorage.getCommentIndex(comment.parentObjectID, comment.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
