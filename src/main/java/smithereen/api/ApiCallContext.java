package smithereen.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	public Map<String, Object> params;
	public Request httpRequest;
	public SizedImage.Format imageFormat;
	public Lang lang;
	public int versionMajor, versionMinor;
	public UserPermissions permissions;

	public ApiCallContext(AppAccessToken token, Account self, Map<String, Object> params, Request httpRequest, int versionMajor, int versionMinor, UserPermissions permissions){
		this.token=token;
		this.self=self;
		this.params=params;
		this.httpRequest=httpRequest;
		this.versionMajor=versionMajor;
		this.versionMinor=versionMinor;
		this.permissions=permissions;
	}

	public ApiCallContext(ApiCallContext toWrap, Map<String, Object> params){
		token=toWrap.token;
		self=toWrap.self;
		httpRequest=toWrap.httpRequest;
		versionMajor=toWrap.versionMajor;
		versionMinor=toWrap.versionMinor;
		permissions=toWrap.permissions;
		this.params=params;
		imageFormat=toWrap.imageFormat;
		lang=toWrap.lang;
	}

	public @NotNull ApiErrorException error(@NotNull ApiErrorType type){
		return new ApiErrorException(new ApiError(type, null, params));
	}

	public @NotNull ApiErrorException error(@NotNull ApiErrorType type, @Nullable String msg){
		return new ApiErrorException(new ApiError(type, msg, params));
	}

	public @NotNull ApiErrorException paramError(@NotNull String msg){
		return error(ApiErrorType.PARAM_INVALID, msg);
	}

	public @Nullable String optParamString(@NotNull String key){
		return getString(key);
	}

	@Contract("_, !null -> !null")
	public @Nullable String optParamString(@NotNull String key, @Nullable String def){
		String v=getString(key);
		return StringUtils.isEmpty(v) ? def : v;
	}

	public @NotNull String requireParamString(@NotNull String key){
		String value=getString(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		return value;
	}

	private @NotNull List<String> commaSeparatedStringList(@NotNull String key, boolean require){
		List<String> value=getCommaSeparatedValues(key);
		if(require && value==null)
			throw paramError(key+" is undefined");
		return value==null ? List.of() : value;
	}

	public @NotNull List<String> optCommaSeparatedStringList(@NotNull String key){
		return commaSeparatedStringList(key, false);
	}

	public @NotNull List<String> requireCommaSeparatedStringList(@NotNull String key){
		return commaSeparatedStringList(key, true);
	}

	private @NotNull Set<String> commaSeparatedStringSet(@NotNull String key, boolean require){
		return new HashSet<>(commaSeparatedStringList(key, require));
	}

	public @NotNull Set<String> optCommaSeparatedStringSet(@NotNull String key){
		return commaSeparatedStringSet(key, false);
	}

	public @NotNull Set<String> requireCommaSeparatedStringSet(@NotNull String key){
		return commaSeparatedStringSet(key, true);
	}

	public int requireParamIntPositive(@NotNull String key){
		if(!params.containsKey(key))
			throw paramError(key+" is undefined");
		try{
			int v=getInt(key);
			if(v<=0)
				throw paramError(key+" is not positive");
			return v;
		}catch(NumberFormatException x){
			throw paramError(key+" is not an integer");
		}
	}

	public int requireParamIntNonZero(@NotNull String key){
		if(!params.containsKey(key))
			throw paramError(key+" is undefined");
		try{
			int v=getInt(key);
			if(v==0)
				throw paramError(key+" must not be 0");
			return v;
		}catch(NumberFormatException x){
			throw paramError(key+" is not an integer");
		}
	}

	public long requireParamLongNonZero(@NotNull String key){
		if(!params.containsKey(key))
			throw paramError(key+" is undefined");
		try{
			long v=getLong(key);
			if(v==0)
				throw paramError(key+" must not be 0");
			return v;
		}catch(NumberFormatException x){
			throw paramError(key+" is not an integer");
		}
	}

	public int optParamInt(@NotNull String key){
		try{
			return getInt(key);
		}catch(NumberFormatException x){
			return 0;
		}
	}

	public long optParamLong(@NotNull String key){
		try{
			return getLong(key);
		}catch(NumberFormatException x){
			return 0;
		}
	}

	public int optParamIntPositive(@NotNull String key){
		try{
			return Math.max(0, getInt(key));
		}catch(NumberFormatException x){
			return 0;
		}
	}

	public int optParamIntPositive(@NotNull String key, int def){
		try{
			return Math.max(0, getInt(key));
		}catch(NumberFormatException x){
			return def;
		}
	}

	public @NotNull List<Integer> optParamCommaSeparatedIntList(@NotNull String key){
		if(hasParam(key))
			return requireParamCommaSeparatedIntList(key);
		return List.of();
	}

	public @NotNull List<Integer> requireParamCommaSeparatedIntList(@NotNull String key){
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

	public boolean optParamBoolean(@NotNull String key){
		return getBoolean(key);
	}

	public @Nullable JsonArray optParamJsonArray(@NotNull String key){
		return switch(params.get(key)){
			case JsonArray ja -> ja;
			case String s -> {
				if(StringUtils.isEmpty(s))
					yield null;
				try{
					JsonElement el=JsonParser.parseString(s);
					if(!(el instanceof JsonArray ar))
						throw paramError(key+" is not a JSON array");
					yield ar;
				}catch(JsonParseException x){
					throw paramError(key+" contains invalid JSON");
				}
			}
			case null, default -> null;
		};
	}

	@NotNull
	public JsonArray requireParamJsonArray(@NotNull String key){
		JsonArray r=optParamJsonArray(key);
		if(r==null)
			throw paramError(key+" is undefined");
		return r;
	}

	public <T> @Nullable T optParamJsonObject(@NotNull String key, @NotNull Class<T> objType){
		String v=optParamString(key);
		if(StringUtils.isEmpty(v))
			return null;
		try{
			return ApiRoutes.gson.fromJson(v, objType);
		}catch(JsonParseException x){
			throw paramError(key+" contains invalid JSON");
		}
	}

	public float requireParamFloat(@NotNull String key){
		if(!params.containsKey(key))
			throw paramError(key+" is undefined");
		try{
			return getFloat(key);
		}catch(NumberFormatException x){
			throw paramError(key+" is not a number");
		}
	}

	public float requireParamFloatInRange(@NotNull String key, float min, float max){
		float v=requireParamFloat(key);
		if(v>=min && v<=max)
			return v;
		throw paramError(key+" is out of range ["+min+", "+max+"]");
	}

	public int getOffset(){
		return optParamIntPositive("offset");
	}

	public int getCount(int def, int max){
		int count=optParamIntPositive("count", def);
		return count<=0 ? def : Math.min(max, count);
	}

	@Contract(pure=true)
	public boolean hasParam(@NotNull String key){
		return params.containsKey(key);
	}

	public boolean hasPermission(@NotNull ClientAppPermission permission){
		return token!=null && token.permissions().contains(permission);
	}

	public void requirePermission(@NotNull ClientAppPermission permission){
		if(!hasPermission(permission))
			throw error(ApiErrorType.NO_PERMISSION, "this requires the "+permission.getScopeValue()+" permission");
	}

	public <E extends Enum<E>> @NotNull E optParamEnum(@NotNull String key, @NotNull Map<String, E> mapping, @NotNull E defValue){
		String value=getString(key);
		if(StringUtils.isEmpty(value))
			return defValue;
		E res=mapping.get(value);
		return res==null ? defValue : res;
	}

	@Nullable
	public <E extends Enum<E>> E optParamEnum(@NotNull String key, @NotNull Map<String, E> mapping){
		String value=getString(key);
		if(StringUtils.isEmpty(value))
			return null;
		return mapping.get(value);
	}

	public <E extends Enum<E>> @NotNull E requireParamEnum(@NotNull String key, @NotNull Map<String, E> mapping){
		String value=getString(key);
		if(StringUtils.isEmpty(value))
			throw paramError(key+" is undefined");
		E res=mapping.get(value);
		if(res==null)
			throw paramError(key+" must be one of "+String.join(", ", mapping.keySet()));
		return res;
	}

	// Type conversion helpers

	@Nullable
	private String getString(String key){
		Object value=params.get(key);
		return switch(value){
			case String s -> s;
			case JsonPrimitive jp -> jp.getAsString();
			case null -> null;
			default -> value.toString();
		};
	}

	private float getFloat(String key){
		Object value=params.get(key);
		return switch(value){
			case Number n -> n.floatValue();
			case String s -> Float.parseFloat(s);
			case JsonPrimitive jp -> jp.getAsFloat();
			case null, default -> throw new NumberFormatException();
		};
	}

	private int getInt(String key){
		Object value=params.get(key);
		return switch(value){
			case Number n -> n.intValue();
			case String s -> Integer.parseInt(s);
			case JsonPrimitive jp -> jp.getAsInt();
			case null, default -> throw new NumberFormatException();
		};
	}

	private long getLong(String key){
		Object value=params.get(key);
		return switch(value){
			case Number n -> n.longValue();
			case String s -> Long.parseLong(s);
			case JsonPrimitive jp -> jp.getAsLong();
			case null, default -> throw new NumberFormatException();
		};
	}

	private boolean getBoolean(String key){
		Object value=params.get(key);
		return switch(value){
			case Boolean b -> b;
			case String s -> "true".equals(s) || "1".equals(s);
			case JsonPrimitive jp when jp.isBoolean() -> jp.getAsBoolean();
			case Number n -> n.intValue()!=0;
			case null, default -> false;
		};
	}

	private List<String> getCommaSeparatedValues(String key){
		Object value=params.get(key);
		return switch(value){
			case String s -> Arrays.stream(s.split(",")).map(String::trim).toList();
			case JsonArray ja -> {
				try{
					yield ja.asList().stream().map(JsonElement::getAsString).toList();
				}catch(IllegalStateException|UnsupportedOperationException x){
					yield null;
				}
			}
			case JsonPrimitive jp -> List.of(jp.toString());
			case Number n -> List.of(n.toString());
			case Boolean b -> List.of(b.toString());
			case List<?> list -> list.stream().filter(Objects::nonNull).map(Object::toString).toList();
			case null, default -> null;
		};
	}
}
