package smithereen.routes;

import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.PhotoSize;
import smithereen.data.SessionInfo;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.Post;
import smithereen.data.feed.PostNewsfeedEntry;
import smithereen.data.User;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class PostRoutes{
	private static JSONObject serializeAttachment(ActivityPubObject att){
		JSONObject o=att.asActivityPubObject(null, new ContextCollector());
		if(att instanceof Document){
			Document d=(Document) att;
			if(StringUtils.isNotEmpty(d.localID)){
				o.put("_lid", d.localID);
				if(d instanceof LocalImage){
					LocalImage im=(LocalImage) d;
					JSONArray sizes=new JSONArray();
					sizes.put(0);
					ArrayList<String> sizeTypes=new ArrayList<>();
					for(PhotoSize size:im.sizes){
						if(size.format!=PhotoSize.Format.JPEG)
							continue;
						sizeTypes.add(size.type.suffix());
						sizes.put(size.width);
						sizes.put(size.height);
					}
					sizes.put(0, String.join(" ", sizeTypes));
					o.put("_sz", sizes);
					o.put("type", "_LocalImage");
				}
				o.remove("url");
				o.remove("id");
			}
		}
		return o;
	}

	public static Object createWallPost(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user=UserStorage.getByUsername(username);
		if(user!=null){
			String text=Utils.sanitizeHTML(req.queryParams("text")).replace("\r", "").trim();
			if(text.length()==0)
				return "Empty post";
			if(!text.startsWith("<p>")){
				String[] paragraphs=text.replaceAll("\n{3,}", "\n\n").split("\n\n");
				StringBuilder sb=new StringBuilder();
				for(String paragraph:paragraphs){
					String p=paragraph.trim().replace("\n", "<br/>");
					if(p.isEmpty())
						continue;
					sb.append("<p>");
					sb.append(p);
					sb.append("</p>");
				}
				text=sb.toString();
			}
			int userID=self.user.id;
			int replyTo=Utils.parseIntOrDefault(req.queryParams("replyTo"), 0);
			int postID;

			StringBuffer sb=new StringBuffer();
			ArrayList<User> mentionedUsers=new ArrayList<>();
			Pattern mentionRegex=Pattern.compile("@([a-zA-Z0-9._-]+)(?:@([a-zA-Z0-9._-]+[a-zA-Z0-9-]+))?");
			Matcher matcher=mentionRegex.matcher(text);
			while(matcher.find()){
				String u=matcher.group(1);
				String d=matcher.group(2);
				User mentionedUser;
				if(d==null){
					mentionedUser=UserStorage.getByUsername(u);
				}else{
					mentionedUser=UserStorage.getByUsername(u+"@"+d);
				}
				if(d!=null && mentionedUser==null){
					try{
						URI uri=ActivityPub.resolveUsername(u, d);
						System.out.println(u+"@"+d+" -> "+uri);
						ActivityPubObject obj=ActivityPub.fetchRemoteObject(uri.toString());
						if(obj instanceof ForeignUser){
							mentionedUser=(User) obj;
							UserStorage.putOrUpdateForeignUser((ForeignUser) obj);
						}
					}catch(IOException x){
						System.out.println("Can't resolve "+u+"@"+d+": "+x.getMessage());
					}
				}
				if(mentionedUser!=null){
					matcher.appendReplacement(sb, "<span class=\"h-card\"><a href=\""+mentionedUser.url+"\" class=\"u-url mention\">$0</a></span>");
					mentionedUsers.add(mentionedUser);
				}else{
					System.out.println("ignoring mention "+matcher.group());
					matcher.appendReplacement(sb, "$0");
				}
			}
			if(!mentionedUsers.isEmpty()){
				matcher.appendTail(sb);
				text=sb.toString();
			}

			String attachments=null;
			SessionInfo sess=Utils.sessionInfo(req);
			if(!sess.postDraftAttachments.isEmpty()){
				if(sess.postDraftAttachments.size()==1){
					attachments=serializeAttachment(sess.postDraftAttachments.get(0)).toString();
				}else{
					JSONArray ar=new JSONArray();
					for(ActivityPubObject o:sess.postDraftAttachments){
						ar.put(serializeAttachment(o));
					}
					attachments=ar.toString();
				}
			}

			if(replyTo!=0){
				Post parent=PostStorage.getPostByID(replyTo);
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
				}
				mentionedUsers.add(parent.user);
				if(parent.replyKey.length>1){
					Post topLevel=PostStorage.getPostByID(parent.replyKey[0]);
					if(topLevel!=null)
						mentionedUsers.add(topLevel.user);
				}
				postID=PostStorage.createUserWallPost(userID, user.id, text, replyKey, mentionedUsers, attachments);
			}else{
				postID=PostStorage.createUserWallPost(userID, user.id, text, null, mentionedUsers, attachments);
			}

			Post post=PostStorage.getPostByID(postID);
			ActivityPubWorker.getInstance().sendCreatePostActivity(post);

			resp.redirect(Utils.back(req));
			sess.postDraftAttachments.clear();
		}else{
			resp.status(404);
			return Utils.wrapError(req, "err_user_not_found");
		}
		return "";
	}

	public static Object feed(Request req, Response resp, Account self) throws SQLException{
		int userID=self.user.id;
		List<NewsfeedEntry> feed=PostStorage.getFeed(userID);
		for(NewsfeedEntry e:feed){
			if(e instanceof PostNewsfeedEntry){
				PostNewsfeedEntry pe=(PostNewsfeedEntry) e;
				if(pe.post!=null)
					pe.post.replies=PostStorage.getRepliesForFeed(e.objectID);
				else
					System.err.println("No post: "+pe);
			}
		}
		JtwigModel model=JtwigModel.newModel().with("title", Utils.lang(req).get("feed")).with("feed", feed).with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments);
		return Utils.renderTemplate(req, "feed", model);
	}

	public static Object standalonePost(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID);
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
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null)
			model.with("draftAttachments", info.postDraftAttachments);
		if(post.replyKey.length>0){
			model.with("prefilledPostText", post.user.firstName+", ");
		}
		return Utils.renderTemplate(req, "wall_post_standalone", model);
	}

	public static Object confirmDelete(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		String back=Utils.back(req);
		return Utils.renderTemplate(req, "generic_confirm", JtwigModel.newModel().with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI("/posts/"+postID+"/delete?_redir="+URLEncoder.encode(back))).with("back", back));
	}

	public static Object delete(Request req, Response resp, Account self) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		Post post=PostStorage.getPostByID(postID);
		if(post==null){
			resp.status(404);
			return Utils.wrapError(req, "err_post_not_found");
		}
		if(!post.canBeManagedBy(self.user)){
			resp.status(403);
			return Utils.wrapError(req, "err_access");
		}
		PostStorage.deletePost(post.id);
		if(Config.isLocal(post.activityPubID)){
			MediaStorageUtils.deleteAttachmentFiles(post.attachment);
		}
		ActivityPubWorker.getInstance().sendDeletePostActivity(post);
		resp.redirect(Utils.back(req));
		return "";
	}
}
