package smithereen.scripting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import spark.utils.StringUtils;

public class ScriptVM{
	// For tests only
	static ScriptValue execute(Script script){
		return execute(script, new ScriptEnvironment(name->null, null));
	}

	public static ScriptValue execute(Script script, ScriptEnvironment env){
		int ip=0;
		Stack<ScriptValue> stack=new Stack<>();
		ScriptValue[] vars=new ScriptValue[script.variableCount];
		int totalOps=0;
		while(true){
			if(++totalOps>10_000)
				throw new ScriptRuntimeException("Too many operations", script.getLineNumber(ip));
//			System.out.println(ip+"\t"+stack);
			int opcode=(int)script.ops[ip++] & 0xff;
			switch(opcode){
				case Op.LOAD_CONST -> stack.push(script.constants[script.operands[ip-1]]);
				case Op.LOAD_NULL -> stack.push(null);
				case Op.LOAD_TRUE -> stack.push(ScriptValue.TRUE);
				case Op.LOAD_FALSE -> stack.push(ScriptValue.FALSE);
				case Op.POP -> stack.pop();
				case Op.GET_VARIABLE -> stack.push(vars[script.operands[ip-1]]);
				case Op.SET_VARIABLE -> vars[script.operands[ip-1]]=stack.peek();
				case Op.IS_EQUAL -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					stack.push(areValuesEqual(a, b, script.getLineNumber(ip-1)) ? ScriptValue.TRUE : ScriptValue.FALSE);
				}
				case Op.IS_NOT_EQUAL -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					stack.push(areValuesEqual(a, b, script.getLineNumber(ip-1)) ? ScriptValue.FALSE : ScriptValue.TRUE);
				}
				case Op.IS_GREATER, Op.IS_GREATER_OR_EQUAL, Op.IS_LESS, Op.IS_LESS_OR_EQUAL -> {
					int line=script.getLineNumber(ip-1);
					double b=getNumberForComparison(stack.pop(), line);
					double a=getNumberForComparison(stack.pop(), line);
					boolean res=switch(opcode){
						case Op.IS_GREATER -> a>b;
						case Op.IS_GREATER_OR_EQUAL -> a>=b;
						case Op.IS_LESS -> a<b;
						case Op.IS_LESS_OR_EQUAL -> a<=b;
						default -> throw new IllegalStateException();
					};
					stack.push(res ? ScriptValue.TRUE : ScriptValue.FALSE);
				}
				case Op.ADD -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
						stack.push(ScriptValue.of(na+nb));
					}else{
						String sa=switch(a){
							case ScriptValue.Num(double n) -> numberToString(n);
							case ScriptValue.Str(String s) -> s;
							case null, default -> throw new ScriptRuntimeException("Invalid operand type for '+'", script.getLineNumber(ip-1));
						};
						String sb=switch(b){
							case ScriptValue.Num(double n) -> numberToString(n);
							case ScriptValue.Str(String s) -> s;
							case null, default -> throw new ScriptRuntimeException("Invalid operand type for '+'", script.getLineNumber(ip-1));
						};
						stack.push(ScriptValue.of(sa+sb));
					}
				}
				case Op.SUBTRACT -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
						stack.push(ScriptValue.of(na-nb));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '-'", script.getLineNumber(ip-1));
					}
				}
				case Op.MULTIPLY -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
						stack.push(ScriptValue.of(na*nb));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '*'", script.getLineNumber(ip-1));
					}
				}
				case Op.DIVIDE -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
						if(nb==0)
							throw new ScriptRuntimeException("Division by zero", script.getLineNumber(ip-1));
						stack.push(ScriptValue.of(na/nb));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '/'", script.getLineNumber(ip-1));
					}
				}
				case Op.REMAINDER -> {
					ScriptValue b=stack.pop();
					ScriptValue a=stack.pop();
					if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
						if(nb==0)
							throw new ScriptRuntimeException("Division by zero", script.getLineNumber(ip-1));
						stack.push(ScriptValue.of(na%nb));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '%'", script.getLineNumber(ip-1));
					}
				}
				case Op.NEGATE_BOOLEAN -> {
					stack.push(castValueToBool(stack.pop()) ? ScriptValue.FALSE : ScriptValue.TRUE);
				}
				case Op.NEGATE_NUMBER -> {
					if(stack.pop() instanceof ScriptValue.Num(double n)){
						stack.push(ScriptValue.of(-n));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for unary '-'", script.getLineNumber(ip-1));
					}
				}
				case Op.JUMP -> ip+=script.operands[ip-1];
				case Op.JUMP_IF_FALSE -> {
					if(!castValueToBool(stack.pop()))
						ip+=script.operands[ip-1];
				}
				case Op.JUMP_IF_TRUE -> {
					if(castValueToBool(stack.pop()))
						ip+=script.operands[ip-1];
				}
				case Op.JUMP_BACKWARDS -> ip-=script.operands[ip-1];
				case Op.RETURN -> {
					if(stack.size()>1)
						throw new IllegalStateException("Stack size must be 1 upon return");
					return stack.pop();
				}
				case Op.NEW_OBJECT -> stack.push(ScriptValue.of(new LinkedHashMap<>()));
				case Op.GET_OBJECT_FIELD -> {
					ScriptValue keyV=stack.pop();
					ScriptValue objV=stack.pop();
					if(!(keyV instanceof ScriptValue.Str(String key)))
						throw new IllegalStateException("Invalid operand types");
					if(objV instanceof ScriptValue.Arr(List<ScriptValue> arr) && "length".equals(key)){
						stack.push(ScriptValue.of(arr.size()));
					}else if(objV instanceof ScriptValue.Str(String s) && "length".equals(key)){
						stack.push(ScriptValue.of(s.length()));
					}else{
						if(!(objV instanceof ScriptValue.Obj(Map<String, ScriptValue> obj)))
							throw new ScriptRuntimeException("Invalid operand types for object property access", script.getLineNumber(ip-1));
						stack.push(obj.get(key));
					}
				}
				case Op.SET_OBJECT_FIELD -> {
					ScriptValue value=stack.pop();
					ScriptValue keyV=stack.pop();
					ScriptValue objV=stack.pop();
					if(!(keyV instanceof ScriptValue.Str(String key)) || !(objV instanceof ScriptValue.Obj(Map<String, ScriptValue> obj)))
						throw new IllegalStateException("Invalid operand types");
					obj.put(key, value);
					if(script.operands[ip-1]==1)
						stack.push(value);
				}
				case Op.DELETE_OBJECT_FIELD -> {
					ScriptValue keyV=stack.pop();
					ScriptValue objV=stack.pop();
					if(!(keyV instanceof ScriptValue.Str(String key)) || !(objV instanceof ScriptValue.Obj(Map<String, ScriptValue> obj)))
						throw new IllegalStateException("Invalid operand types");
					obj.remove(key);
				}
				case Op.NEW_ARRAY -> stack.push(ScriptValue.of(new ArrayList<>()));
				case Op.GET_ARRAY_ELEMENT -> {
					ScriptValue indexV=stack.pop();
					ScriptValue arrayV=stack.pop();
					if(arrayV instanceof ScriptValue.Obj(Map<String, ScriptValue> obj)){
						// object['value']
						String key=switch(indexV){
							case ScriptValue.Num(double n) -> numberToString(n);
							case ScriptValue.Str(String s) -> s;
							default -> throw new ScriptRuntimeException("Invalid operand type for '[]'", script.getLineNumber(ip-1));
						};
						stack.push(obj.get(key));
					}else if(arrayV instanceof ScriptValue.Arr(List<ScriptValue> arr)){
						int index=switch(indexV){
							case ScriptValue.Num(double n) -> (int)n;
							case ScriptValue.Str(String s) -> {
								try{
									yield Integer.parseInt(s);
								}catch(NumberFormatException x){
									throw new ScriptRuntimeException("Invalid operand type for '[]'", script.getLineNumber(ip-1));
								}
							}
							default -> throw new ScriptRuntimeException("Invalid operand type for '[]'", script.getLineNumber(ip-1));
						};
						if(index>=arr.size() || index<0)
							throw new ScriptRuntimeException("Index "+index+" is outside of the bounds of the array", script.getLineNumber(ip-1));
						stack.push(arr.get(index));
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '[]'", script.getLineNumber(ip-1));
					}
				}
				case Op.SET_ARRAY_ELEMENT -> {
					ScriptValue value=stack.pop();
					ScriptValue indexV=stack.pop();
					ScriptValue arrayV=stack.pop();
					if(arrayV instanceof ScriptValue.Obj(Map<String, ScriptValue> obj)){
						// object['value']
						String key=switch(indexV){
							case ScriptValue.Num(double n) -> numberToString(n);
							case ScriptValue.Str(String s) -> s;
							default -> throw new ScriptRuntimeException("Invalid operand type for '[]='", script.getLineNumber(ip-1));
						};
						obj.put(key, value);
					}else if(arrayV instanceof ScriptValue.Arr(List<ScriptValue> arr)){
						int index=switch(indexV){
							case ScriptValue.Num(double n) -> (int)n;
							case ScriptValue.Str(String s) -> {
								try{
									yield Integer.parseInt(s);
								}catch(NumberFormatException x){
									throw new ScriptRuntimeException("Invalid operand type for '[]='", script.getLineNumber(ip-1));
								}
							}
							default -> throw new ScriptRuntimeException("Invalid operand type for '[]='", script.getLineNumber(ip-1));
						};
						if(index>=arr.size() || index<0)
							throw new ScriptRuntimeException("Index "+index+" is outside of the bounds of the array", script.getLineNumber(ip-1));
						arr.set(index, value);
					}else{
						throw new ScriptRuntimeException("Invalid operand type for '[]='", script.getLineNumber(ip-1));
					}
					if(script.operands[ip-1]==1)
						stack.push(value);
				}
				case Op.ADD_ARRAY_ELEMENT -> {
					ScriptValue value=stack.pop();
					ScriptValue arrayV=stack.pop();
					if(!(arrayV instanceof ScriptValue.Arr(List<ScriptValue> array)))
						throw new IllegalStateException("Invalid operand types");
					array.add(value);
				}
				case Op.SELECT_OBJECT_FIELD -> {
					ScriptValue keyV=stack.pop();
					ScriptValue arrV=stack.pop();
					if(!(keyV instanceof ScriptValue.Str(String key)))
						throw new IllegalStateException("Invalid operand types");
					if(!(arrV instanceof ScriptValue.Arr(List<ScriptValue> arr)))
						throw new ScriptRuntimeException("Invalid operand type for '@.'", script.getLineNumber(ip-1));
					stack.push(ScriptValue.of(arr.stream()
							.map(el->el instanceof ScriptValue.Obj(Map<String, ScriptValue> obj) ? obj.get(key) : null)
							.toList()));
				}
				case Op.DUPLICATE -> stack.push(stack.peek());
				case Op.CALL_FUNCTION -> {
					String name=((ScriptValue.Str)script.constants[script.operands[ip-1]]).str();
					BuiltInFunctionRecord fn=env.builtInFunctions.get(name);
					if(fn==null)
						throw new ScriptRuntimeException("Unknown function '"+name+"'", script.getLineNumber(ip-1));
					int argCount=(int)((ScriptValue.Num)stack.pop()).num();
					if(argCount<fn.minArgs || argCount>fn.maxArgs){
						String expectedArgs;
						if(fn.minArgs==fn.maxArgs)
							expectedArgs=fn.minArgs+" argument"+(fn.minArgs==1 ? "" : "s");
						else
							expectedArgs=fn.minArgs+" to "+fn.maxArgs+" arguments";
						throw new ScriptRuntimeException("Invalid argument count for function '"+name+"', expected "+expectedArgs+", got "+argCount, script.getLineNumber(ip-1));
					}
					List<ScriptValue> args=argCount==0 ? List.of() : stack.subList(stack.size()-argCount, stack.size());
					ScriptValue result=fn.impl.invoke(args, script.getLineNumber(ip-1));
					if(!args.isEmpty())
						args.clear();
					stack.push(result);
				}
				case Op.CALL_METHOD -> {
					String name=((ScriptValue.Str)script.constants[script.operands[ip-1]]).str();
					int argCount=(int)((ScriptValue.Num)stack.pop()).num();
					List<ScriptValue> args=argCount==0 ? List.of() : stack.subList(stack.size()-argCount, stack.size());
					if(!args.isEmpty()){
						List<ScriptValue> _args=new ArrayList<>(args);
						args.clear();
						args=_args;
					}
					ScriptValue this_=stack.pop();
					if(this_==null)
						throw new ScriptRuntimeException("Attempted to call method '"+name+"' on a null value", script.getLineNumber(ip-1));
					BuiltInMethodRecord fn=env.builtInMethods.get(new MethodKey(this_.getClass(), name));
					if(fn==null)
						throw new ScriptRuntimeException("Unknown method '"+name+"'", script.getLineNumber(ip-1));
					if(argCount<fn.minArgs || argCount>fn.maxArgs){
						String expectedArgs;
						if(fn.minArgs==fn.maxArgs)
							expectedArgs=fn.minArgs+" argument"+(fn.minArgs==1 ? "" : "s");
						else
							expectedArgs=fn.minArgs+" to "+fn.maxArgs+" arguments";
						throw new ScriptRuntimeException("Invalid argument count for function '"+name+"', expected "+expectedArgs+", got "+argCount, script.getLineNumber(ip-1));
					}
					ScriptValue result=fn.impl.invoke(this_, args, script.getLineNumber(ip-1));
					stack.push(result);
				}
				case Op.CALL_API_METHOD -> {
					if(env.apiCallCallback==null)
						throw new ScriptRuntimeException("API methods are not available in this environment", script.getLineNumber(ip-1));
					String name=((ScriptValue.Str)script.constants[script.operands[ip-1]]).str();
					ScriptValue.Obj methodArgs=(ScriptValue.Obj) stack.pop();
					stack.push(env.apiCallCallback.doApiCall(name, methodArgs.obj()));
				}
				case Op.GET_ARGUMENT -> {
					String name=((ScriptValue.Str)script.constants[script.operands[ip-1]]).str();
					stack.push(ScriptValue.of(env.getArgumentCallback.apply(name)));
				}
				case Op.LOAD_NUMBER_IMM -> stack.push(ScriptValue.of(script.operands[ip-1]));
				default -> throw new ScriptRuntimeException("Unknown opcode "+opcode, script.getLineNumber(ip-1));
			}
		}
	}

	private static boolean castValueToBool(ScriptValue v){
		return switch(v){
			case ScriptValue.Bool(boolean b) -> b;
			case ScriptValue.Str(String s) -> StringUtils.isNotEmpty(s);
			case ScriptValue.Num(double n) -> n!=0;
			case ScriptValue.Obj obj -> true;
			case ScriptValue.Arr arr -> true;
			case null -> false;
		};
	}

	private static String numberToString(double n){
		long intVal=(long)n;
		return n==intVal ? String.valueOf(intVal) : String.valueOf(n);
	}

	private static boolean areValuesEqual(ScriptValue a, ScriptValue b, int lineNumber){
		if(a==null && b==null){
			return true;
		}else if(a instanceof ScriptValue.Bool(boolean ba) && b instanceof ScriptValue.Bool(boolean bb)){
			return ba==bb;
		}else if(a instanceof ScriptValue.Num(double na) && b instanceof ScriptValue.Num(double nb)){
			return na==nb;
		}else{
			String sa=switch(a){
				case ScriptValue.Str(String s) -> s;
				case ScriptValue.Num(double n) -> String.valueOf(n);
				case null -> null;
				default -> throw new ScriptRuntimeException("Comparing values of different or unsupported types", lineNumber);
			};
			String sb=switch(b){
				case ScriptValue.Str(String s) -> s;
				case ScriptValue.Num(double n) -> String.valueOf(n);
				case null -> null;
				default -> throw new ScriptRuntimeException("Comparing values of different or unsupported types", lineNumber);
			};
			return Objects.equals(sa, sb);
		}
	}

	private static double getNumberForComparison(ScriptValue v, int lineNumber){
		return switch(v){
			case ScriptValue.Num(double n) -> n;
			case ScriptValue.Str(String s) -> {
				try{
					yield Double.parseDouble(s);
				}catch(NumberFormatException x){
					throw new ScriptRuntimeException("Comparing values of different or unsupported types", lineNumber);
				}
			}
			case null -> 0;
			default -> throw new ScriptRuntimeException("Comparing values of different or unsupported types", lineNumber);
		};
	}

	public interface BuiltInFunction{
		ScriptValue invoke(List<ScriptValue> args, int lineNumber);
	}

	public interface BuiltInMethod{
		ScriptValue invoke(ScriptValue this_, List<ScriptValue> args, int lineNumber);
	}

	public interface ApiCallCallback{
		ScriptValue doApiCall(String method, Map<String, ScriptValue> params);
	}

	record BuiltInFunctionRecord(int minArgs, int maxArgs, BuiltInFunction impl){}
	record BuiltInMethodRecord(int minArgs, int maxArgs, BuiltInMethod impl){}
	record MethodKey(Class<? extends ScriptValue> thisClass, String name){}
}
