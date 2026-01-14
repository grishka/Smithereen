package smithereen.scripting;

public class ScriptCompilationException extends ScriptingException{

	public ScriptCompilationException(String message, int lineNumber){
		super(message, lineNumber);
	}
}
