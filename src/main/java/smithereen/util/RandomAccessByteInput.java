package smithereen.util;

import java.io.IOException;

public interface RandomAccessByteInput{
	long getSize();

	byte[] read(int length, int position) throws IOException, InterruptedException;
}
