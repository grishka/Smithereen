package smithereen.templates;

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
						case "s" -> "sqs";
						case "m" -> "sqm";
						case "l" -> "sql";
						case "xl" -> "sqxl";
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
			return new SafeString("<span class=\"ava avaPlaceholder size"+typeStr.toUpperCase()+additionalClasses+"\""+(size>0 ? (" style=\"width: "+size+"px;height: "+size+"px\"") : "")+"></span>");
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
		return new SafeString("<span class=\"ava avaHasImage size"+typeStr.toUpperCase()+(args.containsKey("wrapperClasses") ? (" "+args.get("wrapperClasses")) : "")+"\">"+image.generateHTML(type, classes, null, width, height, true)+"</span>");
	}

	@Override
	public List<String> getArgumentNames(){
		return Arrays.asList("type", "size", "classes", "wrapperClasses");
	}
}
