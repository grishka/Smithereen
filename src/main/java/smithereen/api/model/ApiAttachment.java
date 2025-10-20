package smithereen.api.model;

import java.time.Instant;
import java.util.Map;

import smithereen.api.ApiCallContext;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.MailMessage;
import smithereen.model.OwnedContentObject;
import smithereen.model.PostLikeObject;
import smithereen.model.SizedImage;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.AudioAttachment;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.attachments.VideoAttachment;
import smithereen.model.photos.Photo;

public class ApiAttachment{
	public String type;

	public ApiPhoto photo;
	public Graffiti graffiti;
	public Video video;
	public Audio audio;
	public ApiPoll poll;

	public ApiAttachment(Attachment att, ApiCallContext actx, AttachmentHostContentObject parent, Map<Long, Photo> photos){
		switch(att){
			case GraffitiAttachment ga -> {
				type="graffiti";
				graffiti=new Graffiti(ga.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_ORIGINAL, SizedImage.Format.PNG).toString(),
						ga.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_MEDIUM, actx.imageFormat).toString(),
						ga.getWidth(), ga.getHeight());
			}
			case PhotoAttachment pa -> {
				type="photo";
				if(pa.photoID!=0 && photos.containsKey(pa.photoID)){
					this.photo=new ApiPhoto(photos.get(pa.photoID), actx, false);
				}else{
					OwnedContentObject ownedParent=(OwnedContentObject) parent;
					Instant createdAt=switch(parent){
						case PostLikeObject post -> post.createdAt;
						case MailMessage msg -> msg.createdAt;
					};
					this.photo=new ApiPhoto(pa, ownedParent.getOwnerID(), ownedParent.getAuthorID(), createdAt, actx);
				}
			}
			case VideoAttachment va -> {
				type="video";
				video=new Video(va.url.toString(), va.width>0 ? va.width : null, va.height>0 ? va.height : null, va.blurHash, va.description);
			}
			case AudioAttachment aa -> {
				type="audio";
				audio=new Audio(aa.url.toString(), aa.description);
			}
		}
	}

	public ApiAttachment(){}

	public record Video(String url, Integer width, Integer height, String blurhash, String description){}
	public record Audio(String url, String description){}
	public record Graffiti(String url, String previewUrl, int width, int height){}
}
