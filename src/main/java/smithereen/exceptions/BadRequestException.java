package smithereen.exceptions;

/**
 * Throw this from a route handler to produce a 400 Bad Request response.
 */
public class BadRequestException extends RuntimeException{
	public BadRequestException(){
	}

	public BadRequestException(String message){
		super(message);
	}

	public BadRequestException(String message, Throwable cause){
		super(message, cause);
	}

	public BadRequestException(Throwable cause){
		super(cause);
	}

	public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
