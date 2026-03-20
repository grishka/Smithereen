package smithereen.scripting;

public class ScriptRuntimeException extends ScriptingException{
	public ScriptRuntimeException(String message, int lineNumber){
		super(message, lineNumber);
	}
}
