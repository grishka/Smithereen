package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class FormatTimeFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Instant _time=(Instant) args.get("0");
		TimeZone timeZone=(TimeZone) context.getVariable("timeZone");
		ZonedDateTime time=_time.atZone(timeZone.toZoneId());
		return String.format(context.getLocale(), "%d:%02d", time.getHour(), time.getMinute());
	}

	@Override
	public List<String> getArgumentNames(){
		return null;
	}
}
