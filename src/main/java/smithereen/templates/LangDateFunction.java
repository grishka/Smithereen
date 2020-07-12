package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import smithereen.lang.Lang;

public class LangDateFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Object arg=args.get("date");
		if(arg instanceof Date)
			return Lang.get(context.getLocale()).formatDate((Date) arg, (TimeZone)context.getVariable("timeZone"));
		return "????";
	}

	@Override
	public List<String> getArgumentNames(){
		return Collections.singletonList("date");
	}
}
