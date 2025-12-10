package smithereen.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.api.model.ApiError;
import smithereen.api.model.ApiErrorType;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.SizedImage;
import smithereen.model.UserPermissions;
import smithereen.model.apps.AppAccessToken;
import smithereen.model.apps.ClientAppPermission;
import smithereen.routes.ApiRoutes;
import spark.Request;
import spark.utils.StringUtils;

public class ApiCallContext{
	public AppAccessToken token;
	public Account self;
	public Map<String, String> params;
	public Request httpRequest;
	public SizedImage.Format imageFormat;
	public Lang lang;
	public int versionMajor, versionMinor;
	public UserPermissions permissions;

	public ApiCallContext(AppAccessToken token, Account self, Map<String, String> params, Request httpRequest, int versionMajor, int versionMinor, UserPermissions permissions){
		this.token=token;
		this.self=self;
		this.params=params;
		this.httpRequest=httpRequest;
		this.versionMajor=versionMajor;
		this.versionMinor=versionMinor;
		this.permissions=permissions;
	}

	public ApiCallContext(ApiCallContext toWrap, Map<String, String> params){
		token=toWrap.token;
		self=toWrap.self;
		httpRequest=toWrap.httpRequest;
		versionMajor=toWrap.versionMajor;
		versionMinor=toWrap.versionMinor;
		permissions=toWrap.permissions;
		this.params=params;
	}

	public ApiErrorException error(ApiErrorType type){
		return new ApiErrorException(new ApiError(type, null, params));
	}

	public ApiErrorException error(ApiErrorType type, String msg){
		return new ApiErrorException(new ApiError(type, msg, params));
	}

	public ApiErrorException paramError(String msg){
		return error(ApiErrorType.PARAM_INVALID, msg);
	}

	public String optParamString(String key){
		return params.get(key);
	}

	public String optParamString(String key, String def){
		String v=params.get(key);
		return StringUtils.isEmpty(v) ? def : v;
	}

	public String requireParamString(String key){
		String value=params.get(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		return value;
	}

	private List<String> commaSeparatedStringList(String key, boolean require){
		String value=params.get(key);
		if(StringUtils.isEmpty(value)){
			if(require)
				throw paramError(key+" is undefined");
			return List.of();
		}
		return Arrays.stream(value.split(",")).map(String::trim).toList();
	}

	public List<String> optCommaSeparatedStringList(String key){
		return commaSeparatedStringList(key, false);
	}

	public List<String> requireCommaSeparatedStringList(String key){
		return commaSeparatedStringList(key, true);
	}

	private Set<String> commaSeparatedStringSet(String key, boolean require){
		String value=params.get(key);
		if(StringUtils.isEmpty(value)){
			if(require)
				throw paramError(key+" is undefined");
			return Set.of();
		}
		return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	public Set<String> optCommaSeparatedStringSet(String key){
		return commaSeparatedStringSet(key, false);
	}

	public Set<String> requireCommaSeparatedStringSet(String key){
		return commaSeparatedStringSet(key, true);
	}

	public int requireParamIntPositive(String key){
		String value=params.get(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		int v=Utils.safeParseInt(value);
		if(v<=0)
			throw paramError(key+" must be a positive integer");
		return v;
	}

	public int requireParamIntNonZero(String key){
		String value=params.get(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		int v=Utils.safeParseInt(value);
		if(v==0)
			throw paramError(key+" is not an integer");
		return v;
	}

	public int optParamIntPositive(String key){
		return Math.max(0, Utils.safeParseInt(params.get(key)));
	}

	public int optParamIntPositive(String key, int def){
		if(!hasParam(key))
			return def;
		return Math.max(0, Utils.safeParseInt(params.get(key)));
	}

	public List<Integer> optCommaSeparatedIntListParam(String key){
		if(hasParam(key))
			return commaSeparatedIntListParam(key);
		return List.of();
	}

	public List<Integer> commaSeparatedIntListParam(String key){
		List<Integer> res=Arrays.stream(requireParamString(key).split(","))
				.map(String::trim)
				.map(Utils::safeParseInt)
				.filter(i->i!=0)
				.distinct()
				.toList();
		if(res.isEmpty())
			throw paramError(key+" does not contain a valid integer list");
		return res;
	}

	public boolean booleanParam(String key){
		String v=params.get(key);
		return "true".equalsIgnoreCase(v) || "1".equals(v);
	}

	public JsonArray optParamJsonArray(String key){
		String v=optParamString(key);
		if(StringUtils.isEmpty(v))
			return null;
		try{
			JsonElement el=JsonParser.parseString(v);
			if(!(el instanceof JsonArray ar))
				throw paramError(key+" is not a JSON array");
			return ar;
		}catch(JsonParseException x){
			throw paramError(key+" contains invalid JSON");
		}
	}

	public <T> T optParamJsonObject(String key, Class<T> objType){
		String v=optParamString(key);
		if(StringUtils.isEmpty(v))
			return null;
		try{
			return ApiRoutes.gson.fromJson(v, objType);
		}catch(JsonParseException x){
			throw paramError(key+" contains invalid JSON");
		}
	}

	public float requireParamFloat(String key){
		String value=params.get(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		try{
			return Float.parseFloat(value);
		}catch(NumberFormatException x){
			throw paramError(key+" is not a number");
		}
	}

	public float requireParamFloatInRange(String key, float min, float max){
		float v=requireParamFloat(key);
		if(v>=min && v<=max)
			return v;
		throw paramError(key+" is out of range ["+min+", "+max+"]");
	}

	public int getOffset(){
		return Math.max(0, Utils.safeParseInt(params.get("offset")));
	}

	public int getCount(int def, int max){
		int count=Utils.safeParseInt(params.get("count"));
		return count<=0 ? def : Math.min(max, count);
	}

	public boolean hasParam(String key){
		return params.containsKey(key);
	}

	public boolean hasPermission(ClientAppPermission permission){
		return token!=null && token.permissions().contains(permission);
	}

	public void requirePermission(ClientAppPermission permission){
		if(!hasPermission(permission))
			throw error(ApiErrorType.NO_PERMISSION, "this requires the "+permission.getScopeValue()+" permission");
	}
}
