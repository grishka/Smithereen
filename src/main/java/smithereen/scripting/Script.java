package smithereen.scripting;

public class Script{
	final byte[] ops;
	final char[] operands;
	final char[] lineNumbers;
	final ScriptValue[] constants;
	final int variableCount;

	Script(String source){
		Compiler c=new Compiler(source);
		c.compile();
		ops=c.opsBuffer.toByteArray();
		operands=c.operandsBuffer.toCharArray();
		lineNumbers=c.lineNumberBuffer.toCharArray();
		constants=c.constants.toArray(new ScriptValue[0]);
		variableCount=c.variableCount;
	}

	public static Script compile(String source){
		return new Script(source);
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("Constants:\n");
		for(int i=0;i<constants.length;i++){
			sb.append(i);
			sb.append('\t');
			sb.append(constants[i]);
			sb.append('\n');
		}
		sb.append("\nVariable count: ");
		sb.append(variableCount);
		sb.append("\nCode:\n");
		int lineNumber=0;
		for(int i=0;i<ops.length;i++){
			sb.append(i);
			sb.append("\t");
			int opcode=(int)ops[i] & 0xff;
			sb.append(Op.getName(opcode));
			if(Op.hasOperand(opcode)){
				sb.append(" ");
				sb.append((int)operands[i]);
				if(opcode==Op.JUMP || opcode==Op.JUMP_IF_FALSE || opcode==Op.JUMP_IF_TRUE){
					sb.append("\t; -> ");
					sb.append(i+operands[i]+1);
				}
			}
			int curLine=lineNumbers[i];
			if(curLine!=lineNumber){
				lineNumber=curLine;
				sb.append("\t; Line ");
				sb.append(lineNumber);
			}
			sb.append('\n');
		}

		return sb.toString();
	}

	public int getLineNumber(int ip){
		return lineNumbers[ip];
	}
}
