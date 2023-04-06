package smithereen.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.Mention;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.AudioAttachment;
import smithereen.data.attachments.GraffitiAttachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.attachments.VideoAttachment;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.jsonld.JLD;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaCache;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class Post extends ActivityPubObject{
	public int id;
	public User user;
	public Actor owner;
	private boolean deleted;

	public String userLink;

	public int[] replyKey={};

	public List<Post> repliesObjects=new ArrayList<>();
	public int totalTopLevelComments;
	public int replyCount;
	public boolean local;
	public Set<User> mentionedUsers=Collections.emptySet();
	public Poll poll;
	public String source;

	public FederationState federationState;

	private ActivityPubObject activityPubTarget;
	private boolean loadedRepliesCountKnown;
	private int loadedRepliesCount;

	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();
		post.fillFromResultSet(res);
		return post;
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt("id");

		String apid=res.getString("ap_id");
		if(apid==null){
			activityPubID=Config.localURI("/posts/"+id);
			url=activityPubID;
			local=true;
		}else{
			activityPubID=URI.create(apid);
			url=URI.create(res.getString("ap_url"));
		}

		byte[] rk=res.getBytes("reply_key");
		replyKey=Utils.deserializeIntArray(rk);
		if(replyKey==null)
			replyKey=new int[0];

		if(replyKey.length>0){
			inReplyTo=PostStorage.getActivityPubID(replyKey[replyKey.length-1]);
		}

		String _replies=res.getString("ap_replies");
		if(_replies!=null)
			replies=new LinkOrObject(URI.create(_replies));

		int uid=res.getInt("author_id");
		if(!res.wasNull()){
			user=UserStorage.getById(uid);
			int ownerUserID=res.getInt("owner_user_id");
			if(!res.wasNull())
				owner=UserStorage.getById(ownerUserID);
			else
				owner=GroupStorage.getById(res.getInt("owner_group_id"));
		}else{
			deleted=true;
			return;
		}

		content=res.getString("text");
		published=DatabaseUtils.getInstant(res, "created_at");
		updated=DatabaseUtils.getInstant(res, "updated_at");
		summary=res.getString("content_warning");
		attributedTo=user.activityPubID;

		if(local){
			if(owner instanceof User || replyKey.length>0){
				to=Collections.singletonList(LinkOrObject.PUBLIC);
				if(replyKey.length==0)
					cc=Collections.singletonList(new LinkOrObject(user.getFollowersURL()));
				else
					cc=Collections.emptyList();
			}else if(owner instanceof Group){
				to=List.of(LinkOrObject.PUBLIC, new LinkOrObject(owner.getFollowersURL()));
				cc=Collections.emptyList();
			}else{
				to=Arrays.asList(LinkOrObject.PUBLIC, new LinkOrObject(owner.activityPubID));
				cc=Collections.emptyList();
			}
		}else{
			cc=Collections.emptyList();
			to=Collections.emptyList();
		}

		String att=res.getString("attachments");
		if(att!=null){
			try{
				attachment=parseSingleObjectOrArray(JsonParser.parseString(att), ParserContext.LOCAL);
			}catch(Exception ignore){}
		}

		userLink=user.url.toString();

		int[] mentions=Utils.deserializeIntArray(res.getBytes("mentions"));
		if(mentions!=null && mentions.length>0){
			mentionedUsers=new HashSet<>();
			if(tag==null)
				tag=new ArrayList<>();
			for(int id:mentions){
				User user=UserStorage.getById(id);
				if(user!=null){
					mentionedUsers.add(user);
					if(local){
						addToCC(user.activityPubID);
						Mention mention=new Mention();
						mention.href=user.activityPubID;
						tag.add(mention);
					}
				}
			}
		}

		replyCount=res.getInt("reply_count");
		int pollID=res.getInt("poll_id");
		if(!res.wasNull()){
			poll=PostStorage.getPoll(pollID, activityPubID);
		}

		federationState=FederationState.values()[res.getInt("federation_state")];
		source=res.getString("source");
	}

	public boolean hasContentWarning(){
		return summary!=null;
	}

	@Override
	public String getType(){
		if(deleted)
			return "Tombstone";
		if(poll!=null)
			return "Question";
		return "Note";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		JsonObject root=super.asActivityPubObject(obj, contextCollector);

		ActivityPubCollection replies=new ActivityPubCollection(false);
		replies.activityPubID=Config.localURI("/posts/"+id+"/replies");
		CollectionPage repliesPage=new CollectionPage(false);
		repliesPage.next=Config.localURI("/posts/"+id+"/replies?page=1");
		repliesPage.partOf=replies.activityPubID;
		repliesPage.items=Collections.emptyList();
		replies.first=new LinkOrObject(repliesPage);
		root.add("replies", replies.asActivityPubObject(new JsonObject(), contextCollector));

		if(deleted){
			root.addProperty("formerType", "Note");
			return root;
		}
		root.addProperty("sensitive", hasContentWarning());
		contextCollector.addAlias("sensitive", "as:sensitive");
		if(root.has("content"))
			root.addProperty("content", Utils.postprocessPostHTMLForActivityPub(content));

		if((!(owner instanceof User) || user.id!=((User)owner).id)){
			ActivityPubCollection wall=new ActivityPubCollection(false);
			wall.activityPubID=owner.getWallURL();
			wall.attributedTo=owner.activityPubID;
			root.add("target", wall.asActivityPubObject(new JsonObject(), contextCollector));
		}
		root.addProperty("likes", Config.localURI("/posts/"+id+"/likes").toString());

		if(poll!=null){
			root.addProperty("name", poll.question);
			JsonArray opts=new JsonArray();
			for(PollOption opt: poll.options){
				opts.add(opt.asActivityPubObject(new JsonObject(), contextCollector));
			}
			root.add(poll.multipleChoice ? "anyOf" : "oneOf", opts);
			if(poll.endTime!=null){
				root.addProperty(poll.endTime.toEpochMilli()<System.currentTimeMillis() ? "closed" : "endTime", Utils.formatDateAsISO(poll.endTime));
			}
			root.addProperty("votersCount", poll.numVoters);
			root.addProperty("nonAnonymous", !poll.anonymous);
			contextCollector.addAlias("toot", JLD.MASTODON);
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addAlias("votersCount", "toot:votersCount");
			contextCollector.addAlias("nonAnonymous", "sm:nonAnonymous");
		}

		return root;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		// fix for Lemmy (and possibly something else)
		boolean hasBogusURL=url!=null && !url.getHost().equalsIgnoreCase(activityPubID.getHost());
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
		String type=obj.get("type").getAsString();
		boolean isPoll=type.equals("Question") && (obj.has("oneOf") || obj.has("anyOf"));
		if(content!=null && !parserContext.isLocal){
			if(StringUtils.isNotEmpty(name) && !isPoll)
				content="<p><b>"+name+"</b></p>"+content;
			if(hasBogusURL)
				content=content+"<p><a href=\""+url+"\">"+url+"</a></p>";
			content=Utils.sanitizeHTML(content);
			if(obj.has("sensitive") && obj.get("sensitive").getAsBoolean() && summary!=null){
				summary=Utils.sanitizeHTML(summary);
			}else{
				summary=null;
			}
		}
		if(hasBogusURL)
			url=activityPubID;
		try{
			user=UserStorage.getUserByActivityPubID(attributedTo);
			if(url==null)
				url=activityPubID;
			if(published==null)
				published=Instant.now();

			ActivityPubObject target=parse(optObject(obj, "target"), parserContext);
			if(target instanceof ActivityPubCollection && target.attributedTo!=null && target.activityPubID!=null && inReplyTo==null){
				URI ownerID=target.attributedTo;
				if(Config.isLocal(ownerID)){
					String[] parts=ownerID.getPath().split("/");
					if(parts.length==3){ // "", "users", id
						int id=Utils.parseIntOrDefault(parts[2], 0);
						if("users".equals(parts[1])){
							owner=UserStorage.getById(id);
							if(owner instanceof ForeignUser)
								owner=null;
						}else if("groups".equals(parts[1])){
							owner=GroupStorage.getById(id);
							if(owner instanceof ForeignGroup)
								owner=null;
						}
					}
				}else{
					owner=UserStorage.getForeignUserByActivityPubID(ownerID);
					if(owner==null)
						owner=GroupStorage.getForeignGroupByActivityPubID(ownerID);

					activityPubTarget=target;
				}
				if(owner!=null && !target.activityPubID.equals(owner.getWallURL()))
					owner=null;
			}else{
				owner=user;
			}
		}catch(SQLException x){
			throw new IllegalStateException(x);
		}
		if(isPoll){
			poll=new Poll();
			poll.multipleChoice=obj.has("anyOf");
			poll.question=obj.has("name") ? obj.get("name").getAsString() : null;
			poll.numVoters=obj.has("votersCount") ? obj.get("votersCount").getAsInt() : 0;
			JsonArray opts=obj.getAsJsonArray(obj.has("anyOf") ? "anyOf" : "oneOf");
			poll.anonymous=!obj.has("nonAnonymous") || !optBoolean(obj, "nonAnonymous");
			poll.options=new ArrayList<>(opts.size());
			poll.activityPubID=activityPubID;
			if(endTime!=null){
				poll.endTime=endTime;
			}else if(obj.has("closed")){
				poll.endTime=tryParseDate(obj.get("closed").getAsString());
			}
			for(JsonElement _opt:opts){
				JsonObject opt=_opt.getAsJsonObject();
				PollOption o=new PollOption();
				o.parseActivityPubObject(opt, parserContext);
				ensureHostMatchesID(o.activityPubID, "answer.id");
				poll.options.add(o);
			}
			if(poll.options.isEmpty())
				poll=null;
		}
		if(replies!=null){
			if(replies.object!=null)
				replies.object.validate(activityPubID, "replies");
			else
				ensureHostMatchesID(replies.link, "replies");
		}
		return this;
	}

	public String serializeAttachments(){
		if(attachment==null)
			return null;
		return serializeObjectArrayCompact(attachment, new ContextCollector()).toString();
	}

	public boolean canBeManagedBy(User user){
		if(user==null)
			return false;
		return (owner instanceof User && ((User)owner).id==user.id) || this.user.id==user.id;
	}

	public URI getInternalURL(){
		return Config.localURI("/posts/"+id);
	}

	public void setParent(Post parent){
		if(inReplyTo!=null && !inReplyTo.equals(parent.activityPubID))
			throw new IllegalStateException("inReplyTo != parent.id");
		replyKey=new int[parent.replyKey.length+1];
		System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
		replyKey[replyKey.length-1]=parent.id;
		inReplyTo=parent.activityPubID;
		if(tag==null)
			tag=new ArrayList<>();
		else if(!(tag instanceof ArrayList))
			tag=new ArrayList<>(tag);
		Mention mention=new Mention();
		mention.href=parent.user.activityPubID;
		tag.add(mention);
		if(mentionedUsers.isEmpty())
			mentionedUsers=new HashSet<>();
		mentionedUsers.add(parent.user);
		owner=parent.owner;
	}

	public int getReplyLevel(){
		return replyKey.length;
	}

	// Reply key for posts that reply to this one.
	public int[] getReplyKeyForReplies(){
		int[] r=new int[replyKey.length+1];
		System.arraycopy(replyKey, 0, r, 0, replyKey.length);
		r[r.length-1]=id;
		return r;
	}

	public boolean isDeleted(){
		return deleted;
	}

	// for use in templates
	public int getReplyChainElement(int level){
		return replyKey[level];
	}

	public void addToCC(URI uri){
		LinkOrObject l=new LinkOrObject(uri);
		if(!cc.contains(l)){
			if(!(cc instanceof ArrayList)){
				cc=new ArrayList<>(cc);
			}
			cc.add(l);
		}
	}

	public List<Attachment> getProcessedAttachments(){
		ArrayList<Attachment> result=new ArrayList<>();
		int i=0;
		for(ActivityPubObject o:attachment){
			String mediaType=o.mediaType==null ? "" : o.mediaType;
			if(o instanceof Image || mediaType.startsWith("image/")){
				PhotoAttachment att=o instanceof Image img && img.isGraffiti ? new GraffitiAttachment() : new PhotoAttachment();
				if(o instanceof LocalImage li){
					att.image=li;
				}else{
					// TODO make this less ugly
					MediaCache.PhotoItem item;
					try{
						item=(MediaCache.PhotoItem) MediaCache.getInstance().get(o.url);
					}catch(SQLException x){
						throw new InternalServerErrorException(x);
					}
					if(item!=null){
						att.image=new CachedRemoteImage(item);
					}else{
						SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
						if(o instanceof Document im){
							if(im.width>0 && im.height>0){
								size=new SizedImage.Dimensions(im.width, im.height);
							}
						}
						att.image=new NonCachedRemoteImage(new NonCachedRemoteImage.PostPhotoArgs(id, i), size);
					}
				}
				if(o instanceof Document doc){
					if(StringUtils.isNotEmpty(doc.blurHash))
						att.blurHash=doc.blurHash;
				}
				result.add(att);
			}else if(mediaType.startsWith("video/")){
				VideoAttachment att=new VideoAttachment();
				att.url=o.url;
				result.add(att);
			}else if(mediaType.startsWith("audio/")){
				AudioAttachment att=new AudioAttachment();
				att.url=o.url;
				result.add(att);
			}
			i++;
		}
		return result;
	}

	public String getShortTitle(){
		return getShortTitle(100);
	}

	public String getShortTitle(int maxLen){
		if(StringUtils.isNotEmpty(summary)){
			return summary;
		}
		if(StringUtils.isNotEmpty(content)){
			return Utils.truncateOnWordBoundary(content, maxLen);
		}
		return "";
	}

	public void getAllReplyIDs(Collection<Integer> out){
		for(Post reply: repliesObjects){
			out.add(reply.id);
			reply.getAllReplyIDs(out);
		}
	}

	public boolean isGroupOwner(){
		return owner instanceof Group;
	}

	@Override
	public void resolveDependencies(ApplicationContext context, boolean allowFetching, boolean allowStorage){
		if(user==null){
			user=context.getObjectLinkResolver().resolve(attributedTo, ForeignUser.class, allowFetching, allowStorage, false);
		}
		if(owner==null && activityPubTarget!=null){
			owner=context.getObjectLinkResolver().resolve(activityPubTarget.attributedTo, Actor.class, allowFetching, allowStorage, false);
			if(!activityPubTarget.activityPubID.equals(owner.getWallURL()))
				owner=null;
		}
		if(owner==null)
			owner=user;
	}

	@Override
	public void storeDependencies(ApplicationContext context){
		if(user instanceof ForeignUser && user.id==0)
			context.getObjectLinkResolver().storeOrUpdateRemoteObject(user);
		if(owner!=user && ((owner instanceof ForeignUser && ((ForeignUser) owner).id==0) || (owner instanceof ForeignGroup && ((ForeignGroup) owner).id==0)))
			context.getObjectLinkResolver().storeOrUpdateRemoteObject(owner);
	}

	public URI getRepliesURL(){
		if(replies==null)
			return null;
		if(replies.link!=null)
			return replies.link;
		return replies.object.activityPubID;
	}

	public int getMissingRepliesCount(){
		return replyCount-getLoadedRepliesCount();
	}

	public int getLoadedRepliesCount(){
		if(loadedRepliesCountKnown)
			return loadedRepliesCount;
		loadedRepliesCount=0;
		for(Post reply:repliesObjects){
			loadedRepliesCount+=reply.getLoadedRepliesCount()+1;
		}
		loadedRepliesCountKnown=true;
		return loadedRepliesCount;
	}

	public int getLoadableRepliesCount(){
		int count=replyCount;
		for(Post reply:repliesObjects){
			count-=reply.replyCount+1;
		}
		return count;
	}
}
