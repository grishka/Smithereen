package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Objects;

import smithereen.Config;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.model.SizedImage;
import smithereen.storage.ImgProxy;

public class LocalImage extends Image implements SizedImage{
	public String path;
	public Dimensions size=Dimensions.UNKNOWN;

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		localID=obj.get("_lid").getAsString();
		JsonArray s=obj.getAsJsonArray("_sz");
		path=Objects.requireNonNullElse(optString(obj, "_p"), "post_media");
		width=s.get(0).getAsInt();
		height=s.get(1).getAsInt();
		size=new Dimensions(width, height);
		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		ImgProxy.UrlBuilder builder=new ImgProxy.UrlBuilder("local://"+Config.imgproxyLocalUploads+"/"+path+"/"+localID+".webp")
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
			im.url=new ImgProxy.UrlBuilder("local://"+Config.imgproxyLocalUploads+"/"+path+"/"+localID+".webp")
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
		ImgProxy.UrlBuilder builder=new ImgProxy.UrlBuilder("local://"+Config.imgproxyLocalUploads+"/"+path+"/"+localID+".webp")
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
}
