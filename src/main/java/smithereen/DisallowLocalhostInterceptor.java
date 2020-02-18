package smithereen;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class DisallowLocalhostInterceptor implements Interceptor{
	@Override
	public Response intercept(Chain chain) throws IOException{
		if(!Config.DEBUG && chain.connection().socket().getInetAddress().isLoopbackAddress())
			throw new IOException("Localhost connections are not allowed");
		return chain.proceed(chain.request());
	}
}
