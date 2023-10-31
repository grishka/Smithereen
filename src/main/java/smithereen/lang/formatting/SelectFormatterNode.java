package smithereen.lang.formatting;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import smithereen.model.User;
import smithereen.lang.Lang;

public class SelectFormatterNode extends FormatterNode{
	private String id;
	private Map<String, List<FormatterNode>> subMessages;

	public SelectFormatterNode(String id, Map<String, List<FormatterNode>> subMessages){
		this.id=id;
		this.subMessages=subMessages;
	}

	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		Object _arg=args.get(id);
		String arg;
		if(_arg instanceof User.Gender g){
			arg=switch(g){
				case UNKNOWN, OTHER -> "other";
				case MALE -> "male";
				case FEMALE -> "female";
			};
		}else if(_arg instanceof Enum<?> e){
			arg=e.name().toLowerCase();
		}else{
			arg=Objects.toString(_arg);
		}
		List<FormatterNode> sub=subMessages.containsKey(arg) ? subMessages.get(arg) : subMessages.get("other");
		for(FormatterNode node:sub){
			node.formatInto(buf, args, lang);
		}
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append("chooseLangOption(a['").append(id).append("'], a, {");
		int i=0;
		for(String k:subMessages.keySet()){
			buf.append('"').append(k).append("\":function(a){return ");
			List<FormatterNode> nodes=subMessages.get(k);
			int j=0;
			for(FormatterNode node:nodes){
				node.toJS(buf);
				if(j<nodes.size()-1)
					buf.append('+');
				j++;
			}
			buf.append(";}");
			if(i<subMessages.size()-1)
				buf.append(',');
			i++;
		}
		buf.append("})");
	}

	@Override
	public String toString(){
		return "SelectFormatterNode{"+
				"id='"+id+'\''+
				", subMessages="+subMessages+
				'}';
	}
}
