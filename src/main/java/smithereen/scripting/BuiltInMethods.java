package smithereen.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

class BuiltInMethods{
	// region Array

	public static ScriptValue arrayPush(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		arr.addAll(args);
		return ScriptValue.of(arr.size());
	}

	public static ScriptValue arrayPop(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		return arr.isEmpty() ? null : arr.removeLast();
	}

	public static ScriptValue arraySlice(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr)this_).arr();
		if(!(args.getFirst() instanceof ScriptValue.Num(double _start)))
			throw new ScriptRuntimeException("Invalid argument type for array.slice", lineNumber);
		int start=(int) _start;

		if(start<0 && start>=-arr.size()){
			start+=arr.size();
		}else if(start<-arr.size()){
			start=0;
		}else if(start>=arr.size()){
			return ScriptValue.of(new ArrayList<>());
		}

		int end;
		if(args.size()==2 && args.get(1) instanceof ScriptValue.Num(double _end)){
			end=(int) _end;
			if(end<0 && end>=-arr.size()){
				end+=arr.size();
			}else if(end<-arr.size()){
				end=0;
			}else if(end>arr.size()){
				end=arr.size();
			}
		}else{
			end=arr.size();
		}

		if(end<=start)
			return ScriptValue.of(new ArrayList<>());

		return ScriptValue.of(new ArrayList<>(arr.subList(start, end)));
	}

	public static ScriptValue arrayShift(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		return arr.isEmpty() ? null : arr.removeFirst();
	}

	public static ScriptValue arrayUnshift(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		arr.addAll(0, args);
		return ScriptValue.of(arr.size());
	}

	public static ScriptValue arraySplice(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		if(!(args.getFirst() instanceof ScriptValue.Num(double _start)))
			throw new ScriptRuntimeException("Invalid argument type for array.splice", lineNumber);
		int start=(int) _start;

		if(start<0 && start>=-arr.size()){
			start+=arr.size();
		}else if(start<-arr.size()){
			start=0;
		}else if(start>=arr.size()){
			start=arr.size();
		}

		int deleteCount;
		if(args.size()>1 && args.get(1) instanceof ScriptValue.Num(double _deleteCount)){
			deleteCount=Math.min(Math.max(0, (int) _deleteCount), arr.size()-start);
		}else{
			deleteCount=arr.size()-start;
		}

		List<ScriptValue> subList=arr.subList(start, start+deleteCount);
		ArrayList<ScriptValue> result=new ArrayList<>(subList);

		subList.clear();
		if(args.size()>2){
			subList.addAll(args.subList(2, args.size()));
		}

		return ScriptValue.of(result);
	}

	public static ScriptValue arrayIndexOf(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		List<ScriptValue> arr=((ScriptValue.Arr) this_).arr();
		return ScriptValue.of(arr.indexOf(args.getFirst()));
	}

	// endregion
	// region String

	public static ScriptValue stringSplit(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		if(!(args.getFirst() instanceof ScriptValue.Str(String separator)))
			throw new ScriptRuntimeException("Invalid argument type for string.split", lineNumber);
		String str=((ScriptValue.Str)this_).str();
		if(separator.isEmpty())
			throw new ScriptRuntimeException("Empty string passed as separator to string.split", lineNumber);
		int limit=-1;
		if(args.size()==2 && args.get(1) instanceof ScriptValue.Num(double _limit))
			limit=(int)_limit;
		return ScriptValue.of(Arrays.stream(str.split(Pattern.quote(separator), limit)).map(s->(ScriptValue)ScriptValue.of(s)).toList());
	}

	public static ScriptValue stringSubstr(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		if(!(args.getFirst() instanceof ScriptValue.Num(double _start)))
			throw new ScriptRuntimeException("Invalid argument type for string.substr", lineNumber);
		String str=((ScriptValue.Str)this_).str();
		int start=(int)_start;
		if(start>=str.length()){
			return ScriptValue.of("");
		}else if(start<0){
			start=Math.max(0, start+str.length());
		}

		int length;
		if(args.size()==2 && args.get(1) instanceof ScriptValue.Num(double _length)){
			length=(int)_length;
			if(length<0 || length<_length-1)
				return ScriptValue.of("");
			if(start+length>str.length())
				length=str.length()-start;
		}else{
			length=str.length()-start;
		}

		return ScriptValue.of(str.substring(start, start+length));
	}

	public static ScriptValue stringIndexOf(ScriptValue this_, List<ScriptValue> args, int lineNumber){
		if(!(args.getFirst() instanceof ScriptValue.Str(String searchString)))
			throw new ScriptRuntimeException("Invalid argument type for string.indexOf", lineNumber);
		String str=((ScriptValue.Str) this_).str();
		return ScriptValue.of(str.indexOf(searchString));
	}

	// endregion
}
