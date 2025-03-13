package smithereen.templates.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class InlineTextResourceFunction implements Function{
	private static final Logger log=LoggerFactory.getLogger(InlineTextResourceFunction.class);

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		String resourcePath=(String)args.get("path");
		try(InputStream in = InlineTextResourceFunction.class.getClassLoader().getResourceAsStream("public/res/"+resourcePath)) {
			if(in==null) {
				log.error("Resource '{}' not found", resourcePath);
				return "";
			}
			return new SafeString(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n")));
		}catch(IOException e){
			log.error("Could not open resource '{}'", resourcePath, e);
			return "";
		}
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("path");
	}
}
