package smithereen.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import smithereen.BuildInfo;
import smithereen.Config;

public class ExtendedHttpClient extends HttpClient{
	private final HttpClient realClient;

	ExtendedHttpClient(HttpClient realClient){
		this.realClient=realClient;
	}

	public static Builder newBuilder(){
		return new ExtendedHttpClientBuilder(HttpClient.newBuilder());
	}

	@Override
	public Optional<CookieHandler> cookieHandler(){
		return realClient.cookieHandler();
	}

	@Override
	public Optional<Duration> connectTimeout(){
		return realClient.connectTimeout();
	}

	@Override
	public Redirect followRedirects(){
		return realClient.followRedirects();
	}

	@Override
	public Optional<ProxySelector> proxy(){
		return realClient.proxy();
	}

	@Override
	public SSLContext sslContext(){
		return realClient.sslContext();
	}

	@Override
	public SSLParameters sslParameters(){
		return realClient.sslParameters();
	}

	@Override
	public Optional<Authenticator> authenticator(){
		return realClient.authenticator();
	}

	@Override
	public Version version(){
		return realClient.version();
	}

	@Override
	public Optional<Executor> executor(){
		return realClient.executor();
	}

	@Override
	public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException{
		return realClient.send(maybeAddUserAgent(request), responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler){
		return realClient.sendAsync(maybeAddUserAgent(request), responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler){
		return realClient.sendAsync(maybeAddUserAgent(request), responseBodyHandler, pushPromiseHandler);
	}

	@Override
	public WebSocket.Builder newWebSocketBuilder(){
		return realClient.newWebSocketBuilder();
	}

	@Override
	public void shutdown(){
		realClient.shutdown();
	}

	@Override
	public boolean awaitTermination(Duration duration) throws InterruptedException{
		return realClient.awaitTermination(duration);
	}

	@Override
	public boolean isTerminated(){
		return realClient.isTerminated();
	}

	@Override
	public void shutdownNow(){
		realClient.shutdownNow();
	}

	@Override
	public void close(){
		realClient.close();
	}

	private HttpRequest maybeAddUserAgent(HttpRequest req){
		if(req.headers().firstValue("user-agent").isPresent())
			return req;
		return HttpRequest.newBuilder(req, (k, v)->true)
				.header("User-Agent", "Smithereen/"+BuildInfo.VERSION+" +https://"+Config.domain+"/")
				.build();
	}
}
