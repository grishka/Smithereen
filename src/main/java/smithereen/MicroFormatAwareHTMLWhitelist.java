package smithereen;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class MicroFormatAwareHTMLWhitelist extends Whitelist{

	private static final List<String> ALLOWED_CLASSES=Arrays.asList(
			"h-card",
			"u-url",
			"mention", "hashtag", "invisible"
	);

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
					if(uri.getHost()==null)
						return false;
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
