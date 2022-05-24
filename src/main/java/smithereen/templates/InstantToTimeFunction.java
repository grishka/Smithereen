package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class InstantToTimeFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Instant instant=(Instant) args.get("instant");
		if(instant==null)
			return "";
		TimeZone tz=Templates.getVariableRegardless(context, "timeZone");
		LocalTime time=LocalTime.ofInstant(instant, tz.toZoneId());
		return String.format(Locale.US, "%02d:%02d", time.getHour(), time.getMinute());
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("instant");
	}
}
