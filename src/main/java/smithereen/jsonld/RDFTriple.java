package smithereen.jsonld;

import org.json.JSONArray;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public RDFTriple(RDFTriple other){
		subject=other.subject;
		predicate=other.predicate;
		object=other.object;
		graphName=other.graphName;
	}

	public static RDFTriple parse(String s){
		if(!s.endsWith(" ."))
			throw new IllegalArgumentException("Malformed RDF triple '"+s+"'");
		String[] parts=s.substring(0, s.length()-2).split(" ", 3);
		String _subject=parts[0];
		String _predicate=parts[1];
		String _object=parts[2];
		Object subject;
		URI predicate;
		Object object;
		if(_subject.charAt(0)=='<'){
			subject=URI.create(_subject.substring(1, _subject.length()-1));
		}else{
			subject=_subject;
		}
		predicate=URI.create(_predicate.substring(1, _predicate.length()-1));
		String graphName=null;
		if(_object.charAt(0)=='<'){
			object=URI.create(_object.substring(1, _object.indexOf('>')));
			int index=_object.indexOf(' ');
			if(index!=-1){
				graphName=_object.substring(index+1);
			}
		}else if(_object.charAt(0)=='_'){
			int index=_object.indexOf(' ');
			if(index==-1){
				object=_object;
			}else{
				object=_object.substring(0, index);
				graphName=_object.substring(index+1);
			}
		}else{
			Pattern ptn=Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"(?:\\^\\^<([^>]+)>|@(\\w+))?");
			Matcher m=ptn.matcher(_object);
			if(!m.find())
				throw new IllegalArgumentException("Malformed RDF literal "+_object);
			String lex=new JSONArray("[\""+m.group(1)+"\"]").getString(0);
			URI datatype;
			if(m.group(2)!=null)
				datatype=URI.create(m.group(2));
			else
				datatype=URI.create(RDF.NS_RDF+(m.group(3)==null ? "string" : "langString"));
			object=new RDFLiteral(lex, datatype, m.group(3));
			int literalLen=m.group().length();
			if(literalLen<_object.length()){
				graphName=_object.substring(literalLen+1);
			}
		}
		RDFTriple r=new RDFTriple(subject, predicate, object);
		if(graphName!=null){
			if(graphName.charAt(0)=='<')
				r.graphName=URI.create(graphName.substring(1, graphName.length()-1));
			else
				r.graphName=graphName;
		}
		return r;
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
