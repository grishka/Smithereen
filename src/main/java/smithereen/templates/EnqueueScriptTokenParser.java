package smithereen.templates;

import com.mitchellbosecke.pebble.error.ParserException;
import com.mitchellbosecke.pebble.extension.NodeVisitor;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.lexer.Token;
import com.mitchellbosecke.pebble.lexer.TokenStream;
import com.mitchellbosecke.pebble.node.AbstractRenderableNode;
import com.mitchellbosecke.pebble.node.BodyNode;
import com.mitchellbosecke.pebble.node.RenderableNode;
import com.mitchellbosecke.pebble.parser.Parser;
import com.mitchellbosecke.pebble.template.EvaluationContextImpl;
import com.mitchellbosecke.pebble.template.PebbleTemplateImpl;
import com.mitchellbosecke.pebble.template.Scope;
import com.mitchellbosecke.pebble.tokenParser.TokenParser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
			Scope lastScope=scopes.get(scopes.size()-1);
			String bottomScripts=(String) Objects.requireNonNullElse(lastScope.get("_bottomScripts"), "");
			lastScope.put("_bottomScripts", bottomScripts+sw.toString().trim()+"\n");
		}

		@Override
		public void accept(NodeVisitor visitor){
			visitor.visit(this);
		}
	}
}
