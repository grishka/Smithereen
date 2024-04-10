package smithereen.http;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

class ExtendedHttpClientBuilder implements HttpClient.Builder{
	private final HttpClient.Builder realBuilder;

	public ExtendedHttpClientBuilder(HttpClient.Builder realBuilder){
		this.realBuilder=realBuilder;
	}

	@Override
	public HttpClient.Builder cookieHandler(CookieHandler cookieHandler){
		realBuilder.cookieHandler(cookieHandler);
		return this;
	}

	@Override
	public HttpClient.Builder connectTimeout(Duration duration){
		realBuilder.connectTimeout(duration);
		return this;
	}

	@Override
	public HttpClient.Builder sslContext(SSLContext sslContext){
		realBuilder.sslContext(sslContext);
		return this;
	}

	@Override
	public HttpClient.Builder sslParameters(SSLParameters sslParameters){
		realBuilder.sslParameters(sslParameters);
		return this;
	}

	@Override
	public HttpClient.Builder executor(Executor executor){
		realBuilder.executor(executor);
		return this;
	}

	@Override
	public HttpClient.Builder followRedirects(HttpClient.Redirect policy){
		realBuilder.followRedirects(policy);
		return this;
	}

	@Override
	public HttpClient.Builder version(HttpClient.Version version){
		realBuilder.version(version);
		return this;
	}

	@Override
	public HttpClient.Builder priority(int priority){
		realBuilder.priority(priority);
		return this;
	}

	@Override
	public HttpClient.Builder proxy(ProxySelector proxySelector){
		realBuilder.proxy(proxySelector);
		return this;
	}

	@Override
	public HttpClient.Builder authenticator(Authenticator authenticator){
		realBuilder.authenticator(authenticator);
		return this;
	}

	@Override
	public HttpClient build(){
		return new ExtendedHttpClient(realBuilder.build());
	}
}
