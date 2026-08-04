package smithereen.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class PrefixTree<K, V>{
	private static class Node<K, V>{
		public HashMap<K, Node<K, V>> children;
		public V value;

		@Override
		public String toString(){
			return "Node{"+
					"children="+children+
					", value="+value+
					'}';
		}
	}

	private final Node<K, V> root=new Node<>();

	@Nullable
	public V find(List<K> key){
		Node<K, V> x=root;
		for(K keyPart:key){
			if(x.children==null)
				return x.value;
			x=x.children.get(keyPart);
			if(x==null)
				return null;
		}
		return x.value;
	}

	public synchronized void insert(List<K> key, V value){
		Node<K, V> x=root;
		for(K keyPart:key){
			if(x.children==null)
				x.children=new HashMap<>();
			x=x.children.computeIfAbsent(keyPart, k->new Node<>());
		}
		x.value=value;
	}

	public synchronized void delete(List<K> key){
		delete(root, key);
	}

	private Node<K, V> delete(Node<K, V> x, List<K> key){
		if(x==null){
			return null;
		}else if(key.isEmpty()){
			x.value=null;
		}else{
			Node<K, V> node=delete(x.children.get(key.getFirst()), key.subList(1, key.size()));
			if(node==null)
				x.children.remove(key.getFirst());
			else
				x.children.put(key.getFirst(), node);
		}
		if(x.value!=null)
			return x;
		return x.children!=null && !x.children.isEmpty() ? x : null;
	}

	@Override
	public String toString(){
		return "PrefixTree{"+
				"root="+root+
				'}';
	}
}
