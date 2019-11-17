package smithereen.routes;

import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
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
				String text=Utils.sanitizeHTML(req.queryParams("text")).replace("\r", "").trim();
				if(text.length()==0)
					return "Empty post";
				if(!text.startsWith("<p>")){
					String[] paragraphs=text.replaceAll("\n{3,}", "\n\n").split("\n\n");
					StringBuilder sb=new StringBuilder();
					for(String paragraph:paragraphs){
						String p=paragraph.trim().replace("\n", "<br/>");
						sb.append("<p>");
						sb.append(p);
						sb.append("</p>");
					}
					text=sb.toString();
				}
				int userID=self.user.id;
				int replyTo=Utils.parseIntOrDefault(req.queryParams("replyTo"), 0);
				int postID;
				if(replyTo!=0){
					Post parent=PostStorage.getPostByID(0, replyTo);
					if(parent==null){
						resp.status(404);
						return Utils.wrapError(req, "err_post_not_found");
					}
					int[] replyKey=new int[parent.replyKey.length+1];
					System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
					replyKey[replyKey.length-1]=parent.id;
					// comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
					if(parent.replyKey.length>0 && text.startsWith("<p>"+parent.user.firstName+", ")){
						text="<p><span class=\"h-card\"><a href=\""+parent.user.url+"\" class=\"u-url mention\">"+parent.user.firstName+"</a></span>"+text.substring(parent.user.firstName.length()+3);
						System.out.println(text);
					}
					postID=PostStorage.createUserWallPost(userID, user.id, text, replyKey);
				}else{
					postID=PostStorage.createUserWallPost(userID, user.id, text, null);
				}

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
			for(Post post:feed){
				post.replies=PostStorage.getRepliesForFeed(post.id);
			}
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
		int[] replyKey=new int[post.replyKey.length+1];
		System.arraycopy(post.replyKey, 0, replyKey, 0, post.replyKey.length);
		replyKey[replyKey.length-1]=post.id;
		post.replies=PostStorage.getReplies(replyKey);
		JtwigModel model=JtwigModel.newModel();
		model.with("post", post);
		if(post.replyKey.length>0){
			model.with("prefilledPostText", post.user.firstName+", ");
		}
		return Utils.renderTemplate(req, "wall_post_standalone", model);
	}

	public static Object confirmDelete(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
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
			return Utils.renderTemplate(req, "generic_confirm", JtwigModel.newModel().with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI(username+"/posts/"+postID+"/delete")));
		}
		return "";
	}

	public static Object delete(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
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
			if(!post.canBeManagedBy(self.user)){
				resp.status(403);
				return Utils.wrapError(req, "err_access");
			}
			PostStorage.deletePost(post.id);
			resp.redirect("/feed"); // TODO redirect properly
		}
		return "";
	}
}
