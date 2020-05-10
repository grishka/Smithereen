package smithereen.routes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.BuildInfo;
import smithereen.ObjectNotFoundException;
import smithereen.activitypub.ActivityPub;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.data.notifications.NotificationUtils;
import smithereen.jsonld.JLDProcessor;
import smithereen.jsonld.LinkedDataSignatures;
import smithereen.storage.LikeStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class ActivityPubRoutes{

	private static final String CONTENT_TYPE="application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"";

	public static Object userActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user;
		if(username!=null)
			user=UserStorage.getByUsername(username);
		else
			user=UserStorage.getById(Utils.parseIntOrDefault(req.params(":id"), 0));
		if(user!=null && !(user instanceof ForeignUser)){
			resp.type(CONTENT_TYPE);
			return user.asRootActivityPubObject();
		}
		resp.status(404);
		return "";
	}

	public static Object post(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return "Post not found";
		}
		Post post=PostStorage.getPostByID(postID);
		if(post==null || !Config.isLocal(post.activityPubID)){
			resp.status(404);
			return "Post not found";
		}
		resp.type(CONTENT_TYPE);
		return post.asRootActivityPubObject();
	}

	public static Object inbox(Request req, Response resp) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			resp.status(404);
			return "User not found";
		}
		return inbox(req, resp, user);
	}

	public static Object sharedInbox(Request req, Response resp) throws SQLException{
		return inbox(req, resp, null);
	}

	public static Object outbox(Request req, Response resp) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			resp.status(404);
			return "User not found";
		}
		int _minID=Utils.parseIntOrDefault(req.queryParams("min_id"), -1);
		int _maxID=Utils.parseIntOrDefault(req.queryParams("max_id"), -1);
		int minID=Math.max(0, _minID);
		int maxID=Math.max(0, _maxID);
		int[] _total={0};
		List<Post> posts=PostStorage.getUserWall(user.id, minID, maxID, 0, _total);
		int total=_total[0];
		CollectionPage page=new CollectionPage(true);
		page.totalItems=total;
		page.items=new ArrayList<>();
		try{
			for(Post post:posts){
				Create activity=new Create();
				activity.object=new LinkOrObject(post);
				activity.published=post.published;
				activity.to=post.to;
				activity.cc=post.cc;
				activity.actor=new LinkOrObject(post.attributedTo);
				activity.activityPubID=new URI(post.activityPubID.getScheme(), post.activityPubID.getSchemeSpecificPart()+"/activityCreate", null);
				page.items.add(new LinkOrObject(activity));
			}
			URI baseURI=Config.localURI("/users/"+user.id+"/outbox");
			page.partOf=baseURI;
			if(posts.size()>0){
				page.next=URI.create(baseURI+"?max_id="+(posts.get(posts.size()-1).id-1));
				page.prev=URI.create(baseURI+"?min_id="+posts.get(0).id);
			}
			if(_minID!=-1)
				page.activityPubID=URI.create(baseURI+"?min_id="+minID);
			else
				page.activityPubID=URI.create(baseURI+"?max_id="+maxID);
			if(_minID==-1 && _maxID==-1){
				ActivityPubCollection collection=new ActivityPubCollection(true);
				collection.activityPubID=page.partOf;
				collection.totalItems=total;
				collection.first=new LinkOrObject(page);
				return collection.asRootActivityPubObject();
			}
		}catch(URISyntaxException ignore){}
		resp.type(CONTENT_TYPE);
		return page.asRootActivityPubObject();
	}

	public static Object externalInteraction(Request req, Response resp, Account self) throws SQLException{
		// ?type=reblog
		// ?type=favourite
		// ?type=reply
		// user/remote_follow
		String ref=req.headers("referer");
		ActivityPubObject remoteObj;
		try{
			URI uri=new URI(req.queryParams("uri"));
			if(!"https".equals(uri.getScheme()) && !(Config.useHTTP && "http".equals(uri.getScheme()))){
				// try parsing as "username@domain" or "acct:username@domain"
				String rawUri=uri.getSchemeSpecificPart();
				if(rawUri.matches("^[^@\\s]+@[^@\\s]{4,}$")){
					String[] parts=rawUri.split("@");
					uri=ActivityPub.resolveUsername(parts[0], parts[1]);
					if(!"https".equals(uri.getScheme()) && !(Config.useHTTP && "http".equals(uri.getScheme()))){
						return "Failed to resolve object URI via webfinger";
					}
				}else{
					return "Invalid remote object URI";
				}
			}
			remoteObj=ActivityPub.fetchRemoteObject(uri.toString());
			if(remoteObj==null){
				return "Error fetching remote object";
			}
			if(remoteObj.activityPubID.getHost()==null || uri.getHost()==null || !remoteObj.activityPubID.getHost().equals(uri.getHost())){
				return "Object ID host doesn't match URI host";
			}
		}catch(IOException|JSONException|URISyntaxException x){
			return x.getMessage();
		}
		if(remoteObj instanceof ForeignUser){
			try{
				ForeignUser foreignUser=(ForeignUser) remoteObj;
//					System.out.println(foreignUser);
				UserStorage.putOrUpdateForeignUser(foreignUser);
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, foreignUser.id);
				if(status==FriendshipStatus.REQUEST_SENT){
					return Utils.wrapError(req, resp, "err_friend_req_already_sent");
				}else if(status==FriendshipStatus.FOLLOWING){
					return Utils.wrapError(req, resp, "err_already_following");
				}else if(status==FriendshipStatus.FRIENDS){
					return Utils.wrapError(req, resp, "err_already_friends");
				}
				JtwigModel model=JtwigModel.newModel().with("user", foreignUser);
				return Utils.renderTemplate(req, "remote_follow", model);
			}catch(Exception x){
				x.printStackTrace();
				return x.toString();
			}
		}else if(remoteObj instanceof Post){
			try{
				Post post=(Post) remoteObj;
				if(post.user==null){
					ActivityPubObject obj=ActivityPub.fetchRemoteObject(post.attributedTo.toString());
					if(obj instanceof ForeignUser){
						post.user=(ForeignUser) obj;
						UserStorage.putOrUpdateForeignUser((ForeignUser) obj);
					}else{
						throw new IllegalArgumentException("Error fetching post author");
					}
				}
				if(post.owner==null){
					throw new UnsupportedOperationException("no post owner user - not yet implemented");
				}
				PostStorage.putForeignWallPost(post);
				resp.redirect("/posts/"+post.id);
				return "";
			}catch(Exception x){
				x.printStackTrace();
				return x.toString();
			}
		}
		return "Referer: "+Utils.sanitizeHTML(ref)+"<hr/>URL: "+Utils.sanitizeHTML(req.queryParams("uri"))+"<hr/>Object:<br/><pre>"+Utils.sanitizeHTML(remoteObj.toString())+"</pre>";
	}

	public static Object remoteFollow(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User _user=UserStorage.getByUsername(username);
//		System.out.println(_user);
		if(!(_user instanceof ForeignUser)){
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
		ForeignUser user=(ForeignUser) _user;
		FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
		if(status==FriendshipStatus.REQUEST_SENT){
			return Utils.wrapError(req, resp, "err_friend_req_already_sent");
		}else if(status==FriendshipStatus.FOLLOWING){
			return Utils.wrapError(req, resp, "err_already_following");
		}else if(status==FriendshipStatus.FRIENDS){
			return Utils.wrapError(req, resp, "err_already_friends");
		}
		try{
			String msg=req.queryParams("message");
			if(user.supportsFriendRequests()){
				UserStorage.putFriendRequest(self.user.id, user.id, msg, false);
			}else{
				UserStorage.followUser(self.user.id, user.id, false);
			}
			Follow follow=new Follow();
			follow.actor=new LinkOrObject(self.user.activityPubID);
			follow.object=new LinkOrObject(user.activityPubID);
			follow.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "follow"+user.id);
			ActivityPub.postActivity(user.inbox, follow, self.user);
			if(user.supportsFriendRequests()){
				Offer offer=new Offer();
				offer.actor=new LinkOrObject(self.user.activityPubID);
				offer.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "friend_request"+user.id);
				if(StringUtils.isNotEmpty(msg)){
					offer.content=msg;
				}
				Follow revFollow=new Follow();
				revFollow.actor=new LinkOrObject(user.activityPubID);
				revFollow.object=new LinkOrObject(self.user.activityPubID);
				offer.object=new LinkOrObject(revFollow);
				ActivityPub.postActivity(user.inbox, offer, self.user);
			}
			return "Success";
		}catch(URISyntaxException ignore){
		}catch(IOException x){
			x.printStackTrace();
			return x.toString();
		}
		return "";
	}

	public static Object nodeInfo(Request req, Response resp) throws SQLException{
		resp.type("application/json; profile=\"http://nodeinfo.diaspora.software/ns/schema/2.0#\"");
		JSONObject root=new JSONObject();
		root.put("version", "2.0");
		root.put("protocols", Collections.singletonList("activitypub"));
		JSONObject software=new JSONObject();
		software.put("name", "smithereen");
		software.put("version", BuildInfo.VERSION);
		root.put("software", software);
		root.put("openRegistrations", false);
		JSONObject usage=new JSONObject();
		JSONObject users=new JSONObject();
		users.put("total", UserStorage.getLocalUserCount());
		usage.put("users", users);
		usage.put("localPosts", PostStorage.getLocalPostCount(false));
		usage.put("localComments", PostStorage.getLocalPostCount(true));
		root.put("usage", usage);
		JSONObject services=new JSONObject();
		services.put("inbound", Collections.EMPTY_LIST);
		services.put("outbound", Collections.EMPTY_LIST);
		root.put("services", services);
		JSONObject meta=new JSONObject();
		JSONArray caps=new JSONArray();
		caps.put("friendRequests");
		meta.put("capabilities", caps);
		root.put("metadata", meta);
		return root;
	}

	private static Object inbox(Request req, Response resp, User owner) throws SQLException{
		if(req.headers("digest")!=null){
			if(!verifyHttpDigest(req.headers("digest"), req.bodyAsBytes())){
				resp.status(400);
				return "Digest verification failed";
			}
		}
		String body=req.body();
		System.out.println(body);
		JSONObject rawActivity=new JSONObject(body);
		JSONObject obj=JLDProcessor.convertToLocalContext(rawActivity);
		Activity activity;
		try{
			ActivityPubObject o=ActivityPubObject.parse(obj);
			if(o instanceof Activity)
				activity=(Activity)o;
			else
				throw new IllegalArgumentException("Unsupported object type");
		}catch(Exception x){
			x.printStackTrace();
			resp.status(400);
			return x.toString();
		}
		try{
			if(Config.isLocal(activity.actor.link))
				throw new IllegalArgumentException("User domain must be different from this server");
		}catch(Exception x){
			x.printStackTrace();
			resp.status(400);
			return x.toString();
		}
		ForeignUser user=UserStorage.getForeignUserByActivityPubID(activity.actor.link);
		if(user==null || user.lastUpdated==null || System.currentTimeMillis()-user.lastUpdated.getTime()>24L*60*60*1000){
			try{
				ActivityPubObject userObj=ActivityPub.fetchRemoteObject(activity.actor.link.toString());
				if(!(userObj instanceof ForeignUser)){
					resp.status(400);
					return "Actor object must have Person type";
				}
				user=(ForeignUser) userObj;
				UserStorage.putOrUpdateForeignUser(user);
			}catch(SQLException x){
				throw new SQLException(x);
			}catch(Exception x){
				x.printStackTrace();
				resp.status(400);
				return x.toString();
			}
		}

		User httpSigOwner;
		try{
			httpSigOwner=verifyHttpSignature(req, user);
		}catch(Exception x){
			x.printStackTrace();
			resp.status(400);
			return x.toString();
		}

		// if the activity has an LD-signature, verify that and allow any (cached) user to sign the HTTP signature
		// if it does not, the owner of the HTTP signature must match the actor
		if(rawActivity.has("signature")){
			JSONObject sig=rawActivity.getJSONObject("signature");
			try{
				URI keyID=URI.create(sig.getString("creator"));
				URI userID=Utils.userIdFromKeyId(keyID);
				if(!userID.equals(user.activityPubID)){
					resp.status(400);
					return "LD-signature creator is not activity actor";
				}
				if(!LinkedDataSignatures.verify(rawActivity, user.publicKey)){
					resp.status(400);
					return "LD-signature verification failed";
				}
				System.out.println("verified LD signature by "+userID);
			}catch(Exception x){
				x.printStackTrace();
				resp.status(400);
				return x.toString();
			}
		}else{
			if(!user.equals(httpSigOwner)){
				resp.status(400);
				return "In the absence of an LD-signature, HTTP signature must be made by the activity actor";
			}
			System.out.println("verified HTTP signature by "+httpSigOwner.activityPubID);
		}


		try{
			switch(activity.getType()){
				case "Create":
					handleCreateActivity(user, (Create) activity);
					break;
				case "Announce":
					handleAnnounceActivity(user, (Announce) activity);
					break;
				case "Follow":
					handleFollowActivity(user, (Follow) activity);
					break;
				case "Accept":
					handleAcceptActivity(user, (Accept) activity);
					break;
				case "Like":
					handleLikeActivity(user, (Like) activity);
					break;
				case "Undo":
					handleUndoActivity(user, (Undo) activity);
					break;
				case "Delete":
					handleDeleteActivity(user, (Delete) activity);
					break;
				case "Update":
					handleUpdateActivity(user, (Update) activity);
					break;
				case "Reject":
					handleRejectActivity(user, (Reject)activity);
					break;
				case "Offer":
					handleOfferActivity(user, (Offer)activity);
					break;
				default:
					throw new IllegalArgumentException("Activity type "+activity.getType()+" is not supported");
			}
		}catch(SQLException x){
			x.printStackTrace();
			throw new SQLException(x);
		}catch(ObjectNotFoundException x){
			x.printStackTrace();
			resp.status(404);
			return x.toString();
		}catch(Exception x){
			x.printStackTrace();
			resp.status(400);
			return x.toString();
		}
		return "";
	}


	private static Object followersOrFollowing(Request req, Response resp, boolean f) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			resp.status(404);
			return "User not found";
		}
		int[] _total={0};
		int pageIndex=Math.max(1, Utils.parseIntOrDefault(req.queryParams("page"), 1));
		int offset=(pageIndex-1)*50;
		int count=50;
		List<URI> followers=UserStorage.getUserFollowerURIs(user.id, f, offset, count, _total);
		int total=_total[0];
		int lastPage=total/50;
		CollectionPage page=new CollectionPage(true);
		ArrayList<LinkOrObject> list=new ArrayList<>();
		for(URI uri:followers){
			list.add(new LinkOrObject(uri));
		}
		page.items=list;
		page.totalItems=total;
		URI baseURI=Config.localURI("/users/"+user.id+"/"+(f ? "followers" : "following"));
		page.activityPubID=URI.create(baseURI+"?page="+pageIndex);
		page.partOf=baseURI;
		if(pageIndex>1){
			page.first=new LinkOrObject(URI.create(baseURI+"?page=1"));
			page.prev=URI.create(baseURI+"?page="+(pageIndex-1));
		}
		if(pageIndex<lastPage){
			page.last=URI.create(baseURI+"?page="+lastPage);
			page.next=URI.create(baseURI+"?page="+(pageIndex+1));
		}
		if(pageIndex==1 && req.queryParams("page")==null){
			ActivityPubCollection collection=new ActivityPubCollection(true);
			collection.totalItems=total;
			collection.first=new LinkOrObject(page);
			collection.activityPubID=page.partOf;
			return collection.asRootActivityPubObject();
		}
		resp.type(CONTENT_TYPE);
		return page.asRootActivityPubObject();
	}

	public static Object userFollowers(Request req, Response resp) throws SQLException{
		return followersOrFollowing(req, resp, true);
	}

	public static Object userFollowing(Request req, Response resp) throws SQLException{
		return followersOrFollowing(req, resp, false);
	}



	private static User verifyHttpSignature(Request req, User userHint) throws ParseException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, SQLException{
		String sigHeader=req.headers("Signature");
		if(sigHeader==null)
			throw new IllegalArgumentException("Request is missing Signature header");
		String[] parts=sigHeader.split(",");
		String keyId=null;
		byte[] signature=null;
		List<String> headers=null;
		for(String part:parts){
			String[] kv=part.split("=", 2);
			String key=kv[0];
			String value=kv[1];
			if(value.charAt(0)=='"'){
				value=value.substring(1, value.length()-1);
			}
			switch(key){
				case "keyId":
					keyId=value;
					break;
				case "signature":
					signature=Base64.getDecoder().decode(value);
					break;
				case "headers":
					headers=Arrays.asList(value.split(" "));
					break;
			}
		}
		if(keyId==null)
			throw new IllegalArgumentException("Signature header is missing keyId field");
		if(signature==null)
			throw new IllegalArgumentException("Signature header is missing signature field");
		if(headers==null)
			throw new IllegalArgumentException("Signature header is missing headers field");
		if(!headers.contains("(request-target)"))
			throw new IllegalArgumentException("(request-target) is not in signed headers");
		if(!headers.contains("date"))
			throw new IllegalArgumentException("date is not in signed headers");
		if(!headers.contains("host"))
			throw new IllegalArgumentException("host is not in signed headers");

		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		long unixtime=dateFormat.parse(req.headers("date")).getTime();
		long now=System.currentTimeMillis();
		long diff=now-unixtime;
		if(diff>30000L)
			throw new IllegalArgumentException("Date is too far in the future (difference: "+diff+"ms)");
		if(diff<-30000L)
			throw new IllegalArgumentException("Date is too far in the past (difference: "+diff+"ms)");

		URI userID=Utils.userIdFromKeyId(URI.create(keyId));
		User user;
		if(userHint.activityPubID.equals(userID))
			user=userHint;
		else
			user=UserStorage.getUserByActivityPubID(userID);
		if(user==null)
			throw new IllegalArgumentException("Request signed by unknown user: "+userID);

		ArrayList<String> sigParts=new ArrayList<>();
		for(String header:headers){
			String value;
			if(header.equals("(request-target)")){
				value=req.requestMethod().toLowerCase()+" "+req.pathInfo();
			}else{
				value=req.headers(header);
			}
			sigParts.add(header+": "+value);
		}
		String sigStr=String.join("\n", sigParts);
		Signature sig=Signature.getInstance("SHA256withRSA");
		sig.initVerify(user.publicKey);
		sig.update(sigStr.getBytes(StandardCharsets.UTF_8));
		if(!sig.verify(signature)){
			System.out.println("Failed sig: "+sigHeader);
			System.out.println("Failed sig string: '"+sigStr+"'");
			throw new IllegalArgumentException("Signature failed to verify");
		}
		return user;
	}

	private static boolean verifyHttpDigest(String digestHeader, byte[] bodyData){
		String[] parts=digestHeader.split(",");
		for(String part:parts){
			String[] kv=part.split("=", 2);
			if(kv[0].equalsIgnoreCase("sha-256")){
				byte[] hash=Base64.getDecoder().decode(kv[1]);
				try{
					MessageDigest md=MessageDigest.getInstance("SHA-256");
					md.update(bodyData);
					byte[] myHash=md.digest();
					return Arrays.equals(hash, myHash);
				}catch(Exception x){
					return false;
				}
			}
		}
		return true;
	}





	//region Activity handlers

	private static void handleCreateActivity(ForeignUser user, Create act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Links are not accepted in Create activity");
		ActivityPubObject object=act.object.object;
		if(!object.attributedTo.equals(user.activityPubID))
			throw new IllegalArgumentException("object.attributedTo and actor.id must match");
		if(object instanceof Post){
			if(PostStorage.getPostByID(object.activityPubID)!=null){
				// Already exists. Ignore and return 200 OK.
				return;
			}
			Post post=(Post) object;
			if(post.user==null || post.user.id!=user.id)
				throw new IllegalArgumentException("Can only create posts for self");
			if(post.owner==null)
				throw new IllegalArgumentException("Unknown wall owner (from partOf, which must be an outbox URI if present)");
			post.content=Utils.sanitizeHTML(post.content);
			boolean isPublic=false;
			if(post.to==null || post.to.isEmpty()){
				if(post.cc==null || post.cc.isEmpty()){
					throw new IllegalArgumentException("to or cc are both empty");
				}else{
					for(LinkOrObject cc:post.cc){
						if(cc.link==null)
							throw new IllegalArgumentException("post.cc must only contain links");
						if(ActivityPub.isPublic(cc.link)){
							isPublic=true;
							break;
						}
					}
				}
			}else{
				for(LinkOrObject to:post.to){
					if(to.link==null)
						throw new IllegalArgumentException("post.to must only contain links");
					if(ActivityPub.isPublic(to.link)){
						isPublic=true;
						break;
					}
				}
			}
			if(!isPublic)
				throw new IllegalArgumentException("Only public posts are supported");
			if(post.user==post.owner && post.inReplyTo==null){
				URI followers=user.getFollowersURL();
				boolean addressesAnyFollowers=false;
				for(LinkOrObject l:post.to){
					if(followers.equals(l.link)){
						addressesAnyFollowers=true;
						break;
					}
				}
				if(!addressesAnyFollowers){
					for(LinkOrObject l:post.cc){
						if(followers.equals(l.link)){
							addressesAnyFollowers=true;
							break;
						}
					}
				}
				if(!addressesAnyFollowers){
					System.out.println("Dropping this post because it's public but doesn't address any followers");
					return;
				}
			}
			if(post.summary!=null)
				post.summary=Utils.sanitizeHTML(post.summary);
			if(post.tag!=null){
				for(ActivityPubObject tag:post.tag){
					if(tag instanceof Mention){
						URI uri=((Mention) tag).href;
						User mentionedUser=UserStorage.getUserByActivityPubID(uri);
						if(mentionedUser!=null){
							if(post.mentionedUsers.isEmpty())
								post.mentionedUsers=new ArrayList<>();
							if(!post.mentionedUsers.contains(mentionedUser))
								post.mentionedUsers.add(mentionedUser);
						}
					}
				}
			}
			if(post.inReplyTo!=null){
				if(post.inReplyTo.equals(post.activityPubID))
					throw new IllegalArgumentException("Post can't be a reply to itself. This makes no sense.");
				Post parent=PostStorage.getPostByID(post.inReplyTo);
				if(parent!=null){
					post.setParent(parent);
					PostStorage.putForeignWallPost(post);
					NotificationUtils.putNotificationsForPost(post, parent);
				}else{
					System.out.println("Don't have parent post "+post.inReplyTo+" for "+post.activityPubID);
					ActivityPubWorker.getInstance().fetchReplyThread(post);
				}
			}else{
				PostStorage.putForeignWallPost(post);
				NotificationUtils.putNotificationsForPost(post, null);
			}
		}else{
			throw new IllegalArgumentException("Type "+object.getType()+" not supported in Create");
		}
	}

	private static void handleFollowActivity(ForeignUser actor, Follow act) throws URISyntaxException, SQLException{
		URI url=act.object.link;
		if(!Config.isLocal(url))
			throw new IllegalArgumentException("Target user is not from this server");
		User user=UserStorage.getUserByActivityPubID(url);
		if(user==null)
			throw new IllegalArgumentException("User not found");
		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING)
			throw new IllegalArgumentException("Already following");
		UserStorage.followUser(actor.id, user.id, true);
		UserStorage.deleteFriendRequest(actor.id, user.id);

		Accept accept=new Accept();
		accept.actor=new LinkOrObject(user.activityPubID);
		accept.object=new LinkOrObject(act);
		accept.activityPubID=new URI(user.activityPubID.getScheme(), user.activityPubID.getSchemeSpecificPart(), "acceptFollow"+actor.id);
		try{
			ActivityPub.postActivity(actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox, accept, user);
		}catch(Exception x){
			x.printStackTrace();
		}

		Notification n=new Notification();
		n.type=status==FriendshipStatus.REQUEST_RECVD ? Notification.Type.FRIEND_REQ_ACCEPT : Notification.Type.FOLLOW;
		n.actorID=actor.id;
		NotificationsStorage.putNotification(user.id, n);
	}

	private static void handleAcceptActivity(ForeignUser user, Accept act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Undo activity should include a complete object of the activity being undone");
		ActivityPubObject object=act.object.object;
		if(!(object instanceof Activity))
			throw new IllegalArgumentException("Undo activity object must be a subtype of Activity");
		Activity objectActivity=(Activity)object;
		switch(objectActivity.getType()){
			case "Follow":
				handleAcceptFollowActivity(user, (Follow)objectActivity);
				break;
			default:
				throw new IllegalArgumentException("Unsupported activity type in Accept: "+objectActivity.getType());
		}
	}

	private static void handleLikeActivity(ForeignUser user, Like act) throws SQLException{
		if(act.object.link==null)
			throw new IllegalArgumentException("Like object must be a link");
		Post post=PostStorage.getPostByID(act.object.link);
		if(post==null)
			throw new ObjectNotFoundException("Post not found");
		LikeStorage.setPostLiked(user.id, post.id, true);
		if(!(post.user instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.LIKE;
			n.actorID=user.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.user.id, n);
		}
	}

	private static void handleAnnounceActivity(ForeignUser user, Announce act) throws SQLException{
		URI objURI=act.object.link;
		if(objURI==null)
			throw new IllegalArgumentException("Announce object must be a link");
		if(Config.isLocal(objURI)){
			Post post=PostStorage.getPostByID(objURI);
			if(post==null)
				throw new ObjectNotFoundException("Post not found");
			long time=act.published==null ? System.currentTimeMillis() : act.published.getTime();
			NewsfeedStorage.putRetoot(user.id, post.id, new Timestamp(time));
			if(!(post.user instanceof ForeignUser)){
				Notification n=new Notification();
				n.type=Notification.Type.RETOOT;
				n.actorID=user.id;
				n.objectID=post.id;
				n.objectType=Notification.ObjectType.POST;
				NotificationsStorage.putNotification(post.user.id, n);
			}
		}else{
			try{
				ActivityPubObject obj=ActivityPub.fetchRemoteObject(objURI.toString());
				if(obj instanceof Post){
					Post post=(Post) obj;
					ForeignUser author=(ForeignUser) UserStorage.getUserByActivityPubID(post.attributedTo);
					if(author==null){
						ActivityPubObject _author=ActivityPub.fetchRemoteObject(post.attributedTo.toString());
						if(!(_author instanceof ForeignUser)){
							throw new IllegalArgumentException("Post author isn't a user");
						}
						author=(ForeignUser) _author;
						UserStorage.putOrUpdateForeignUser(author);
					}
					post.owner=post.user=author;
					PostStorage.putForeignWallPost(post);
					long time=act.published==null ? System.currentTimeMillis() : act.published.getTime();
					NewsfeedStorage.putRetoot(user.id, post.id, new Timestamp(time));
				}else if(obj==null){
					throw new IllegalArgumentException("Failed to fetch reposted object");
				}else{
					throw new IllegalArgumentException("Unsupported object type in Announce: "+obj.getType());
				}
			}catch(IOException e){
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to fetch reposted object: "+e.getMessage());
			}
		}
	}

	private static void handleUndoActivity(ForeignUser user, Undo act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Undo activity should include a complete object of the activity being undone");
		ActivityPubObject object=act.object.object;
		if(!(object instanceof Activity))
			throw new IllegalArgumentException("Undo activity object must be a subtype of Activity");
		Activity objectActivity=(Activity)object;
		if(!objectActivity.actor.link.equals(user.activityPubID))
			throw new IllegalArgumentException("Actor in Undo and in the activity being undone don't match");
		switch(objectActivity.getType()){
			case "Follow":
				handleUndoFollowActivity(user, (Follow)objectActivity);
				break;
			case "Like":
				handleUndoLikeActivity(user, (Like)objectActivity);
				break;
			case "Announce":
				handleUndoAnnounceActivity(user, (Announce)objectActivity);
				break;
			case "Accept":
				handleUndoAcceptActivity(user, (Accept)objectActivity);
				break;
			default:
				throw new IllegalArgumentException("Unsupported activity type in Undo: "+objectActivity.getType());
		}
	}

	private static void handleDeleteActivity(ForeignUser user, Delete act) throws SQLException{
		ActivityPubObject obj;
		URI uri;
		if(act.object.object==null || act.object.object instanceof Tombstone){
			if(act.object.object==null)
				uri=act.object.link;
			else
				uri=act.object.object.activityPubID;
			Post post=PostStorage.getPostByID(uri);
			if(post!=null){
				obj=post;
			}else{
				User _user=UserStorage.getForeignUserByActivityPubID(uri);
				if(_user!=null){
					obj=_user;
				}else{
					obj=null;
				}
			}
		}else{
			obj=act.object.object;
			uri=obj.activityPubID;
		}
		if(obj==null){
			System.out.println("Delete: Object '"+uri+"' does not exist");
			return;
		}

		if(obj instanceof Post){
			handleDeletePostActivity(user, (Post) obj);
		}else if(obj instanceof User){
			User o=(User) obj;
			if(!o.equals(user)){
				throw new IllegalArgumentException("User can only delete themselves");
			}
			handleDeleteUserActivity(user);
		}
	}

	private static void handleUpdateActivity(ForeignUser actor, Update act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Update activity is required to have an entire object inlined");
		if(act.object.object instanceof Post){
			handleUpdatePostActivity(actor, (Post) act.object.object);
		}else if(act.object.object instanceof User){
			User o=(User)act.object.object;
			if(!o.activityPubID.equals(actor.activityPubID)){
				throw new IllegalArgumentException("User can only update themselves");
			}
			handleUpdateUserActivity(actor);
		}
	}

	private static void handleRejectActivity(ForeignUser actor, Reject act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Reject activity should include a complete object of the activity being rejected");
		ActivityPubObject object=act.object.object;
		if(!(object instanceof Activity))
			throw new IllegalArgumentException("Reject activity object must be a subtype of Activity");
		Activity objectActivity=(Activity)object;
		switch(objectActivity.getType()){
			case "Follow":
				handleRejectFollowActivity(actor, (Follow)objectActivity);
				break;
			case "Offer":
				handleRejectOfferActivity(actor, (Offer)objectActivity);
				break;
			default:
				throw new IllegalArgumentException("Unsupported activity type in Reject: "+objectActivity.getType());
		}
	}

	private static void handleOfferActivity(ForeignUser actor, Offer act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Offer should include an object");
		switch(act.object.object.getType()){
			case "Follow":
				handleFriendRequestActivity(actor, (Follow) act.object.object, act.content);
				break;
		}
	}

	//region Undo subtype handlers

	private static void handleUndoFollowActivity(ForeignUser actor, Follow act) throws SQLException{
		URI url=act.object.link;
		if(!Config.isLocal(url))
			throw new IllegalArgumentException("Target user is not from this server");
		User user=UserStorage.getUserByActivityPubID(url);
		if(user==null)
			throw new IllegalArgumentException("User not found");

		UserStorage.unfriendUser(actor.id, user.id);
		System.out.println(actor.getFullUsername()+" remotely unfollowed "+user.getFullUsername());
	}

	private static void handleUndoAcceptActivity(ForeignUser actor, Accept act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Undo activity should include a complete object of the activity being undone");
		ActivityPubObject object=act.object.object;
		if(!(object instanceof Activity))
			throw new IllegalArgumentException("Undo activity object must be a subtype of Activity");
		Activity objectActivity=(Activity)object;
		switch(objectActivity.getType()){
			case "Follow":
				handleUndoAcceptFollowActivity(actor, (Follow)objectActivity);
				break;
			default:
				throw new IllegalArgumentException("Unsupported activity type in Accept: "+objectActivity.getType());
		}
	}

	private static void handleUndoAcceptFollowActivity(ForeignUser actor, Follow activity) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(activity.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		UserStorage.setFollowAccepted(follower.id, actor.id, false);
	}

	private static void handleUndoAnnounceActivity(ForeignUser actor, Announce activity) throws SQLException{
		if(!actor.activityPubID.equals(activity.actor.link))
			throw new IllegalArgumentException("Actors must match");
		Post post=PostStorage.getPostByID(activity.object.link);
		if(post==null)
			throw new ObjectNotFoundException("Post not found");
		NewsfeedStorage.deleteRetoot(actor.id, post.id);
	}

	private static void handleUndoLikeActivity(ForeignUser actor, Like act) throws SQLException{
		if(act.object.link==null)
			throw new IllegalArgumentException("Like object must be a link");
		Post post=PostStorage.getPostByID(act.object.link);
		if(post==null)
			throw new ObjectNotFoundException("Post not found");
		LikeStorage.setPostLiked(actor.id, post.id, false);
	}

	//endregion
	//region Delete subtype handlers

	private static void handleDeletePostActivity(ForeignUser actor, Post post) throws SQLException{
		System.out.println("delete post here");
		if(post.canBeManagedBy(actor)){
			PostStorage.deletePost(post.id);
			NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
		}else{
			throw new IllegalArgumentException("No access to delete this post");
		}
	}

	private static void handleDeleteUserActivity(ForeignUser actor){
		System.out.println("Deleting users is not implemented");
	}

	//endregion

	//region Update subtype handlers

	private static void handleUpdatePostActivity(ForeignUser actor, Post post) throws SQLException{
		if(post.canBeManagedBy(actor)){
			PostStorage.putForeignWallPost(post);
		}else{
			throw new IllegalArgumentException("No access to update this post");
		}
	}

	private static void handleUpdateUserActivity(ForeignUser actor) throws SQLException{
		UserStorage.putOrUpdateForeignUser(actor);
	}

	//endregion

	//region Accept subtype handlers
	private static void handleAcceptFollowActivity(ForeignUser actor, Follow activity) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(activity.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		UserStorage.setFollowAccepted(follower.id, actor.id, true);
	}
	//endregion

	//region Reject subtype handlers
	private static void handleRejectFollowActivity(ForeignUser actor, Follow activity) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(activity.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		UserStorage.unfriendUser(follower.id, actor.id);
	}

	private static void handleRejectOfferActivity(ForeignUser actor, Offer act) throws SQLException{
		if(act.object.object==null)
			throw new IllegalArgumentException("Reject{Offer} must contain an object in offer.object");
		switch(act.object.object.getType()){
			case "Follow":
				handleRejectFriendRequestActivity(actor, (Follow) act.object.object);
				break;
			default:
				throw new IllegalArgumentException("Unsupported object type in Reject{Offer}: "+act.object.object.getType());
		}
	}

	private static void handleRejectFriendRequestActivity(ForeignUser actor, Follow act) throws SQLException{
		if(act.object.link==null)
			throw new IllegalArgumentException("follow.object must be a link");
		if(act.actor.link==null)
			throw new IllegalArgumentException("follow.actor must be a link");
		if(!act.actor.link.equals(actor.activityPubID))
			throw new IllegalArgumentException("follow.object must match reject.actor");
		User user=UserStorage.getUserByActivityPubID(act.object.link);
		if(user==null)
			throw new ObjectNotFoundException("User not found");
		UserStorage.deleteFriendRequest(actor.id, user.id);
	}
	//endregion

	//region Offer subtype handlers
	private static void handleFriendRequestActivity(ForeignUser actor, Follow act, String msg) throws SQLException{
		if(!act.object.link.equals(actor.activityPubID))
			throw new IllegalArgumentException("Friend request must be Offer{Follow} with offer.actor==follow.object");
		if(act.actor.link==null)
			throw new IllegalArgumentException("Follow actor must be a link");
		User user=UserStorage.getUserByActivityPubID(act.actor.link);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException("User not found");

		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.NONE || status==FriendshipStatus.FOLLOWING){
			UserStorage.putFriendRequest(actor.id, user.id, msg, true);
		}else if(status==FriendshipStatus.FRIENDS){
			throw new IllegalArgumentException("Already friends");
		}else if(status==FriendshipStatus.REQUEST_RECVD){
			throw new IllegalArgumentException("Incoming friend request already received");
		}else{ // REQ_SENT
			throw new IllegalArgumentException("Friend request already sent");
		}
	}
	//endregion

	//endregion
}
