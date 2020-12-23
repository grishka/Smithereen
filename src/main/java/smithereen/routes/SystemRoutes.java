package smithereen.routes;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.data.Account;
import smithereen.data.CachedRemoteImage;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.User;
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
		SizedImage.Type sizeType;
		SizedImage.Format format;

		switch(req.queryParams("format")){
			case "jpeg":
			case "jpg":
				format=SizedImage.Format.JPEG;
				break;
			case "webp":
				format=SizedImage.Format.WEBP;
				break;
			default:
				return "";
		}
		sizeType=SizedImage.Type.fromSuffix(req.queryParams("size"));
		if(sizeType==null)
			return "";

		float[] cropRegion=null;
		User user=null;

		if("user_ava".equals(type)){
			itemType=MediaCache.ItemType.AVATAR;
			mime="image/jpeg";
			user=UserStorage.getById(Utils.parseIntOrDefault(req.queryParams("user_id"), 0));
			if(user==null || Config.isLocal(user.activityPubID)){
				return "";
			}
			Image im=user.getBestAvatarImage();
			if(im!=null && im.url!=null){
				cropRegion=user.getAvatarCropRegion();
				uri=im.url;
				if(StringUtils.isNotEmpty(im.mediaType))
					mime=im.mediaType;
				else
					mime="image/jpeg";
			}
		}else if("post_photo".equals(type)){
			itemType=MediaCache.ItemType.PHOTO;
			Post post=PostStorage.getPostByID(Utils.parseIntOrDefault(req.queryParams("post_id"), 0), false);
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
					resp.redirect(new CachedRemoteImage((MediaCache.PhotoItem) existing, cropRegion).getUriForSizeAndFormat(sizeType, format).toString());
					return "";
				}
				try{
					MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.downloadAndPut(uri, mime, itemType);
					if(item==null){
						if(itemType==MediaCache.ItemType.AVATAR && req.queryParams("retrying")==null){
							ActivityPubObject obj=ActivityPub.fetchRemoteObject(user.activityPubID.toString());
							if(obj instanceof ForeignUser){
								ForeignUser updatedUser=(ForeignUser) obj;
								UserStorage.putOrUpdateForeignUser(updatedUser);
								resp.redirect(Config.localURI("/system/downloadExternalMedia?type=user_ava&user_id="+updatedUser.id+"&size="+sizeType.suffix()+"&format="+format.fileExtension()+"&retrying").toString());
							}
						}
						resp.redirect(uri.toString());
					}else{
						resp.redirect(new CachedRemoteImage(item, cropRegion).getUriForSizeAndFormat(sizeType, format).toString());
					}
					return "";
				}catch(IOException x){
					x.printStackTrace();
				}
				resp.redirect(uri.toString());
			}
		}
		return "";
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
//				MediaStorageUtils.writeResizedImages(img, new int[]{200, 400, 800, 1280, 2560}, new PhotoSize.Type[]{PhotoSize.Type.XSMALL, PhotoSize.Type.SMALL, PhotoSize.Type.MEDIUM, PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE},
//						93, 87, keyHex, postMediaDir, Config.uploadURLPath+"/post_media", photo.sizes);
				int[] outSize={0,0};
				MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, 93, keyHex, postMediaDir, outSize);

				SessionInfo sess=Utils.sessionInfo(req);
				photo.localID=keyHex;
				photo.mediaType="image/jpeg";
				photo.path="post_media";
				photo.width=outSize[0];
				photo.height=outSize[1];
				photo.blurHash=img.blurHash(4, 4);
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
				thumbs.put("jpeg", photo.getUriForSizeAndFormat(SizedImage.Type.SMALL, SizedImage.Format.JPEG).toString());
				thumbs.put("webp", photo.getUriForSizeAndFormat(SizedImage.Type.SMALL, SizedImage.Format.WEBP).toString());
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
