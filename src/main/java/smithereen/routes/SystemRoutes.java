package smithereen.routes;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.data.PhotoSize;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.storage.MediaCache;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

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
}
