package smithereen.templates.functions;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import smithereen.lang.Lang;
import smithereen.templates.Templates;

public class LangDateFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Object arg=args.get("date");
		boolean forceAbsolute=(Boolean) args.getOrDefault("forceAbsolute", Boolean.FALSE);
		Lang lang=Lang.get(context.getLocale());
		ZoneId timeZone=Templates.getVariableRegardless(context, "timeZone");
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
					case "fullyAbsolute" -> {
						return lang.formatDateFullyAbsolute(instant, timeZone, false);
					}
					case "fullyAbsoluteWithSeconds" -> {
						return lang.formatDateFullyAbsolute(instant, timeZone, true);
					}
					case "short" -> {
						return lang.formatDateShort(instant, timeZone);
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
