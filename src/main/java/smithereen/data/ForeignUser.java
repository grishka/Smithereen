package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;

import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;

public class ForeignUser extends User{

	public String domain;
	public URI url;
	public URI inbox;
	public URI outbox;
	public URI sharedInbox;
	public URI followers;
	public URI following;
	public Timestamp lastUpdated;

	public static ForeignUser fromResultSet(ResultSet res) throws SQLException{
		ForeignUser user=new ForeignUser();
		user.fillFromResultSet(res);
		return user;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		super.fillFromResultSet(res);
		domain=res.getString("domain");
		activityPubID=tryParseURL(res.getString("ap_id"));
		url=tryParseURL(res.getString("ap_url"));
		inbox=tryParseURL(res.getString("ap_inbox"));
		outbox=tryParseURL(res.getString("ap_outbox"));
		sharedInbox=tryParseURL(res.getString("ap_shared_inbox"));
		followers=tryParseURL(res.getString("ap_followers"));
		following=tryParseURL(res.getString("ap_following"));
		lastUpdated=res.getTimestamp("last_updated");
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("ForeignUser{");
		sb.append(super.toString());
		if(domain!=null){
			sb.append("domain='");
			sb.append(domain);
			sb.append('\'');
		}
		if(activityPubID!=null){
			sb.append(", activityPubID='");
			sb.append(activityPubID);
			sb.append('\'');
		}
		if(url!=null){
			sb.append(", url='");
			sb.append(url);
			sb.append('\'');
		}
		if(inbox!=null){
			sb.append(", inbox='");
			sb.append(inbox);
			sb.append('\'');
		}
		if(outbox!=null){
			sb.append(", outbox='");
			sb.append(outbox);
			sb.append('\'');
		}
		if(sharedInbox!=null){
			sb.append(", sharedInbox='");
			sb.append(sharedInbox);
			sb.append('\'');
		}
		if(followers!=null){
			sb.append(", followers='");
			sb.append(followers);
			sb.append('\'');
		}
		if(following!=null){
			sb.append(", following='");
			sb.append(following);
			sb.append('\'');
		}
		if(lastUpdated!=null){
			sb.append(", lastUpdated=");
			sb.append(lastUpdated);
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception{
		super.parseActivityPubObject(obj);
		username=obj.optString("preferredUsername", null);
		if(username==null){
			username=Utils.getLastPathSegment(activityPubID);
		}
		domain=activityPubID.getHost();
		url=tryParseURL(obj.getString("url"));
		if(obj.has("firstName")){
			firstName=obj.getString("firstName");
			lastName=obj.optString("lastName", null);
		}else{
			firstName=name;
		}
		if(obj.has("birthDate")){
			birthDate=Date.valueOf(obj.getString("birthDate"));
		}
		inbox=tryParseURL(obj.getString("inbox"));
		outbox=tryParseURL(obj.optString("outbox", null));
		followers=tryParseURL(obj.optString("followers", null));
		following=tryParseURL(obj.optString("following", null));
		if(obj.has("endpoints")){
			JSONObject endpoints=obj.getJSONObject("endpoints");
			sharedInbox=tryParseURL(endpoints.optString("sharedInbox", null));
		}
		if(sharedInbox==null)
			sharedInbox=inbox;
		if(obj.has("gender")){
			switch(obj.getString("gender")){
				case "sc:Male":
					gender=Gender.MALE;
					break;
				case "sc:Female":
					gender=Gender.FEMALE;
					break;
				default:
					gender=Gender.UNKNOWN;
					break;
			}
		}else{
			gender=Gender.UNKNOWN;
		}
		JSONObject pkey=obj.getJSONObject("publicKey");
		URI keyOwner=tryParseURL(pkey.getString("owner"));
		if(!keyOwner.equals(activityPubID))
			throw new IllegalArgumentException("Key owner ("+keyOwner+") is not equal to user ID ("+activityPubID+")");
		String pkeyEncoded=pkey.getString("publicKeyPem");
		pkeyEncoded=pkeyEncoded.replaceAll("-----(BEGIN|END) (RSA )?PUBLIC KEY-----", "").replace("\n", "").trim();
		byte[] key=Base64.getDecoder().decode(pkeyEncoded);
		try{
			X509EncodedKeySpec spec=new X509EncodedKeySpec(key);
			publicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
		}catch(InvalidKeySpecException x){
			// a simpler RSA key format, used at least by Misskey
			// FWIW, Misskey user objects also contain a key "isCat" which I ignore
			RSAPublicKeySpec spec=decodeSimpleRSAKey(key);
			publicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
		}
		manuallyApprovesFollowers=obj.optBoolean("manuallyApprovesFollowers", false);
		return this;
	}

	@Override
	public String getFullUsername(){
		return username+"@"+domain;
	}

	@Override
	public URI getFollowersURL(){
		return followers;
	}

	private static RSAPublicKeySpec decodeSimpleRSAKey(byte[] key) throws IOException{
		ByteArrayInputStream in=new ByteArrayInputStream(key);
		int id=in.read();
		if(id!=0x30)
			throw new IOException("Must start with SEQUENCE");
		int seqLen=readDerLength(in);
		id=in.read();
		if(id!=2)
			throw new IOException("SEQUENCE must be followed by INTEGER");
		int modLen=readDerLength(in);
		byte[] modBytes=new byte[modLen];
		in.read(modBytes);
		id=in.read();
		if(id!=2)
			throw new IOException("SEQUENCE must be followed by INTEGER");
		int expLen=readDerLength(in);
		byte[] expBytes=new byte[expLen];
		in.read(expBytes);
		return new RSAPublicKeySpec(new BigInteger(modBytes), new BigInteger(expBytes));
	}

	private static int readDerLength(InputStream in) throws IOException{
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
}
