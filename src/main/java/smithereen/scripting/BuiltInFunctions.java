package smithereen.scripting;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BuiltInFunctions{
	private static final Pattern INT_REGEX=Pattern.compile("^(-?\\d+)");
	private static final Pattern DOUBLE_REGEX=Pattern.compile("^(-?\\d+(\\.\\d+)?(e\\d+)?)");

	public static ScriptValue parseInt(List<ScriptValue> args, int lineNumber){
		if(args.size()!=1)
			throw new ScriptCompilationException("Expected 1 argument, got "+args.size(), lineNumber);
		return ScriptValue.of(switch(args.getFirst()){
			case ScriptValue.Bool(boolean b) -> b ? 1 : 0;
			case ScriptValue.Str(String s) -> {
				Matcher m=INT_REGEX.matcher(s);
				if(m.find()){
					try{
						yield Long.parseLong(m.group(1));
					}catch(NumberFormatException ignore){}
				}
				yield 0;
			}
			case ScriptValue.Num(double n) -> (long)n;
			case ScriptValue.Obj(Map<String, ScriptValue> obj) -> obj.isEmpty() ? 0 : 1;
			case ScriptValue.Arr(List<ScriptValue> arr) -> arr.isEmpty() ? 0 : 1;
			case null -> 0;
		});
	}

	public static ScriptValue parseDouble(List<ScriptValue> args, int lineNumber){
		if(args.size()!=1)
			throw new ScriptCompilationException("Expected 1 argument, got "+args.size(), lineNumber);
		return ScriptValue.of(switch(args.getFirst()){
			case ScriptValue.Bool(boolean b) -> b ? 1 : 0;
			case ScriptValue.Str(String s) -> {
				Matcher m=DOUBLE_REGEX.matcher(s);
				if(m.find()){
					try{
						yield Double.parseDouble(m.group(1));
					}catch(NumberFormatException ignore){}
				}
				yield 0;
			}
			case ScriptValue.Num(double n) -> n;
			case ScriptValue.Obj(Map<String, ScriptValue> obj) -> obj.isEmpty() ? 0 : 1;
			case ScriptValue.Arr(List<ScriptValue> arr) -> arr.isEmpty() ? 0 : 1;
			case null -> 0;
		});
	}
}
