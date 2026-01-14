package smithereen.api.methods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.api.ApiCallContext;
import smithereen.api.ApiDispatcher;
import smithereen.api.ApiErrorException;
import smithereen.api.model.ApiError;
import smithereen.api.model.ApiErrorType;
import smithereen.routes.ApiRoutes;
import smithereen.scripting.Script;
import smithereen.scripting.ScriptCompilationException;
import smithereen.scripting.ScriptEnvironment;
import smithereen.scripting.ScriptRuntimeException;
import smithereen.scripting.ScriptVM;
import smithereen.scripting.ScriptValue;
import smithereen.util.CryptoUtils;

public class ExecuteMethods{
	private static final LruCache<ScriptCacheKey, Script> scriptCache=new LruCache<>(1000);
	private static final int MAX_METHOD_CALLS=25;

	public static Object execute(ApplicationContext ctx, ApiCallContext actx){
		String code=actx.requireParamString("code");
		byte[] hash=CryptoUtils.sha1(code.getBytes(StandardCharsets.UTF_8));
		ScriptCacheKey key=new ScriptCacheKey(actx.token.appID(), hash);
		Script s=scriptCache.get(key);
		if(s==null){
			try{
				s=Script.compile(code);
				scriptCache.put(key, s);
			}catch(ScriptCompilationException x){
				throw actx.error(ApiErrorType.EXECUTE_COMPILE_FAILED, x.getMessage()+" on line "+x.lineNumber);
			}
		}
		int[] methodCalls={0};
		ScriptEnvironment env=new ScriptEnvironment(actx::optParamString, (method, params)->{
			if("execute".equals(method)){
				throw actx.error(ApiErrorType.EXECUTE_RUNTIME_ERROR, "Can't call execute from within execute");
			}
			methodCalls[0]++;
			if(methodCalls[0]>MAX_METHOD_CALLS){
				throw actx.error(ApiErrorType.EXECUTE_RUNTIME_ERROR, "Too many API calls");
			}
			HashMap<String, Object> convertedParams=new HashMap<>();
			params.forEach((k, v)->{
				if(v!=null){
					convertedParams.put(k, convertScriptValueToParam(v));
				}
			});
			try{
				return convertJsonToScriptValue(ApiRoutes.gson.toJsonTree(ApiDispatcher.doApiCall(method, ctx, new ApiCallContext(actx, convertedParams))));
			}catch(ApiErrorException x){
				List<ApiError> errors=actx.httpRequest.attribute("executeErrors");
				if(errors==null){
					errors=new ArrayList<>();
					actx.httpRequest.attribute("executeErrors", errors);
				}
				x.error.method=method;
				errors.add(x.error);
				return ScriptValue.FALSE;
			}
		});
		try{
			ScriptValue result=ScriptVM.execute(s, env);
			actx.httpRequest.attribute("serializeNulls", true);
			return result;
		}catch(ScriptRuntimeException x){
			throw actx.error(ApiErrorType.EXECUTE_RUNTIME_ERROR, x.getMessage()+" on line "+x.lineNumber);
		}
	}

	private record ScriptCacheKey(long appID, byte[] hash){}

	private static Object convertScriptValueToParam(ScriptValue v){
		return switch(v){
			case ScriptValue.Str(String str) -> str;
			case ScriptValue.Num(double num) when num%1==0 -> (long)num;
			case ScriptValue.Num(double num) -> num;
			case ScriptValue.Bool(boolean bool) -> bool;
			case ScriptValue.Arr(List<ScriptValue> arr) -> ApiRoutes.gson.toJsonTree(arr);
			case ScriptValue.Obj(Map<String, ScriptValue> obj) -> ApiRoutes.gson.toJsonTree(obj);
		};
	}

	private static ScriptValue convertJsonToScriptValue(JsonElement el){
		return switch(el){
			case JsonPrimitive p -> {
				if(p.isBoolean()){
					yield p.getAsBoolean() ? ScriptValue.TRUE : ScriptValue.FALSE;
				}else if(p.isNumber()){
					yield ScriptValue.of(p.getAsDouble());
				}else{
					yield ScriptValue.of(p.getAsString());
				}
			}
			case JsonObject obj -> {
				LinkedHashMap<String, ScriptValue> sObj=new LinkedHashMap<>();
				for(String key:obj.keySet()){
					sObj.put(key, convertJsonToScriptValue(obj.get(key)));
				}
				yield ScriptValue.of(sObj);
			}
			case JsonArray arr -> {
				ArrayList<ScriptValue> sArr=new ArrayList<>();
				for(JsonElement ae:arr){
					sArr.add(convertJsonToScriptValue(ae));
				}
				yield ScriptValue.of(sArr);
			}
			default -> throw new IllegalStateException("Unexpected value: " + el);
		};
	}
}
