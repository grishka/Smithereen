package smithereen.templates;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

import smithereen.text.TextProcessor;

public class StripHTMLFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		Boolean keepLineBreaks=(Boolean) args.get("keepLineBreaks");
		return TextProcessor.stripHTML(input.toString(), keepLineBreaks==null || keepLineBreaks);
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("keepLineBreaks");
	}
}
