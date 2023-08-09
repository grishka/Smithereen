package smithereen.exceptions;

public class UserContentUnavailableException extends UserActionNotAllowedException{
	public UserContentUnavailableException(){
		super();
	}

	public UserContentUnavailableException(String message){
		super(message);
	}

	public UserContentUnavailableException(String message, Throwable cause){
		super(message, cause);
	}

	public UserContentUnavailableException(Throwable cause){
		super(cause);
	}
}
