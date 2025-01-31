package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.jsonld.JLD;
import smithereen.lang.Lang;
import smithereen.model.MailMessage;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentReplyParent;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public abstract sealed class NoteOrQuestion extends ActivityPubObject permits Note, Question, NoteTombstone{
	private static final int MAX_MENTIONS=10;
	private static final Logger LOG=LoggerFactory.getLogger(NoteOrQuestion.class);

	public LinkOrObject replies;
	public Boolean sensitive;
	public ActivityPubCollection target;
	public URI likes;
	public URI quoteRepostID;
	public String action;

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
			if(!Objects.equals(target.activityPubID, owner.getWallURL()))
				throw new FederationException("target.id points to an unknown collection "+target.attributedTo);
			post.ownerID=owner.getOwnerID();
		}else{
			post.ownerID=author.id;
		}

		// fix for Lemmy (and possibly something else)
		boolean hasBogusURL=url!=null && !url.getHost().equalsIgnoreCase(activityPubID.getHost()) && !url.getHost().equalsIgnoreCase("www."+activityPubID.getHost());

		String text=content==null ? "" : content;
		if(hasBogusURL)
			text=text+"<p><a href=\""+url+"\">"+url+"</a></p>";
		text=TextProcessor.sanitizeHTML(text);
		post.text=text;
		post.createdAt=published!=null ? published : Instant.now();
		post.updatedAt=updated;
		if(sensitive!=null && sensitive){
			if(StringUtils.isNotEmpty(summary))
				post.contentWarning=summary;
			else
				post.contentWarning=""; // Will be rendered as a translatable default string
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

		if("AvatarUpdate".equals(action)){
			// Must have exactly one photo attached
			if(post.attachments!=null && post.attachments.size()==1 && post.attachments.getFirst() instanceof Image img && img.photoApID!=null && post.ownerID==post.authorID){
				try{
					context.getObjectLinkResolver().resolveNative(img.photoApID, Photo.class, true, true, false, author, false);
					post.text="";
					post.action=Post.Action.AVATAR_UPDATE;
				}catch(ObjectNotFoundException ignore){}
			}
		}

		return post;
	}

	public static NoteOrQuestion fromNativePost(Post post, ApplicationContext context){
		NoteOrQuestion noq;
		if(post.isDeleted()){
			noq=new LocalPostNoteTombstone(post);
		}else if(post.poll!=null){
			Question q=new LocalPostQuestion(post);
			q.name=post.poll.question;
			q.votersCount=post.poll.numVoters;
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
			noq=new LocalPostNote(post);
		}

		Set<URI> to=new HashSet<>(), cc=new HashSet<>();
		to.add(ActivityPub.AS_PUBLIC);

		noq.activityPubID=post.getActivityPubID();
		noq.url=post.activityPubURL==null ? noq.activityPubID : post.activityPubURL;
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
		if(post.poll!=null && StringUtils.isNotEmpty(post.poll.question)){
			noq.content+="<p class=\"smithereenPollQuestion\"><i>"+TextProcessor.escapeHTML(post.poll.question)+"</i></p>";
		}
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

		if(post.repostOf!=0){
			try{
				Post repost=context.getWallController().getPostOrThrow(post.repostOf);
				noq.quoteRepostID=repost.getActivityPubID();
				cc.add(context.getUsersController().getUserOrThrow(repost.authorID).activityPubID);

				Document doc=Jsoup.parseBodyFragment(noq.content);
				doc.outputSettings().prettyPrint(false).indentAmount(0);
				Element root=doc.body();
				Element parentP;
				if(root.childrenSize()==0){
					parentP=doc.createElement("p");
					root.appendChild(parentP);
				}else{
					parentP=root.children().getLast();
				}
				parentP.appendChild(
						doc.createElement("span")
								.addClass("quote-inline")
								.appendChildren(List.of(
										doc.createElement("br"),
										doc.createElement("br"),
										new TextNode("RE: "),
										doc.createElement("a")
												.attr("href", repost.getActivityPubURL().toString())
												.text(repost.getActivityPubURL().toString())
								))
				);
				noq.content=root.html();
			}catch(ObjectNotFoundException ignore){}
		}

		if(post.action!=null){
			switch(post.action){
				case AVATAR_UPDATE -> {
					noq.action="AvatarUpdate";
					noq.content+="<p class=\"smithereenAvatarUpdate\"><i>"+Lang.get(Locale.US).get("post_action_updated_avatar", Map.of("gender", author.gender))+"</i></p>";
				}
			}
		}

		noq.to=to.stream().map(LinkOrObject::new).toList();
		noq.cc=cc.stream().map(LinkOrObject::new).toList();

		noq.attachment=resolveLocalPhotoIDsInAttachments(context, post.attachments);
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
		n.attachment=resolveLocalPhotoIDsInAttachments(context, msg.attachments);
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
		msg.text=TextProcessor.sanitizeHTML(content);
		msg.subject=StringUtils.isNotEmpty(name) ? name : summary;
		msg.attachments=attachment;
		msg.createdAt=published!=null ? published : Instant.now();
		msg.updatedAt=updated;
		msg.inReplyTo=inReplyTo;

		msg.to=new HashSet<>();
		if(to!=null){
			for(LinkOrObject id:to){
				try{
					User user=context.getObjectLinkResolver().resolve(id.link, User.class, true, true, false);
					msg.to.add(user.id);
				}catch(ObjectNotFoundException ignore){}
			}
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

		if(msg.to.isEmpty() && !msg.cc.isEmpty()){ // Some servers would omit `to` and put all recipients into `cc` instead.
			msg.to.addAll(msg.cc);
			msg.cc=Set.of();
		}

		return msg;
	}

	public static NoteOrQuestion fromNativeComment(Comment comment, ApplicationContext context){
		NoteOrQuestion n=comment.isDeleted() ? new LocalCommentNoteTombstone(comment) : new LocalCommentNote(comment);

		User author=context.getUsersController().getUserOrThrow(comment.authorID);
		CommentableContentObject parent=context.getCommentsController().getCommentParentIgnoringPrivacy(comment);
		Actor parentOwner=parent.getOwnerID()>0 ? context.getUsersController().getUserOrThrow(parent.getOwnerID()) : context.getGroupsController().getGroupOrThrow(-parent.getOwnerID());

		n.activityPubID=comment.getActivityPubID();

		if(comment.getReplyLevel()>0){
			Comment parentComment=context.getCommentsController().getCommentIgnoringPrivacy(comment.replyKey.getLast());
			n.inReplyTo=parentComment.getActivityPubID();
		}else{
			n.inReplyTo=parent.getActivityPubID();
		}

		ActivityPubCollection replies=new ActivityPubCollection(false);
		replies.activityPubID=new UriBuilder(comment.getActivityPubID()).appendPath("replies").build();
		CollectionPage repliesPage=new CollectionPage(false);
		repliesPage.next=new UriBuilder(replies.activityPubID).queryParam("page", "1").build();
		repliesPage.partOf=replies.activityPubID;
		repliesPage.items=Collections.emptyList();
		replies.first=new LinkOrObject(repliesPage);
		n.replies=new LinkOrObject(replies);

		ActivityPubCollection target=new ActivityPubCollection(false);
		target.attributedTo=parentOwner.activityPubID;
		target.activityPubID=parent.getCommentCollectionID(context);
		n.target=target;
		n.url=comment.getActivityPubURL();

		if(comment.isDeleted())
			return n;

		n.attributedTo=author.activityPubID;
		n.content=comment.text;
		n.published=comment.createdAt;
		n.updated=comment.updatedAt;
		if(comment.hasContentWarning()){
			n.summary=comment.contentWarning;
			n.sensitive=true;
		}else{
			n.sensitive=false;
		}
		n.tag=new ArrayList<>();
		if(!comment.mentionedUserIDs.isEmpty()){
			for(User u:context.getUsersController().getUsers(comment.mentionedUserIDs).values()){
				Mention mention=new Mention();
				mention.href=u.activityPubID;
				n.tag.add(mention);
			}
		}
		n.attachment=resolveLocalPhotoIDsInAttachments(context, comment.attachments);
		n.likes=new UriBuilder(n.activityPubID).appendPath("likes").build();

		return n;
	}

	public Comment asNativeComment(ApplicationContext context){
		Comment comment=new Comment();

		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		if(target==null)
			throw new FederationException("target is required");
		ensureHostMatchesID(attributedTo, "attributedTo");
		comment.id=context.getCommentsController().getCommentIDByActivityPubID(activityPubID);

		Actor owner=context.getObjectLinkResolver().resolve(target.attributedTo, Actor.class, true, true, false);
		User author=context.getObjectLinkResolver().resolve(attributedTo, User.class, true, true, false);
		comment.authorID=author.id;
		comment.ownerID=owner.getOwnerID();
		if(inReplyTo==null)
			throw new FederationException("inReplyTo is required");
		CommentReplyParent replyParent=context.getObjectLinkResolver().resolveNative(inReplyTo, CommentReplyParent.class, true, true, false, owner, true);
		CommentableContentObject parentObj=switch(replyParent){
			case CommentableContentObject _parentObj -> {
				comment.replyKey=List.of();
				comment.parentObjectID=_parentObj.getCommentParentID();
				yield _parentObj;
			}
			case Comment parentComment -> {
				comment.replyKey=parentComment.getReplyKeyForReplies();
				comment.parentObjectID=parentComment.parentObjectID;
				yield context.getCommentsController().getCommentParentIgnoringPrivacy(comment);
			}
		};
		if(!Objects.equals(target.activityPubID, parentObj.getCommentCollectionID(context)))
			throw new FederationException("target.id does not match the expected comment collection ID");

		comment.setActivityPubID(activityPubID);
		comment.activityPubURL=url==null ? activityPubID : url;
		ensureHostMatchesID(comment.activityPubURL, "url");
		if(replies!=null){
			comment.activityPubReplies=replies.getObjectID();
			ensureHostMatchesID(comment.activityPubReplies, "replies");
		}

		comment.text=TextProcessor.sanitizeHTML(content);
		comment.createdAt=published!=null ? published : Instant.now();
		comment.updatedAt=updated;
		if(sensitive!=null && sensitive){
			if(StringUtils.isNotEmpty(summary))
				comment.contentWarning=summary;
			else
				comment.contentWarning=""; // Will be rendered as a translatable default string
		}

		if(attachment!=null && attachment.size()>10)
			comment.attachments=attachment.subList(0, 10);
		else
			comment.attachments=attachment;

		if(tag!=null){
			comment.mentionedUserIDs=new HashSet<>();
			int mentionCount=0;
			for(ActivityPubObject obj:tag){
				if(obj instanceof Mention mention){
					try{
						User mentionedUser=context.getObjectLinkResolver().resolve(mention.href, User.class, true, true, false);
						comment.mentionedUserIDs.add(mentionedUser.id);
						mentionCount++;
						if(mentionCount==MAX_MENTIONS)
							break;
					}catch(Exception x){
						LOG.debug("Failed to resolve mention for href={}", mention.href, x);
					}
				}
			}
		}

		return comment;
	}

	public boolean isWallPostOrComment(ApplicationContext context){
		if(target==null)
			return true;

		Actor owner=context.getObjectLinkResolver().resolveLocally(target.attributedTo, Actor.class);
		return Objects.equals(owner.getWallURL(), target.activityPubID); // TODO change this when I make wall comments go into their own collection
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		JsonElement _content=obj.get("content");
		if(_content!=null && _content.isJsonArray()){
			content=_content.getAsJsonArray().get(0).getAsString();
		}else if(obj.has("contentMap")){
			// Handle the case when there's a `@language` in the `@context`
			JsonElement _contentMap=obj.get("contentMap");
			if(_contentMap.isJsonObject()){
				JsonObject contentMap=_contentMap.getAsJsonObject();
				if(contentMap.size()>0){
					_content=contentMap.get(contentMap.keySet().iterator().next());
					if(_content instanceof JsonArray ja){
						content=ja.get(0).getAsString();
					}else if(_content instanceof JsonPrimitive jp){
						content=jp.getAsString();
					}
				}
			}
		}
		sensitive=optBoolean(obj, "sensitive");
		target=parse(optObject(obj, "target"), parserContext) instanceof ActivityPubCollection apc ? apc : null;
		replies=tryParseLinkOrObject(obj.get("replies"), parserContext);
		quoteRepostID=tryParseURL(optString(obj, "_misskey_quote"));
		if(quoteRepostID==null){
			// For when there's no "@id" in context
			quoteRepostID=tryParseURL(optString(obj, JLD.MISSKEY+"_misskey_quote"));
		}
		if(quoteRepostID==null){
			// Pleroma, Akkoma and possibly other "*oma"s
			quoteRepostID=tryParseURL(optString(obj, "quoteUrl"));
		}
		action=optString(obj, "action");

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
			obj.addProperty("content", TextProcessor.postprocessPostHTMLForActivityPub(content));
		if(target!=null)
			obj.add("target", target.asActivityPubObject(new JsonObject(), serializerContext));
		if(likes!=null)
			obj.addProperty("likes", likes.toString());
		if(quoteRepostID!=null){
			serializerContext.addType("_misskey_quote", JLD.MISSKEY+"_misskey_quote", "@id");
			obj.addProperty("_misskey_quote", quoteRepostID.toString());
			serializerContext.addAlias("quoteUrl", "as:quoteUrl");
			obj.addProperty("quoteUrl", quoteRepostID.toString());
		}
		if(action!=null){
			serializerContext.addSmIdType("action");
			serializerContext.addSmAlias(action);
			obj.addProperty("action", action);
		}

		return obj;
	}

	public URI getQuoteRepostID(){
		if(quoteRepostID!=null)
			return quoteRepostID;
		// TODO also support object links when it becomes clear how they will be implemented in Mastodon
		return null;
	}

	private static List<ActivityPubObject> resolveLocalPhotoIDsInAttachments(ApplicationContext ctx, List<ActivityPubObject> attachments){
		if(attachments==null)
			return null;

		Set<Long> needPhotos=attachments.stream()
				.map(a->a instanceof LocalImage li && li.photoID!=0 ? li : null)
				.filter(Objects::nonNull)
				.map(li->li.photoID)
				.collect(Collectors.toSet());
		if(needPhotos.isEmpty())
			return attachments;
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		for(ActivityPubObject a:attachments){
			if(a instanceof LocalImage li && li.photoID!=0 && photos.get(li.photoID) instanceof Photo photo){
				li.photoApID=photo.getActivityPubID();
			}
		}
		return attachments;
	}
}
