package smithereen.scripting;

import java.io.CharArrayWriter;

class MutableCharArrayWriter extends CharArrayWriter{
	public void set(int index, char value){
		buf[index]=value;
	}
}
