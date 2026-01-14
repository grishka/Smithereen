package smithereen.api.model;

import java.util.List;
import java.util.Map;

import spark.utils.StringUtils;

public class ApiError{
	public int errorCode;
	public String errorMsg;
	public transient ApiErrorType errorType;
	public List<RequestParameter> requestParams;
	public String method;

	public ApiError(ApiErrorType type, String message, Map<String, Object> params){
		errorType=type;
		errorCode=type.code;
		errorMsg=type.message;
		if(StringUtils.isNotEmpty(message))
			errorMsg+=": "+message;
		requestParams=params.entrySet().stream().map(e->new RequestParameter(e.getKey(), e.getValue().toString())).toList();
	}

	public record RequestParameter(String key, String value){}
}
