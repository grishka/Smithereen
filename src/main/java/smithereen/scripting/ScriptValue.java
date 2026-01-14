package smithereen.scripting;

import java.util.List;
import java.util.Map;

public sealed interface ScriptValue{
	Bool TRUE=new Bool(true);
	Bool FALSE=new Bool(false);

	record Bool(boolean bool) implements ScriptValue{}
	record Str(String str) implements ScriptValue{}
	record Num(double num) implements ScriptValue{}
	record Obj(Map<String, ScriptValue> obj) implements ScriptValue{}
	record Arr(List<ScriptValue> arr) implements ScriptValue{}

	static ScriptValue.Bool of(boolean b){
		return b ? TRUE : FALSE;
	}

	static ScriptValue.Str of(String s){
		return new Str(s);
	}

	static ScriptValue.Num of(long n){
		return new ScriptValue.Num(n);
	}

	static ScriptValue.Num of(double n){
		return new ScriptValue.Num(n);
	}

	static ScriptValue.Obj of(Map<String, ScriptValue> obj){
		return new ScriptValue.Obj(obj);
	}

	static ScriptValue.Arr of(List<ScriptValue> arr){
		return new ScriptValue.Arr(arr);
	}
}
