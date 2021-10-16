package smithereen.lang.formatting;

import java.util.Map;

import smithereen.lang.Lang;

class LiteralFormatterNode extends FormatterNode{
	private final String s;

	public LiteralFormatterNode(String s){
		this.s=s;
	}

	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		buf.append(s);
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\n")).append('"');
	}

	@Override
	public String toString(){
		return "LiteralFormatterNode{"+
				"s='"+s+'\''+
				'}';
	}
}
