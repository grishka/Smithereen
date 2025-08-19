package smithereen.model.groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.activitypub.objects.LocalImage;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.CachedRemoteImage;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.storage.MediaCache;
import smithereen.util.XTEA;

public final class GroupLink{
	private static final Logger LOG=LoggerFactory.getLogger(GroupLink.class);

	public long id;
	public int groupID;
	public URI url;
	public String title;
	public ObjectLinkResolver.ObjectTypeAndID object;
	public SizedImage image;
	public int displayOrder;
	public URI apImageURL;

	public static GroupLink fromResultSet(ResultSet res) throws SQLException{
		GroupLink l=new GroupLink();
		l.id=res.getLong("id");
		l.groupID=res.getInt("group_id");
		l.url=URI.create(res.getString("url"));
		l.title=res.getString("title");
		l.displayOrder=res.getInt("display_order");

		int objType=res.getInt("object_type");
		if(!res.wasNull()){
			l.object=new ObjectLinkResolver.ObjectTypeAndID(ObjectLinkResolver.ObjectType.fromID(objType), res.getLong("object_id"));
		}
		String apImageURL=res.getString("ap_image_url");
		long imageID=res.getLong("image_id");
		if(imageID!=0){
			l.image=new LocalImage(imageID);
		}else if(apImageURL!=null){
			l.apImageURL=URI.create(apImageURL);
		}
		return l;
	}

	public SizedImage getImage(){
		if(image!=null)
			return image;
		if(apImageURL==null)
			return null;

		MediaCache cache=MediaCache.getInstance();
		try{
			MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.get(apImageURL);
			if(item!=null)
				return new CachedRemoteImage(item, apImageURL);
			return new NonCachedRemoteImage(new NonCachedRemoteImage.GroupLinkArgs(this), SizedImage.Dimensions.UNKNOWN, apImageURL);
		}catch(SQLException x){
			LOG.warn("Failed to get link image form media cache", x);
		}
		return null;
	}

	public String getTitleLangKey(){
		if(object==null)
			return null;
		return switch(object.type()){
			case POST -> "content_type_post";
			case PHOTO_ALBUM -> "photo_album";
			case PHOTO -> "content_type_photo";
			case COMMENT -> "content_type_comment";
			case BOARD_TOPIC -> "board_topic";
			default -> null;
		};
	}

	public String getDescription(){
		return object!=null ? title : url.getHost();
	}
}
