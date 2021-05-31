package smithereen.jsonld;

public class JLDException extends RuntimeException{
	public JLDException(String message){
		super(message);
	}

	public JLDException(String message, Throwable cause){
		super(message, cause);
	}

	public JLDException(Throwable cause){
		super(cause);
	}
}
