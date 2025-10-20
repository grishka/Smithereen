package smithereen.api.model;

import java.util.List;
import java.util.Map;

import smithereen.api.ApiCallContext;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.UserInteractions;

public class ApiPoll{
	public int id;
	public int ownerId;
	public long created;
	public String question;
	public int votes;
	public List<Answer> answers;
	public boolean anonymous;
	public boolean multiple;
	public List<Integer> answerIds;
	public long endDate;
	public boolean closed;
	public boolean canEdit;
	public boolean canVote;
	public int authorId;

	public record Answer(int id, String text, int votes, double rate){}

	public ApiPoll(Poll poll, Post parent, Map<Integer, UserInteractions> interactions, ApiCallContext actx){
		id=poll.id;
		ownerId=poll.ownerID;
		created=parent.createdAt.getEpochSecond();
		question=poll.question;
		if(poll.numVoters>0){
			votes=poll.numVoters;
		}else{
			votes=0;
			for(PollOption opt:poll.options){
				votes+=opt.numVotes;
			}
		}
		anonymous=poll.anonymous;
		multiple=poll.multipleChoice;
		endDate=poll.endTime==null ? 0 : poll.endTime.getEpochSecond();
		closed=poll.isExpired();
		canEdit=actx.permissions!=null && actx.permissions.canEditPost(parent);
		answers=poll.options.stream().map(o->new Answer(o.id, o.text, o.numVotes, votes>0 ? o.numVotes/(double)votes*100.0 : 0)).toList();
		authorId=parent.authorID;
		if(actx.self!=null){
			UserInteractions postInteractions=interactions.get(parent.id);
			if(postInteractions!=null){
				canVote=postInteractions.canLike && postInteractions.pollChoices==null;
				if(postInteractions.pollChoices!=null)
					answerIds=postInteractions.pollChoices;
			}
		}
	}
}
