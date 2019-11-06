package smithereen.routes;

import org.json.JSONException;
import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.ObjectNotFoundException;
import smithereen.activitypub.ActivityPub;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.jsonld.JLD;
import smithereen.jsonld.JLDDocument;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class ActivityPubRoutes{

	private static JSONObject idAndTypeObject(String id, String type){
		JSONObject o=new JSONObject();
		o.put("@id", id);
		o.put("@type", type);
		return o;
	}

	public static Object userActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null && !(user instanceof ForeignUser)){
			return user.asRootActivityPubObject();
		}
		resp.status(404);
		return "";
	}

	public static Object post(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null){
			resp.status(404);
			return "User not found";
		}
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return "Post not found";
		}
		Post post=PostStorage.getPostByID(user.id, postID);
		if(post==null){
			resp.status(404);
			return "Post not found";
		}
		return post.asRootActivityPubObject();
	}

	public static Object inbox(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		if(username.contains("@")){
			resp.status(400);
			return "Only local users accepted here";
		}
		User user=UserStorage.getByUsername(username);
		if(user==null){
			resp.status(404);
			return "User not found";
		}
		return inbox(req, resp, user);
	}

	public static Object sharedInbox(Request req, Response resp) throws SQLException{
		return inbox(req, resp, null);
	}

	public static Object outbox(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null){
			resp.status(404);
			return "User not found";
		}
		int _minID=Utils.parseIntOrDefault(req.queryParams("min_id"), -1);
		int _maxID=Utils.parseIntOrDefault(req.queryParams("max_id"), -1);
		int minID=Math.max(0, _minID);
		int maxID=Math.max(0, _maxID);
		int[] _total={0};
		List<Post> posts=PostStorage.getUserWall(user.id, minID, maxID, _total);
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
			URI baseURI=Config.localURI(user.username+"/activitypub/outbox");
			page.partOf=baseURI;
			if(posts.size()>0){
				page.next=URI.create(baseURI+"?max_id="+posts.get(posts.size()-1).id);
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
		return page.asRootActivityPubObject();
	}

	public static Object externalInteraction(Request req, Response resp){
		// ?type=reblog
		// ?type=favourite
		// ?type=reply
		// user/remote_follow

		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			String ref=req.headers("referer");
			ActivityPubObject remoteObj;
			try{
				remoteObj=ActivityPub.fetchRemoteObject(req.queryParams("uri"));
			}catch(IOException|JSONException x){
				return x.getMessage();
			}
			if(remoteObj instanceof ForeignUser){
				try{
					ForeignUser foreignUser=(ForeignUser) remoteObj;
					System.out.println(foreignUser);
					UserStorage.putOrUpdateForeignUser(foreignUser);
					FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, foreignUser.id);
					if(status==FriendshipStatus.REQUEST_SENT){
						return Utils.wrapError(req, "err_friend_req_already_sent");
					}else if(status==FriendshipStatus.FOLLOWING){
						return Utils.wrapError(req, "err_already_following");
					}else if(status==FriendshipStatus.FRIENDS){
						return Utils.wrapError(req, "err_already_friends");
					}
					JtwigModel model=JtwigModel.newModel().with("user", foreignUser);
					return Utils.renderTemplate(req, "remote_follow", model);
				}catch(Exception x){
					x.printStackTrace();
					return x.toString();
				}
			}
			return "Referer: "+ref+"<hr/>URL: "+req.queryParams("uri")+"<hr/>Object:<br/><pre>"+remoteObj.toString().replace("<", "&lt;")+"</pre>";
		}
		return "";
	}

	public static Object remoteFollow(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
			String username=req.params(":username");
			User _user=UserStorage.getByUsername(username);
			System.out.println(_user);
			if(!(_user instanceof ForeignUser)){
				return Utils.wrapError(req, "err_user_not_found");
			}
			ForeignUser user=(ForeignUser) _user;
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
			if(status==FriendshipStatus.REQUEST_SENT){
				return Utils.wrapError(req, "err_friend_req_already_sent");
			}else if(status==FriendshipStatus.FOLLOWING){
				return Utils.wrapError(req, "err_already_following");
			}else if(status==FriendshipStatus.FRIENDS){
				return Utils.wrapError(req, "err_already_friends");
			}
			try{
				Follow follow=new Follow();
				follow.actor=new LinkOrObject(self.user.activityPubID);
				follow.object=new LinkOrObject(user.activityPubID);
				follow.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "follow"+user.id);
				ActivityPub.postActivity(user.sharedInbox, follow, self.user);
				UserStorage.followUser(self.user.id, user.id);
				return "Success";
			}catch(URISyntaxException ignore){
			}catch(IOException x){
				x.printStackTrace();
				return x.toString();
			}
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
		software.put("version", "0.0.1-pre-alpha");
		root.put("software", software);
		root.put("openRegistrations", false);
		JSONObject usage=new JSONObject();
		JSONObject users=new JSONObject();
		users.put("total", UserStorage.getLocalUserCount());
		usage.put("users", users);
		usage.put("localPosts", 0);
		usage.put("localComments", 0);
		root.put("usage", usage);
		JSONObject services=new JSONObject();
		services.put("inbound", Collections.EMPTY_LIST);
		services.put("outbound", Collections.EMPTY_LIST);
		root.put("services", services);
		JSONObject meta=new JSONObject();
		meta.put("supports_friend_requests", true);
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
		JSONObject obj=JLDDocument.convertToLocalContext(new JSONObject(req.body()));
		System.out.println(obj);
		Activity activity;
		try{
			ActivityPubObject o=ActivityPubObject.parse(obj);
			if(o instanceof Activity)
				activity=(Activity)o;
			else
				throw new IllegalArgumentException("Unsupported object type");
		}catch(Exception x){
			System.out.println(x);
			resp.status(400);
			return x.toString();
		}
		try{
			if(activity.actor.link.getHost().equalsIgnoreCase(Config.domain))
				throw new IllegalArgumentException("User domain must be different from this server");
		}catch(Exception x){
			System.out.println(x);
			resp.status(400);
			return x.toString();
		}
		ForeignUser user=UserStorage.getForeignUserByActivityPubID(activity.actor.link.toString());
		if(user==null){
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
				System.out.println(x);
				resp.status(400);
				return x.toString();
			}
		}
		try{
			verifyHttpSignature(req, user.publicKey);
		}catch(Exception x){
			System.out.println(x);
			resp.status(400);
			return x.toString();
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
				default:
					throw new IllegalArgumentException("Activity type "+activity.getType()+" is not supported");
			}
		}catch(SQLException x){
			throw new SQLException(x);
		}catch(ObjectNotFoundException x){
			resp.status(404);
			return x.toString();
		}catch(Exception x){
			System.out.println(x);
			resp.status(400);
			return x.toString();
		}
		return "";
	}


	private static Object followersOrFollowing(Request req, Response resp, boolean f) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null){
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
		URI baseURI=Config.localURI(username+"/activitypub/"+(f ? "followers" : "following"));
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
		return page.asRootActivityPubObject();
	}

	public static Object userFollowers(Request req, Response resp) throws SQLException{
		return followersOrFollowing(req, resp, true);
	}

	public static Object userFollowing(Request req, Response resp) throws SQLException{
		return followersOrFollowing(req, resp, false);
	}



	private static void verifyHttpSignature(Request req, PublicKey publicKey) throws ParseException, NoSuchAlgorithmException, InvalidKeyException, SignatureException{
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
		sig.initVerify(publicKey);
		sig.update(sigStr.getBytes(StandardCharsets.UTF_8));
		if(!sig.verify(signature))
			throw new IllegalArgumentException("Signature failed to verify");
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
			Post post=(Post) object;
			post.content=Utils.sanitizeHTML(post.content);
			post.owner=post.user=user;
			if(post.summary!=null)
				post.summary=Utils.sanitizeHTML(post.summary);
			PostStorage.putForeignWallPost(post);
		}
	}

	private static void handleFollowActivity(ForeignUser actor, Follow act) throws URISyntaxException, SQLException{
		URI url=act.object.link;
		if(!url.getHost().equalsIgnoreCase(Config.domain))
			throw new IllegalArgumentException("Target user is not from this server");
		String username=url.getPath().substring(1);
		if(!Utils.isValidUsername(username) || Utils.isReservedUsername(username))
			throw new IllegalArgumentException("Invalid username for target user");
		User user=UserStorage.getByUsername(username);
		if(user==null)
			throw new IllegalArgumentException("User not found");
		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING)
			throw new IllegalArgumentException("Already following");
		UserStorage.followUser(actor.id, user.id);

		Accept accept=new Accept();
		accept.actor=new LinkOrObject(user.activityPubID);
		accept.object=new LinkOrObject(act);
		accept.activityPubID=new URI(user.activityPubID.getScheme(), user.activityPubID.getSchemeSpecificPart(), "acceptFollow"+actor.id);
		try{
			ActivityPub.postActivity(actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox, accept, user);
		}catch(Exception x){
			x.printStackTrace();
		}
	}

	private static void handleAcceptActivity(ForeignUser user, Accept act){

	}

	private static void handleLikeActivity(ForeignUser user, Like act){

	}

	private static void handleAnnounceActivity(ForeignUser user, Announce act){

	}

	private static void handleUndoActivity(ForeignUser user, Undo act) throws URISyntaxException, SQLException{
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

				break;
			case "Announce":

				break;
			default:
				throw new IllegalArgumentException("Unsupported activity type in Undo: "+objectActivity.getType());
		}
	}

	private static void handleDeleteActivity(ForeignUser user, Delete act) throws SQLException{
		ActivityPubObject obj;
		if(act.object.object==null || act.object.object instanceof Tombstone){
			URI uri;
			if(act.object.object==null)
				uri=act.object.link;
			else
				uri=act.object.object.activityPubID;
			Post post=PostStorage.getPostByID(uri);
			if(post!=null){
				obj=post;
			}else{
				User _user=UserStorage.getForeignUserByActivityPubID(uri.toString());
				if(_user!=null){
					obj=_user;
				}else{
					obj=null;
				}
			}
		}else{
			obj=act.object.object;
		}
		if(obj==null){
			throw new ObjectNotFoundException("Object being deleted does not exist");
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
			if(!o.equals(actor)){
				throw new IllegalArgumentException("User can only update themselves");
			}
			handleUpdateUserActivity(actor);
		}
	}

	//region Undo subtype handlers

	private static void handleUndoFollowActivity(ForeignUser actor, Follow act) throws SQLException{
		URI url=act.object.link;
		if(!url.getHost().equalsIgnoreCase(Config.domain))
			throw new IllegalArgumentException("Target user is not from this server");
		String username=url.getPath().substring(1);
		if(!Utils.isValidUsername(username) || Utils.isReservedUsername(username))
			throw new IllegalArgumentException("Invalid username for target user");
		User user=UserStorage.getByUsername(username);
		if(user==null)
			throw new IllegalArgumentException("User not found");

		UserStorage.unfriendUser(actor.id, user.id);
		System.out.println(actor.getFullUsername()+" remotely unfollowed "+user.getFullUsername());
	}

	//endregion
	//region Delete subtype handlers

	private static void handleDeletePostActivity(ForeignUser actor, Post post) throws SQLException{
		if(post.canBeManagedBy(actor)){
			PostStorage.deletePost(post.id);
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

	//endregion
}
