package smithereen.templates;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

import smithereen.text.TextProcessor;

public class PostprocessHTMLFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		return new SafeString(TextProcessor.postprocessPostHTMLForDisplay((String)input, (boolean)args.getOrDefault("forceTargetBlank", Boolean.FALSE)));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("forceTargetBlank");
	}
}
