package smithereen.templates.filters;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import smithereen.activitypub.objects.Actor;
import smithereen.model.Group;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.apps.ClientApp;
import smithereen.templates.Templates;

public class PictureForAvatarFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		String additionalClasses="";
		SizedImage image=switch(input){
			case Actor actor -> {
				if(actor instanceof Group)
					additionalClasses=" group";
				yield actor.getAvatar();
			}
			case SizedImage img -> img;
			case ClientApp app -> app.getLogo();
			case null, default -> null;
		};

		String typeStr=(String) args.get("type");
		SizedImage.Type type=SizedImage.Type.fromSuffix(
					switch(typeStr){
						case "s" -> "a";
						case "m" -> "b";
						case "l" -> "c";
						case "xl" -> "d";
						default -> typeStr;
					}
				);
		int size=type.getMaxWidth();
		boolean isRect=type.isRect();
		if(isRect)
			typeStr=typeStr.substring(1);
		if(args.containsKey("size"))
			size=Templates.asInt(args.get("size"));
		if(image==null){
			if(args.containsKey("wrapperClasses"))
				additionalClasses+=" "+args.get("wrapperClasses").toString();
			if(input==null)
				additionalClasses+=" deleted";
			return new SafeString("<span class=\"ava avaPlaceholder size"+typeStr.toUpperCase()+additionalClasses+"\""+(size>0 ? (" style=\"--ava-width: "+size+"px;--ava-height: "+size+"px\"") : "")+"></span>");
		}

		int width, height;
		if(isRect){
			SizedImage.Dimensions sz=image.getDimensionsForSize(type);
			width=sz.width;
			height=sz.height;
		}else{
			width=height=size;
		}

		List<String> classes=new ArrayList<>();
		if(args.containsKey("classes")){
			classes.add(args.get("classes").toString());
		}
		List<String> wrapperClasses=args.containsKey("wrapperClasses") ? List.of(args.get("wrapperClasses").toString().split(" ")) : List.of();
		return new SafeString(image.generateAvatarHTML(type, width, height, classes, wrapperClasses));
	}

	@Override
	public List<String> getArgumentNames(){
		return Arrays.asList("type", "size", "classes", "wrapperClasses");
	}
}
