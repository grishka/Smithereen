package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.controllers.PhotosController;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.storage.utils.Pair;

public class FetchBoardTopicCommentsTask extends ForwardPaginatingCollectionTask{
	private static final Logger LOG=LoggerFactory.getLogger(FetchBoardTopicCommentsTask.class);
	private final ActivityPubWorker apw;
	private final HashMap<URI, Future<Void>> fetchingBoardTopics;
	private final BoardTopic nativeTopic;
	private final ActivityPubBoardTopic topic;

	public FetchBoardTopicCommentsTask(ApplicationContext context, ActivityPubBoardTopic collection, BoardTopic nativeTopic, ActivityPubWorker apw, HashMap<URI, Future<Void>> fetchingBoardTopics){
		super(context, collection);
		topic=collection;
		this.apw=apw;
		this.fetchingBoardTopics=fetchingBoardTopics;
		this.nativeTopic=nativeTopic;
		maxItems=PhotosController.MAX_PHOTOS_PER_ALBUM;
	}

	@Override
	protected void compute(){
		try{
			super.compute();
		}finally{
			synchronized(apw){
				fetchingBoardTopics.remove(collectionID);
			}
		}
	}

	@Override
	protected void doOneCollectionPage(CollectionPage page){
		try{
			for(LinkOrObject lo:page.items){
				NoteOrQuestion comment;
				if(lo.link!=null){
					comment=context.getObjectLinkResolver().resolve(lo.link, NoteOrQuestion.class, true, false, false);
				}else if(lo.object instanceof NoteOrQuestion noq){
					comment=noq;
				}else{
					LOG.debug("Board topic item is of unexpected type {}, skipping", lo.object.getType());
					continue;
				}
				Comment nComment=comment.asNativeComment(context);
				if(!nativeTopic.getCommentParentID().equals(nComment.parentObjectID)){
					LOG.debug("Comment has an unexpected parent object {}", nComment.parentObjectID);
					continue;
				}
				context.getCommentsController().putOrUpdateForeignComment(nComment);
			}
		}catch(Exception x){
			LOG.warn("Error processing photo album page", x);
		}
	}
}
