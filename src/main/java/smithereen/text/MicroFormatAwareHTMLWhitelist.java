package smithereen.text;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import smithereen.util.UriBuilder;

@SuppressWarnings("deprecation")
public class MicroFormatAwareHTMLWhitelist extends Whitelist{

	private static final List<String> ALLOWED_CLASSES=Arrays.asList(
			"h-card",
			"u-url",
			"mention", "hashtag", "invisible",
			"quote-inline"
	);
	private static final Pattern NON_IDN_CHAR_REGEX=Pattern.compile("[^a-z\\d.:-]", Pattern.CASE_INSENSITIVE);

	public MicroFormatAwareHTMLWhitelist(){
		addTags("a", "b", "i", "u", "s", "code", "p", "em", "strong", "span", "sarcasm", "sub", "sup", "br", "pre");
		addAttributes("a", "href", "data-user-id");
		addProtocols("a", "href", "http", "https");
	}

	@Override
	protected boolean isSafeAttribute(String tagName, Element el, Attribute attr){
		if(tagName.equals("a")){
			if(attr.getKey().equals("rel"))
				return attr.getValue().equals("tag");
			if(attr.getKey().equals("href") && super.isSafeAttribute(tagName, el, attr)){
				try{
					URI uri=new URI(attr.getValue());
					if(uri.getAuthority()==null)
						return false;
					String authority=uri.getAuthority();
					if(NON_IDN_CHAR_REGEX.matcher(authority).find()){
						uri=new UriBuilder(uri).authority(IDN.toASCII(authority)).build();
						attr.setValue(uri.toString());
					}
//					if(!Config.isLocal(uri)){
//						el.attr("target", "_blank");
//					}
				}catch(URISyntaxException x){
					return false;
				}
				return true;
			}
//			if(attr.getKey().equals("target") && attr.getValue().equals("_blank")){
//				if(el.hasAttr("href")){
//					try{
//						return !Config.isLocal(new URI(el.attr("href")));
//					}catch(URISyntaxException x){
//						return false;
//					}
//				}
//			}
		}
		if(super.isSafeAttribute(tagName, el, attr))
			return true;
		if(attr.getKey().equals("class")){
			String[] classList=attr.getValue().split(" ");
			String[] filteredClassList=Arrays.stream(classList).filter(ALLOWED_CLASSES::contains).toArray(String[]::new);
			if(filteredClassList.length>0){
				attr.setValue(String.join(" ", filteredClassList));
				return true;
			}
		}
		return false;
	}
}
