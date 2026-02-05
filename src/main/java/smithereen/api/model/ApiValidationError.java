package smithereen.api.model;

import java.util.Map;

public class ApiValidationError extends ApiError{
	public final String validationUrl;

	public ApiValidationError(ApiErrorType type, String message, Map<String, Object> params, String validationUrl){
		super(type, message, params);
		this.validationUrl=validationUrl;
	}
}
