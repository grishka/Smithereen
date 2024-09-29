package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.User;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoMetadata;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;

public class ActivityPubPhoto extends ActivityPubObject{
	public ActivityPubObject target;
	public int displayOrder;

	@Override
	public String getType(){
		return "Photo";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.add("target", target.asActivityPubObject(new JsonObject(), serializerContext));
		obj.addProperty("displayOrder", displayOrder);
		serializerContext.addSmAlias("displayOrder");
		serializerContext.addSmAlias("Photo");
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		target=ActivityPubObject.parse(optObject(obj, "target"), parserContext);
		displayOrder=optInt(obj, "displayOrder");
		return this;
	}

	public static ActivityPubPhoto fromNativePhoto(Photo photo, PhotoAlbum album, ApplicationContext context){
		LocalActivityPubPhoto p=new LocalActivityPubPhoto(photo);
		p.activityPubID=p.url=photo.getActivityPubID();
		try{
			User author=context.getUsersController().getUserOrThrow(photo.authorID);
			p.attributedTo=author.activityPubID;
		}catch(ObjectNotFoundException x){
			p.attributedTo=Config.localURI("/users/"+photo.authorID);
		}
		ActivityPubPhotoAlbum target=new ActivityPubPhotoAlbum();
		target.activityPubID=album.getActivityPubID();
		target.attributedTo=(album.ownerID>0 ? context.getUsersController().getUserOrThrow(album.ownerID) : context.getGroupsController().getGroupOrThrow(-album.ownerID)).activityPubID;
		p.target=target;
		p.summary=TextProcessor.postprocessPostHTMLForActivityPub(photo.description);
		p.published=photo.createdAt;
		p.displayOrder=photo.displayOrder;

		ActivityPubCollection replies=new ActivityPubCollection(false);
		replies.activityPubID=new UriBuilder(p.activityPubID).appendPath("replies").build();
		CollectionPage repliesPage=new CollectionPage(false);
		repliesPage.next=new UriBuilder(replies.activityPubID).queryParam("page", "1").build();
		repliesPage.partOf=replies.activityPubID;
		repliesPage.items=Collections.emptyList();
		replies.first=new LinkOrObject(repliesPage);
		p.replies=new LinkOrObject(replies);

		Image image=new Image();
		image.url=photo.image.getOriginalURI();
		image.width=photo.getWidth();
		image.height=photo.getHeight();
		image.blurHash=photo.getBlurHash();
		p.image=List.of(image);

		return p;
	}

	public Photo asNativePhoto(ApplicationContext context){
		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		ensureHostMatchesID(attributedTo, "attributedTo");
		if(!(target instanceof ActivityPubPhotoAlbum album))
			throw new FederationException("target is required and needs to be a PhotoAlbum");
		if(image==null || image.isEmpty() || image.getFirst()==null)
			throw new FederationException("image is required");

		Photo p=new Photo();
		p.id=context.getPhotosController().getPhotoIdByActivityPubId(activityPubID);
		p.apID=activityPubID;
		p.authorID=context.getObjectLinkResolver().resolve(attributedTo, User.class, true, true, false).id;
		PhotoAlbum nAlbum=context.getObjectLinkResolver().resolveNative(album.activityPubID, PhotoAlbum.class, true, true, false, (JsonObject) null, false);
		p.albumID=nAlbum.id;
		p.ownerID=nAlbum.ownerID;
		p.description=TextProcessor.sanitizeHTML(summary);
		p.createdAt=published!=null ? published : Instant.now();
		p.displayOrder=displayOrder;

		Image image=this.image.getFirst();
		if(image.url==null)
			throw new FederationException("image.url is required");
		if(image.width==0 || image.height==0)
			throw new FederationException("image.width and .height are required");

		p.remoteSrc=image.url;
		p.metadata=new PhotoMetadata();
		p.metadata.apURL=url!=null ? url : activityPubID;
		p.metadata.width=image.width;
		p.metadata.height=image.height;
		p.metadata.blurhash=image.blurHash;
		if(replies!=null){
			if(replies.link!=null)
				p.metadata.apReplies=replies.link;
			else
				p.metadata.apReplies=replies.object.activityPubID;
		}

		return p;
	}
}
