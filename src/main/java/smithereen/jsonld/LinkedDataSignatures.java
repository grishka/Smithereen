package smithereen.jsonld;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Date;

import smithereen.Utils;

public class LinkedDataSignatures{
	private static final Logger LOG=LoggerFactory.getLogger(LinkedDataSignatures.class);

	public static void sign(JsonObject toSign, PrivateKey pkey, String keyID){
		JsonObject options=new JsonObject();
		options.addProperty("creator", keyID);
		options.addProperty("created", Utils.formatDateAsISO(new Date()));
		options.addProperty("@context", JLD.W3_IDENTITY);

		String cOptions=URDNA2015.canonicalize(options, null);
		String cData=URDNA2015.canonicalize(toSign, null);

		try{
			MessageDigest sha256=MessageDigest.getInstance("SHA-256");
			String optionsHash=Utils.byteArrayToHexString(sha256.digest(cOptions.getBytes(StandardCharsets.UTF_8)));
			String dataHash=Utils.byteArrayToHexString(sha256.digest(cData.getBytes(StandardCharsets.UTF_8)));
			byte[] sigData=(optionsHash+dataHash).getBytes(StandardCharsets.UTF_8);

			Signature signer=Signature.getInstance("SHA256withRSA");
			signer.initSign(pkey);
			signer.update(sigData);
			byte[] sig=signer.sign();

			options.addProperty("type", "RsaSignature2017");
			options.addProperty("signatureValue", Base64.getEncoder().encodeToString(sig));
			options.remove("@context");
			toSign.add("signature", options);
		}catch(NoSuchAlgorithmException|InvalidKeyException|SignatureException x){
			LOG.error("Exception while creating LD-signature with key {}", keyID, x);
		}
	}

	public static boolean verify(JsonObject obj, PublicKey pkey){
		JsonObject sig=obj.getAsJsonObject("signature");
		if(!sig.get("type").getAsString().equals("RsaSignature2017"))
			return false;
		byte[] signature=Base64.getDecoder().decode(sig.get("signatureValue").getAsString());
		JsonObject options=new JsonObject();
		options.addProperty("@context", JLD.W3_IDENTITY);
		for(String key:sig.keySet()){
			switch(key){
				case "type":
				case "id":
				case "signatureValue":
					continue;
				default:
					options.add(key, sig.get(key));
			}
		}
		JsonObject data=new JsonObject();
		for(String key:obj.keySet()){
			if(!"signature".equals(key))
				data.add(key, obj.get(key));
		}

		String cOptions=URDNA2015.canonicalize(options, null);
		String cData=URDNA2015.canonicalize(data, null);

		try{
			MessageDigest sha256=MessageDigest.getInstance("SHA-256");
			String optionsHash=Utils.byteArrayToHexString(sha256.digest(cOptions.getBytes(StandardCharsets.UTF_8)));
			String dataHash=Utils.byteArrayToHexString(sha256.digest(cData.getBytes(StandardCharsets.UTF_8)));
			byte[] sigData=(optionsHash+dataHash).getBytes(StandardCharsets.UTF_8);

			Signature verifier=Signature.getInstance("SHA256withRSA");
			verifier.initVerify(pkey);
			verifier.update(sigData);
			return verifier.verify(signature);
		}catch(NoSuchAlgorithmException|SignatureException|InvalidKeyException ignore){}

		return false;
	}
}
