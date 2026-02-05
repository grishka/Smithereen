package smithereen.api.model;

import java.util.Map;

public class ApiCaptchaError extends ApiError{
	public Captcha captcha;

	public ApiCaptchaError(ApiErrorType type, String message, Map<String, Object> params){
		super(type, message, params);
	}

	public record Captcha(String url, int width, int height, String sid, String hint){}
}
