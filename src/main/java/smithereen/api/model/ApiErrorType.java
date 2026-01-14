package smithereen.api.model;

// VKontakte-compatible error codes, see https://dev.vk.com/ru/reference/errors
public enum ApiErrorType{
	OTHER_ERROR(1, "Other error"),
	//APP_DISABLED(2, "Application is disabled"),
	UNKNOWN_METHOD(3, "Unknown method passed"),
	//INVALID_SIGNATURE(4),
	USER_AUTH_FAILED(5, "User authorization failed", 401),
	TOO_MANY_REQUESTS(6, "Too many requests per second", 429),
	NO_PERMISSION(7, "Permission to perform this action is denied", 403),
	BAD_REQUEST(8, "Invalid request"),
	TOO_MANY_SIMILAR_ACTIONS(9, "Flood control", 429),
	INTERNAL_SERVER_ERROR(10, "Internal server error", 500),
	EXECUTE_COMPILE_FAILED(12, "Code compilation failed", 422),
	EXECUTE_RUNTIME_ERROR(13, "Runtime error occurred", 422),
	CAPTCHA_NEEDED(14, "Captcha needed"),
	ACCESS_DENIED(15, "Access denied", 403),
	VALIDATION_NEEDED(17, "Validation required"),
	ACCOUNT_SUSPENDED(18, "Account banned", 403),
	//STANDALONE_APPS_ONLY(20, "Permission to perform this action is denied for non-standalone applications"),
	//CONFIRMATION_NEEDED(24, "Confirmation required"),
	PARAM_INVALID(100, "One of the parameters specified was missing or invalid"),
	ACTION_NOT_APPLICABLE(101, "This action is not applicable to this object", 422),
	NOT_FOUND(104, "Not found", 404),

	TOO_MANY_FRIEND_LISTS(173, "Too many friend lists"),
	CANT_FRIEND_SELF(174, "Can't add oneself as a friend"),
	CANT_ADD_FRIEND_BLOCKED(175, "Can't add this user as a friend because they blocked you", 403),
	CANT_ADD_FRIEND_YOU_BLOCKED(176, "Can't add this user as a friend because you blocked them", 403),

	CANT_INVITE_TO_GROUP(180, "Can't invite this user to this group", 403),
	CANT_PROMOTE_GROUP_MEMBER(181, "Can't add or update this group manager"),
	CANT_DEMOTE_GROUP_MEMBER(182, "Can't remove this group manager"),

	TOO_MANY_FRIENDS(242, "Too many friends"),

	TOO_MANY_PHOTO_ALBUMS(302, "Too many photo albums"),

	REMOTE_FETCH_NETWORK_ERROR(401, "Failed to fetch remote object due to a network error", 422),
	REMOTE_FETCH_UNSUPPORTED_TYPE(402, "Remote object is of an unsupported type", 422),
	REMOTE_FETCH_TIMEOUT(403, "Request to the remote server timed out", 422),
	REMOTE_FETCH_NOT_FOUND(404, "Remote object not found", 404),
	REMOTE_FETCH_OTHER_ERROR(405, "Failed to fetch remote object due to other error", 422),
	;

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
