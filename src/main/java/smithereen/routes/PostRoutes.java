package smithereen.routes;

import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.Account;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.jsonld.JLD;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class PostRoutes{
	public static Object createWallPost(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			String username=req.params(":username");
			User user=UserStorage.getByUsername(username);
			Account self=req.session().attribute("account");
			if(user!=null){
				String text=Utils.sanitizeHTML(req.queryParams("text")).trim();
				if(text.length()==0)
					return "Empty post";
				int userID=((Account) req.session().attribute("account")).user.id;
				int postID=PostStorage.createUserWallPost(userID, user.id, text);

				Post post=PostStorage.getPostByID(user.id, postID);
				ActivityPubWorker.getInstance().sendCreatePostActivity(post);

				resp.redirect("/feed");
			}else{
				resp.status(404);
				return Utils.wrapError(req, "err_user_not_found");
			}
		}
		return "";
	}

	public static Object feed(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
			int userID=((Account)req.session().attribute("account")).user.id;
			List<Post> feed=PostStorage.getFeed(userID);
			JtwigModel model=JtwigModel.newModel().with("title", "Feed").with("posts", feed);
			return Utils.renderTemplate(req, "feed", model);
		}
		return "";
	}

	public static Object standalonePost(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user==null){
			resp.status(404);
			return Utils.wrapError(req, "err_user_not_found");
		}
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(user.id, postID);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		JtwigModel model=JtwigModel.newModel();
		model.with("post", post);
		return Utils.renderTemplate(req, "wall_post_standalone", model);
	}
}
