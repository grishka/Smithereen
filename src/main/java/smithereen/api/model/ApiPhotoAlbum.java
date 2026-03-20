package smithereen.api.model;

import java.util.Map;

import smithereen.activitypub.objects.Actor;
import smithereen.api.ApiCallContext;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.util.XTEA;

public class ApiPhotoAlbum{
	public String id;
	public String apId;
	public String url;
	public int ownerId;
	public boolean isSystem;
	public String title;
	public String description;
	public String coverId;
	public long created;
	public long updated;
	public int size;

	// Only if `need_covers` is true
	public ApiPhoto cover;

	// Only current user's non-system albums
	public ApiPrivacySetting privacyView, privacyComment;

	// Only group albums
	public Boolean canUpload;

	// Only managed group admins
	public Boolean uploadsByAdminsOnly, commentsDisabled;

	public ApiPhotoAlbum(PhotoAlbum pa, ApiCallContext actx, Actor owner, Map<Long, Photo> photos){
		id=pa.getIdString();
		apId=pa.getActivityPubID().toString();
		url=pa.getActivityPubURL().toString();
		ownerId=pa.getOwnerID();
		isSystem=pa.systemType!=null;
		if(pa.systemType==null)
			title=pa.title;
		else
			title=pa.getLocalizedTitle(actx.lang, actx.self==null ? null : actx.self.user, owner);
		description=pa.description;
		if(pa.coverID!=0){
			coverId=XTEA.encodeObjectID(pa.coverID, ObfuscatedObjectIDType.PHOTO);
			if(photos!=null){
				Photo p=photos.get(pa.coverID);
				if(p!=null)
					cover=new ApiPhoto(p, actx, null, null);
			}
		}
		size=pa.numPhotos;

		if(actx.self!=null && pa.systemType==null){
			if(pa.ownerID==actx.self.user.id && actx.hasPermission(ClientAppPermission.PHOTOS_READ)){
				privacyView=new ApiPrivacySetting(pa.viewPrivacy);
				privacyComment=new ApiPrivacySetting(pa.commentPrivacy);
			}
			if(pa.ownerID<0){
				canUpload=!pa.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS) || actx.permissions.canManageGroup(-pa.ownerID);
				if(actx.permissions.canManageGroup(-pa.ownerID)){
					uploadsByAdminsOnly=pa.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS);
					commentsDisabled=pa.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
				}
			}
		}
	}
}
