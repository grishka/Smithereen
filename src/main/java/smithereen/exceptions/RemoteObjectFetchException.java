package smithereen.exceptions;

import java.net.URI;

public class RemoteObjectFetchException extends FederationException{
	public final ErrorType error;
	public final URI uri;

	public RemoteObjectFetchException(ErrorType error, URI uri){
		this.error=error;
		this.uri=uri;
	}

	public RemoteObjectFetchException(String message, ErrorType error, URI uri){
		super(message);
		this.error=error;
		this.uri=uri;
	}

	public RemoteObjectFetchException(String message, Throwable cause, ErrorType error, URI uri){
		super(message, cause);
		this.error=error;
		this.uri=uri;
	}

	public RemoteObjectFetchException(Throwable cause, ErrorType error, URI uri){
		super(cause);
		this.error=error;
		this.uri=uri;
	}

	public enum ErrorType{
		UNSUPPORTED_OBJECT_TYPE,
		TIMEOUT,
		NETWORK_ERROR,
		NOT_FOUND,
		OTHER_ERROR
	}
}
