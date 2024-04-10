package smithereen.http;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpResponse;

public class ReaderBodyHandler implements HttpResponse.BodyHandler<Reader>{
	@Override
	public HttpResponse.BodySubscriber<Reader> apply(HttpResponse.ResponseInfo responseInfo){
		return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofInputStream(), in->new InputStreamReader(in, HttpContentType.from(responseInfo.headers()).getCharset()));
	}
}
