package smithereen.templates.functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.template.Scope;
import smithereen.templates.Templates;
import spark.Request;

public class AddExtraScriptFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		String name=(String) args.get("name");
		String hash=Templates.staticHashes.get(name);
		if(hash==null)
			throw new IllegalArgumentException("Static file hash for "+name+" is not known. "+self.getName()+":"+lineNumber);
		Scope lastScope=((EvaluationContextImpl)context).getScopeChain().getGlobalScopes().getLast();
		if(!lastScope.containsKey("_extraScriptFiles"))
			lastScope.put("_extraScriptFiles", new HashMap<String, String>());
		HashMap<String, String> extraScriptFiles=(HashMap<String, String>) lastScope.get("_extraScriptFiles");
		extraScriptFiles.put(name, hash);
		Request req=(Request) context.getVariable("_request");
		if(req.attribute("extraScriptFiles")==null)
			req.attribute("extraScriptFiles", new HashSet<String>());
		HashSet<String> requestExtraScripts=req.attribute("extraScriptFiles");
		requestExtraScripts.add(name);
		return null;
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("name");
	}
}
