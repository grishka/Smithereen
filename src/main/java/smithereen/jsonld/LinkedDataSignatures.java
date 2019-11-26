package smithereen.jsonld;

import org.json.JSONObject;

import java.net.URI;
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
	public static void sign(JSONObject toSign, PrivateKey pkey, String keyID){
		JSONObject options=new JSONObject();
		options.put("creator", keyID);
		options.put("created", Utils.formatDateAsISO(new Date()));
		options.put("@context", JLD.W3_IDENTITY);

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

			options.put("type", "RsaSignature2017");
			options.put("signatureValue", Base64.getEncoder().encodeToString(sig));
			options.remove("@context");
			toSign.put("signature", options);
		}catch(NoSuchAlgorithmException|InvalidKeyException|SignatureException x){
			x.printStackTrace();
		}
	}

	public static boolean verify(JSONObject obj, PublicKey pkey){
		JSONObject sig=obj.getJSONObject("signature");
		if(!sig.getString("type").equals("RsaSignature2017"))
			return false;
		byte[] signature=Base64.getDecoder().decode(sig.getString("signatureValue"));
		JSONObject options=new JSONObject();
		options.put("@context", JLD.W3_IDENTITY);
		for(String key:sig.keySet()){
			switch(key){
				case "type":
				case "id":
				case "signatureValue":
					continue;
				default:
					options.put(key, sig.get(key));
			}
		}
		JSONObject data=new JSONObject();
		for(String key:obj.keySet()){
			if(!"signature".equals(key))
				data.put(key, obj.get(key));
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
