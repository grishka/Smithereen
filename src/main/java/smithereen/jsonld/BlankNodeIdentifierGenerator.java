package smithereen.jsonld;

import java.util.HashMap;

/*package*/ class BlankNodeIdentifierGenerator{
	private int counter=0;
	private HashMap<String, String> identifierMap=new HashMap<>();

	public String generate(String identifier){
		if(identifier!=null && identifierMap.containsKey(identifier))
			return identifierMap.get(identifier);
		String id="_:b"+counter;
		counter++;
		if(identifier!=null)
			identifierMap.put(identifier, id);
		return id;
	}
}
