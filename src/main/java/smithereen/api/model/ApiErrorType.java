package smithereen.api.model;

// VKontakte-compatible error codes, see https://dev.vk.com/ru/reference/errors
public enum ApiErrorType{
	UNKNOWN_ERROR(1, "Other error"),
	APP_DISABLED(2, "Application is disabled"),
	UNKNOWN_METHOD(3, "Unknown method passed"),
	//INVALID_SIGNATURE(4),
	USER_AUTH_FAILED(5, "User authorization failed", 401),
	TOO_MANY_REQUESTS(6, "Too many requests per second", 429),
	NO_PERMISSION(7, "Permission to perform this action is denied", 403),
	BAD_REQUEST(8, "Invalid request"),
	TOO_MANY_SIMILAR_ACTIONS(9, "Flood control", 429),
	INTERNAL_SERVER_ERROR(10, "Internal server error", 500),
	CAPTCHA_NEEDED(14, "Captcha needed"),
	ACCESS_DENIED(15, "Access denied", 403),
	VALIDATION_NEEDED(17, "Validation required"),
	ACCOUNT_SUSPENDED(18, "Account banned", 403),
	STANDALONE_APPS_ONLY(20, "Permission to perform this action is denied for non-standalone applications"),
	CONFIRMATION_NEEDED(24, "Confirmation required"),
	PARAM_INVALID(100, "One of the parameters specified was missing or invalid");

	public final int code;
	public final String message;
	public final int httpStatusCode;

	ApiErrorType(int code, String message){
		this.code=code;
		this.message=message;
		this.httpStatusCode=400;
	}

	ApiErrorType(int code, String message, int httpStatusCode){
		this.code=code;
		this.message=message;
		this.httpStatusCode=httpStatusCode;
	}
}
