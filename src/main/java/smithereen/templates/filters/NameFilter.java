package smithereen.templates.filters;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.model.Group;
import smithereen.model.User;

public class NameFilter implements Filter{
	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		if(input instanceof User user){
			String format=(String) args.getOrDefault("format", "full");
			return switch(format){
				case "first" -> user.firstName;
				case "last" -> user.lastName;
				case "middle" -> user.middleName;
				case "maiden" -> user.maidenName;
				case "full" -> user.getFullName();
				case "complete" -> user.getCompleteName();
				case "firstAndGender" -> user.getFirstAndGender();
				case "fullAndGender" -> user.getFirstLastAndGender();
				default -> throw new IllegalStateException("Unexpected value: "+format);
			};
		}else if(input instanceof Group group){
			return group.name;
		}
		return "DELETED";
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("format");
	}
}
