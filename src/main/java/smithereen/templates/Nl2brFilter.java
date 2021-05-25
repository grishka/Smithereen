package smithereen.templates;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

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
