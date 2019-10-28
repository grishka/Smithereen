package smithereen.jsonld;

import org.json.JSONException;

public class JLDException extends JSONException{
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
