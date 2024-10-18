package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.ApplicationContext;
import smithereen.activitypub.SerializerContext;
import smithereen.model.User;
import smithereen.model.photos.PhotoAlbum;

public class LocalActivityPubPhotoAlbum extends ActivityPubPhotoAlbum{
	public final PhotoAlbum nativeAlbum;

	public LocalActivityPubPhotoAlbum(PhotoAlbum nativeAlbum){
		this.nativeAlbum=nativeAlbum;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);
		if(nativeAlbum.ownerID>0){
			User owner=serializerContext.appContext.getUsersController().getUserOrThrow(nativeAlbum.ownerID);
			serializerContext.addSmAlias("viewPrivacy");
			serializerContext.addSmAlias("commentPrivacy");
			obj.add("viewPrivacy", nativeAlbum.viewPrivacy.serializeForActivityPub(owner, serializerContext));
			obj.add("commentPrivacy", nativeAlbum.commentPrivacy.serializeForActivityPub(owner, serializerContext));
		}else{
			serializerContext.addSmAlias("uploadsRestricted");
			serializerContext.addSmAlias("commentingDisabled");
			obj.addProperty("uploadsRestricted", restrictUploads);
			obj.addProperty("commentingDisabled", disableCommenting);
		}
		obj.addProperty("displayOrder", nativeAlbum.displayOrder);
		obj.addProperty("comments", activityPubComments.toString());
		if(nativeAlbum.systemType!=null){
			String type=switch(nativeAlbum.systemType){
				case SAVED -> "SavedPhotos";
				case AVATARS -> "ProfilePictures";
				case TAGGED -> throw new IllegalArgumentException();
			};
			serializerContext.addSmIdType("systemAlbumType");
			serializerContext.addSmAlias(type);
			obj.addProperty("systemAlbumType", type);
		}
		return obj;
	}

	@Override
	public PhotoAlbum asNativePhotoAlbum(ApplicationContext context){
		return nativeAlbum;
	}
}
