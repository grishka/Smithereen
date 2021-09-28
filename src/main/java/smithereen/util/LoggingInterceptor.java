package smithereen.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoggingInterceptor implements Interceptor{
	private static final Logger LOG=LoggerFactory.getLogger(LoggingInterceptor.class);

	@Override
	public @NotNull Response intercept(Chain chain) throws IOException{
		Request req=chain.request();
		LOG.info("{} {} {}", req.method(), req.url().encodedPath()+Objects.requireNonNullElse(req.url().encodedQuery(), ""), chain.connection().protocol().toString().toUpperCase());
		LOG.info("{}", req.headers());
		Response resp=chain.proceed(req);
		LOG.info("{} {} {}", resp.protocol().toString().toUpperCase(), resp.code(), resp.message());
		LOG.info("{}", resp.headers());
		return resp;
	}
}
