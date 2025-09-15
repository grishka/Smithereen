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
import smithereen.templates.Templates;

public class PictureForAvatarFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		SizedImage image;
		String additionalClasses="";
		if(input instanceof Actor actor){
			image=actor.getAvatar();
			if(actor instanceof Group)
				additionalClasses=" group";
		}else{
			image=null;
		}

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
		classes.add("avaImage");
		if(args.containsKey("classes")){
			classes.add(args.get("classes").toString());
		}
		return new SafeString("<span class=\"ava avaHasImage size"+typeStr.toUpperCase()+(args.containsKey("wrapperClasses") ? (" "+args.get("wrapperClasses")) : "")+"\""+(isRect || args.containsKey("size") ? (" style=\"--ava-width: "+width+"px;--ava-height: "+height+"px\"") : "")+">"
				+image.generateHTML(type, classes, null, 0, 0, true, null)+"</span>");
	}

	@Override
	public List<String> getArgumentNames(){
		return Arrays.asList("type", "size", "classes", "wrapperClasses");
	}
}
