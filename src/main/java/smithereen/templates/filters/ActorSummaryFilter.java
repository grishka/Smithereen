package smithereen.templates.filters;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.activitypub.objects.Actor;
import smithereen.lang.Lang;
import smithereen.model.User;
import smithereen.templates.Templates;
import spark.utils.StringUtils;

public class ActorSummaryFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		Actor actor=(Actor) input;
		String status=actor.getStatusText();
		if(status!=null)
			return status;
		if(actor instanceof User user){
			if(user.birthDate!=null || StringUtils.isNotEmpty(user.location)){
				List<String> parts=new ArrayList<>();
				Lang lang=Lang.get(context.getLocale());
				if(user.birthDate!=null){
					ZoneId timeZone=Templates.getVariableRegardless(context, "timeZone");
					if(timeZone==null)
						timeZone=ZoneId.systemDefault();
					int age=(int)user.birthDate.atStartOfDay(timeZone).until(ZonedDateTime.now(timeZone), ChronoUnit.YEARS);
					parts.add(lang.get("X_years", Map.of("count", age)));
				}
				if(StringUtils.isNotEmpty(user.location))
					parts.add(user.location);
				return String.join(", ", parts);
			}
		}
		return "";
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of();
	}
}
