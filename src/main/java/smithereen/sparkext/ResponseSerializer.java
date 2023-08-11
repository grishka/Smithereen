package smithereen.sparkext;

import java.io.IOException;
import java.io.OutputStream;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface ResponseSerializer<T>{
	void serialize(OutputStream out, T obj, Request req, Response resp) throws IOException;
}
