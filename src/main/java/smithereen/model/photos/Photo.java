package smithereen.model.photos;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.LikeableContentObject;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.SizedImage;
import smithereen.model.attachments.SizedAttachment;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.notifications.Notification;
import smithereen.storage.DatabaseUtils;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class Photo implements SizedAttachment, OwnedContentObject, LikeableContentObject, ActivityPubRepresentable{
	public long id;
	public int ownerID;
	public int authorID;
	public long albumID;
	public long localFileID;
	public URI remoteSrc;
	public String description;
	public Instant createdAt;
	public PhotoMetadata metadata;
	public URI apID;
	public int displayOrder;

	public SizedImage image;

	public static Photo fromResultSet(ResultSet res) throws SQLException{
		Photo p=new Photo();
		p.id=res.getLong("id");
		p.ownerID=res.getInt("owner_id");
		p.authorID=res.getInt("author_id");
		p.albumID=res.getLong("album_id");
		p.localFileID=res.getLong("local_file_id");
		String remoteSrc=res.getString("remote_src");
		if(StringUtils.isNotEmpty(remoteSrc))
			p.remoteSrc=URI.create(remoteSrc);
		p.description=res.getString("description");
		p.createdAt=DatabaseUtils.getInstant(res, "created_at");
		String meta=res.getString("metadata");
		if(meta!=null)
			p.metadata=Utils.gson.fromJson(meta, PhotoMetadata.class);
		String apID=res.getString("ap_id");
		if(apID!=null)
			p.apID=URI.create(apID);
		p.displayOrder=res.getInt("display_order");
		return p;
	}

	public String getURL(){
		return "/photos/"+Utils.encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO));
	}

	public String getIdString(){
		return Utils.encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO));
	}

	@Override
	public int getWidth(){
		return image.getOriginalDimensions().width;
	}

	@Override
	public int getHeight(){
		return image.getOriginalDimensions().height;
	}

	@Override
	public boolean isSizeKnown(){
		return true;
	}

	@Override
	public Like.ObjectType getLikeObjectType(){
		return Like.ObjectType.PHOTO;
	}

	@Override
	public Notification.ObjectType getObjectTypeForLikeNotifications(){
		return Notification.ObjectType.PHOTO;
	}

	@Override
	public int getOwnerID(){
		return ownerID;
	}

	@Override
	public int getAuthorID(){
		return authorID;
	}

	@Override
	public long getObjectID(){
		return id;
	}

	@Override
	public URI getActivityPubID(){
		if(apID!=null)
			return apID;
		return UriBuilder.local().path("photos", getIdString()).build();
	}

	public URI getActivityPubURL(){
		if(metadata!=null && metadata.apURL!=null)
			return metadata.apURL;
		return getActivityPubID();
	}

	public String getBlurHash(){
		if(image instanceof LocalImage li)
			return li.blurHash;
		return metadata.blurhash;
	}

	public PhotoViewerInlineData getSinglePhotoViewerData(){
		return new PhotoViewerInlineData(0, "single/"+getIdString(), image.getURLsForPhotoViewer());
	}
}
