package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.unbescape.html.HtmlEscape;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import smithereen.lang.Lang;

public class LangFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Locale locale=context.getLocale();
		if(args.size()<1)
			throw new IllegalArgumentException("Not enough arguments");

		String key=(String) args.get("key");
		if(args.size()==1){
			return new SafeString(Lang.get(locale).get(key));
		}else{
			if(!(args.get("vars") instanceof Map)){
				throw new IllegalArgumentException("wrong arg types "+args);
			}
			//noinspection unchecked
			Map<String, Object> formatArgs=(Map<String, Object>) args.get("vars");
			String formatted=Lang.get(locale).get(key, formatArgs);
			if(args.get("links") instanceof Map links){
				//noinspection unchecked
				return new SafeString(substituteLinks(formatted, links));
			}else{
				return new SafeString(formatted);
			}
		}
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("key", "vars", "links");
	}

	private String substituteLinks(String str, Map<String, Object> links){
		Element root=Jsoup.parseBodyFragment(str).body();
		for(String id:links.keySet()){
			Element link=root.getElementById(id);
			if(link==null)
				continue;
			link.removeAttr("id");
			//noinspection unchecked
			Map<String, Object> attrs=(Map<String, Object>) links.get(id);
			for(String attr:attrs.keySet()){
				Object value=attrs.get(attr);
				if(attr.equals("_")){
					link.tagName(value.toString());
				}else if(value instanceof Boolean b)
					link.attr(attr, b);
				else if(value instanceof String s)
					link.attr(attr, s);
			}
		}
		return root.html();
	}
}
