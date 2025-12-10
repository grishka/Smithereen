package smithereen.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import smithereen.api.ApiCallContext;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.model.UserInteractions;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.photos.Photo;
import smithereen.util.XTEA;

public class ApiPhoto{
	private static final List<SizedImage.Type> SIZES=List.of(
			SizedImage.Type.PHOTO_THUMB_SMALL,
			SizedImage.Type.PHOTO_THUMB_MEDIUM,
			SizedImage.Type.PHOTO_SMALL,
			SizedImage.Type.PHOTO_MEDIUM,
			SizedImage.Type.PHOTO_LARGE,
			SizedImage.Type.PHOTO_ORIGINAL
	);

	public String id;
	public String apId;
	public String url;
	public String albumId;
	public int ownerId;
	public int userId;
	public String text;
	public long date;
	public String blurhash;
	public List<Size> sizes;
	public int width, height;

	public transient long rawID;

	// Extended fields
	public Likes likes;
	public Integer comments;
	public Boolean canComment;
	public Integer tags;

	// photos.getNewTags fields
	public Integer placerId;
	public Long tagCreated, tagId;

	public record Size(String type, String url, int width, int height){}
	public record Likes(int count, boolean canLike, boolean userLikes){}

	public ApiPhoto(Photo photo, ApiCallContext actx, Map<Long, UserInteractions> photosInteractions, Map<Long, Integer> tagCounts){
		id=photo.getIdString();
		apId=photo.getActivityPubID().toString();
		url=photo.getActivityPubURL().toString();
		albumId=XTEA.encodeObjectID(photo.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM);
		ownerId=photo.ownerID;
		userId=photo.authorID;
		text=photo.description;
		date=photo.createdAt.getEpochSecond();
		blurhash=photo.getBlurHash();
		populateSizes(photo.image, actx);
		width=photo.getWidth();
		height=photo.getHeight();
		rawID=photo.id;

		if(photosInteractions!=null){
			UserInteractions interactions=photosInteractions.get(photo.id);
			if(interactions!=null){
				likes=new Likes(interactions.likeCount, interactions.canLike, interactions.isLiked);
				comments=interactions.commentCount;
				canComment=interactions.canComment;
			}
			tags=tagCounts.getOrDefault(photo.id, 0);
		}
	}

	public ApiPhoto(PhotoAttachment att, int ownerId, int authorId, Instant parentCreatedAt, ApiCallContext actx){
		this.ownerId=ownerId;
		this.userId=authorId;
		text=att.description;
		date=parentCreatedAt.getEpochSecond();
		blurhash=att.blurHash;
		populateSizes(att.image, actx);
		width=att.getWidth();
		height=att.getHeight();
	}

	private void populateSizes(SizedImage image, ApiCallContext actx){
		sizes=new ArrayList<>();
		for(SizedImage.Type sz:SIZES){
			SizedImage.Dimensions dimensions=image.getDimensionsForSize(sz);
			String url=image.getUriForSizeAndFormat(sz, actx.imageFormat).toString();
			sizes.add(new Size(sz.suffix(), url, dimensions.width, dimensions.height));
			if(dimensions.width<sz.getMaxWidth() && dimensions.height<sz.getMaxHeight())
				break;
		}
	}
}
