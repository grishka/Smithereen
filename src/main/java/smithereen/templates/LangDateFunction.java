package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.time.LocalDate;
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
		if(arg instanceof java.sql.Date)
			return Lang.get(context.getLocale()).formatDay(((java.sql.Date) arg).toLocalDate());
		if(arg instanceof LocalDate)
			return Lang.get(context.getLocale()).formatDay((LocalDate) arg);
		if(arg instanceof Date)
			return Lang.get(context.getLocale()).formatDate((Date) arg, (TimeZone) context.getVariable("timeZone"), (Boolean) args.getOrDefault("forceAbsolute", Boolean.FALSE));
		return "????";
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("date", "forceAbsolute");
	}
}
