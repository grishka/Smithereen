package smithereen.templates;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
		boolean forceAbsolute=(Boolean) args.getOrDefault("forceAbsolute", Boolean.FALSE);
		Lang lang=Lang.get(context.getLocale());
		ZoneId timeZone=(ZoneId) context.getVariable("timeZone");
		if(arg instanceof java.sql.Date sd)
			return lang.formatDay(sd.toLocalDate());
		if(arg instanceof LocalDate ld)
			return forceAbsolute ? lang.formatDay(ld) : lang.formatDayRelative(ld, timeZone);
		if(arg instanceof Instant instant){
			String format=(String) args.get("format");
			if(format!=null){
				switch(format){
					case "timeOrDay" -> {
						return lang.formatTimeOrDay(instant, timeZone);
					}
				}
			}
			return lang.formatDate(instant, timeZone, forceAbsolute);
		}
		return "????";
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("date", "forceAbsolute", "format");
	}
}
