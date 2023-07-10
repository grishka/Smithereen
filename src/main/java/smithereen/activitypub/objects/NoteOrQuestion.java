package smithereen.activitypub.objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.data.Post;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.exceptions.FederationException;
import spark.utils.StringUtils;

public abstract sealed class NoteOrQuestion extends ActivityPubObject permits Note, Question, NoteTombstone{
	private static final int MAX_MENTIONS=10;
	private static final Logger LOG=LoggerFactory.getLogger(NoteOrQuestion.class);

	public LinkOrObject replies;
	public Boolean sensitive;
	public ActivityPubCollection target;
	public URI likes;

	public Post asNativePost(ApplicationContext context){
		Post post=new Post();

		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		ensureHostMatchesID(attributedTo, "attributedTo");
		post.id=context.getWallController().getPostIDByActivityPubID(activityPubID);

		User author=context.getObjectLinkResolver().resolve(attributedTo, User.class, true, true, false);
		post.authorID=author.id;
		if(inReplyTo!=null){
			Post parent=context.getWallController().getPostOrThrow(inReplyTo);
			post.replyKey=parent.getReplyKeyForReplies();
			post.ownerID=parent.ownerID;
		}else if(target!=null && target.attributedTo!=null){
			Actor owner=context.getObjectLinkResolver().resolve(target.attributedTo, Actor.class, true, true, false);
			if(!Objects.equals(target.attributedTo, owner.getWallURL()))
				throw new FederationException("target.attributedTo points to an unknown collection");
			post.ownerID=owner.getOwnerID();
		}else{
			post.ownerID=author.id;
		}

		// fix for Lemmy (and possibly something else)
		boolean hasBogusURL=url!=null && !url.getHost().equalsIgnoreCase(activityPubID.getHost());

		String text=content;
		if(hasBogusURL)
			text=text+"<p><a href=\""+url+"\">"+url+"</a></p>";
		text=Utils.sanitizeHTML(text);
		post.text=text;
		post.createdAt=published!=null ? published : Instant.now();
		post.updatedAt=updated;
		if(sensitive!=null && sensitive && StringUtils.isNotEmpty(summary)){
			post.contentWarning=summary;
		}

		post.setActivityPubID(activityPubID);
		post.activityPubURL=hasBogusURL || url==null ? activityPubID : url;
		post.activityPubReplies=replies!=null ? replies.getObjectID() : null;
		if(post.activityPubReplies!=null)
			ensureHostMatchesID(post.activityPubReplies, "replies");
		post.attachments=attachment;

		if(tag!=null){
			post.mentionedUserIDs=new HashSet<>();
			int mentionCount=0;
			for(ActivityPubObject obj:tag){
				if(obj instanceof Mention mention){
					try{
						User mentionedUser=context.getObjectLinkResolver().resolve(mention.href, User.class, true, true, false);
						post.mentionedUserIDs.add(mentionedUser.id);
						mentionCount++;
						if(mentionCount==MAX_MENTIONS)
							break;
					}catch(Exception x){
						LOG.debug("Failed to resolve mention for href={}", mention.href, x);
					}
				}
			}
		}

		return post;
	}

	public static NoteOrQuestion fromNativePost(Post post, ApplicationContext context){
		NoteOrQuestion noq;
		if(post.isDeleted()){
			noq=new NoteTombstone();
		}else if(post.poll!=null){
			Question q=new Question();
			q.name=post.poll.question;
			List<ActivityPubObject> opts=post.poll.options.stream().map(opt->{
				Note n=new Note();
				n.name=opt.text;
				n.activityPubID=opt.getActivityPubID(post.getActivityPubID());
				ActivityPubCollection replies=new ActivityPubCollection(false);
				replies.totalItems=opt.numVotes;
				if(!post.poll.anonymous){
					replies.activityPubID=Config.localURI("/activitypub/objects/polls/"+post.poll.id+"/options/"+opt.id+"/votes");
					replies.items=List.of();
				}
				n.replies=new LinkOrObject(replies);
				return (ActivityPubObject)n;
			}).toList();
			if(post.poll.multipleChoice)
				q.anyOf=opts;
			else
				q.oneOf=opts;

			q.nonAnonymous=!post.poll.anonymous;

			if(post.poll.endTime!=null){
				if(post.poll.endTime.isBefore(Instant.now()))
					q.closed=post.poll.endTime;
				else
					q.endTime=post.poll.endTime;
			}

			noq=q;
		}else{
			noq=new Note();
		}

		Set<URI> to=new HashSet<>(), cc=new HashSet<>();
		to.add(ActivityPub.AS_PUBLIC);


		noq.activityPubID=noq.url=post.getActivityPubID();
		if(post.activityPubReplies!=null){
			noq.replies=new LinkOrObject(post.activityPubReplies);
		}else if(post.isLocal()){
			ActivityPubCollection replies=new ActivityPubCollection(false);
			replies.activityPubID=Config.localURI("/posts/"+post.id+"/replies");
			CollectionPage repliesPage=new CollectionPage(false);
			repliesPage.next=Config.localURI("/posts/"+post.id+"/replies?page=1");
			repliesPage.partOf=replies.activityPubID;
			repliesPage.items=Collections.emptyList();
			replies.first=new LinkOrObject(repliesPage);
			noq.replies=new LinkOrObject(replies);
		}
		if(!post.replyKey.isEmpty()){
			noq.inReplyTo=context.getWallController().getPostOrThrow(post.replyKey.get(post.replyKey.size()-1)).getActivityPubID();
		}
		if(post.isDeleted())
			return noq;

		User author=context.getUsersController().getUserOrThrow(post.authorID);
		noq.content=post.text;
		noq.attributedTo=author.activityPubID;
		noq.published=post.createdAt;
		noq.updated=post.updatedAt;
		if(post.hasContentWarning()){
			noq.summary=post.contentWarning;
			noq.sensitive=true;
		}else{
			noq.sensitive=false;
		}
		if(post.ownerID!=post.authorID && post.replyKey.isEmpty()){
			Actor owner;
			if(post.ownerID>0)
				owner=context.getUsersController().getUserOrThrow(post.ownerID);
			else
				owner=context.getGroupsController().getGroupOrThrow(-post.ownerID);
			ActivityPubCollection target=new ActivityPubCollection(false);
			target.attributedTo=owner.activityPubID;
			target.activityPubID=owner.getWallURL();
			noq.target=target;
			to.add(owner.activityPubID);
		}
		if(post.ownerID==post.authorID && post.replyKey.isEmpty()){
			to.add(author.getFollowersURL());
		}

		noq.tag=new ArrayList<>();
		if(!post.mentionedUserIDs.isEmpty()){
			for(User u:context.getUsersController().getUsers(post.mentionedUserIDs).values()){
				Mention mention=new Mention();
				mention.href=u.activityPubID;
				noq.tag.add(mention);
				cc.add(u.activityPubID);
			}
		}
		noq.to=to.stream().map(LinkOrObject::new).toList();
		noq.cc=cc.stream().map(LinkOrObject::new).toList();

		noq.attachment=post.attachments;
		noq.likes=new UriBuilder(noq.activityPubID).appendPath("likes").build();

		return noq;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		JsonElement _content=obj.get("content");
		if(_content!=null && _content.isJsonArray()){
			content=_content.getAsJsonArray().get(0).getAsString();
		}else if(obj.has("contentMap")){
			// Pleroma compatibility workaround
			// TODO find out why "content" gets dropped during JSON-LD processing
			JsonElement _contentMap=obj.get("contentMap");
			if(_contentMap.isJsonObject()){
				JsonObject contentMap=_contentMap.getAsJsonObject();
				if(contentMap.size()>0){
					_content=contentMap.get(contentMap.keySet().iterator().next());
					if(_content!=null && _content.isJsonArray()){
						content=_content.getAsJsonArray().get(0).getAsString();
					}
				}
			}
		}
		sensitive=optBoolean(obj, "sensitive");
		target=parse(optObject(obj, "target"), parserContext) instanceof ActivityPubCollection apc ? apc : null;
		replies=tryParseLinkOrObject(obj.get("replies"), parserContext);

		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		super.asActivityPubObject(obj, contextCollector);

		obj.add("replies", replies.serialize(contextCollector));
		if(sensitive!=null)
			obj.addProperty("sensitive", sensitive);
		contextCollector.addAlias("sensitive", "as:sensitive");
		if(obj.has("content"))
			obj.addProperty("content", Utils.postprocessPostHTMLForActivityPub(content));
		if(target!=null)
			obj.add("target", target.asActivityPubObject(obj, contextCollector));
		if(likes!=null)
			obj.addProperty("likes", likes.toString());

//		if(deleted){
//			obj.addProperty("formerType", "Note");
//			return obj;
//		}

		return obj;
	}
}
