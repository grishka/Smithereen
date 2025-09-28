package smithereen.api;

import smithereen.api.model.ApiError;

public class ApiErrorException extends RuntimeException{
	public final ApiError error;
	public ApiErrorException(ApiError error){
		this.error=error;
	}
}
