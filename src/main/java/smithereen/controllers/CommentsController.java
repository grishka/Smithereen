package smithereen.controllers;

import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.storage.CommentStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import spark.utils.StringUtils;

public class CommentsController{
	private static final Logger LOG=LoggerFactory.getLogger(CommentsController.class);

	private final ApplicationContext context;

	public CommentsController(ApplicationContext context){
		this.context=context;
	}

	public Comment createComment(@NotNull User self, @NotNull CommentableContentObject parent, @Nullable Comment inReplyTo,
								 @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs){
		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(parent);
		if(context.getPrivacyController().isUserBlocked(self, oaa.owner()))
			throw new UserActionNotAllowedException();
		CommentParentObjectID parentID=parent.getCommentParentID();

		switch(parent){
			case Photo photo -> {
				PhotoAlbum album=context.getPhotosController().getAlbum(photo.albumID, self);
				if(oaa.owner() instanceof User ownerUser)
					context.getPrivacyController().enforceUserPrivacy(self, ownerUser, album.commentPrivacy, false);
				else if(album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING))
					throw new UserActionNotAllowedException();
			}
		}

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
				MediaStorageUtils.fillAttachmentObjects(attachObjects, attachmentIDs, attachmentCount, maxAttachments);
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
			Comment comment=CommentStorage.getComment(id);

			if(comment.attachments!=null){
				for(ActivityPubObject att:comment.attachments){
					if(att instanceof LocalImage li){
						MediaStorage.createMediaFileReference(li.fileID, comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT, comment.getOwnerID());
					}
				}
			}

			// TODO notifications
			// TODO federate

			return comment;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<CommentParentObjectID, PaginatedList<CommentViewModel>> getCommentsForFeed(Collection<CommentParentObjectID> ids, boolean flat, int limit){
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
						case THREADED -> postMap.get(post.post.replyKey.getLast());
						case TWO_LEVEL -> postMap.get(post.post.replyKey.get(1));
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

	public void deleteComment(User self, Comment comment){
		if(comment.ownerID>0 && self.id!=comment.ownerID && self.id!=comment.authorID)
			throw new UserActionNotAllowedException();
		if(comment.ownerID<0 && self.id!=comment.authorID)
			context.getGroupsController().enforceUserAdminLevel(context.getGroupsController().getGroupOrThrow(-comment.ownerID), self, Group.AdminLevel.MODERATOR);
		CommentableContentObject parent=getCommentParent(self, comment);
		try{
			CommentStorage.deleteComment(comment);
			if(comment.isLocal() && comment.attachments!=null){
				MediaStorage.deleteMediaFileReferences(comment.id, MediaFileReferenceType.COMMENT_ATTACHMENT);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		// TODO delete notifications
		// TODO delete likes
		// TODO federate
	}

	public CommentableContentObject getCommentParent(User self, Comment comment){
		CommentableContentObject obj=switch(comment.parentObjectID.type()){
			case PHOTO -> context.getPhotosController().getPhotoIgnoringPrivacy(comment.parentObjectID.id());
		};
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
	public Comment editComment(@NotNull User self, Comment comment, @NotNull String textSource, @NotNull FormattedTextFormat sourceFormat, @Nullable String contentWarning, @NotNull List<String> attachmentIDs){
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
								attachObjects.add(li);
							}
						}else{
							attachObjects.add(att);
						}
					}
				}

				if(!newlyAddedAttachments.isEmpty()){
					MediaStorageUtils.fillAttachmentObjects(attachObjects, newlyAddedAttachments, attachmentCount, maxAttachments);
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

			// TODO federate

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
}
