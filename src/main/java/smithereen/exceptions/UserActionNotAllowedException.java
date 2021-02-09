package smithereen.exceptions;

public class UserActionNotAllowedException extends RuntimeException{
	public UserActionNotAllowedException(){
		super();
	}

	public UserActionNotAllowedException(String message){
		super(message);
	}

	public UserActionNotAllowedException(String message, Throwable cause){
		super(message, cause);
	}

	public UserActionNotAllowedException(Throwable cause){
		super(cause);
	}
}
