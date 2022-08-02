package smithereen.exceptions;

public class FormValidationException extends UserErrorException{
	public FormValidationException(){
		super();
	}

	public FormValidationException(String message){
		super(message);
	}

	public FormValidationException(String message, Throwable cause){
		super(message, cause);
	}

	public FormValidationException(Throwable cause){
		super(cause);
	}
}
