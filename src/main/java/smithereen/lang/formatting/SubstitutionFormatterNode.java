package smithereen.lang.formatting;

import io.pebbletemplates.pebble.extension.escaper.SafeString;

import org.unbescape.html.HtmlEscape;

import java.util.Map;
import java.util.Objects;

import smithereen.lang.Lang;

public class SubstitutionFormatterNode extends FormatterNode{
	private final String id;

	public SubstitutionFormatterNode(String id){
		this.id=id;
	}

	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		if(args.containsKey(id)){
			Object arg=args.get(id);
			if(arg instanceof SafeString)
				buf.append(convertArgument(arg));
			else
				buf.append(HtmlEscape.escapeHtml4Xml(convertArgument(arg)));
		}else{
			buf.append("{").append(id).append(" missing}");
		}
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append("a['").append(id).append("']");
	}

	@Override
	public String toString(){
		return "SubstitutionFormatterNode{"+
				"id='"+id+'\''+
				'}';
	}

	@SuppressWarnings("unchecked")
	private String convertArgument(Object arg){
		// Convert maps with names (used for inflection in Russian) to strings
		if(arg instanceof Map){
			Map<String, Object> map=(Map<String, Object>)arg;
			if(map.containsKey("gender") && map.containsKey("first")){
				if(map.containsKey("last"))
					return map.get("first")+" "+map.get("last");
				return map.get("first").toString();
			}
		}
		return Objects.toString(arg);
	}
}
