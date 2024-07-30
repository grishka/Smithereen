package smithereen.templates.filters;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

import smithereen.model.SizedImage;

public class PictureForPhotoFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		if(input instanceof SizedImage img){
			SizedImage.Type type=SizedImage.Type.fromSuffix(args.get("size").toString());
			SizedImage.Dimensions size=img.getDimensionsForSize(type);
			return new SafeString(img.generateHTML(type, null, null, size.width, size.height, (boolean)args.getOrDefault("add2x", Boolean.TRUE)));
		}
		throw new IllegalArgumentException("Input is an unknown type: "+input);
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("size", "add2x");
	}
}
