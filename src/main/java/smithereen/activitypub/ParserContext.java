package smithereen.activitypub;

public class ParserContext{
	public static final ParserContext LOCAL=new ParserContext(true);
	public static final ParserContext FOREIGN=new ParserContext(false);

	public final boolean isLocal;

	public ParserContext(boolean isLocal){
		this.isLocal=isLocal;
	}
}
