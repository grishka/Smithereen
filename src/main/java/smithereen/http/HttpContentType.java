package smithereen.http;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public record HttpContentType(String mimeType, Map<String, String> attributes){
	public static HttpContentType from(HttpHeaders headers){
		String contentType=headers.firstValue("content-type").orElse("text/html; charset=UTF-8");
		int index=contentType.indexOf(';');
		if(index==-1){
			return new HttpContentType(contentType, Map.of());
		}
		return new HttpContentType(contentType.substring(0, index), HttpHeaderParser.parseAttributes(contentType.substring(index+1)));
	}

	public Charset getCharset(){
		if(attributes.containsKey("charset")){
			Charset.forName(attributes.get("charset"), StandardCharsets.UTF_8);
		}
		return StandardCharsets.UTF_8;
	}

	public boolean matches(HttpContentType other){
		if(!mimeType.equalsIgnoreCase(other.mimeType))
			return false;
		for(Map.Entry<String, String> attr:other.attributes.entrySet()){
			if(!Objects.equals(attr.getValue(), attributes.get(attr.getKey())))
				return false;
		}
		return true;
	}

	public boolean matches(String mimeType){
		return mimeType.equalsIgnoreCase(this.mimeType);
	}
}
