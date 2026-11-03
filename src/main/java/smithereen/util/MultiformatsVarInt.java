package smithereen.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * <a href="https://github.com/multiformats/unsigned-varint">https://github.com/multiformats/unsigned-varint</a>
 */
public class MultiformatsVarInt{
	public static long read(InputStream in) throws IOException{
		long r=0;
		for(int i=0;i<9;i++){
			int b=in.read();
			long value=b & 0x7f;
			r|=value << (i*7);
			if((b & 0x80)==0)
				break;
		}
		return r;
	}
}
