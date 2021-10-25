package smithereen.lang.formatting;

import java.text.NumberFormat;
import java.util.Map;

import smithereen.lang.Lang;

public class NumberFormatterNode extends FormatterNode{
	private final String id;
	private final String format;

	public NumberFormatterNode(String id, String format){
		this.id=id;
		this.format=format;
	}

	@Override
	public void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang){
		Object arg=args.get(id);
		if(arg==null){
			buf.append("{").append(id).append(" missing}");
			return;
		}else if(!(arg instanceof Number)){
			buf.append("NaN");
			return;
		}

		buf.append(NumberFormat.getInstance(lang.getLocale()).format(arg));
	}

	@Override
	public void toJS(StringBuilder buf){
		buf.append("formatNumber(a['").append(id).append("'])");
	}

	@Override
	public String toString(){
		return "NumberFormatterNode{"+
				"id='"+id+'\''+
				", format='"+format+'\''+
				'}';
	}
}
