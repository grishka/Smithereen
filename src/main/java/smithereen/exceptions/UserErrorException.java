package smithereen.exceptions;

import java.util.Map;

public class UserErrorException extends RuntimeException{
	public Map<String, Object> langArgs;

	public UserErrorException(){
	}

	public UserErrorException(String message){
		super(message);
	}

	public UserErrorException(String message, Map<String, Object> langArgs){
		super(message);
		this.langArgs=langArgs;
	}

	public UserErrorException(String message, Throwable cause){
		super(message, cause);
	}

	public UserErrorException(Throwable cause){
		super(cause);
	}
}
