package smithereen.scripting;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

class Compiler{
	private static final int PRECEDENCE_NONE=0;
	private static final int PRECEDENCE_ASSIGNMENT=1;
	private static final int PRECEDENCE_OR=2;
	private static final int PRECEDENCE_AND=3;
	private static final int PRECEDENCE_EQUALITY=4;
	private static final int PRECEDENCE_COMPARISON=5;
	private static final int PRECEDENCE_TERM=6;
	private static final int PRECEDENCE_FACTOR=7;
	private static final int PRECEDENCE_UNARY=8;
	private static final int PRECEDENCE_CALL=9;
	private static final int PRECEDENCE_PRIMARY=10;

	private final Tokenizer tokenizer;
	private Token current, previous;
	MutableByteArrayOutputStream opsBuffer=new MutableByteArrayOutputStream();
	MutableCharArrayWriter operandsBuffer=new MutableCharArrayWriter();
	CharArrayWriter lineNumberBuffer=new CharArrayWriter();
	ArrayList<ScriptValue> constants=new ArrayList<>();
	int variableCount=0;
	private HashMap<ScriptValue, Integer> constantIDs=new HashMap<>();
	private boolean insideDeleteStatement;

	private interface ParseFunction{
		void parse(boolean canAssign);
	}
	private record ParseRule(ParseFunction prefix, ParseFunction infix, int precedence){}
	private final EnumMap<TokenType, ParseRule> rules=new EnumMap<>(TokenType.class);
	private static final ParseRule DEFAULT_RULE=new ParseRule(null, null, PRECEDENCE_NONE);

	private record Variable(String name, int depth){}
	private final ArrayList<Variable> variables=new ArrayList<>();
	private int currentScopeDepth=0;
	private record LoopInfo(List<Integer> breakJumps, List<Integer> continueJumps){}
	private final ArrayList<LoopInfo> loops=new ArrayList<>();

	{
		rules.put(TokenType.LEFT_PAREN, new ParseRule(this::parseGrouping, null, PRECEDENCE_NONE));
		rules.put(TokenType.MINUS, new ParseRule(this::parseUnaryOperator, this::parseBinaryOperator, PRECEDENCE_TERM));
		rules.put(TokenType.PLUS, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_TERM));
		rules.put(TokenType.SLASH, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_FACTOR));
		rules.put(TokenType.STAR, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_FACTOR));
		rules.put(TokenType.PERCENT, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_FACTOR));
		rules.put(TokenType.EXCLAMATION, new ParseRule(this::parseUnaryOperator, null, PRECEDENCE_NONE));
		rules.put(TokenType.NOT_EQUAL, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_EQUALITY));
		rules.put(TokenType.DOUBLE_EQUAL, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_EQUALITY));
		rules.put(TokenType.GREATER, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_COMPARISON));
		rules.put(TokenType.GREATER_EQUAL, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_COMPARISON));
		rules.put(TokenType.LESS, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_COMPARISON));
		rules.put(TokenType.LESS_EQUAL, new ParseRule(null, this::parseBinaryOperator, PRECEDENCE_COMPARISON));
		rules.put(TokenType.IDENTIFIER, new ParseRule(this::parseVariable, null, PRECEDENCE_NONE));
		rules.put(TokenType.STRING, new ParseRule(this::parseString, null, PRECEDENCE_NONE));
		rules.put(TokenType.NUMBER, new ParseRule(this::parseNumber, null, PRECEDENCE_NONE));
		rules.put(TokenType.AND, new ParseRule(null, this::parseAnd, PRECEDENCE_AND));
		rules.put(TokenType.OR, new ParseRule(null, this::parseOr, PRECEDENCE_OR));
		rules.put(TokenType.FALSE, new ParseRule(this::parseLiteral, null, PRECEDENCE_NONE));
		rules.put(TokenType.TRUE, new ParseRule(this::parseLiteral, null, PRECEDENCE_NONE));
		rules.put(TokenType.NULL, new ParseRule(this::parseLiteral, null, PRECEDENCE_NONE));
		rules.put(TokenType.QUESTION, new ParseRule(null, this::parseTernaryOperator, PRECEDENCE_ASSIGNMENT));
		rules.put(TokenType.LEFT_BRACE, new ParseRule(this::parseObject, null, PRECEDENCE_NONE));
		rules.put(TokenType.LEFT_BRACKET, new ParseRule(this::parseArray, this::parseArraySubscript, PRECEDENCE_CALL));
		rules.put(TokenType.DOT, new ParseRule(null, this::parseDot, PRECEDENCE_CALL));
		rules.put(TokenType.AT_DOT, new ParseRule(null, this::parseAtDot, PRECEDENCE_CALL));
	}

	public Compiler(String source){
		tokenizer=new Tokenizer(source);
	}

	private int defineConstant(ScriptValue v){
		return constantIDs.computeIfAbsent(v, _v->{
			int id=constants.size();
			if(id>0xffff)
				throw new ScriptCompilationException("Too many constant values", previous.lineNumber());
			constants.add(v);
			return id;
		});
	}

	private void emitInstruction(int opcode, int operand){
		opsBuffer.write(opcode);
		operandsBuffer.write(operand);
		lineNumberBuffer.write(previous.lineNumber());
	}

	private void emitInstruction(int opcode){
		emitInstruction(opcode, 0);
	}

	private void advance(){
		previous=current;
		current=tokenizer.getNext();
	}

	private void consume(TokenType type, String errorMessage){
		if(current.type()==type){
			advance();
			return;
		}
		throw new ScriptCompilationException(errorMessage, current.lineNumber());
	}

	private boolean check(TokenType type){
		return current.type()==type;
	}

	private boolean match(TokenType type){
		if(check(type)){
			advance();
			return true;
		}
		return false;
	}

	public void compile(){
		advance();
		while(!match(TokenType.EOF)){
			parseDeclaration();
		}
		variableCount=Math.max(variableCount, variables.size());
		if(opsBuffer.size()==0 || opsBuffer.get(opsBuffer.size()-1)!=Op.RETURN){
			// The script doesn't end with a return statement. Add `return null`.
			emitInstruction(Op.LOAD_NULL);
			emitInstruction(Op.RETURN);
		}
	}

	private void parseDeclaration(){
		if(match(TokenType.VAR)){
			parseVarDeclaration();
		}else{
			parseStatement();
		}
	}

	private void parseVarDeclaration(){
		consume(TokenType.IDENTIFIER, "Expected variable name");
		String name=previous.value();
		for(int i=variables.size()-1;i>=0;i--){
			Variable v=variables.get(i);
			if(v.depth<currentScopeDepth)
				break;
			if(name.equals(v.name))
				throw new ScriptCompilationException("Variable '"+name+"' is already defined in this scope", previous.lineNumber());
		}
		int varIndex=variables.size();
		variables.add(new Variable(name, currentScopeDepth));
		if(match(TokenType.EQUAL)){
			parseExpression();
			emitInstruction(Op.SET_VARIABLE, varIndex);
			emitInstruction(Op.POP);
		}
		consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
	}

	private void parseStatement(){
		if(match(TokenType.LEFT_BRACE)){
			beginScope();
			parseBlock();
			endScope();
		}else if(match(TokenType.IF)){
			parseIfStatement();
		}else if(match(TokenType.FOR)){
			parseForStatement();
		}else if(match(TokenType.WHILE)){
			parseWhileStatement();
		}else if(match(TokenType.RETURN)){
			parseReturnStatement();
		}else if(match(TokenType.BREAK)){
			parseBreakStatement();
		}else if(match(TokenType.CONTINUE)){
			parseContinueStatement();
		}else if(match(TokenType.DELETE)){
			parseDeleteStatement();
		}else{
			parseExpressionStatement();
		}
	}

	private void beginScope(){
		currentScopeDepth++;
	}

	private void endScope(){
		currentScopeDepth--;
		variableCount=Math.max(variableCount, variables.size());
		variables.removeIf(v->v.depth>currentScopeDepth);
	}

	private void patchJump(int location){
		int offset=opsBuffer.size()-location-1;
		if(offset>0xffff)
			throw new ScriptCompilationException("Too many opcodes to jump over", previous.lineNumber());
		operandsBuffer.set(location, (char)offset);
	}

	private void parseBlock(){
		while(!check(TokenType.RIGHT_BRACE) && !check(TokenType.EOF)){
			parseDeclaration();
		}
		consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
	}

	private void parseIfStatement(){
		consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'");
		parseExpression();
		consume(TokenType.RIGHT_PAREN, "Expected ')' after condition");
		int thenJump=opsBuffer.size();
		emitInstruction(Op.JUMP_IF_FALSE);
		parseStatement();
		int elseJump=opsBuffer.size();
		emitInstruction(Op.JUMP);
		patchJump(thenJump);

		if(match(TokenType.ELSE))
			parseStatement();
		patchJump(elseJump);
	}

	private void parseTernaryOperator(boolean canAssign){
		int falseJump=opsBuffer.size();
		emitInstruction(Op.JUMP_IF_FALSE);
		parseExpression();
		int endJump=opsBuffer.size();
		emitInstruction(Op.JUMP);
		consume(TokenType.COLON, "Expected ':' after the 'true' branch of the ternary operator");
		patchJump(falseJump);
		parseExpression();
		patchJump(endJump);
	}

	private void parseForStatement(){
		LoopInfo loopInfo=new LoopInfo(new ArrayList<>(), new ArrayList<>());
		loops.add(loopInfo);
		beginScope();
		consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'");
		if(match(TokenType.SEMICOLON)){
			// No initializer
		}else if(match(TokenType.VAR)){
			parseVarDeclaration();
		}else{
			parseExpressionStatement();
		}

		int loopStart=opsBuffer.size();
		int exitJump=-1;
		if(!match(TokenType.SEMICOLON)){
			parseExpression();
			consume(TokenType.SEMICOLON, "Expected ';' after loop condition");
			exitJump=opsBuffer.size();
			emitInstruction(Op.JUMP_IF_FALSE);
		}

		if(!match(TokenType.RIGHT_PAREN)){
			int bodyJump=opsBuffer.size();
			emitInstruction(Op.JUMP);
			int incrementStart=opsBuffer.size();
			parseExpression();
			emitInstruction(Op.POP);
			consume(TokenType.RIGHT_PAREN, "Expected ')' after loop clauses");

			int offset=opsBuffer.size()-loopStart+1;
			if(offset>0xffff)
				throw new ScriptCompilationException("Too many operations in loop", previous.lineNumber());
			emitInstruction(Op.JUMP_BACKWARDS, offset);
			loopStart=incrementStart;
			patchJump(bodyJump);
		}

		parseStatement();
		int offset=opsBuffer.size()-loopStart+1;
		if(offset>0xffff)
			throw new ScriptCompilationException("Too many operations in loop", previous.lineNumber());
		emitInstruction(Op.JUMP_BACKWARDS, offset);

		if(exitJump!=-1){
			patchJump(exitJump);
		}
		endScope();
		for(int breakLocation:loopInfo.breakJumps){
			patchJump(breakLocation);
		}
		for(int continueLocation:loopInfo.continueJumps){
			offset=continueLocation-loopStart+1;
			operandsBuffer.set(continueLocation, (char)offset);
		}
		loops.remove(loopInfo);
	}

	private void parseWhileStatement(){
		LoopInfo loopInfo=new LoopInfo(new ArrayList<>(), new ArrayList<>());
		loops.add(loopInfo);
		int loopStart=opsBuffer.size();
		consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'");
		parseExpression();
		consume(TokenType.RIGHT_PAREN, "Expected ')' after condition");

		int exitJump=opsBuffer.size();
		emitInstruction(Op.JUMP_IF_FALSE);
		parseStatement();
		int offset=opsBuffer.size()-loopStart+1;
		if(offset>0xffff)
			throw new ScriptCompilationException("Too many operations in loop", previous.lineNumber());
		emitInstruction(Op.JUMP_BACKWARDS, offset);
		patchJump(exitJump);
		for(int breakLocation:loopInfo.breakJumps){
			patchJump(breakLocation);
		}
		for(int continueLocation:loopInfo.continueJumps){
			offset=continueLocation-loopStart+1;
			operandsBuffer.set(continueLocation, (char)offset);
		}
		loops.remove(loopInfo);
	}

	private void parseReturnStatement(){
		parseExpression();
		consume(TokenType.SEMICOLON, "Expected ';' after return expression");
		emitInstruction(Op.RETURN);
	}

	private void parseBreakStatement(){
		consume(TokenType.SEMICOLON, "Expected ';' after 'break'");
		if(loops.isEmpty())
			throw new ScriptCompilationException("'break' outside of a loop", previous.lineNumber());
		int loc=opsBuffer.size();
		emitInstruction(Op.JUMP);
		loops.getLast().breakJumps.add(loc);
	}

	private void parseContinueStatement(){
		consume(TokenType.SEMICOLON, "Expected ';' after 'continue'");
		if(loops.isEmpty())
			throw new ScriptCompilationException("'continue' outside of a loop", previous.lineNumber());
		int loc=opsBuffer.size();
		emitInstruction(Op.JUMP_BACKWARDS);
		loops.getLast().continueJumps.add(loc);
	}

	private void parseDeleteStatement(){
		insideDeleteStatement=true;
		parseExpression();
		if(opsBuffer.get(opsBuffer.size()-1)!=Op.DELETE_OBJECT_FIELD)
			throw new ScriptCompilationException("'delete' on something other than an object field", previous.lineNumber());
		insideDeleteStatement=false;
		consume(TokenType.SEMICOLON, "Expected ';' after delete");
	}

	private void parseExpressionStatement(){
		parseExpression();
		consume(TokenType.SEMICOLON, "Expected ';' after expression");
		emitInstruction(Op.POP);
	}

	private void parseExpression(){
		parsePrecedence(PRECEDENCE_ASSIGNMENT);
	}

	private void parsePrecedence(int precedence){
		advance();
		ParseRule prefixRule=rules.get(previous.type());
		if(prefixRule==null || prefixRule.prefix==null)
			throw new ScriptCompilationException("Expected expression", previous.lineNumber());

		boolean canAssign=precedence<=PRECEDENCE_ASSIGNMENT;
		prefixRule.prefix.parse(canAssign);

		while(precedence<=rules.getOrDefault(current.type(), DEFAULT_RULE).precedence){
			advance();
			rules.get(previous.type()).infix.parse(canAssign);
		}

		if(canAssign && match(TokenType.EQUAL))
			throw new ScriptCompilationException("Invalid assignment target", previous.lineNumber());
	}

	private void parseUnaryOperator(boolean canAssign){
		TokenType operatorType=previous.type();
		parsePrecedence(PRECEDENCE_UNARY);
		switch(operatorType){
			case EXCLAMATION -> emitInstruction(Op.NEGATE_BOOLEAN);
			case MINUS -> emitInstruction(Op.NEGATE_NUMBER);
		}
	}

	private void parseBinaryOperator(boolean canAssign){
		TokenType operatorType=previous.type();
		ParseRule rule=rules.get(operatorType);
		parsePrecedence(rule.precedence+1);

		switch(operatorType){
			case NOT_EQUAL -> emitInstruction(Op.IS_NOT_EQUAL);
			case DOUBLE_EQUAL -> emitInstruction(Op.IS_EQUAL);
			case GREATER -> emitInstruction(Op.IS_GREATER);
			case GREATER_EQUAL -> emitInstruction(Op.IS_GREATER_OR_EQUAL);
			case LESS -> emitInstruction(Op.IS_LESS);
			case LESS_EQUAL -> emitInstruction(Op.IS_LESS_OR_EQUAL);
			case PLUS -> emitInstruction(Op.ADD);
			case MINUS -> emitInstruction(Op.SUBTRACT);
			case STAR -> emitInstruction(Op.MULTIPLY);
			case SLASH -> emitInstruction(Op.DIVIDE);
			case PERCENT -> emitInstruction(Op.REMAINDER);
		}
	}

	private void parseGrouping(boolean canAssign){
		parseExpression();
		consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
	}

	private void parseVariable(boolean canAssign){
		int varIndex=-1;
		String name=previous.value();

		if(match(TokenType.LEFT_PAREN)){
			parseMethodOrFunctionCall();
			int nameID=defineConstant(new ScriptValue.Str(name));
			emitInstruction(Op.CALL_FUNCTION, nameID);
			return;
		}

		if("Args".equals(name)){
			consume(TokenType.DOT, "Expected '.' after 'Args'");
			consume(TokenType.IDENTIFIER, "Expected argument name after 'Args.'");
			int nameID=defineConstant(new ScriptValue.Str(previous.value()));
			emitInstruction(Op.GET_ARGUMENT, nameID);
			return;
		}

		if("API".equals(name)){
			consume(TokenType.DOT, "Expected '.' after 'API'");
			consume(TokenType.IDENTIFIER, "Expected API method name after 'API.'");
			String methodName=previous.value();
			if(match(TokenType.DOT)){
				consume(TokenType.IDENTIFIER, "Expected API method name after section name");
				methodName+="."+previous.value();
			}
			consume(TokenType.LEFT_PAREN, "Expected '(' after API method name");
			if(match(TokenType.LEFT_BRACE)){
				parseObject(false);
			}else{
				emitInstruction(Op.NEW_OBJECT); // Treat 'API.section.method()' as 'API.section.method({})'
			}
			consume(TokenType.RIGHT_PAREN, "Expected ')' after API method arguments");
			int nameID=defineConstant(ScriptValue.of(methodName));
			emitInstruction(Op.CALL_API_METHOD, nameID);
			return;
		}

		for(int i=variables.size()-1;i>=0;i--){
			Variable v=variables.get(i);
			if(v.name.equals(name)){
				varIndex=i;
				break;
			}
		}
		if(varIndex==-1){
			throw new ScriptCompilationException("Identifier '"+name+"' is not defined", previous.lineNumber());
		}

		if(canAssign && match(TokenType.EQUAL)){
			parseExpression();
			emitInstruction(Op.SET_VARIABLE, varIndex);
		}else{
			emitInstruction(Op.GET_VARIABLE, varIndex);
		}
	}

	private void parseString(boolean canAssign){
		emitInstruction(Op.LOAD_CONST, defineConstant(new ScriptValue.Str(previous.value())));
	}

	private void parseNumber(boolean canAssign){
		emitInstruction(Op.LOAD_CONST, defineConstant(new ScriptValue.Num(Double.parseDouble(previous.value()))));
	}

	private void parseAnd(boolean canAssign){
		emitInstruction(Op.DUPLICATE);
		int endJump=opsBuffer.size();
		emitInstruction(Op.JUMP_IF_FALSE);
		emitInstruction(Op.POP);
		parsePrecedence(PRECEDENCE_AND);
		patchJump(endJump);
	}

	private void parseOr(boolean canAssign){
		emitInstruction(Op.DUPLICATE);
		int elseJump=opsBuffer.size();
		emitInstruction(Op.JUMP_IF_FALSE);
		int endJump=opsBuffer.size();
		emitInstruction(Op.JUMP);

		patchJump(elseJump);
		emitInstruction(Op.POP);
		parsePrecedence(PRECEDENCE_OR);
		patchJump(endJump);
	}

	private void parseLiteral(boolean canAssign){
		switch(previous.type()){
			case FALSE -> emitInstruction(Op.LOAD_FALSE);
			case TRUE -> emitInstruction(Op.LOAD_TRUE);
			case NULL -> emitInstruction(Op.LOAD_NULL);
		}
	}

	private void parseObject(boolean canAssign){
		emitInstruction(Op.NEW_OBJECT);
		if(!match(TokenType.RIGHT_BRACE)){
			do{
				if(!match(TokenType.IDENTIFIER) && !match(TokenType.STRING))
					throw new ScriptCompilationException("Expected object field name", current.lineNumber());
				emitInstruction(Op.DUPLICATE);
				String fieldName=previous.value();
				consume(TokenType.COLON, "Expected ':' after object field name");
				int nameID=defineConstant(new ScriptValue.Str(fieldName));
				emitInstruction(Op.LOAD_CONST, nameID);
				parseExpression();
				emitInstruction(Op.SET_OBJECT_FIELD);
			}while(match(TokenType.COMMA));
			consume(TokenType.RIGHT_BRACE, "Expected '}' after object fields");
		}
	}

	private void parseDot(boolean canAssign){
		consume(TokenType.IDENTIFIER, "Expected name after '.'");
		String fieldName=previous.value();
		int nameID=defineConstant(new ScriptValue.Str(fieldName));

		if(match(TokenType.LEFT_PAREN)){
			parseMethodOrFunctionCall();
			emitInstruction(Op.CALL_METHOD, nameID);
		}else if(canAssign && match(TokenType.EQUAL)){
			emitInstruction(Op.LOAD_CONST, nameID);
			parseExpression();
			emitInstruction(Op.SET_OBJECT_FIELD, 1);
		}else{
			emitInstruction(Op.LOAD_CONST, nameID);
			if(insideDeleteStatement && check(TokenType.SEMICOLON)){
				emitInstruction(Op.DELETE_OBJECT_FIELD);
			}else{
				emitInstruction(Op.GET_OBJECT_FIELD);
			}
		}
	}

	private void parseArray(boolean canAssign){
		emitInstruction(Op.NEW_ARRAY);
		if(!match(TokenType.RIGHT_BRACKET)){
			do{
				emitInstruction(Op.DUPLICATE);
				parseExpression();
				emitInstruction(Op.ADD_ARRAY_ELEMENT);
			}while(match(TokenType.COMMA));
			consume(TokenType.RIGHT_BRACKET, "Expected ']' after array elements");
		}
	}

	private void parseArraySubscript(boolean canAssign){
		parseExpression();
		consume(TokenType.RIGHT_BRACKET, "Expected ']' after array subscript");
		if(canAssign && match(TokenType.EQUAL)){
			parseExpression();
			emitInstruction(Op.SET_ARRAY_ELEMENT, 1);
		}else{
			emitInstruction(Op.GET_ARRAY_ELEMENT);
		}
	}

	private void parseAtDot(boolean canAssign){
		consume(TokenType.IDENTIFIER, "Expected name after '@.'");
		String fieldName=previous.value();
		int nameID=defineConstant(new ScriptValue.Str(fieldName));
		emitInstruction(Op.LOAD_CONST, nameID);
		emitInstruction(Op.SELECT_OBJECT_FIELD);
	}

	private void parseMethodOrFunctionCall(){
		int count=0;
		if(!match(TokenType.RIGHT_PAREN)){
			do{
				parseExpression();
				count++;
			}while(match(TokenType.COMMA));
			consume(TokenType.RIGHT_PAREN, "Expected ')' after argument list");
		}
		emitInstruction(Op.LOAD_NUMBER_IMM, count);
	}
}
