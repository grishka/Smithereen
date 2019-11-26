package smithereen.jsonld;

import java.net.URI;

public class RDFLiteral{
	public String lexicalForm;
	public URI datatype;
	public String languageTag;

	public RDFLiteral(String lexicalForm, URI datatype, String languageTag){
		this.lexicalForm=lexicalForm;
		this.datatype=datatype;
		this.languageTag=languageTag;
	}

	private static String quote(String s){
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	@Override
	public String toString(){
		if(datatype.toString().equals(RDF.NS_RDF+"string")){
			return '"'+quote(lexicalForm)+'"';
		}else if(datatype.toString().equals(RDF.NS_RDF+"langString")){
			return '"'+quote(lexicalForm)+"\"@"+languageTag;
		}
		return '"'+quote(lexicalForm)+"\"^^<"+datatype+'>';
	}
}
