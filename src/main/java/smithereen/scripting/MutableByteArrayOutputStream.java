package smithereen.scripting;

import java.io.ByteArrayOutputStream;

class MutableByteArrayOutputStream extends ByteArrayOutputStream{
	public void set(int index, int value){
		buf[index]=(byte)value;
	}

	public int get(int index){
		return (int)buf[index] & 0xff;
	}
}
