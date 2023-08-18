package smithereen.templates;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

import smithereen.data.UriBuilder;

public class AddQueryParamsFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		String url=(String) args.get("url");
		Map<String, Object> params=(Map<String, Object>) args.get("params");
		UriBuilder builder=new UriBuilder(url);
		params.forEach((key, value)->{
			if(value!=null)
				builder.replaceQueryParam(key, value.toString());
			else
				builder.removeQueryParam(key);
		});
		return builder.build().toString();
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("url", "params");
	}
}
