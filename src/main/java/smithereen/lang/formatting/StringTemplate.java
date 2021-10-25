package smithereen.lang.formatting;

import java.util.List;
import java.util.Map;

import smithereen.lang.Lang;

public class StringTemplate{
	private List<FormatterNode> nodes;

	StringTemplate(List<FormatterNode> nodes){
		this.nodes=nodes;
	}

	public String format(Map<String, Object> args, Lang lang){
		StringBuilder sb=new StringBuilder();
		for(FormatterNode node:nodes){
			node.formatInto(sb, args, lang);
		}
		return sb.toString();
	}

	@Override
	public String toString(){
		return "StringTemplate{"+
				"nodes="+nodes+
				'}';
	}

	public String getAsJS(){
		StringBuilder sb=new StringBuilder("function(a){return ");
		int i=0;
		for(FormatterNode node:nodes){
			node.toJS(sb);
			if(i<nodes.size()-1)
				sb.append('+');
			i++;
		}
		sb.append(";}");
		return sb.toString();
	}
}
