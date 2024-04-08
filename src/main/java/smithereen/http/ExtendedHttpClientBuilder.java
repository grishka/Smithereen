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
		return realBuilder.cookieHandler(cookieHandler);
	}

	@Override
	public HttpClient.Builder connectTimeout(Duration duration){
		return realBuilder.connectTimeout(duration);
	}

	@Override
	public HttpClient.Builder sslContext(SSLContext sslContext){
		return realBuilder.sslContext(sslContext);
	}

	@Override
	public HttpClient.Builder sslParameters(SSLParameters sslParameters){
		return realBuilder.sslParameters(sslParameters);
	}

	@Override
	public HttpClient.Builder executor(Executor executor){
		return realBuilder.executor(executor);
	}

	@Override
	public HttpClient.Builder followRedirects(HttpClient.Redirect policy){
		return realBuilder.followRedirects(policy);
	}

	@Override
	public HttpClient.Builder version(HttpClient.Version version){
		return realBuilder.version(version);
	}

	@Override
	public HttpClient.Builder priority(int priority){
		return realBuilder.priority(priority);
	}

	@Override
	public HttpClient.Builder proxy(ProxySelector proxySelector){
		return realBuilder.proxy(proxySelector);
	}

	@Override
	public HttpClient.Builder authenticator(Authenticator authenticator){
		return realBuilder.authenticator(authenticator);
	}

	@Override
	public HttpClient build(){
		return new ExtendedHttpClient(realBuilder.build());
	}
}
