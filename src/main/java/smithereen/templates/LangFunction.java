package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.unbescape.html.HtmlEscape;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import smithereen.lang.Lang;

public class LangFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Locale locale=context.getLocale();
		if(args.size()<1)
			throw new IllegalArgumentException("Not enough arguments");

		String key=(String) args.get("0");
		if(args.size()==1){
			return new SafeString(Lang.get(locale).get(key));
		}else{
			if(!(args.get("1") instanceof Map)){
				throw new IllegalArgumentException("wrong arg types "+args);
			}
			Map<String, Object> formatArgs=(Map<String, Object>) args.get("1");
			return new SafeString(Lang.get(locale).get(key, formatArgs));
		}
	}

	@Override
	public List<String> getArgumentNames(){
		return null;
	}
}
