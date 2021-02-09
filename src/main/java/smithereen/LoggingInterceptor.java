package smithereen;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoggingInterceptor implements Interceptor{

	@Override
	public Response intercept(Chain chain) throws IOException{
		Request req=chain.request();
		System.out.println(req.method()+" "+req.url().encodedPath()+Objects.requireNonNullElse(req.url().encodedQuery(), "")+" "+chain.connection().protocol().toString().toUpperCase());
		System.out.println(req.headers());
		System.out.println();
		Response resp=chain.proceed(req);
		System.out.println(resp.protocol().toString().toUpperCase()+" "+resp.code()+" "+resp.message());
		System.out.println(resp.headers());
		return resp;
	}
}
