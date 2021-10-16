package smithereen.lang.formatting;

import java.util.Map;

import smithereen.lang.Lang;

abstract class FormatterNode{
	public abstract void formatInto(StringBuilder buf, Map<String, Object> args, Lang lang);
	public abstract void toJS(StringBuilder buf);
}
