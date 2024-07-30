package smithereen.templates.functions;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class FormatTimeFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Instant _time=(Instant) args.get("0");
		ZoneId timeZone=(ZoneId) context.getVariable("timeZone");
		ZonedDateTime time=_time.atZone(timeZone);
		return String.format(context.getLocale(), "%d:%02d", time.getHour(), time.getMinute());
	}

	@Override
	public List<String> getArgumentNames(){
		return null;
	}
}
