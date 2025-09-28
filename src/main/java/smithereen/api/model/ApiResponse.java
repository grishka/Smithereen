package smithereen.api.model;

public class ApiResponse implements ApiSerializable{
	public Object response;

	public ApiResponse(Object response){
		this.response=response;
	}
}
