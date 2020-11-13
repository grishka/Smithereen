package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.unbescape.html.HtmlEscape;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import smithereen.data.User;
import smithereen.lang.Lang;

public class LangInflectedFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Locale locale=context.getLocale();
		String first=HtmlEscape.escapeHtml4Xml((String)args.get("first"));
		String last=HtmlEscape.escapeHtml4Xml((String)args.get("last"));
		String middle=HtmlEscape.escapeHtml4Xml((String)args.get("middle"));
		String[] formatArgs;
		if(args.containsKey("args")){
			List<Object> fargs=(List<Object>) args.get("args");
			formatArgs=fargs.stream().map(Object::toString).map(HtmlEscape::escapeHtml4Xml).toArray(String[]::new);
		}else{
			formatArgs=new String[0];
		}

		return new SafeString(Lang.get(locale).inflected((String)args.get("key"), (User.Gender)args.get("gender"), first, last, middle, formatArgs));
	}

	@Override
	public List<String> getArgumentNames(){
		return Arrays.asList("key", "first", "middle", "last", "gender", "args");
	}
}
