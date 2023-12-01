package smithereen.templates;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.model.Group;
import smithereen.model.User;

public class ProfileUrlFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		int id=((Number)args.get("id")).intValue();
		if(id>0){
			Map<Integer, User> users=(Map<Integer, User>) context.getVariable("users");
			if(users==null)
				return "/id"+id;
			User user=users.get(id);
			if(user==null)
				return "/id"+id;
			return user.getProfileURL();
		}else if(id<0){
			id=-id;
			Map<Integer, Group> groups=(Map<Integer, Group>) context.getVariable("groups");
			if(groups==null)
				return "/club"+id;
			Group group=groups.get(id);
			if(group==null)
				return "/club"+id;
			return group.getProfileURL();
		}
		return null;
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("id");
	}
}
