package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.json.JSONWriter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		return JSONWriter.valueToString(args.get("obj"));
	}

	@Override
	public List<String> getArgumentNames(){
		return Collections.singletonList("obj");
	}
}
