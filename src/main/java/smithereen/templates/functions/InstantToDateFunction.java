package smithereen.templates.functions;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.templates.Templates;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InstantToDateFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Instant instant=(Instant) args.get("instant");
		if(instant==null)
			return "";
		ZoneId tz=Templates.getVariableRegardless(context, "timeZone");
		return LocalDate.ofInstant(instant, Objects.requireNonNull(tz));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("instant");
	}
}
