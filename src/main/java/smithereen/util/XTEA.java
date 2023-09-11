package smithereen.util;

import smithereen.Config;
import smithereen.data.ObfuscatedObjectIDType;

public class XTEA{
	private static final int NUM_ROUNDS=32;
	private static final int DELTA=0x9E3779B9;

	public static long encrypt(long v, int[] key){
		if(key.length!=4)
			throw new IllegalArgumentException();
		int v0=(int)(v >> 32);
		int v1=(int)v;
		int sum=0;
		for(int i=0;i<NUM_ROUNDS;i++){
			v0+=(((v1 << 4) ^ ((v1 >> 5) & 0x07FFFFFF))+v1) ^ (sum+key[sum & 3]);
			sum+=DELTA;
			v1+=(((v0 << 4) ^ ((v0 >> 5) & 0x07FFFFFF))+v0) ^ (sum+key[(sum >> 11) & 3]);
		}
		return ((long)v0 << 32) | ((long)v1 & 0xFFFFFFFFL);
	}

	public static long decrypt(long v, int[] key){
		if(key.length!=4)
			throw new IllegalArgumentException();
		int v0=(int)(v >> 32);
		int v1=(int)v;
		//noinspection NumericOverflow
		int sum=DELTA*NUM_ROUNDS;
		for(int i=0;i<NUM_ROUNDS;i++){
			v1-=(((v0 << 4) ^ ((v0 >> 5) & 0x07FFFFFF))+v0) ^ (sum+key[(sum >> 11) & 3]);
			sum-=DELTA;
			v0-=(((v1 << 4) ^ ((v1 >> 5) & 0x07FFFFFF))+v1) ^ (sum+key[sum & 3]);
		}
		return ((long)v0 << 32) | ((long)v1 & 0xFFFFFFFFL);
	}

	public static long obfuscateObjectID(long id, ObfuscatedObjectIDType type){
		return encrypt(id, Config.objectIdObfuscationKeysByType[type.ordinal()]);
	}

	public static long deobfuscateObjectID(long id, ObfuscatedObjectIDType type){
		return decrypt(id, Config.objectIdObfuscationKeysByType[type.ordinal()]);
	}
}
