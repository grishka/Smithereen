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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.exceptions.BadRequestException;
import smithereen.model.MailMessage;
import smithereen.model.Post;
import smithereen.model.UriBuilder;
import smithereen.model.User;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.ObjectNotFoundException;
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
		if(attachment!=null && attachment.size()>10)
			post.attachments=attachment.subList(0, 10);
		else
			post.attachments=attachment;

		HashSet<URI> mentionedUserIDs=new HashSet<>();
		if(tag!=null){
			post.mentionedUserIDs=new HashSet<>();
			int mentionCount=0;
			for(ActivityPubObject obj:tag){
				if(obj instanceof Mention mention){
					mentionedUserIDs.add(mention.href);
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

		Set<URI> recipients=Stream.of(to, cc).filter(Objects::nonNull).flatMap(List::stream).map(l->l.link).collect(Collectors.toSet());
		URI followers=author.getFollowersURL();
		URI friends=author.getFriendsURL();
		if(recipients.contains(ActivityPub.AS_PUBLIC) || recipients.contains(URI.create("as:Public"))){
			post.privacy=Post.Privacy.PUBLIC;
		}else if(followers!=null && recipients.contains(followers)){
			post.privacy=!mentionedUserIDs.isEmpty() && recipients.containsAll(mentionedUserIDs) ? Post.Privacy.FOLLOWERS_AND_MENTIONED : Post.Privacy.FOLLOWERS_ONLY;
		}else if(friends!=null && recipients.contains(friends)){
			post.privacy=Post.Privacy.FRIENDS_ONLY;
		}else{
			throw new FederationException("Unable to determine post privacy from to+cc: "+recipients+" (must contain at least one known collection ID)");
		}

		if(post.privacy!=Post.Privacy.PUBLIC && post.ownerID!=post.authorID && post.getReplyLevel()==0){
			throw new BadRequestException("Wall-to-wall posts can't be private. Wall owner controls their visibility instead");
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
				n.activityPubID=post.isLocal() ? opt.getActivityPubID(post.getActivityPubID()) : opt.activityPubID;
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

	public static Note fromNativeMessage(MailMessage msg, ApplicationContext context){
		User sender=context.getUsersController().getUserOrThrow(msg.senderID);
		HashSet<Integer> needUsers=new HashSet<>();
		needUsers.addAll(msg.to);
		needUsers.addAll(msg.cc);
		Map<Integer, User> users=context.getUsersController().getUsers(needUsers);

		Note n=new Note();
		n.activityPubID=msg.getActivityPubID();
		n.content=msg.text;
		n.name=msg.subject;
		n.attachment=msg.attachments;
		n.published=msg.createdAt;
		n.updated=msg.updatedAt;
		n.attributedTo=sender.activityPubID;
		n.to=msg.to.stream().map(users::get).filter(Objects::nonNull).map(u->new LinkOrObject(u.activityPubID)).toList();
		n.cc=msg.cc.stream().map(users::get).filter(Objects::nonNull).map(u->new LinkOrObject(u.activityPubID)).toList();
		n.tag=users.values().stream().map(u->{
			Mention m=new Mention();
			m.href=u.activityPubID;
			return (ActivityPubObject)m;
		}).toList();
		if(msg.replyInfo!=null){
			try{
				n.inReplyTo=switch(msg.replyInfo.type()){
					case POST -> context.getWallController().getPostOrThrow((int) msg.replyInfo.id()).getActivityPubID();
					case MESSAGE -> context.getMailController().getMessage(sender, msg.replyInfo.id(), false).getActivityPubID();
				};
			}catch(ObjectNotFoundException ignore){}
		}
		return n;
	}

	public MailMessage asNativeMessage(ApplicationContext context){
		MailMessage msg=new MailMessage();

		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		ensureHostMatchesID(attributedTo, "attributedTo");

		msg.activityPubID=activityPubID;
		User sender=context.getObjectLinkResolver().resolve(attributedTo, User.class, true, true, false);
		msg.senderID=sender.id;
		msg.text=Utils.sanitizeHTML(content);
		msg.subject=StringUtils.isNotEmpty(name) ? name : summary;
		msg.attachments=attachment;
		msg.createdAt=published!=null ? published : Instant.now();
		msg.updatedAt=updated;
		msg.inReplyTo=inReplyTo;

		msg.to=new HashSet<>();
		for(LinkOrObject id:to){
			try{
				User user=context.getObjectLinkResolver().resolve(id.link, User.class, true, true, false);
				msg.to.add(user.id);
			}catch(ObjectNotFoundException ignore){}
		}
		if(cc!=null){
			msg.cc=new HashSet<>();
			for(LinkOrObject id:cc){
				try{
					User user=context.getObjectLinkResolver().resolve(id.link, User.class, true, true, false);
					msg.cc.add(user.id);
				}catch(ObjectNotFoundException ignore){}
			}
		}else{
			msg.cc=Set.of();
		}

		return msg;
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
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		if(replies!=null)
			obj.add("replies", replies.serialize(serializerContext));
		if(sensitive!=null)
			obj.addProperty("sensitive", sensitive);
		serializerContext.addAlias("sensitive", "as:sensitive");
		if(obj.has("content"))
			obj.addProperty("content", Utils.postprocessPostHTMLForActivityPub(content));
		if(target!=null)
			obj.add("target", target.asActivityPubObject(new JsonObject(), serializerContext));
		if(likes!=null)
			obj.addProperty("likes", likes.toString());

		return obj;
	}
}
