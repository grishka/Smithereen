package smithereen.templates;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import smithereen.lang.Lang;
import smithereen.text.TextProcessor;

public class LangFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Locale locale=context.getLocale();
		if(args.size()<1)
			throw new IllegalArgumentException("Not enough arguments");

		String key=(String) args.get("key");
		if(args.size()==1){
			return new SafeString(nl2br(Lang.get(locale).get(key)));
		}else{
			if(!(args.get("vars") instanceof Map)){
				throw new IllegalArgumentException("wrong arg types "+args);
			}
			//noinspection unchecked
			Map<String, Object> formatArgs=(Map<String, Object>) args.get("vars");
			String formatted=Lang.get(locale).get(key, formatArgs);
			if(args.get("links") instanceof Map links){
				//noinspection unchecked
				return new SafeString(nl2br(TextProcessor.substituteLinks(formatted, links)));
			}else{
				return new SafeString(nl2br(formatted));
			}
		}
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("key", "vars", "links");
	}

	private String nl2br(String in){
		return in.replace("\n", "<br/>");
	}
}
