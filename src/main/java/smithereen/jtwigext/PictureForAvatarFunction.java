package smithereen.jtwigext;

import org.jtwig.escape.EscapeEngine;
import org.jtwig.escape.NoneEscapeEngine;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.render.context.RenderContextHolder;

import java.math.BigDecimal;
import java.util.List;

import smithereen.data.PhotoSize;
import smithereen.storage.MediaStorageUtils;

public class PictureForAvatarFunction extends SimpleJtwigFunction{
	@Override
	public String name(){
		return "pictureForAvatar";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());

		List<PhotoSize> sizes=(List<PhotoSize>) functionRequest.get(0);
		String _type=(String) functionRequest.get(1);
		PhotoSize.Type type, type2x;
		int size;
		switch(_type){
			case "s":
				type=PhotoSize.Type.SMALL;
				type2x=PhotoSize.Type.MEDIUM;
				size=50;
				break;
			case "m":
				type=PhotoSize.Type.MEDIUM;
				type2x=PhotoSize.Type.LARGE;
				size=100;
				break;
			case "l":
				type=PhotoSize.Type.LARGE;
				type2x=PhotoSize.Type.XLARGE;
				size=200;
				break;
			case "xl":
				type2x=type=PhotoSize.Type.XLARGE;
				size=400;
				break;
			default:
				throw new IllegalArgumentException("Wrong size type "+_type);
		}
		if(functionRequest.getNumberOfArguments()>2)
			size=((BigDecimal)functionRequest.get(2)).intValue();
		if(sizes==null)
			return "<span class=\"ava avaPlaceholder\" style=\"width: "+size+"px;height: "+size+"px\"></span>";

		PhotoSize jpeg1x=MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.JPEG, type),
				jpeg2x=MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.JPEG, type2x),
				webp1x=MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type),
				webp2x=MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type2x);

		return "<picture>" +
				"<source srcset=\""+webp1x.src+", "+webp2x.src+" 2x\" type=\"image/webp\"/>" +
				"<source srcset=\""+jpeg1x.src+", "+jpeg2x.src+" 2x\" type=\"image/jpeg\"/>" +
				"<img src=\""+jpeg1x.src+"\" width=\""+size+"\" height=\""+size+"\" class=\"ava\"/>" +
				"</picture>";
	}
}
