package smithereen.model;

import com.google.gson.JsonArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.AudioAttachment;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.attachments.VideoAttachment;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import spark.utils.StringUtils;

public interface AttachmentHostContentObject{
	List<ActivityPubObject> getAttachments();
	NonCachedRemoteImage.Args getPhotoArgs(int index);

	default List<Attachment> getProcessedAttachments(){
		ArrayList<Attachment> result=new ArrayList<>();
		int i=0;
		for(ActivityPubObject o:getAttachments()){
			String mediaType=o.mediaType==null ? "" : o.mediaType;
			if(o instanceof Image || mediaType.startsWith("image/")){
				PhotoAttachment att=o instanceof Image img && img.isGraffiti ? new GraffitiAttachment() : new PhotoAttachment();
				if(o instanceof LocalImage li){
					att.image=li;
				}else{
					// TODO make this less ugly
					MediaCache.PhotoItem item;
					try{
						item=(MediaCache.PhotoItem) MediaCache.getInstance().get(o.url);
					}catch(SQLException x){
						throw new InternalServerErrorException(x);
					}
					if(item!=null){
						att.image=new CachedRemoteImage(item);
					}else{
						SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
						if(o instanceof Document im){
							if(im.width>0 && im.height>0){
								size=new SizedImage.Dimensions(im.width, im.height);
							}
						}
						att.image=new NonCachedRemoteImage(getPhotoArgs(i), size);
					}
				}
				if(o instanceof Document doc){
					if(StringUtils.isNotEmpty(doc.blurHash))
						att.blurHash=doc.blurHash;
				}
				result.add(att);
			}else if(mediaType.startsWith("video/")){
				VideoAttachment att=new VideoAttachment();
				att.url=o.url;
				result.add(att);
			}else if(mediaType.startsWith("audio/")){
				AudioAttachment att=new AudioAttachment();
				att.url=o.url;
				result.add(att);
			}
			i++;
		}
		return result;
	}

	default String getSerializedAttachments(){
		List<ActivityPubObject> attachObjects=getAttachments();
		if(attachObjects!=null && !attachObjects.isEmpty()){
			if(attachObjects.size()==1){
				return MediaStorageUtils.serializeAttachment(attachObjects.get(0)).toString();
			}else{
				JsonArray ar=new JsonArray();
				for(ActivityPubObject o:attachObjects){
					ar.add(MediaStorageUtils.serializeAttachment(o));
				}
				return ar.toString();
			}
		}
		return null;
	}
}
