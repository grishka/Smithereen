package smithereen.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.PostStorage;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public final class Post implements ActivityPubRepresentable, OwnedContentObject, AttachmentHostContentObject, ReportableContentObject{
	public int id;
	public int authorID;
	// userID or -groupID
	public int ownerID;
	public String text;
	public List<ActivityPubObject> attachments; // TODO move away from AP objects here
	public int repostOf;
	public Instant createdAt;
	public String contentWarning;
	public Instant updatedAt;
	public List<Integer> replyKey=List.of();
	public Set<Integer> mentionedUserIDs=Set.of();
	public int replyCount;
	public Poll poll;
	public FederationState federationState=FederationState.NONE;

	private URI activityPubID;
	public URI activityPubURL;
	public URI activityPubReplies;
	public boolean isReplyToUnknownPost;
	public boolean deleted;
	public Privacy privacy=Privacy.PUBLIC;
	public EnumSet<Flag> flags=EnumSet.noneOf(Flag.class);

	public boolean hasContentWarning(){
		return contentWarning!=null;
	}

	public String getContentWarning(){
		return contentWarning;
	}

	@Override
	public URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return UriBuilder.local().path("posts", String.valueOf(id)).build();
	}

	public URI getActivityPubURL(){
		if(activityPubURL!=null)
			return activityPubURL;
		return UriBuilder.local().path("posts", String.valueOf(id)).build();
	}

	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();

		post.id=res.getInt("id");
		post.ownerID=res.getInt("owner_user_id");
		if(res.wasNull())
			post.ownerID=-res.getInt("owner_group_id");
		post.replyKey=Utils.deserializeIntList(res.getBytes("reply_key"));

		post.authorID=res.getInt("author_id");
		if(res.wasNull()){
			post.deleted=true;
			return post;
		}

		post.text=res.getString("text");

		String att=res.getString("attachments");
		if(att!=null){
			try{
				post.attachments=ActivityPubObject.parseSingleObjectOrArray(JsonParser.parseString(att), ParserContext.LOCAL);
			}catch(Exception ignore){}
		}

		post.repostOf=res.getInt("repost_of");
		String id=res.getString("ap_id");
		if(id!=null)
			post.setActivityPubID(URI.create(id));
		String url=res.getString("ap_url");
		if(url!=null)
			post.activityPubURL=URI.create(url);
		post.createdAt=DatabaseUtils.getInstant(res, "created_at");
		post.contentWarning=res.getString("content_warning");
		post.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		post.mentionedUserIDs=Utils.deserializeIntSet(res.getBytes("mentions"));
		post.replyCount=res.getInt("reply_count");
		String replies=res.getString("ap_replies");
		if(replies!=null)
			post.activityPubReplies=URI.create(replies);
		post.federationState=FederationState.values()[res.getInt("federation_state")];

		int pollID=res.getInt("poll_id");
		if(!res.wasNull()){
			post.poll=PostStorage.getPoll(pollID, post.activityPubID);
		}
		post.privacy=Privacy.values()[res.getInt("privacy")];
		Utils.deserializeEnumSet(post.flags, Flag.class, res.getLong("flags"));

		return post;
	}

	public int getReplyLevel(){
		return replyKey.size();
	}

	public boolean isDeleted(){
		return deleted;
	}

	// for use in templates
	public int getReplyChainElement(int level){
		return replyKey.get(level);
	}

	public String serializeAttachments(){
		if(attachments==null)
			return null;
		return ActivityPubObject.serializeObjectArrayCompact(attachments, new SerializerContext(null, (String)null)).toString();
	}

	public boolean canBeManagedBy(User user){
		if(user==null)
			return false;
		return ownerID==user.id || authorID==user.id;
	}

	public URI getInternalURL(){
		return Config.localURI("/posts/"+id);
	}

	public List<Integer> getReplyKeyForReplies(){
		ArrayList<Integer> rk=new ArrayList<>(replyKey);
		rk.add(id);
		return rk;
	}

	public boolean isGroupOwner(){
		return ownerID<0;
	}

	public boolean isLocal(){
		return activityPubID==null;
	}

	public String getShortTitle(){
		return getShortTitle(100);
	}

	public String getShortTitle(int maxLen){
		if(StringUtils.isNotEmpty(contentWarning)){
			return contentWarning;
		}
		if(StringUtils.isNotEmpty(text)){
			return Utils.truncateOnWordBoundary(text, maxLen);
		}
		return "";
	}

	@Override
	public int getOwnerID(){
		return ownerID;
	}

	@Override
	public int getAuthorID(){
		return authorID;
	}

	public void setActivityPubID(URI activityPubID){
		this.activityPubID=activityPubID;
	}

	@Override
	public List<ActivityPubObject> getAttachments(){
		return attachments;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return new NonCachedRemoteImage.PostPhotoArgs(id, index);
	}

	@Override
	public JsonObject serializeForReport(int targetID, Set<Long> outFileIDs){
		if(authorID!=targetID && ownerID!=targetID)
			return null;
		JsonObjectBuilder jb=new JsonObjectBuilder()
				.add("type", "post")
				.add("id", id)
				.add("owner", ownerID)
				.add("author", authorID)
				.add("created_at", createdAt.getEpochSecond())
				.add("text", text);
		if(getReplyLevel()>0)
			jb.add("replyKey", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeIntList(replyKey)));
		if(attachments!=null && !attachments.isEmpty())
			jb.add("attachments", ReportableContentObject.serializeMediaAttachments(attachments, outFileIDs));
		if(poll!=null){
			jb.add("pollQuestion", poll.question);
			JsonArrayBuilder jab=new JsonArrayBuilder();
			for(PollOption opt:poll.options){
				jab.add(opt.text);
			}
			jb.add("pollOptions", jab.build());
		}
		if(hasContentWarning())
			jb.add("cw", contentWarning);
		return jb.build();
	}

	@Override
	public void fillFromReport(JsonObject jo){
		id=jo.get("id").getAsInt();
		ownerID=jo.get("owner").getAsInt();
		authorID=jo.get("author").getAsInt();
		createdAt=Instant.ofEpochSecond(jo.get("created_at").getAsLong());
		text=jo.get("text").getAsString();
		if(jo.has("replyKey")){
			replyKey=Utils.deserializeIntList(Base64.getDecoder().decode(jo.get("replyKey").getAsString()));
		}
		if(jo.has("attachments")){
			attachments=new ArrayList<>();
			for(JsonElement jatt:jo.getAsJsonArray("attachments")){
				attachments.add(ActivityPubObject.parse(jatt.getAsJsonObject(), ParserContext.LOCAL));
			}
		}
		if(jo.has("pollQuestion")){
			poll=new Poll();
			poll.question=jo.get("pollQuestion").getAsString();
			poll.options=new ArrayList<>();
			for(JsonElement jopt:jo.getAsJsonArray("pollOptions")){
				PollOption opt=new PollOption();
				opt.text=jopt.getAsString();
				poll.options.add(opt);
			}
		}
		if(jo.has("cw"))
			contentWarning=jo.get("cw").getAsString();
	}

	public boolean isMastodonStyleRepost(){
		return flags.contains(Flag.MASTODON_STYLE_REPOST);
	}

	public int getIDForInteractions(){
		// Mastodon-style repost posts can't be interacted with
		return flags.contains(Flag.MASTODON_STYLE_REPOST) ? repostOf : id;
	}

	public void setRepostedPost(Post post){
		repostOf=post.id;

		// Strip the RE: ... part from text
		// <p>Quote repost test<br><br>RE: <a href="https://misskey.io/notes/86woec5nlm">https://misskey.io/notes/86woec5nlm</a></p>
		Element root=Jsoup.parseBodyFragment(text).body();
		// Find and remove the <a>
		Elements elements=root.getElementsByAttributeValue("href", post.getActivityPubURL().toString());
		if(!elements.isEmpty()){
			Element el=elements.getLast(); // Post may contain more than one link to this URL
			// Find and remove the preceding "RE:"
			if(el.previousSibling() instanceof TextNode tn && tn.text().trim().equalsIgnoreCase("RE:")){
				Node possiblyBr=tn.previousSibling();
				// Remove trailing <br>'s before "RE:"
				while(possiblyBr instanceof Element el1 && el1.tagName().equalsIgnoreCase("br")){
					Node brSibling=possiblyBr.previousSibling();
					possiblyBr.remove();
					possiblyBr=brSibling;
				}
				tn.remove();
			}
			el.remove();
			text=root.html();
		}
	}

	public enum Privacy{
		PUBLIC(null),
		FOLLOWERS_AND_MENTIONED("post_visible_to_followers_mentioned"),
		FOLLOWERS_ONLY("post_visible_to_followers"),
		FRIENDS_ONLY("post_visible_to_friends");

		public final String langKey;

		Privacy(String langKey){
			this.langKey=langKey;
		}
	}

	public enum Flag{
		MASTODON_STYLE_REPOST,
	}
}
