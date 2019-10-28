package smithereen.jsonld;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;

public class JLDContext implements Cloneable{

	public URI baseIRI;
	public HashMap<String, TermDefinition> termDefinitions=new HashMap<>();
	public String vocabularyMapping;
	public String defaultLanguage;

	@Override
	public JLDContext clone(){
		try{
			return (JLDContext) super.clone();
		}catch(CloneNotSupportedException x){
			throw new RuntimeException(x);
		}
	}

	@Override
	public String toString(){
		return "JLDContext{"+
				"baseIRI="+baseIRI+
				", termDefinitions="+termDefinitions+
				", vocabularyMapping="+vocabularyMapping+
				'}';
	}

	public static class TermDefinition implements Cloneable{
		public String iriMapping;
		public String typeMapping;
		public String containerMapping;
		public String languageMapping;
		public boolean reverseProperty;
		public boolean hasLanguageMapping;

		@Override
		public TermDefinition clone(){
			try{
				return (TermDefinition) super.clone();
			}catch(CloneNotSupportedException x){
				throw new RuntimeException(x);
			}
		}

		@Override
		public String toString(){
			return "TermDefinition{"+
					"iriMapping='"+iriMapping+'\''+
					", typeMapping='"+typeMapping+'\''+
					", containerMapping='"+containerMapping+'\''+
					", reverseProperty="+reverseProperty+
					'}';
		}
	}
}
