package smithereen.templates.functions;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.lang.Lang;

public class FormatFileSizeFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		return Lang.get(context.getLocale()).formatFileSize((Long)args.get("size"));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("size");
	}
}
