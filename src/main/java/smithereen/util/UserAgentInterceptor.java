package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import smithereen.BuildInfo;
import smithereen.Config;

public class UserAgentInterceptor implements Interceptor{
	@Override
	public @NotNull Response intercept(Chain chain) throws IOException{
		Request req=chain.request();
		req=req.newBuilder()
				.header("User-Agent", "Smithereen/"+BuildInfo.VERSION+" ("+Config.domain+") "+req.header("User-Agent"))
				.build();
		return chain.proceed(req);
	}
}
