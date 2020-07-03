package smithereen;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

import static smithereen.Utils.parseIntOrDefault;

public class ObjectLinkResolver{

	private static final Pattern POSTS=Pattern.compile("^/posts/(\\d+)$");
	private static final Pattern USERS=Pattern.compile("^/users/(\\d+)$");

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

	@NotNull
	public static ActivityPubObject resolve(URI link) throws SQLException{
		if(!Config.isLocal(link)){
			User user=UserStorage.getUserByActivityPubID(link);
			if(user!=null)
				return user;
			Post post=PostStorage.getPostByID(link);
			if(post!=null)
				return post;
			throw new ObjectNotFoundException("Can't resolve remote object: "+link);
		}

		Matcher matcher=POSTS.matcher(link.getPath());
		if(matcher.find()){
			return getPost(matcher.group(1));
		}

		matcher=USERS.matcher(link.getPath());
		if(matcher.find()){
			return getUser(matcher.group(1));
		}

		throw new ObjectNotFoundException("Invalid local URI");
	}
}
