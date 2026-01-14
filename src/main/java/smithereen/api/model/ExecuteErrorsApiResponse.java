package smithereen.api.model;

import java.util.List;

public class ExecuteErrorsApiResponse extends ApiResponse{
	public List<ApiError> executeErrors;

	public ExecuteErrorsApiResponse(Object response){
		super(response);
	}
}
