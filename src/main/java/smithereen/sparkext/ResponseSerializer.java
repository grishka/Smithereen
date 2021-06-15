package smithereen.sparkext;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface ResponseSerializer<T>{
	void serialize(OutputStream out, T obj) throws IOException;
}
