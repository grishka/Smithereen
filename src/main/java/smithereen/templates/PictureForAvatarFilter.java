package smithereen.templates;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import smithereen.activitypub.objects.Actor;
import smithereen.data.Group;
import smithereen.data.SizedImage;
import smithereen.data.User;

public class PictureForAvatarFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		SizedImage image;
		String additionalClasses="";
		if(input instanceof Actor){
			Actor actor=(Actor) input;
			image=actor.getAvatar();
			if(actor instanceof User && ((User)actor).gender==User.Gender.FEMALE)
				additionalClasses=" female";
			else if(actor instanceof Group)
				additionalClasses=" group";
		}else{
			return "";
		}

		String _type=(String) args.get("type");
		SizedImage.Type type, type2x;
		int size;
		boolean isRect=false;
		switch(_type){
			case "s":
				type=SizedImage.Type.SQUARE_SMALL;
				type2x=SizedImage.Type.SQUARE_MEDIUM;
				size=50;
				break;
			case "m":
				type=SizedImage.Type.SQUARE_MEDIUM;
				type2x=SizedImage.Type.SQUARE_LARGE;
				size=100;
				break;
			case "l":
				type=SizedImage.Type.SQUARE_LARGE;
				type2x=SizedImage.Type.SQUARE_XLARGE;
				size=200;
				break;
			case "xl":
				type2x=type=SizedImage.Type.SQUARE_XLARGE;
				size=400;
				break;
			case "rl":
				type=SizedImage.Type.RECT_LARGE;
				type2x=SizedImage.Type.RECT_XLARGE;
				size=200;
				_type="l";
				isRect=true;
				break;
			case "rxl":
				type=type2x=SizedImage.Type.RECT_XLARGE;
				size=400;
				_type="xl";
				isRect=true;
				break;
			default:
				throw new IllegalArgumentException("Wrong size type "+_type);
		}
		if(args.containsKey("size"))
			size=Templates.asInt(args.get("size"));
		if(image==null)
			return new SafeString("<span class=\"ava avaPlaceholder size"+_type.toUpperCase()+additionalClasses+"\" style=\"width: "+size+"px;height: "+size+"px\"></span>");

		URI jpeg1x=image.getUriForSizeAndFormat(type, SizedImage.Format.JPEG),
				jpeg2x=image.getUriForSizeAndFormat(type2x, SizedImage.Format.JPEG),
				webp1x=image.getUriForSizeAndFormat(type, SizedImage.Format.WEBP),
				webp2x=image.getUriForSizeAndFormat(type2x, SizedImage.Format.WEBP);

		int width, height;
		if(isRect){
			SizedImage.Dimensions sz=image.getDimensionsForSize(type);
			width=sz.width;
			height=sz.height;
		}else{
			width=height=size;
		}

		String classes="avaImage";
		if(args.containsKey("classes")){
			classes+=" "+args.get("classes");
		}

		return new SafeString("<span class=\"ava avaHasImage size"+_type.toUpperCase()+"\"><picture>" +
				"<source srcset=\""+webp1x+", "+webp2x+" 2x\" type=\"image/webp\"/>" +
				"<source srcset=\""+jpeg1x+", "+jpeg2x+" 2x\" type=\"image/jpeg\"/>" +
				"<img src=\""+jpeg1x+"\" width=\""+width+"\" height=\""+height+"\" class=\""+classes+"\"/>" +
				"</picture></span>");
	}

	@Override
	public List<String> getArgumentNames(){
		return Arrays.asList("type", "size", "classes");
	}
}
