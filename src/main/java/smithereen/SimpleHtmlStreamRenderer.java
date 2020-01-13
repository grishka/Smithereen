package smithereen;

import org.owasp.html.HtmlStreamEventReceiver;

import java.util.List;

/**
 * An HtmlStreamEventReceiver that doesn't meddle with text
 */
public class SimpleHtmlStreamRenderer implements HtmlStreamEventReceiver{

	private StringBuilder sb;

	public SimpleHtmlStreamRenderer(StringBuilder sb){
		this.sb=sb;
	}

	@Override
	public void openDocument(){

	}

	@Override
	public void closeDocument(){

	}

	@Override
	public void openTag(String name, List<String> attrs){
		sb.append('<');
		sb.append(name);
		if(!attrs.isEmpty()){
			for(int i=0;i<attrs.size();i+=2){
				String attr=attrs.get(i);
				String value=attrs.get(i+1);
				sb.append(' ');
				sb.append(attr);
				if(!value.isEmpty()){
					sb.append("=\"");
					sb.append(value.replace("\"", "&quot;"));
					sb.append('"');
				}
			}
		}
		sb.append('>');
	}

	@Override
	public void closeTag(String tag){
		sb.append("</");
		sb.append(tag);
		sb.append('>');
	}

	@Override
	public void text(String s){
		sb.append(s.replace(">", "&gt;").replace("<", "&lt;"));
	}
}
