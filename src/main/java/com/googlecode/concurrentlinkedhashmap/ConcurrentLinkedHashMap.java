package com.googlecode.concurrentlinkedhashmap;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLinkedHashMap<K, V> extends ConcurrentHashMap<K, V>{

	public static class Builder<K, V>{
		public Builder initialCapacity(int capacity){
			return this;
		}

		public Builder maximumWeightedCapacity(long capacity){
			return this;
		}

		public ConcurrentLinkedHashMap<K, V> build(){
			return new ConcurrentLinkedHashMap<>();
		}
	}
}
