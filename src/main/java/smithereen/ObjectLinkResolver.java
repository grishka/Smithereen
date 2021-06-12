package smithereen;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

import static smithereen.Utils.parseIntOrDefault;

public class ObjectLinkResolver{

	private static final Pattern POSTS=Pattern.compile("^/posts/(\\d+)$");
	private static final Pattern USERS=Pattern.compile("^/users/(\\d+)$");
	private static final Pattern GROUPS=Pattern.compile("^/groups/(\\d+)$");

	private static Post getPost(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException("Invalid local post ID");
		Post post=PostStorage.getPostByID(id, false);
		if(post==null)
			throw new ObjectNotFoundException("Post with ID "+id+" not found");
		return post;
	}

	private static User getUser(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException();
		return user;
	}

	private static Group getGroup(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Group group=GroupStorage.getByID(id);
		if(group==null || group instanceof ForeignGroup)
			throw new ObjectNotFoundException();
		return group;
	}

	public static ActivityPubObject resolve(URI link) throws SQLException{
		return resolve(link, ActivityPubObject.class, false, true, false);
	}

	@NotNull
	public static <T extends ActivityPubObject> T resolve(URI link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch) throws SQLException{
		if(Config.DEBUG)
			System.out.println("Resolving ActivityPub link: "+link+", expected type: "+expectedType.getName());
		if(!Config.isLocal(link)){
			if(!forceRefetch){
				User user=UserStorage.getUserByActivityPubID(link);
				if(user!=null)
					return ensureTypeAndCast(user, expectedType);
				ForeignGroup group=GroupStorage.getForeignGroupByActivityPubID(link);
				if(group!=null)
					return ensureTypeAndCast(group, expectedType);
				Post post=PostStorage.getPostByID(link);
				if(post!=null)
					return ensureTypeAndCast(post, expectedType);
			}
			if(allowFetching){
				try{
					ActivityPubObject obj=ActivityPub.fetchRemoteObject(link.toString());
					if(obj!=null){
						T o=ensureTypeAndCast(obj, expectedType);
						o.resolveDependencies(allowFetching, allowStorage);
						if(allowStorage)
							storeOrUpdateRemoteObject(o);
						return o;
					}
				}catch(IOException x){
					x.printStackTrace();
				}
			}
			throw new ObjectNotFoundException("Can't resolve remote object: "+link);
		}

		Matcher matcher=POSTS.matcher(link.getPath());
		if(matcher.find()){
			return ensureTypeAndCast(getPost(matcher.group(1)), expectedType);
		}

		matcher=USERS.matcher(link.getPath());
		if(matcher.find()){
			return ensureTypeAndCast(getUser(matcher.group(1)), expectedType);
		}

		matcher=GROUPS.matcher(link.getPath());
		if(matcher.find()){
			return ensureTypeAndCast(getGroup(matcher.group(1)), expectedType);
		}

		throw new ObjectNotFoundException("Invalid local URI");
	}

	public static void storeOrUpdateRemoteObject(ActivityPubObject o) throws SQLException{
		o.storeDependencies();
		if(o instanceof ForeignUser)
			UserStorage.putOrUpdateForeignUser((ForeignUser) o);
		else if(o instanceof ForeignGroup)
			GroupStorage.putOrUpdateForeignGroup((ForeignGroup) o);
		else if(o instanceof Post)
			PostStorage.putForeignWallPost((Post) o);
	}

	private static <T extends ActivityPubObject> T ensureTypeAndCast(ActivityPubObject obj, Class<T> type){
		if(type.isInstance(obj))
			return type.cast(obj);
		throw new IllegalStateException("Expected object of type "+type.getName()+", but got "+obj.getClass().getName()+" instead");
	}
}
