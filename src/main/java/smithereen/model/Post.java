package smithereen.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.notifications.Notification;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.reports.ReportableContentObjectID;
import smithereen.model.reports.ReportableContentObjectType;
import smithereen.model.reports.ReportedPost;
import smithereen.storage.PostStorage;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;

public sealed class Post extends PostLikeObject implements ActivityPubRepresentable, ReportableContentObject, LikeableContentObject permits ReportedPost{
	public int id;
	public int repostOf;
	public List<Integer> replyKey=List.of();
	public Poll poll;

	public boolean isReplyToUnknownPost;
	public Privacy privacy=Privacy.PUBLIC;
	public EnumSet<Flag> flags=EnumSet.noneOf(Flag.class);
	public Action action;

	@Override
	public URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return UriBuilder.local().path("posts", String.valueOf(id)).build();
	}

	@Override
	public URI getActivityPubURL(){
		if(activityPubURL!=null)
			return activityPubURL;
		return UriBuilder.local().path("posts", String.valueOf(id)).build();
	}

	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();
		post.fillFromResultSet(res);
		return post;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt("id");
		replyKey=Utils.deserializeIntList(res.getBytes("reply_key"));

		super.fillFromResultSet(res);
		if(deleted)
			return;

		repostOf=res.getInt("repost_of");

		int pollID=res.getInt("poll_id");
		if(!res.wasNull()){
			poll=PostStorage.getPoll(pollID, activityPubID);
		}
		privacy=Privacy.values()[res.getInt("privacy")];
		Utils.deserializeEnumSet(flags, Flag.class, res.getLong("flags"));
		int _action=res.getInt("action");
		if(!res.wasNull())
			action=Action.values()[_action];
	}

	@Override
	public int getReplyLevel(){
		return replyKey.size();
	}

	// for use in templates
	public int getReplyChainElement(int level){
		return replyKey.get(level);
	}

	public boolean canBeManagedBy(Actor actor){
		if(actor==null)
			return false;
		return ownerID==actor.getOwnerID() || authorID==actor.getOwnerID();
	}

	@Override
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

	@Override
	public long getObjectID(){
		return id;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return new NonCachedRemoteImage.PostPhotoArgs(id, index);
	}

	@Override
	public String getPhotoListID(){
		return "posts/"+id;
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
	public void fillFromReport(int reportID, JsonObject jo){
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

	@Override
	public ReportableContentObjectID getReportableObjectID(){
		return new ReportableContentObjectID(ReportableContentObjectType.POST, id);
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
		// Try to find <span class="quote-inline"> first
		Element quoteFallbackEl=root.selectFirst(".quote-inline");
		if(quoteFallbackEl!=null){
			quoteFallbackEl.remove();
			text=root.html();
		}else{
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
	}

	@Override
	public Like.ObjectType getLikeObjectType(){
		return Like.ObjectType.POST;
	}

	@Override
	public Notification.ObjectType getObjectTypeForLikeNotifications(){
		return Notification.ObjectType.POST;
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
		/**
		 * This is an `Announce{Note}` repost that doesn't have its own comment thread and can't be interacted with.
		 * All comments and interactions are delegated to the reposted post.
		 */
		MASTODON_STYLE_REPOST,
		/**
		 * This is a comment (reply key not empty) on a wall-to-wall (owner id != author id) post.
		 */
		TOP_IS_WALL_TO_WALL,
	}

	public enum Action{
		AVATAR_UPDATE,
	}
}
