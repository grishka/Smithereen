package smithereen.exceptions;

public class FloodControlViolationException extends RuntimeException{
	public FloodControlViolationException(){
	}

	public FloodControlViolationException(String message){
		super(message);
	}

	public FloodControlViolationException(String message, Throwable cause){
		super(message, cause);
	}

	public FloodControlViolationException(Throwable cause){
		super(cause);
	}

	public FloodControlViolationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
