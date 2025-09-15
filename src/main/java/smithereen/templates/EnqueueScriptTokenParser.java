package smithereen.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.extension.NodeVisitor;
import io.pebbletemplates.pebble.lexer.Token;
import io.pebbletemplates.pebble.lexer.TokenStream;
import io.pebbletemplates.pebble.node.AbstractRenderableNode;
import io.pebbletemplates.pebble.node.BodyNode;
import io.pebbletemplates.pebble.node.RenderableNode;
import io.pebbletemplates.pebble.parser.Parser;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;
import io.pebbletemplates.pebble.template.Scope;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

public class EnqueueScriptTokenParser implements TokenParser{
	@Override
	public String getTag(){
		return "script";
	}

	@Override
	public RenderableNode parse(Token token, Parser parser){
		TokenStream stream = parser.getStream();
		int lineNumber = token.getLineNumber();
		Token name = stream.next();

		String scriptName=null;

		if(name.test(Token.Type.NAME) || name.test(Token.Type.STRING)){
			scriptName=name.getValue();
			stream.next();
		}

		stream.expect(Token.Type.EXECUTE_END);
		BodyNode blockBody = parser.subparse(tkn -> tkn.test(Token.Type.NAME, "endscript"));
		Token endblock = stream.current();
		if (!endblock.test(Token.Type.NAME, "endscript")) {
			throw new ParserException(null,
					"endscript tag should be present with script tag starting line number ",
					token.getLineNumber(), stream.getFilename());
		}

		// skip the 'endblock' token
		stream.next();
		stream.expect(Token.Type.EXECUTE_END);
		return new Node(lineNumber, blockBody, scriptName);
	}

	private static class Node extends AbstractRenderableNode{
		private BodyNode content;
		private String scriptName;

		public Node(int lineNumber, BodyNode content, String scriptName){
			super(lineNumber);
			this.content=content;
			this.scriptName=scriptName;
		}

		@Override
		public void render(PebbleTemplateImpl self, Writer writer, EvaluationContextImpl context) throws IOException{
			if(scriptName!=null){
				ArrayList<String> names=(ArrayList<String>) context.getVariable("_bottomScriptNames");
				if(names==null){
					names=new ArrayList<>();
					context.getScopeChain().set("_bottomScriptNames", names);
				}
				if(names.contains(scriptName))
					return;
				names.add(scriptName);
			}
			StringWriter sw=new StringWriter();
			content.render(self, sw, context);
			List<Scope> scopes=context.getScopeChain().getGlobalScopes();
			Scope lastScope=scopes.getLast();
			String bottomScripts=(String) Objects.requireNonNullElse(lastScope.get("_bottomScripts"), "");
			lastScope.put("_bottomScripts", bottomScripts+sw.toString().trim()+"\n");
		}

		@Override
		public void accept(NodeVisitor visitor){
			visitor.visit(this);
			visitor.visit(content);
		}
	}
}
