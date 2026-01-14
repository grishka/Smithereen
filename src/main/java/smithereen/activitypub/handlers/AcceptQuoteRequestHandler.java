package smithereen.activitypub.handlers;

import java.net.URI;
import java.util.Objects;

import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.QuoteRequest;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignUser;
import smithereen.model.Post;

public class AcceptQuoteRequestHandler extends ActivityTypeHandler<ForeignUser, Accept, QuoteRequest>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Accept activity, QuoteRequest object){
		URI result=null;
		if(activity.result!=null && !activity.result.isEmpty()){
			result=activity.result.getFirst().link;
		}
		if(result==null)
			throw new BadRequestException("result is required");
		URI instrument=object.instrument==null ? null : object.instrument.link;
		if(instrument==null)
			throw new BadRequestException("instrument is required");
		if(object.object==null)
			throw new BadRequestException("object.object is required");
		if(!Config.isLocal(instrument)){
			LOG.warn("Received an Accept{QuoteRequest} from {} with non-local `instrument`: {}", actor.activityPubID, instrument);
			return;
		}
		Post post=context.appContext.getObjectLinkResolver().resolveLocally(instrument, Post.class);
		if(post.repostOf==0 || post.flags.contains(Post.Flag.MASTODON_STYLE_REPOST)){
			LOG.warn("Received an Accept{QuoteRequest} from {} for a post that isn't a quote repost: {}", actor.activityPubID, instrument);
			return;
		}
		if(post.mastodonQuoteAuth!=null){
			LOG.debug("Post {} already has quote authorization {}, ignoring repeated Accept{QuoteRequest}", instrument, post.mastodonQuoteAuth);
			return;
		}
		Post reposted=context.appContext.getWallController().getPostOrThrow(post.repostOf);
		if(!Objects.equals(reposted.getActivityPubID(), object.object.link)){
			LOG.warn("object.object {} in Accept{QuoteRequest} from {} does not match reposted post ID {}", object.object.link, actor.activityPubID, reposted.getActivityPubID());
			return;
		}
		if(reposted.authorID!=actor.id){
			LOG.warn("Reposted post author ID {} does not match Accept{QuoteRequest} actor {}", reposted.authorID, actor.id);
			return;
		}
		context.appContext.getWallController().setPostQuoteAuthorization(post, result);
	}
}
