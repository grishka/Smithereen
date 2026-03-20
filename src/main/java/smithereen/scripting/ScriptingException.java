package smithereen.scripting;

public abstract class ScriptingException extends RuntimeException{
	public final int lineNumber;

	public ScriptingException(String message, int lineNumber){
		super(message);
		this.lineNumber=lineNumber;
	}
}
