package smithereen.exceptions;

public class FederationException extends RuntimeException{
	public FederationException(){
		super();
	}

	public FederationException(String message){
		super(message);
	}

	public FederationException(String message, Throwable cause){
		super(message, cause);
	}

	public FederationException(Throwable cause){
		super(cause);
	}
}
