package smithereen.templates.functions;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.activitypub.objects.Actor;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.User;

public class ProfileRelFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Actor actor=null;
		Object arg=args.get("actor");
		if(arg instanceof Actor a){
			actor=a;
		}else if(arg instanceof Integer id){
			if(id>0){
				Map<Integer, User> users=(Map<Integer, User>) context.getVariable("users");
				actor=users.get(id);
			}else{
				Map<Integer, Group> groups=(Map<Integer, Group>) context.getVariable("groups");
				actor=groups.get(-id);
			}
		}

		if(actor instanceof ForeignUser || actor instanceof ForeignGroup){
			return new SafeString(" rel=\"nofollow\"");
		}

		return "";
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("actor");
	}
}
