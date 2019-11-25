package smithereen.jsonld;

import java.net.URI;

public class RDFTriple{

	// IRI or blank node
	public Object subject;
	// IRI
	public URI predicate;
	// IRI, literal or blank node
	public Object object;
	// IRI or blank node
	public Object graphName;

	public RDFTriple(Object subject, URI predicate, Object object){
		this.subject=subject;
		this.predicate=predicate;
		this.object=object;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder();
		if(subject instanceof URI){
			sb.append('<');
			sb.append(subject);
			sb.append("> ");
		}else{
			sb.append(subject);
			sb.append(' ');
		}
		sb.append('<');
		sb.append(predicate);
		sb.append("> ");
		if(object instanceof URI){
			sb.append('<');
			sb.append(object);
			sb.append('>');
		}else{
			sb.append(object);
		}
		if(graphName!=null)
			sb.append(' ');
		if(graphName instanceof URI){
			sb.append('<');
			sb.append(graphName);
			sb.append('>');
		}else if(graphName!=null){
			sb.append(graphName);
		}
		sb.append(" .");
		return sb.toString();
	}
}
