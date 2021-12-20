package smithereen.exceptions;

public class UserErrorException extends RuntimeException{
	public UserErrorException(){
	}

	public UserErrorException(String message){
		super(message);
	}

	public UserErrorException(String message, Throwable cause){
		super(message, cause);
	}

	public UserErrorException(Throwable cause){
		super(cause);
	}
}
