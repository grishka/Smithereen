package smithereen.exceptions;

public class UnsupportedRemoteObjectTypeException extends ObjectNotFoundException{
	public UnsupportedRemoteObjectTypeException(){
	}

	public UnsupportedRemoteObjectTypeException(String message){
		super(message);
	}

	public UnsupportedRemoteObjectTypeException(String message, Throwable cause){
		super(message, cause);
	}

	public UnsupportedRemoteObjectTypeException(Throwable cause){
		super(cause);
	}

	public UnsupportedRemoteObjectTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
