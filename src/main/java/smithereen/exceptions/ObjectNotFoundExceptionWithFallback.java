package smithereen.exceptions;

public class ObjectNotFoundExceptionWithFallback extends ObjectNotFoundException{
	public final Object fallback;

	public ObjectNotFoundExceptionWithFallback(String message, Object fallback){
		super(message);
		this.fallback=fallback;
	}

	public ObjectNotFoundExceptionWithFallback(String message, Throwable cause, Object fallback){
		super(message, cause);
		this.fallback=fallback;
	}
}
