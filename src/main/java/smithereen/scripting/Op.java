package smithereen.scripting;

import java.lang.reflect.Field;

class Op{
	public static final int LOAD_CONST=0;
	public static final int LOAD_NULL=1;
	public static final int LOAD_TRUE=2;
	public static final int LOAD_FALSE=3;
	public static final int POP=4;
	public static final int GET_VARIABLE=5;
	public static final int SET_VARIABLE=6;
	public static final int IS_EQUAL=7;
	public static final int IS_NOT_EQUAL=8;
	public static final int IS_GREATER=9;
	public static final int IS_GREATER_OR_EQUAL=10;
	public static final int IS_LESS=11;
	public static final int IS_LESS_OR_EQUAL=12;
	public static final int ADD=13;
	public static final int SUBTRACT=14;
	public static final int MULTIPLY=15;
	public static final int DIVIDE=16;
	public static final int REMAINDER=17;
	public static final int NEGATE_BOOLEAN=18;
	public static final int NEGATE_NUMBER=19;
	public static final int JUMP=20;
	public static final int JUMP_IF_FALSE=21;
	public static final int JUMP_IF_TRUE=22;
	public static final int JUMP_BACKWARDS=23;
	public static final int RETURN=24;
	public static final int NEW_OBJECT=25;
	public static final int GET_OBJECT_FIELD=26;
	public static final int SET_OBJECT_FIELD=27;
	public static final int DELETE_OBJECT_FIELD=28;
	public static final int NEW_ARRAY=29;
	public static final int GET_ARRAY_ELEMENT=30;
	public static final int SET_ARRAY_ELEMENT=31;
	public static final int ADD_ARRAY_ELEMENT=32;
	public static final int SELECT_OBJECT_FIELD=33;
	public static final int DUPLICATE=34;
	public static final int CALL_FUNCTION=35;
	public static final int CALL_METHOD=36;
	public static final int CALL_API_METHOD=37;
	public static final int GET_ARGUMENT=38;
	public static final int LOAD_NUMBER_IMM=39;

	private static final String[] names;
	static{
		Field[] fields=Op.class.getFields();
		names=new String[fields.length];
		try{
			for(Field field:fields){
				names[field.getInt(null)]=field.getName();
			}
		}catch(Throwable x){
			throw new RuntimeException(x);
		}
	}

	public static String getName(int opcode){
		if(opcode<0 || opcode>=names.length)
			throw new IllegalArgumentException("Unknown opcode");
		return names[opcode];
	}

	public static boolean hasOperand(int opcode){
		return opcode==LOAD_CONST || opcode==GET_VARIABLE || opcode==SET_VARIABLE || opcode==JUMP
				|| opcode==JUMP_IF_FALSE || opcode==JUMP_IF_TRUE || opcode==JUMP_BACKWARDS
				|| opcode==SET_OBJECT_FIELD || opcode==SET_ARRAY_ELEMENT || opcode==CALL_FUNCTION
				|| opcode==CALL_METHOD || opcode==CALL_API_METHOD || opcode==GET_ARGUMENT
				|| opcode==LOAD_NUMBER_IMM;
	}
}
