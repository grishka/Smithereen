package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.LocalPostNote;
import smithereen.activitypub.objects.LocalPostQuestion;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.QuoteRequest;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.User;

public class QuoteRequestNoteHandler extends ActivityTypeHandler<ForeignUser, QuoteRequest, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, QuoteRequest activity, NoteOrQuestion object){
		if(activity.instrument==null)
			throw new BadRequestException("`instrument` field is required");
		if(!(object instanceof LocalPostNote || object instanceof LocalPostQuestion))
			throw new UserActionNotAllowedException("Not allowed for this object type");

		Post post=object.asNativePost(context.appContext);
		if(!post.isLocal())
			throw new UserActionNotAllowedException("This post isn't local");
		if(post.privacy!=Post.Privacy.PUBLIC || (post.replyKey.isEmpty() && post.ownerID!=post.authorID))
			throw new UserActionNotAllowedException("Not allowed for this post");
		if(!post.replyKey.isEmpty()){
			Post topLevel=context.appContext.getWallController().getPostOrThrow(post.replyKey.getFirst());
			if(topLevel.ownerID!=topLevel.authorID)
				throw new UserActionNotAllowedException("Not allowed for this post");
		}

		NoteOrQuestion repostSource;
		if(activity.instrument.link!=null){
			try{
				repostSource=context.appContext.getObjectLinkResolver().resolve(activity.instrument.link, NoteOrQuestion.class, true, false, false);
			}catch(ObjectNotFoundException x){
				throw new BadRequestException("`instrument` must contain a Note or point to one", x);
			}
		}else if(activity.instrument.object instanceof NoteOrQuestion noq){
			repostSource=noq;
		}else{
			throw new BadRequestException("`instrument` must contain a Note or point to one");
		}
		Post repost=repostSource.asNativePost(context.appContext);
		if(repost.repostOf!=post.id)
			throw new UserActionNotAllowedException("Quote URL does not match `object`");

		boolean isNew=repost.id==0;
		context.appContext.getWallController().loadAndPreprocessRemotePostMentions(repost, repostSource);
		context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(repost, repostSource);
		if(isNew)
			context.appContext.getNotificationsController().createNotificationsForObject(repost);

		User author=context.appContext.getUsersController().getUserOrThrow(post.authorID);
		context.appContext.getActivityPubWorker().sendAcceptQuoteRequest(author, repost, post, actor, activity.activityPubID);
	}
}
