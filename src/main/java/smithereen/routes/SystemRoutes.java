package smithereen.routes;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.data.Account;
import smithereen.data.PhotoSize;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.data.WebDeltaResponseBuilder;
import smithereen.libvips.VImage;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SystemRoutes{
	public static Object downloadExternalMedia(Request req, Response resp) throws SQLException{
		MediaCache cache=MediaCache.getInstance();
		String type=req.queryParams("type");
		String mime;
		URI uri=null;
		MediaCache.ItemType itemType;
		PhotoSize.Type sizeType;
		PhotoSize.Format format;

		switch(req.queryParams("format")){
			case "jpeg":
			case "jpg":
				format=PhotoSize.Format.JPEG;
				break;
			case "webp":
				format=PhotoSize.Format.WEBP;
				break;
			default:
				return "";
		}
		switch(req.queryParams("size")){
			case "xs":
				sizeType=PhotoSize.Type.XSMALL;
				break;
			case "s":
				sizeType=PhotoSize.Type.SMALL;
				break;
			case "m":
				sizeType=PhotoSize.Type.MEDIUM;
				break;
			case "l":
				sizeType=PhotoSize.Type.LARGE;
				break;
			case "xl":
				sizeType=PhotoSize.Type.XLARGE;
				break;
			default:
				return "";
		}

		if("user_ava".equals(type)){
			itemType=MediaCache.ItemType.AVATAR;
			mime="image/jpeg";
			User user=UserStorage.getById(Utils.parseIntOrDefault(req.queryParams("user_id"), 0));
			if(user==null || Config.isLocal(user.activityPubID)){
				return "";
			}
			if(user.icon!=null && user.icon.get(0).url!=null){
				Image im=user.icon.get(0);
				uri=im.url;
				if(StringUtils.isNotEmpty(im.mediaType))
					mime=im.mediaType;
			}
		}else if("post_photo".equals(type)){
			itemType=MediaCache.ItemType.PHOTO;
			Post post=PostStorage.getPostByID(Utils.parseIntOrDefault(req.queryParams("post_id"), 0));
			if(post==null || Config.isLocal(post.activityPubID))
				return "";
			int index=Utils.parseIntOrDefault(req.queryParams("index"), 0);
			if(index>=post.attachment.size() || index<0)
				return "";
			ActivityPubObject att=post.attachment.get(index);
			if(!(att instanceof Document))
				return "";
			if(att.mediaType==null || !att.mediaType.startsWith("image/"))
				return "";
			Document img=(Document)att;
			mime=img.mediaType;
			uri=img.url;
		}else{
			return "";
		}

		if(uri!=null){
			MediaCache.Item existing=cache.get(uri);
			if(mime.startsWith("image/")){
				if(existing!=null){
					resp.redirect(getBestSizeURL(((MediaCache.PhotoItem)existing).sizes, sizeType, format));
					return "";
				}
				try{
					MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.downloadAndPut(uri, mime, itemType);
					if(item==null)
						resp.redirect(uri.toString());
					else
						resp.redirect(getBestSizeURL(item.sizes, sizeType, format));
					return "";
				}catch(IOException x){
					x.printStackTrace();
				}
				resp.redirect(uri.toString());
			}
		}
		return "";
	}

	private static String getBestSizeURL(List<PhotoSize> sizes, PhotoSize.Type size, PhotoSize.Format format){
		for(PhotoSize s:sizes){
			if(s.type==size && s.format==format)
				return s.src.toString();
		}
		return null;
	}

	public static Object uploadPostPhoto(Request req, Response resp, Account self) throws SQLException{
		try{
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 10*1024*1024, -1L, 0));
			Part part=req.raw().getPart("file");
			if(part.getSize()>10*1024*1024){
				throw new IOException("file too large");
			}

			byte[] key=MessageDigest.getInstance("MD5").digest((self.user.username+","+System.currentTimeMillis()+","+part.getSubmittedFileName()).getBytes(StandardCharsets.UTF_8));
			String keyHex=Utils.byteArrayToHexString(key);
			String mime=part.getContentType();
			if(!mime.startsWith("image/"))
				throw new IOException("incorrect mime type");

			File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			File temp=new File(tmpDir, keyHex);
			part.write(keyHex);
			VImage img=new VImage(temp.getAbsolutePath());

			LocalImage photo=new LocalImage();
			File postMediaDir=new File(Config.uploadPath, "post_media");
			postMediaDir.mkdirs();
			try{
				MediaStorageUtils.writeResizedImages(img, new int[]{200, 400, 800, 1280, 2560}, new PhotoSize.Type[]{PhotoSize.Type.XSMALL, PhotoSize.Type.SMALL, PhotoSize.Type.MEDIUM, PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE},
						93, 87, keyHex, postMediaDir, Config.uploadURLPath+"/post_media", photo.sizes);

				SessionInfo sess=Utils.sessionInfo(req);
				photo.localID=keyHex;
				photo.mediaType="image/jpeg";
				photo.path="post_media";
				if(req.queryParams("draft")!=null)
					sess.postDraftAttachments.add(photo);
				MediaCache.putDraftAttachment(photo, self.id);

				temp.delete();
			}finally{
				img.release();
			}

			if(isAjax(req)){
				resp.type("application/json");
				JSONObject obj=new JSONObject();
				obj.put("id", keyHex);
				JSONObject thumbs=new JSONObject();
				thumbs.put("jpeg", getBestSizeURL(photo.sizes, PhotoSize.Type.XSMALL, PhotoSize.Format.JPEG));
				thumbs.put("webp", getBestSizeURL(photo.sizes, PhotoSize.Type.XSMALL, PhotoSize.Format.WEBP));
				obj.put("thumbs", thumbs);
				return obj;
			}
			resp.redirect(Utils.back(req));
		}catch(IOException|ServletException|NoSuchAlgorithmException x){
			x.printStackTrace();
		}
		return "";
	}

	public static Object deleteDraftAttachment(Request req, Response resp, Account self) throws Exception{
		SessionInfo sess=Utils.sessionInfo(req);
		String id=req.queryParams("id");
		if(id==null){
			resp.status(400);
			return "";
		}
		if(MediaCache.deleteDraftAttachment(id, self.id)){
			for(ActivityPubObject o : sess.postDraftAttachments){
				if(o instanceof Document){
					if(id.equals(((Document) o).localID)){
						sess.postDraftAttachments.remove(o);
						break;
					}
				}
			}
		}
		if(isAjax(req)){
			resp.type("application/json");
			return "[]";
		}
		resp.redirect(Utils.back(req));
		return "";
	}
}
