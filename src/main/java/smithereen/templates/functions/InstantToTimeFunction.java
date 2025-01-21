package smithereen.templates.functions;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.templates.Templates;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InstantToTimeFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Instant instant=(Instant) args.get("instant");
		if(instant==null)
			return "";
		ZoneId tz=Templates.getVariableRegardless(context, "timeZone");
		LocalTime time=LocalTime.ofInstant(instant, tz);
		return String.format(Locale.US, "%02d:%02d", time.getHour(), time.getMinute());
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("instant");
	}
}
