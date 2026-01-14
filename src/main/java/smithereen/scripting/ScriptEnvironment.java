package smithereen.scripting;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ScriptEnvironment{
	Map<String, ScriptVM.BuiltInFunctionRecord> builtInFunctions=new HashMap<>();
	Map<ScriptVM.MethodKey, ScriptVM.BuiltInMethodRecord> builtInMethods=new HashMap<>();
	final Function<String, String> getArgumentCallback;
	final ScriptVM.ApiCallCallback apiCallCallback;

	public ScriptEnvironment(Function<String, String> getArgumentCallback, ScriptVM.ApiCallCallback apiCallCallback){
		this.getArgumentCallback=getArgumentCallback;
		this.apiCallCallback=apiCallCallback;
		addFunction("parseInt", 1, 1, BuiltInFunctions::parseInt);
		addFunction("parseDouble", 1, 1, BuiltInFunctions::parseDouble);

		addMethod(ScriptValue.Arr.class, "push", 1, 100, BuiltInMethods::arrayPush);
		addMethod(ScriptValue.Arr.class, "slice", 1, 2, BuiltInMethods::arraySlice);
		addMethod(ScriptValue.Arr.class, "pop", 0, 0, BuiltInMethods::arrayPop);
		addMethod(ScriptValue.Arr.class, "shift", 0, 0, BuiltInMethods::arrayShift);
		addMethod(ScriptValue.Arr.class, "unshift", 1, 100, BuiltInMethods::arrayUnshift);
		addMethod(ScriptValue.Arr.class, "splice", 1, 100, BuiltInMethods::arraySplice);
		addMethod(ScriptValue.Arr.class, "indexOf", 1, 1, BuiltInMethods::arrayIndexOf);

		addMethod(ScriptValue.Str.class, "split", 1, 2, BuiltInMethods::stringSplit);
		addMethod(ScriptValue.Str.class, "substr", 1, 2, BuiltInMethods::stringSubstr);
		addMethod(ScriptValue.Str.class, "indexOf", 1, 1, BuiltInMethods::stringIndexOf);
	}

	public void addFunction(String name, int minArgs, int maxArgs, ScriptVM.BuiltInFunction impl){
		builtInFunctions.put(name, new ScriptVM.BuiltInFunctionRecord(minArgs, maxArgs, impl));
	}

	public void addMethod(Class<? extends ScriptValue> thisClass, String name, int minArgs, int maxArgs, ScriptVM.BuiltInMethod impl){
		builtInMethods.put(new ScriptVM.MethodKey(thisClass, name), new ScriptVM.BuiltInMethodRecord(minArgs, maxArgs, impl));
	}
}
