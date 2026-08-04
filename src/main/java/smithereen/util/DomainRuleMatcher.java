package smithereen.util;

import java.util.List;

public class DomainRuleMatcher<V> extends PrefixTree<String, V>{
	public V find(String domain){
		return find(makeKey(domain));
	}

	public void insert(String domain, V rule){
		insert(makeKey(domain), rule);
	}

	public void delete(String domain){
		delete(makeKey(domain));
	}

	private List<String> makeKey(String domain){
		if(domain.endsWith(".")) // example.com. is equivalent to example.com
			domain=domain.substring(0, domain.length()-1);
		return List.of(domain.toLowerCase().split("\\.")).reversed();
	}
}
