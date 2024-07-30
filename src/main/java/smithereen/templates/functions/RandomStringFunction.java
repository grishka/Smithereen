package smithereen.templates.functions;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.Utils;

public class RandomStringFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		return Utils.randomAlphanumericString((Integer)args.getOrDefault("length", 10));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("length");
	}
}
