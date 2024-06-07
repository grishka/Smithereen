package smithereen.templates;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import smithereen.Utils;
import smithereen.lang.Lang;

public class TruncateTextFilter implements Filter{
	@Override
	public Object apply(Object _input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException{
		String input=(String)_input;
		Document doc=Jsoup.parseBodyFragment(input);
		String full=doc.text();
		int len=full.length();
		len+=doc.select("br").size()*50;
		if(len<=500)
			return new SafeString(input);

		int totalLen=0;
		Document truncated=Document.createShell("");
		for(int i=0;i<doc.body().childNodeSize();i++){
			Node el=doc.body().childNode(i);

			String elText;
			if(el instanceof Element){
				Element e=(Element)el;
				elText=e.text();
				if("br".equals(e.tagName()))
					totalLen+=50;
			}else if(el instanceof TextNode){
				elText=((TextNode) el).text();
			}else{
				continue;
			}

			totalLen+=elText.length();
			if(totalLen>300){
				if(totalLen<=500){
					truncated.body().appendChild(el.clone());
				}else{
					Node e=el.clone();
					int initialLen=totalLen-elText.length();
					List<Node> toRemove=new ArrayList<>();
					e.traverse(new NodeVisitor(){
						private int len=initialLen;
						@Override
						public void head(Node node, int depth){
							if(len>=500){
								toRemove.add(node);
							}else if(node instanceof TextNode tn){
								String text=tn.text();
								if(len+text.length()>500){
									tn.text(truncateOnWordBoundary(text, 500-len));
								}
								len+=text.length();
							}
						}

						@Override
						public void tail(Node node, int depth){

						}
					});
					for(Node n:toRemove)
						n.remove();
					truncated.body().appendChild(e);
				}
				break;
			}else{
				truncated.body().appendChild(el.clone());
			}
		}
		if(truncated.text().equals(full))
			return new SafeString(input);

		String randomID=Utils.randomAlphanumericString(15);
		return new SafeString("<input type=\"checkbox\" id=\"textExpander_"+randomID+"\" class=\"textExpander\"/>" +
				"<div class=\"expandableText\">" +
				"<div class=\"full\">"+doc.body().html()+"</div>" +
				"<div class=\"truncated\">"+truncated.body().html()+"</div>" +
				"<label for=\"textExpander_"+randomID+"\">"+Lang.get(context.getLocale()).get("expand_text")+"</label>" +
				"</div>");
	}

	@Override
	public List<String> getArgumentNames(){
		return null;
	}

	public static String truncateOnWordBoundary(String s, int maxLen){
		if(s.length()<=maxLen+20)
			return s;
		int len=Math.max(0, Math.min(s.indexOf(' ', maxLen), maxLen+20));
		return s.substring(0, len)+"...";
	}
}
