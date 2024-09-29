package smithereen.http;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpHeaderParser{
	private static final Pattern ATTR_REGEX=Pattern.compile("(\\S+?)=(\"(?:[^\"\\\\]|\\\\\"|\\\\\\\\)*\"|\\S+)(?:; *|$)");

	public static Map<String, String> parseAttributes(String raw){
		HashMap<String, String> res=new HashMap<>();
		raw=raw.trim();
		Matcher matcher=ATTR_REGEX.matcher(raw);
		while(matcher.find()){
			String key=matcher.group(1);
			String value=matcher.group(2);
			if(value.charAt(0)=='"'){
				value=value.substring(1, value.length()-1).replace("\\", "");
			}
			res.put(key.toLowerCase(), value);
		}
		return res;
	}
}
