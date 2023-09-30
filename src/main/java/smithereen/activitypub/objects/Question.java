package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
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
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		if(anyOf!=null)
			obj.add("anyOf", serializeObjectArray(anyOf, serializerContext));
		if(oneOf!=null)
			obj.add("oneOf", serializeObjectArray(oneOf, serializerContext));
		if(closed!=null)
			obj.addProperty("closed", Utils.formatDateAsISO(closed));
		obj.addProperty("votersCount", votersCount);
		obj.addProperty("nonAnonymous", nonAnonymous);

		serializerContext.addAlias("toot", JLD.MASTODON);
		serializerContext.addAlias("sm", JLD.SMITHEREEN);
		serializerContext.addAlias("votersCount", "toot:votersCount");
		serializerContext.addAlias("nonAnonymous", "sm:nonAnonymous");

		return obj;
	}
}
