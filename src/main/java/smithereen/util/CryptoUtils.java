package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

public class CryptoUtils{
	public static PublicKey decodeEcPublicKey(byte[] encoded){
		if(encoded.length<32)
			throw new IllegalArgumentException("Encoded key must be at least 32 bytes, got only "+encoded.length);
		byte[] reversed=new byte[encoded.length];
		for(int i=0;i<encoded.length;i++)
			reversed[i]=encoded[encoded.length-i-1];
		EdECPublicKeySpec spec=new EdECPublicKeySpec(NamedParameterSpec.ED25519, new EdECPoint(((int)reversed[0] & 0x80)==0x80, new BigInteger(reversed)));
		try{
			return KeyFactory.getInstance("EdDSA").generatePublic(spec);
		}catch(InvalidKeySpecException x){
			throw new IllegalArgumentException(x);
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
	}

	public static byte[] encodeEcPublicKey(@NotNull PublicKey key){
		if(!key.getAlgorithm().equals("EdDSA"))
			throw new IllegalArgumentException("Expected a EdDSA key, got "+key.getAlgorithm());
		try{
			SimpleASN1InputStream in=new SimpleASN1InputStream(new ByteArrayInputStream(key.getEncoded()));
			in.readSequence();
			in.skipSequence(); // Algorithm identifier
			byte[] encoded=in.readBitString();
			if(encoded.length==33 && encoded[0]==0)
				return Arrays.copyOfRange(encoded, 1, 33);
			else if(encoded.length==32)
				return encoded;
			else
				throw new IOException("Unexpected key length "+encoded.length);
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}

	public static byte[] sha256(byte[] input){
		try{
			return MessageDigest.getInstance("SHA256").digest(input);
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
	}

	private static class SimpleASN1InputStream extends FilterInputStream{

		protected SimpleASN1InputStream(InputStream in){
			super(in);
		}

		public int readLength() throws IOException{
			int length=in.read();
			if((length & 0x80)!=0){
				int additionalBytes=length & 0x7F;
				if(additionalBytes>4)
					throw new IOException("Invalid length value");
				length=0;
				for(int i=0;i<additionalBytes;i++){
					length=length<<8;
					length|=in.read() & 0xFF;
				}
			}
			return length;
		}

		public int readSequence() throws IOException{
			if(in.read()!=0x30)
				throw new IOException("Expected SEQUENCE");
			return readLength();
		}

		public int skipSequence() throws IOException{
			int length=readSequence();
			skipNBytes(length);
			return length;
		}

		public byte[] readBitString() throws IOException{
			if(in.read()!=3)
				throw new IOException("Expected BIT STRING");
			int length=readLength();
			return readNBytes(length);
		}
	}
}
