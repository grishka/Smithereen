package smithereen.lang.formatting;

public class ICUMessageSyntaxException extends RuntimeException{
	public ICUMessageSyntaxException(){
		super();
	}

	public ICUMessageSyntaxException(String message){
		super(message);
	}

	public ICUMessageSyntaxException(String message, Throwable cause){
		super(message, cause);
	}

	public ICUMessageSyntaxException(Throwable cause){
		super(cause);
	}
}
