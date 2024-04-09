package smithereen.http;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import smithereen.storage.utils.Pair;

public class FormBodyPublisherBuilder{
	public static final String CONTENT_TYPE="application/x-www-form-urlencoded";

	private List<Pair<String, String>> fields=new ArrayList<>();

	public FormBodyPublisherBuilder add(String key, String value){
		fields.add(new Pair<>(key, value));
		return this;
	}

	public HttpRequest.BodyPublisher build(){
		String body=fields.stream()
				.map(f->URLEncoder.encode(f.first(), StandardCharsets.UTF_8)+"="+URLEncoder.encode(f.second(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
		return HttpRequest.BodyPublishers.ofString(body);
	}
}
