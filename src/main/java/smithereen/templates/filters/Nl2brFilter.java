package smithereen.templates.filters;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import org.unbescape.html.HtmlEscape;

import java.util.List;
import java.util.Map;

public class Nl2brFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		return new SafeString(HtmlEscape.escapeHtml4Xml(input.toString()).replace("\n", "<br/>"));
	}

	@Override
	public List<String> getArgumentNames(){
		return null;
	}
}
