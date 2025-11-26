package smithereen.scripting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TokenizerTests{
	private static List<Token> tokenizeString(String input){
		ArrayList<Token> tokens=new ArrayList<>();
		Tokenizer tokenizer=new Tokenizer(input);
		while(true){
			Token t=tokenizer.getNext();
			tokens.add(t);
			if(t.type()==TokenType.EOF)
				break;
		}
		return tokens;
	}

	@Test
	public void testEmptyString(){
		assertIterableEquals(List.of(new Token(TokenType.EOF, null, 1)), tokenizeString(""));
	}

	@Test
	public void testSingleCharacterTokens(){
		List<Token> tokens=tokenizeString("( ) { } [ ] - + * / % , ; : . ?");
		assertIterableEquals(
				List.of(
						new Token(TokenType.LEFT_PAREN, null, 1),
						new Token(TokenType.RIGHT_PAREN, null, 1),
						new Token(TokenType.LEFT_BRACE, null, 1),
						new Token(TokenType.RIGHT_BRACE, null, 1),
						new Token(TokenType.LEFT_BRACKET, null, 1),
						new Token(TokenType.RIGHT_BRACKET, null, 1),
						new Token(TokenType.MINUS, null, 1),
						new Token(TokenType.PLUS, null, 1),
						new Token(TokenType.STAR, null, 1),
						new Token(TokenType.SLASH, null, 1),
						new Token(TokenType.PERCENT, null, 1),
						new Token(TokenType.COMMA, null, 1),
						new Token(TokenType.SEMICOLON, null, 1),
						new Token(TokenType.COLON, null, 1),
						new Token(TokenType.DOT, null, 1),
						new Token(TokenType.QUESTION, null, 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testDoubleCharacterTokens(){
		List<Token> tokens=tokenizeString("! != = == > >= < <= @. || &&");
		assertIterableEquals(
				List.of(
						new Token(TokenType.EXCLAMATION, null, 1),
						new Token(TokenType.NOT_EQUAL, null, 1),
						new Token(TokenType.EQUAL, null, 1),
						new Token(TokenType.DOUBLE_EQUAL, null, 1),
						new Token(TokenType.GREATER, null, 1),
						new Token(TokenType.GREATER_EQUAL, null, 1),
						new Token(TokenType.LESS, null, 1),
						new Token(TokenType.LESS_EQUAL, null, 1),
						new Token(TokenType.AT_DOT, null, 1),
						new Token(TokenType.OR, null, 1),
						new Token(TokenType.AND, null, 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testKeywords(){
		List<Token> tokens=tokenizeString("var if else for while return break continue delete true false null");
		assertIterableEquals(
				List.of(
						new Token(TokenType.VAR, null, 1),
						new Token(TokenType.IF, null, 1),
						new Token(TokenType.ELSE, null, 1),
						new Token(TokenType.FOR, null, 1),
						new Token(TokenType.WHILE, null, 1),
						new Token(TokenType.RETURN, null, 1),
						new Token(TokenType.BREAK, null, 1),
						new Token(TokenType.CONTINUE, null, 1),
						new Token(TokenType.DELETE, null, 1),
						new Token(TokenType.TRUE, null, 1),
						new Token(TokenType.FALSE, null, 1),
						new Token(TokenType.NULL, null, 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralSimple(){
		List<Token> tokens=tokenizeString("\"this is a string\"");
		assertIterableEquals(
				List.of(
						new Token(TokenType.STRING, "this is a string", 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralSimple2(){
		List<Token> tokens=tokenizeString("'this is a string'");
		assertIterableEquals(
				List.of(
						new Token(TokenType.STRING, "this is a string", 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralWithLineBreak(){
		List<Token> tokens=tokenizeString("\"this is\na string\"");
		assertIterableEquals(
				List.of(
						new Token(TokenType.STRING, "this is\na string", 2),
						new Token(TokenType.EOF, null, 2)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralEscapeSequencesWithDoubleQuotes(){
		List<Token> tokens=tokenizeString("\"\\n \\r \\t \\\\ \\\" \"");
		assertIterableEquals(
				List.of(
						new Token(TokenType.STRING, "\n \r \t \\ \" ", 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralEscapeSequencesWithSingleQuotes(){
		List<Token> tokens=tokenizeString("'\\n \\r \\t \\\\ \\' '");
		assertIterableEquals(
				List.of(
						new Token(TokenType.STRING, "\n \r \t \\ ' ", 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testStringLiteralInvalidEscapeSequence(){
		assertThrows(ScriptCompilationException.class, ()->tokenizeString("\"te\\zst\""), "Invalid escape sequence");
	}

	@Test
	public void testStringLiteralUnterminated(){
		assertThrows(ScriptCompilationException.class, ()->tokenizeString("\"test"), "Unterminated string");
	}

	@Test
	public void testLineComment(){
		List<Token> tokens=tokenizeString("identifierBeforeComment // Comment\nidentifierAfterComment");
		assertIterableEquals(
				List.of(
						new Token(TokenType.IDENTIFIER, "identifierBeforeComment", 1),
						new Token(TokenType.IDENTIFIER, "identifierAfterComment", 2),
						new Token(TokenType.EOF, null, 2)
				),
				tokens
		);
	}

	@Test
	public void testBlockCommentSingleLine(){
		List<Token> tokens=tokenizeString("identifierBeforeComment /* Comment */\nidentifierAfterComment");
		assertIterableEquals(
				List.of(
						new Token(TokenType.IDENTIFIER, "identifierBeforeComment", 1),
						new Token(TokenType.IDENTIFIER, "identifierAfterComment", 2),
						new Token(TokenType.EOF, null, 2)
				),
				tokens
		);
	}

	@Test
	public void testBlockCommentMultipleLines(){
		List<Token> tokens=tokenizeString("identifierBeforeComment /* Comment line 1\nline 2\nline 3 */\nidentifierAfterComment");
		assertIterableEquals(
				List.of(
						new Token(TokenType.IDENTIFIER, "identifierBeforeComment", 1),
						new Token(TokenType.IDENTIFIER, "identifierAfterComment", 4),
						new Token(TokenType.EOF, null, 4)
				),
				tokens
		);
	}

	@Test
	public void testBlockCommentUnterminated(){
		assertThrows(ScriptCompilationException.class, ()->tokenizeString("test /* huh?"), "Unterminated block comment");
	}

	@Test
	public void testNumberLiterals(){
		List<Token> tokens=tokenizeString("123 123.45 127.0.0.1");
		assertIterableEquals(
				List.of(
						new Token(TokenType.NUMBER, "123", 1),
						new Token(TokenType.NUMBER, "123.45", 1),
						new Token(TokenType.NUMBER, "127.0", 1),
						new Token(TokenType.DOT, null, 1),
						new Token(TokenType.NUMBER, "0.1", 1),
						new Token(TokenType.EOF, null, 1)
				),
				tokens
		);
	}

	@Test
	public void testUnexpectedChars(){
		assertThrows(ScriptCompilationException.class, ()->tokenizeString("кириллица"));
	}

	@Test
	public void testCompleteScript(){
		String code="""
				var durov=API.users.get({user_ids: 1, fields: "photo_400"})[0];
				var wall=API.wall.get({'owner_id': 1}).items;
				return [durov, wall]""";
		List<Token> tokens=tokenizeString(code);
		assertIterableEquals(
				List.of(
						new Token(TokenType.VAR, null, 1),
						new Token(TokenType.IDENTIFIER, "durov", 1),
						new Token(TokenType.EQUAL, null, 1),
						new Token(TokenType.IDENTIFIER, "API", 1),
						new Token(TokenType.DOT, null, 1),
						new Token(TokenType.IDENTIFIER, "users", 1),
						new Token(TokenType.DOT, null, 1),
						new Token(TokenType.IDENTIFIER, "get", 1),
						new Token(TokenType.LEFT_PAREN, null, 1),
						new Token(TokenType.LEFT_BRACE, null, 1),
						new Token(TokenType.IDENTIFIER, "user_ids", 1),
						new Token(TokenType.COLON, null, 1),
						new Token(TokenType.NUMBER, "1", 1),
						new Token(TokenType.COMMA, null, 1),
						new Token(TokenType.IDENTIFIER, "fields", 1),
						new Token(TokenType.COLON, null, 1),
						new Token(TokenType.STRING, "photo_400", 1),
						new Token(TokenType.RIGHT_BRACE, null, 1),
						new Token(TokenType.RIGHT_PAREN, null, 1),
						new Token(TokenType.LEFT_BRACKET, null, 1),
						new Token(TokenType.NUMBER, "0", 1),
						new Token(TokenType.RIGHT_BRACKET, null, 1),
						new Token(TokenType.SEMICOLON, null, 1),

						new Token(TokenType.VAR, null, 2),
						new Token(TokenType.IDENTIFIER, "wall", 2),
						new Token(TokenType.EQUAL, null, 2),
						new Token(TokenType.IDENTIFIER, "API", 2),
						new Token(TokenType.DOT, null, 2),
						new Token(TokenType.IDENTIFIER, "wall", 2),
						new Token(TokenType.DOT, null, 2),
						new Token(TokenType.IDENTIFIER, "get", 2),
						new Token(TokenType.LEFT_PAREN, null, 2),
						new Token(TokenType.LEFT_BRACE, null, 2),
						new Token(TokenType.STRING, "owner_id", 2),
						new Token(TokenType.COLON, null, 2),
						new Token(TokenType.NUMBER, "1", 2),
						new Token(TokenType.RIGHT_BRACE, null, 2),
						new Token(TokenType.RIGHT_PAREN, null, 2),
						new Token(TokenType.DOT, null, 2),
						new Token(TokenType.IDENTIFIER, "items", 2),
						new Token(TokenType.SEMICOLON, null, 2),

						new Token(TokenType.RETURN, null, 3),
						new Token(TokenType.LEFT_BRACKET, null, 3),
						new Token(TokenType.IDENTIFIER, "durov", 3),
						new Token(TokenType.COMMA, null, 3),
						new Token(TokenType.IDENTIFIER, "wall", 3),
						new Token(TokenType.RIGHT_BRACKET, null, 3),
						new Token(TokenType.EOF, null, 3)
				),
				tokens
		);
	}

	@Test
	public void testIssue219_1(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("n/"));
	}

	@Test
	public void testIssue219_2(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile(".8."));
	}

	@Test
	public void testIssue219_3(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("{/"));
	}

	@Test
	public void testIssue219_4(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("/"));
	}

	@Test
	public void testIssue219_5(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("8[.8."));
	}

	@Test
	public void testIssue219_6(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("{-w/"));
	}

	@Test
	public void testIssue219_7(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("y(P(hh(y1(S'\\"));
	}
}
