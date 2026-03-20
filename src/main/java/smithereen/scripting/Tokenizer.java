package smithereen.scripting;

class Tokenizer{
	private final String input;
	private int offset=0;
	private int line=1;
	private int tokenStart=0;

	Tokenizer(String input){
		this.input=input;
	}

	public Token getNext(){
		skipWhitespaceAndComments();
		tokenStart=offset;

		if(isAtEnd())
			return makeToken(TokenType.EOF);

		char c=advance();
		if(isAlpha(c))
			return parseIdentifier();
		if(isDigit(c))
			return parseNumber();

		switch(c){
			case '(' -> {
				return makeToken(TokenType.LEFT_PAREN);
			}
			case ')' -> {
				return makeToken(TokenType.RIGHT_PAREN);
			}
			case '{' -> {
				return makeToken(TokenType.LEFT_BRACE);
			}
			case '}' -> {
				return makeToken(TokenType.RIGHT_BRACE);
			}
			case '[' -> {
				return makeToken(TokenType.LEFT_BRACKET);
			}
			case ']' -> {
				return makeToken(TokenType.RIGHT_BRACKET);
			}
			case '-' -> {
				return makeToken(TokenType.MINUS);
			}
			case '+' -> {
				return makeToken(TokenType.PLUS);
			}
			case '/' -> {
				return makeToken(TokenType.SLASH);
			}
			case '*' -> {
				return makeToken(TokenType.STAR);
			}
			case '%' -> {
				return makeToken(TokenType.PERCENT);
			}
			case ',' -> {
				return makeToken(TokenType.COMMA);
			}
			case ';' -> {
				return makeToken(TokenType.SEMICOLON);
			}
			case ':' -> {
				return makeToken(TokenType.COLON);
			}
			case '!' -> {
				return makeToken(match('=') ? TokenType.NOT_EQUAL : TokenType.EXCLAMATION);
			}
			case '=' -> {
				return makeToken(match('=') ? TokenType.DOUBLE_EQUAL : TokenType.EQUAL);
			}
			case '>' -> {
				return makeToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
			}
			case '<' -> {
				return makeToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
			}
			case '@' -> {
				if(match('.'))
					return makeToken(TokenType.AT_DOT);
			}
			case '.' -> {
				return makeToken(TokenType.DOT);
			}
			case '|' -> {
				if(match('|'))
					return makeToken(TokenType.OR);
			}
			case '&' -> {
				if(match('&'))
					return makeToken(TokenType.AND);
			}
			case '?' -> {
				return makeToken(TokenType.QUESTION);
			}
			case '"' -> {
				return parseString(false);
			}
			case '\'' -> {
				return parseString(true);
			}
		}

		throw new ScriptCompilationException("Unexpected character "+(!Character.isISOControl(c) ? ("'"+c+"'") : ("U+"+(int)c)), line);
	}

	private Token parseIdentifier(){
		while(isAlphanumeric(peek()))
			advance();
		TokenType type=switch(input.substring(tokenStart, offset)){
			case "var" -> TokenType.VAR;
			case "if" -> TokenType.IF;
			case "else" -> TokenType.ELSE;
			case "for" -> TokenType.FOR;
			case "while" -> TokenType.WHILE;
			case "return" -> TokenType.RETURN;
			case "break" -> TokenType.BREAK;
			case "continue" -> TokenType.CONTINUE;
			case "delete" -> TokenType.DELETE;
			case "true" -> TokenType.TRUE;
			case "false" -> TokenType.FALSE;
			case "null" -> TokenType.NULL;
			default -> TokenType.IDENTIFIER;
		};
		return type==TokenType.IDENTIFIER ? makeTokenWithValue(type) : makeToken(type);
	}

	private Token parseNumber(){
		while(isDigit(peek()))
			advance();

		// Optional fractional part
		if(peek()=='.' && isDigit(peekNext())){
			do{
				// First iteration consumes the '.'
				advance();
			}while(isDigit(peek()));
		}

		return makeTokenWithValue(TokenType.NUMBER);
	}

	private Token parseString(boolean singleQuote){
		char quote=singleQuote ? '\'' : '"';
		StringBuilder sb=new StringBuilder();
		while(peek()!=quote && !isAtEnd()){
			if(peek()=='\n')
				line++;
			char c=advance();
			if(c=='\\' && !isAtEnd()){
				sb.append(switch(advance()){
					case 'n' -> '\n';
					case 'r' -> '\r';
					case 't' -> '\t';
					case '\\' -> '\\';
					case '"' -> {
						if(singleQuote)
							throw new ScriptCompilationException("Invalid escape sequence", line);
						yield '"';
					}
					case '\'' -> {
						if(!singleQuote)
							throw new ScriptCompilationException("Invalid escape sequence", line);
						yield '\'';
					}
					default -> throw new ScriptCompilationException("Invalid escape sequence", line);
				});
			}else{
				sb.append(c);
			}
		}
		if(isAtEnd())
			throw new ScriptCompilationException("Unterminated string", line);

		advance(); // Closing quote
		return new Token(TokenType.STRING, sb.toString(), line);
	}

	private char advance(){
		return input.charAt(offset++);
	}

	private char peek(){
		return isAtEnd() ? '\0' : input.charAt(offset);
	}

	private boolean isAtEnd(){
		return offset==input.length();
	}

	private char peekNext(){
		return offset>input.length()-2 ? '\0' : input.charAt(offset+1);
	}

	private boolean match(char expected){
		if(isAtEnd() || input.charAt(offset)!=expected)
			return false;
		offset++;
		return true;
	}

	private void skipWhitespaceAndComments(){
		while(true){
			char c=peek();
			switch(c){
				case ' ', '\r', '\t' -> advance();
				case '\n' -> {
					line++;
					advance();
				}
				case '/' -> {
					char next=peekNext();
					if(next=='/'){ // '//' line comment
						while(peek()!='\n' && !isAtEnd())
							advance();
					}else if(next=='*'){ // '/* ... */' block comment
						while(peek()!='*' || peekNext()!='/'){
							if(isAtEnd()){
								throw new ScriptCompilationException("Unterminated block comment", line);
							}else if(advance()=='\n'){
								line++;
							}
						}
						// Consume '*/'
						advance();
						advance();
					}else{
						return;
					}
				}
				default -> {
					return;
				}
			}
		}
	}

	private Token makeToken(TokenType type){
		return new Token(type, null, line);
	}

	private Token makeTokenWithValue(TokenType type){
		return new Token(type, input.substring(tokenStart, offset), line);
	}

	private static boolean isAlpha(char c){
		return (c>='a' && c<='z') || (c>='A' && c<='Z') || c=='_';
	}

	private static boolean isDigit(char c){
		return c>='0' && c<='9';
	}

	private static boolean isAlphanumeric(char c){
		return isAlpha(c) || isDigit(c);
	}
}
