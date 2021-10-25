package smithereen.lang.formatting;

import java.util.List;
import java.util.Map;

import smithereen.lang.Lang;

public class PluralFormatterNode extends FormatterNode{
	private final String id;
	private final Map<String, List<FormatterNode>> subMessages;
	private final int offset;

	public PluralFormatterNode(String id, Map<String, List<FormatterNode>> subMessages, int offset){
		this.id=id;
		this.subMessages=subMessages;
		this.offset=offset;
	}

	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		Object arg=args.get(id);
		List<FormatterNode> chosenMsg;
		if(arg==null){
			buf.append("{").append(id).append(" missing}");
			return;
		}else if(!(arg instanceof Number)){
			chosenMsg=subMessages.get("other");
		}else{
			int n=((Number) arg).intValue();
			String nstr=n+"";
			if(subMessages.containsKey(nstr)){
				chosenMsg=subMessages.get(nstr);
			}else{
				chosenMsg=subMessages.get(lang.getPluralCategory(n).value);
				if(chosenMsg==null)
					chosenMsg=subMessages.get("other");
			}
		}
		for(FormatterNode node:chosenMsg){
			node.formatInto(buf, args, lang);
		}
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append("choosePluralForm(a['").append(id).append("'], a, {");
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
		return "PluralFormatterNode{"+
				"id='"+id+'\''+
				", subMessages="+subMessages+
				", offset="+offset+
				'}';
	}
}
