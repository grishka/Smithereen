package smithereen.text;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.util.UriBuilder;

public class MicroFormatAwareHTMLWhitelist extends Whitelist{

	private static final List<String> ALLOWED_CLASSES=Arrays.asList(
			"h-card",
			"u-url",
			"mention", "hashtag", "invisible",
			"quote-inline"
	);
	private static final Pattern NON_IDN_CHAR_REGEX=Pattern.compile("[^a-z\\d.:-]", Pattern.CASE_INSENSITIVE);

	public MicroFormatAwareHTMLWhitelist(){
		addTags("a", "b", "i", "u", "s", "code", "p", "blockquote", "span", "sub", "sup", "br", "pre");
		addAttributes("a", "href", "data-user-id");
		addProtocols("a", "href", "http", "https");
	}

	@Override
	protected boolean isSafeAttribute(String tagName, Element el, Attribute attr){
		if(tagName.equals("a")){
			if(attr.getKey().equals("rel"))
				return attr.getValue().equals("tag");
			if(attr.getKey().equals("href") && super.isSafeAttribute(tagName, el, attr)){
				return normalizeURL(attr);
			}
		}
		if(super.isSafeAttribute(tagName, el, attr))
			return true;
		if(attr.getKey().equals("class")){
			String[] classList=attr.getValue().split(" ");
			String filteredClassList=Arrays.stream(classList).filter(cn->ALLOWED_CLASSES.contains(cn) || (tagName.equals("code") && cn.startsWith("lang-"))).collect(Collectors.joining(" "));
			if(!filteredClassList.isEmpty()){
				attr.setValue(filteredClassList);
				return true;
			}
		}
		return false;
	}

	private boolean normalizeURL(Attribute attr){
		try{
			URI uri=new URI(attr.getValue());
			if(uri.getScheme().equals("acct"))
				return true;
			if(uri.getAuthority()==null)
				return false;
			String authority=uri.getAuthority();
			if(NON_IDN_CHAR_REGEX.matcher(authority).find()){
				uri=new UriBuilder(uri).authority(IDN.toASCII(authority)).build();
				attr.setValue(uri.toString());
			}
		}catch(URISyntaxException x){
			return false;
		}
		return true;
	}
}
