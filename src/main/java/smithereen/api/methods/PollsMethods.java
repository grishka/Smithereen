package smithereen.api.methods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPoll;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.viewmodel.PostViewModel;

public class PollsMethods{
	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		int id=actx.requireParamIntPositive("poll_id");
		Poll poll=ctx.getWallController().getPollByID(id);
		int postID=ctx.getWallController().getPostIDForPoll(poll);
		if(postID==0)
			throw new ObjectNotFoundException();
		Post post=ctx.getWallController().getPostOrThrow(postID);
		ctx.getPrivacyController().enforcePostPrivacy(actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null, post);
		return new ApiPoll(poll, post, ctx.getWallController().getUserInteractions(List.of(new PostViewModel(post)), actx.self.user), actx);
	}

	public static Object addVote(ApplicationContext ctx, ApiCallContext actx){
		int id=actx.requireParamIntPositive("poll_id");
		Poll poll=ctx.getWallController().getPollByID(id);
		Set<Integer> options=actx.requireCommaSeparatedStringSet("answer_ids")
				.stream()
				.map(Utils::safeParseInt)
				.collect(Collectors.toSet());
		if(!poll.multipleChoice && options.size()>1)
			throw actx.paramError("multiple answers specified for a single-choice poll");
		int[] optionsArr=new int[options.size()];
		Set<Integer> validOptionIDs=poll.options.stream().map(o->o.id).collect(Collectors.toSet());
		int i=0;
		for(int optID:options){
			if(!validOptionIDs.contains(optID))
				throw actx.paramError("invalid answer ID "+optID);
			optionsArr[i++]=optID;
		}
		ctx.getWallController().voteInPoll(actx.self.user, poll, optionsArr);
		return true;
	}

	public static Object getVoters(ApplicationContext ctx, ApiCallContext actx){
		int id=actx.requireParamIntPositive("poll_id");
		int optID=actx.requireParamIntPositive("answer_id");
		Poll poll=ctx.getWallController().getPollByID(id);
		int postID=ctx.getWallController().getPostIDForPoll(poll);
		if(postID==0)
			throw new ObjectNotFoundException();
		Post post=ctx.getWallController().getPostOrThrow(postID);
		ctx.getPrivacyController().enforcePostPrivacy(actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null, post);
		if(poll.anonymous)
			throw actx.error(ApiErrorType.ACCESS_DENIED, "this poll is anonymous");

		PollOption opt=null;
		for(PollOption o:poll.options){
			if(o.id==optID){
				opt=o;
				break;
			}
		}
		if(opt==null)
			throw actx.error(ApiErrorType.NOT_FOUND, "invalid answer ID");

		List<Integer> userIDs=ctx.getWallController().getPollOptionVoters(opt, actx.getOffset(), actx.getCount(100, 1000));
		if(actx.hasParam("fields")){
			return new ApiPaginatedList<>(opt.numVotes, ApiUtils.getUsers(userIDs, ctx, actx));
		}
		return new ApiPaginatedList<>(opt.numVotes, userIDs);
	}

	public static Object create(ApplicationContext ctx, ApiCallContext actx){
		Actor owner=ApiUtils.getOwnerOrSelf(ctx, actx, "owner_id");
		String question=actx.requireParamString("question");
		JsonArray rawOpts=actx.optParamJsonArray("answers");
		if(rawOpts==null)
			throw actx.paramError("answers is undefined");
		if(rawOpts.size()<2 || rawOpts.size()>10)
			throw actx.paramError("answers must contain 2 to 10 elements");
		ArrayList<String> opts=new ArrayList<>();
		for(JsonElement el:rawOpts){
			if(!(el instanceof JsonPrimitive jp) || !jp.isString())
				throw actx.paramError("answers must only contain strings, non-string found at index "+opts.size());
			String opt=el.getAsString().strip();
			if(opt.isEmpty())
				throw actx.paramError("answers["+opts.size()+"] is empty");
			opts.add(opt);
		}
		long rawEndTime=actx.optParamLong("end_time");
		Instant endTime=null;
		if(rawEndTime>System.currentTimeMillis()*1000L+60_000L)
			endTime=Instant.ofEpochSecond(rawEndTime);
		return ctx.getWallController().createPoll(actx.self.user, owner, question, opts, actx.optParamBoolean("anonymous"), actx.optParamBoolean("multiple"), endTime);
	}
}
