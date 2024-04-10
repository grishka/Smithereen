package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.model.SizedImage;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileType;
import smithereen.storage.ImgProxy;
import smithereen.storage.media.MediaFileStorageDriver;

public class LocalImage extends Image implements SizedImage{
	public Dimensions size=Dimensions.UNKNOWN;
	public long fileID;
	public MediaFileRecord fileRecord;

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(obj.has("_fileID"))
			fileID=obj.get("_fileID").getAsLong();
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
				.format(isGraffiti ? SizedImage.Format.PNG : SizedImage.Format.JPEG);
		int croppedWidth=width, croppedHeight=height;
		if(cropRegion!=null){
			int x=Math.round(cropRegion[0]*width);
			int y=Math.round(cropRegion[1]*height);
			builder.crop(x, y, croppedWidth=Math.round(cropRegion[2]*width-x), croppedHeight=Math.round(cropRegion[3]*height-y));
		}
		obj.addProperty("url", builder.build().toString());
		obj.addProperty("width", croppedWidth);
		obj.addProperty("height", croppedHeight);
		if(cropRegion!=null){
			Image im=new Image();
			im.width=width;
			im.height=height;
			im.url=MediaFileStorageDriver.getInstance().getImgProxyURL(fileRecord.id())
					.format(SizedImage.Format.JPEG)
					.build();
			im.mediaType="image/jpeg";
			obj.add("image", im.asActivityPubObject(null, serializerContext));
		}
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
				.resize(size.getResizingType(), size.getMaxWidth(), size.getMaxHeight(), false, false);
		if(cropRegion!=null && size.getResizingType()==ImgProxy.ResizingType.FILL){
			int x=Math.round(cropRegion[0]*width);
			int y=Math.round(cropRegion[1]*height);
			builder.crop(x, y, Math.round(cropRegion[2]*width-x), Math.round(cropRegion[3]*height-y));
		}
		return builder.build();
	}

	@Override
	public Dimensions getOriginalDimensions(){
		return size;
	}

	public void fillIn(MediaFileRecord mfr){
		fileRecord=mfr;
		width=mfr.metadata().width();
		height=mfr.metadata().height();
		size=new Dimensions(width, height);
		cropRegion=mfr.metadata().cropRegion();
		blurHash=mfr.metadata().blurhash();
		isGraffiti=mfr.id().type()==MediaFileType.IMAGE_GRAFFITI;
	}

	public String getLocalID(){
		if(fileRecord==null){
			LOG.warn("Tried to get a local ID for a LocalImage with fileRecord not set (file ID {})", fileID);
			return null;
		}
		return fileRecord.id().getIDForClient();
	}
}
