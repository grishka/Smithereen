package smithereen.util;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlParser{
	private static final DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();

	static{
		try{
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		}catch(ParserConfigurationException e){
			throw new RuntimeException(e);
		}
		factory.setXIncludeAware(false);
	}

	public static DocumentBuilder newDocumentBuilder(){
		try{
			return factory.newDocumentBuilder();
		}catch(ParserConfigurationException e){
			throw new RuntimeException(e);
		}
	}

	public static String serialize(Document doc){
		try{
			Transformer transformer=TransformerFactory.newDefaultInstance().newTransformer();
			StringWriter writer=new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			return writer.toString();
		}catch(TransformerException x){
			throw new RuntimeException(x);
		}
	}

	public static Iterable<Node> iterateNodes(NodeList list){
		return new NodeListIterable(list);
	}

	private record NodeListIterable(NodeList list) implements Iterable<Node>{
		@NotNull
		@Override
		public Iterator<Node> iterator(){
			return new NodeListIterator(list);
		}
	}

	private static final class NodeListIterator implements Iterator<Node>{
		private int current=0;
		private final NodeList list;

		private NodeListIterator(NodeList list){
			this.list=list;
		}

		@Override
		public boolean hasNext(){
			return current<list.getLength();
		}

		@Override
		public Node next(){
			return list.item(current++);
		}
	}
}
