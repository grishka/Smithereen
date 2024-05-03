package smithereen.lang.formatting;

import java.util.Map;

import smithereen.model.User;
import smithereen.lang.Inflector;
import smithereen.lang.Lang;
import smithereen.text.TextProcessor;

public class InflectFormatterNode extends FormatterNode{
	private final String id;
	private final Inflector.Case _case;

	public InflectFormatterNode(String id, Inflector.Case _case){
		this.id=id;
		this._case=_case;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		if(!args.containsKey(id)){
			buf.append("{").append(id).append(" missing}");
			return;
		}
		Object arg=args.get(id);
		if(!(arg instanceof Map)){
			buf.append(arg);
			return;
		}
		Map<String, Object> map=(Map<String, Object>) arg;
		if(!map.containsKey("first") || !map.containsKey("gender")){
			buf.append("{").append(id).append(" has invalid map format}");
			return;
		}
		String first=TextProcessor.escapeHTML((String) map.get("first"));
		String last=TextProcessor.escapeHTML((String) map.get("last"));
		User.Gender gender=(User.Gender) map.get("gender");
		lang.inflected(buf, gender, first, last, _case);
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append("a['").append(id).append("'");
	}
}
