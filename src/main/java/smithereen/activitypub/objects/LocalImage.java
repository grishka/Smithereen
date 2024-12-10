package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileType;
import smithereen.model.photos.AbsoluteImageRect;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.ImageRect;
import smithereen.storage.ImgProxy;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.util.XTEA;

public class LocalImage extends Image implements SizedImage{
	public Dimensions size=Dimensions.UNKNOWN;
	public long fileID;
	public MediaFileRecord fileRecord;
	public long photoID;
	public Rotation rotation;
	public AvatarCropRects avaCropRects;

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(obj.has("_fileID"))
			fileID=obj.get("_fileID").getAsLong();
		if(obj.has("_photoID")){
			photoID=obj.get("_photoID").getAsLong();
			photoApID=Config.localURI("/photos/"+XTEA.encodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO));
		}
		if(obj.has("_rot")){
			rotation=Rotation.valueOf(obj.get("_rot").getAsInt());
		}
		if(obj.has("_crop")){
			avaCropRects=Utils.gson.fromJson(obj.get("_crop"), AvatarCropRects.class);
			cropRegion=new float[]{avaCropRects.thumb().x1(), avaCropRects.thumb().y1(), avaCropRects.thumb().x2(), avaCropRects.thumb().y2()};
		}
		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		if(fileRecord==null){
			LOG.warn("Tried to serialize a LocalImage with fileRecord not set (file ID {})", fileID);
			return obj;
		}
		ImgProxy.UrlBuilder builder=MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
				.format(isGraffiti ? SizedImage.Format.PNG : SizedImage.Format.JPEG)
				.rotate(rotation);
		int croppedWidth, croppedHeight;
		if(rotation==Rotation._90 || rotation==Rotation._270){
			croppedWidth=height;
			croppedHeight=width;
		}else{
			croppedWidth=width;
			croppedHeight=height;
		}
		if(avaCropRects!=null){
			AbsoluteImageRect profileCrop=avaCropRects.profile().makeAbsolute(croppedWidth, croppedHeight);
			AbsoluteImageRect squareCrop=avaCropRects.thumb().makeAbsolute(profileCrop.getWidth(), profileCrop.getHeight());
			croppedWidth=croppedHeight=squareCrop.getWidth();
			builder.crop(profileCrop.x1()+squareCrop.x1(), profileCrop.y1()+squareCrop.y1(), croppedWidth, croppedHeight);

			Image im=new Image();
			im.width=profileCrop.getWidth();
			im.height=profileCrop.getHeight();
			im.url=MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
					.format(Format.JPEG)
					.rotate(rotation)
					.crop(profileCrop.x1(), profileCrop.y1(), profileCrop.getWidth(), profileCrop.getHeight())
					.build();
			im.mediaType="image/jpeg";
			obj.add("image", im.asActivityPubObject(null, serializerContext));
		}else if(cropRegion!=null){
			int x=Math.round(cropRegion[0]*width);
			int y=Math.round(cropRegion[1]*height);
			builder.crop(x, y, croppedWidth=Math.round(cropRegion[2]*width-x), croppedHeight=Math.round(cropRegion[3]*height-y));

			Image im=new Image();
			im.width=width;
			im.height=height;
			im.url=MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
					.format(SizedImage.Format.JPEG)
					.build();
			im.mediaType="image/jpeg";
			obj.add("image", im.asActivityPubObject(null, serializerContext));
		}
		obj.addProperty("url", builder.build().toString());
		obj.addProperty("width", croppedWidth);
		obj.addProperty("height", croppedHeight);
		if(mediaType==null)
			obj.addProperty("mediaType", isGraffiti ? "image/png" : "image/jpeg");
		return obj;
	}

	@Override
	public URI getUriForSizeAndFormat(Type size, Format format){
		if(fileRecord==null){
			LOG.warn("Tried to get a URL for a LocalImage with fileRecord not set (file ID {})", fileID);
			return null;
		}
		ImgProxy.UrlBuilder builder=MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
				.format(format)
				.resize(size.getResizingType(), size.getMaxWidth(), size.getMaxHeight(), false, false)
				.rotate(rotation);
		if(avaCropRects!=null){
			int w, h;
			if(rotation==Rotation._90 || rotation==Rotation._270){
				w=height;
				h=width;
			}else{
				w=width;
				h=height;
			}
			if(size==Type.AVA_RECT || size==Type.AVA_RECT_LARGE){
				AbsoluteImageRect profileCrop=avaCropRects.profile().makeAbsolute(w, h);
				builder.crop(profileCrop.x1(), profileCrop.y1(), profileCrop.getWidth(), profileCrop.getHeight());
			}else if(size==Type.AVA_SQUARE_SMALL || size==Type.AVA_SQUARE_MEDIUM || size==Type.AVA_SQUARE_LARGE || size==Type.AVA_SQUARE_XLARGE){
				AbsoluteImageRect profileCrop=avaCropRects.profile().makeAbsolute(w, h);
				AbsoluteImageRect squareCrop=avaCropRects.thumb().makeAbsolute(profileCrop.getWidth(), profileCrop.getHeight());
				int croppedSize=squareCrop.getWidth();
				builder.crop(profileCrop.x1()+squareCrop.x1(), profileCrop.y1()+squareCrop.y1(), croppedSize, croppedSize);
			}
		}else if(cropRegion!=null && size.getResizingType()==ImgProxy.ResizingType.FILL){
			int x=Math.round(cropRegion[0]*width);
			int y=Math.round(cropRegion[1]*height);
			builder.crop(x, y, Math.round(cropRegion[2]*width-x), Math.round(cropRegion[3]*height-y));
		}
		return builder.build();
	}

	@Override
	public Dimensions getOriginalDimensions(){
		return rotation==Rotation._90 || rotation==Rotation._270 ? new Dimensions(height, width) : size;
	}

	@Override
	public URI getOriginalURI(){
		if(rotation!=null){
			return MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
					.format(Format.WEBP)
					.rotate(rotation)
					.build();
		}
		return MediaFileStorageDriver.getInstance().getFilePublicURL(fileRecord.id());
	}

	public void fillIn(MediaFileRecord mfr){
		fileRecord=mfr;
		width=mfr.metadata().width();
		height=mfr.metadata().height();
		size=new Dimensions(width, height);
		if(cropRegion==null)
			cropRegion=mfr.metadata().cropRegion();
		blurHash=mfr.metadata().blurhash();
		isGraffiti=mfr.id().type()==MediaFileType.IMAGE_GRAFFITI;
	}

	public String getLocalID(){
		if(fileRecord==null){
			LOG.warn("Tried to get a local ID for a LocalImage with fileRecord not set (file ID {})", fileID);
			return null;
		}
		if(photoID!=0)
			return "photo:"+XTEA.encodeObjectID(photoID, ObfuscatedObjectIDType.PHOTO);
		return fileRecord.id().getIDForClient();
	}
}
