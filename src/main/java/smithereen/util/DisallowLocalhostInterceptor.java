package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import smithereen.Config;

public class DisallowLocalhostInterceptor implements Interceptor{
	@Override
	public @NotNull Response intercept(@NotNull Chain chain) throws IOException{
		if(!Config.DEBUG && chain.connection().socket().getInetAddress().isLoopbackAddress())
			throw new IOException("Localhost connections are not allowed");
		return chain.proceed(chain.request());
	}
}
