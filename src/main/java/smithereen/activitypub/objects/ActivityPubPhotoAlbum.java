package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.EnumSet;

import smithereen.ApplicationContext;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.exceptions.FederationException;
import smithereen.model.PrivacySetting;
import smithereen.model.User;
import smithereen.model.photos.PhotoAlbum;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;

public class ActivityPubPhotoAlbum extends ActivityPubCollection{
	public JsonObject viewPrivacy, commentPrivacy;
	public boolean disableCommenting, restrictUploads;
	public int displayOrder;

	public ActivityPubPhotoAlbum(){
		super(true);
	}

	@Override
	public String getType(){
		return "PhotoAlbum";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		serializerContext.addSmAlias("PhotoAlbum");
		return super.asActivityPubObject(obj, serializerContext);
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		viewPrivacy=optObject(obj, "viewPrivacy");
		commentPrivacy=optObject(obj, "commentPrivacy");
		disableCommenting=optBoolean(obj, "commentingDisabled");
		restrictUploads=optBoolean(obj, "uploadsRestricted");
		displayOrder=optInt(obj, "displayOrder");
		return this;
	}

	public static ActivityPubPhotoAlbum fromNativeAlbum(PhotoAlbum album, ApplicationContext context){
		Actor owner=album.ownerID<0 ? context.getGroupsController().getGroupOrThrow(-album.ownerID) : context.getUsersController().getUserOrThrow(album.ownerID);
		LocalActivityPubPhotoAlbum pa=new LocalActivityPubPhotoAlbum(album);
		pa.activityPubID=pa.url=album.getActivityPubID();
		pa.name=album.title;
		pa.summary=TextProcessor.escapeHTML(album.description);
		pa.totalItems=album.numPhotos;
		pa.attributedTo=owner.activityPubID;
		pa.published=album.createdAt;
		pa.updated=album.updatedAt;
		pa.first=new LinkOrObject(new UriBuilder(pa.activityPubID).queryParam("page", "1").build());
		pa.displayOrder=album.displayOrder;
		if(album.coverID!=0){
			pa.preview=new LinkOrObject(context.getPhotosController().getPhotoIgnoringPrivacy(album.coverID).getActivityPubID());
		}
		if(album.ownerID<0){
			pa.disableCommenting=album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
			pa.restrictUploads=album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
		}
		return pa;
	}

	public PhotoAlbum asNativePhotoAlbum(ApplicationContext context){
		PhotoAlbum album=new PhotoAlbum();
		album.id=context.getPhotosController().getAlbumIdByActivityPubId(activityPubID);
		album.activityPubID=activityPubID;
		album.activityPubURL=url!=null ? url : activityPubID;
		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		if(name==null)
			throw new FederationException("name is required");
		ensureHostMatchesID(attributedTo, "attributedTo");
		Actor owner=context.getObjectLinkResolver().resolve(attributedTo, Actor.class, true, true, false);
		album.ownerID=owner.getOwnerID();
		album.title=name;
		album.description=TextProcessor.stripHTML(summary, true);
		album.createdAt=published!=null ? published : Instant.now();
		album.updatedAt=updated!=null ? updated : album.createdAt;
		album.displayOrder=displayOrder;
		album.flags=EnumSet.noneOf(PhotoAlbum.Flag.class);
		if(preview!=null && preview.link!=null)
			album.coverID=context.getPhotosController().getPhotoIdByActivityPubId(preview.link);
		if(owner instanceof User user){
			album.viewPrivacy=PrivacySetting.parseFromActivityPub(user, viewPrivacy);
			if(album.viewPrivacy==null)
				album.viewPrivacy=PrivacySetting.DEFAULT;
			album.commentPrivacy=PrivacySetting.parseFromActivityPub(user, commentPrivacy);
			if(album.commentPrivacy==null)
				album.commentPrivacy=PrivacySetting.DEFAULT;
		}else{
			if(disableCommenting)
				album.flags.add(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
			if(restrictUploads)
				album.flags.add(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
		}
		return album;
	}
}
