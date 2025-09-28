package smithereen.api.model;

public class ApiErrorResponse implements ApiSerializable{
	public final ApiError error;

	public ApiErrorResponse(ApiError error){
		this.error=error;
	}
}
