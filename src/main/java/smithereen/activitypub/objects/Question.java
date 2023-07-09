package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.Post;
import smithereen.exceptions.FederationException;
import smithereen.jsonld.JLD;

public non-sealed class Question extends NoteOrQuestion{
	public List<ActivityPubObject> anyOf, oneOf;
	public int votersCount;
	public boolean nonAnonymous;
	public Instant closed;

	@Override
	public Post asNativePost(ApplicationContext context){
		Post post=super.asNativePost(context);

		Poll poll=new Poll();
		poll.multipleChoice=anyOf!=null;
		poll.question=name;
		poll.numVoters=votersCount;
		poll.anonymous=!nonAnonymous;
		List<ActivityPubObject> opts=anyOf==null ? oneOf : anyOf;
		poll.options=opts.stream().map(ao->{
			if(!(ao instanceof Note n))
				return null;
			PollOption opt=new PollOption();
			opt.activityPubID=ao.activityPubID;
			if(opt.activityPubID!=null)
				ensureHostMatchesID(opt.activityPubID, "option.id");
			opt.text=ao.name;
			if(n.replies!=null && n.replies.object instanceof ActivityPubCollection r){
				opt.numVotes=r.totalItems;
			}
			return opt;
		}).filter(Objects::nonNull).toList();
		poll.activityPubID=activityPubID;
		if(endTime!=null){
			poll.endTime=endTime;
		}else if(closed!=null){
			poll.endTime=closed;
		}
		if(!poll.options.isEmpty())
			post.poll=poll;

		return post;
	}

	@Override
	public String getType(){
		return "Question";
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		oneOf=parseSingleObjectOrArray(obj.get("oneOf"), parserContext);
		anyOf=parseSingleObjectOrArray(obj.get("anyOf"), parserContext);
		votersCount=optInt(obj, "votersCount");
		nonAnonymous=optBoolean(obj, "nonAnonymous");
		closed=tryParseDate(optString(obj, "closed"));

		if(oneOf==null && anyOf==null)
			throw new FederationException("Either oneOf or anyOf is required for a Question");

		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		super.asActivityPubObject(obj, contextCollector);

		if(anyOf!=null)
			obj.add("anyOf", serializeObjectArray(anyOf, contextCollector));
		if(oneOf!=null)
			obj.add("oneOf", serializeObjectArray(oneOf, contextCollector));
		if(closed!=null)
			obj.addProperty("closed", Utils.formatDateAsISO(closed));
		obj.addProperty("votersCount", votersCount);
		obj.addProperty("nonAnonymous", nonAnonymous);

		contextCollector.addAlias("toot", JLD.MASTODON);
		contextCollector.addAlias("sm", JLD.SMITHEREEN);
		contextCollector.addAlias("votersCount", "toot:votersCount");
		contextCollector.addAlias("nonAnonymous", "sm:nonAnonymous");

		return obj;
	}
}
