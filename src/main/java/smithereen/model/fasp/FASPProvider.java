package smithereen.model.fasp;

import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import smithereen.util.CryptoUtils;

public class FASPProvider{
	public long id;
	public boolean confirmed;
	public String name;
	public URI baseUrl;
	public URI signInUrl;
	public String remoteID;
	public PublicKey publicKey;
	public PrivateKey privateKey;
	public Map<FASPCapability, String> capabilities=new HashMap<>();
	public List<String> unknownCapabilities=new ArrayList<>();
	public Map<FASPCapability, String> enabledCapabilities=new HashMap<>();
	public Map<String, URI> privacyPolicy=Map.of();
	public String contactEmail;
	public int actorID;
	public Instant createdAt;

	public static FASPProvider fromResultSet(ResultSet res) throws SQLException{
		FASPProvider p=new FASPProvider();
		p.id=res.getLong("id");
		p.confirmed=res.getBoolean("confirmed");
		p.name=res.getString("name");
		p.baseUrl=URI.create(res.getString("base_url"));
		String signInUrl=res.getString("sign_in_url");
		if(signInUrl!=null)
			p.signInUrl=URI.create(signInUrl);
		p.remoteID=res.getString("remote_id");
		byte[] publicKey=res.getBytes("public_key");
		byte[] privateKey=res.getBytes("private_key");
		try{
			KeyFactory kf=KeyFactory.getInstance("EdDSA");
			p.publicKey=kf.generatePublic(new X509EncodedKeySpec(publicKey));
			p.privateKey=kf.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		}catch(NoSuchAlgorithmException | InvalidKeySpecException x){
			throw new RuntimeException(x);
		}
		Utils.gson.fromJson(res.getString("capabilities"), new TypeToken<Map<String, String>>(){}).forEach((id, version)->{
			FASPCapability knownCapability=FASPCapability.fromID(id);
			if(knownCapability!=null)
				p.capabilities.put(knownCapability, version);
			else
				p.unknownCapabilities.add(id);
		});
		Utils.gson.fromJson(res.getString("enabled_capabilities"), new TypeToken<Map<String, String>>(){}).forEach((id, version)->p.enabledCapabilities.put(FASPCapability.fromID(id), version));
		String privacyPolicy=res.getString("privacy_policy");
		if(privacyPolicy!=null)
			p.privacyPolicy=Utils.gson.fromJson(privacyPolicy, new TypeToken<>(){});
		p.contactEmail=res.getString("contact_email");
		p.actorID=res.getInt("actor_id");
		p.createdAt=DatabaseUtils.getInstant(res, "created_at");

		return p;
	}

	public String getPublicKeyFingerprint(){
		return Base64.getEncoder().encodeToString(CryptoUtils.sha256(CryptoUtils.encodeEcPublicKey(publicKey)));
	}
}
