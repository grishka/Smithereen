package smithereen.activitypub.objects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.exceptions.FederationException;
import smithereen.model.ActorStatus;
import smithereen.model.CachedRemoteImage;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.SizedImage;
import smithereen.exceptions.BadRequestException;
import smithereen.jsonld.JLD;
import smithereen.storage.MediaCache;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public abstract class Actor extends ActivityPubObject{
	public static final int USERNAME_MAX_LENGTH=64;

	public String username;
	transient public List<SigningKey> publicKeys=List.of();
	transient public PrivateKey privateKey;
	public String domain;
	public URI inbox;
	public URI outbox;
	public URI sharedInbox;
	public URI followers;
	public URI following;
	public URI collectionQueryEndpoint;
	public Instant lastUpdated;
	public ActorStatus status;

	public String aboutSource;

	public String getProfileURL(String action){
		return "/"+getFullUsername()+"/"+action;
	}

	public String getProfileURL(){
		return "/"+getFullUsername();
	}

	public String getAbsoluteProfileURL(){
		return Config.localURI(getProfileURL()).toString();
	}

	public boolean hasAvatar(){
		Image img=getBestAvatarImage();
		return img!=null && (img instanceof LocalImage || img.url!=null);
	}

	public Image getAvatarImage(){
		if(icon!=null && !icon.isEmpty())
			return icon.getFirst();
		return null;
	}

	public Image getBestAvatarImage(){
		Image icon=this.icon!=null ? this.icon.getFirst() : null;
		if(icon==null)
			return null;
		if(icon instanceof LocalImage)
			return icon;
		if(icon.image!=null && !icon.image.isEmpty() && icon.image.getFirst().width>0 && icon.image.getFirst().height>0)
			return icon.image.getFirst();

		if(this.icon.size()==1)
			return icon;
		Image largest=null;
		int largestArea=0;
		for(Image img:this.icon){
			int area=img.width*img.height;
			if(area>largestArea){
				largestArea=area;
				largest=img;
			}
		}
		return largest;
	}

	public float[] getAvatarCropRegion(){
		Image icon=this.icon!=null ? this.icon.getFirst() : null;
		if(icon==null)
			return null;
		return icon.cropRegion;
	}

	public String getFullUsername(){
		return username;
	}

	public URI getFollowersURL(){
		String userURL=activityPubID.toString();
		return URI.create(userURL+"/followers");
	}

	public SizedImage getAvatar(){
		Image icon=getBestAvatarImage();
		if(icon==null)
			return null;
		if(icon instanceof LocalImage){
			return (LocalImage) icon;
		}
		if(icon.url==null)
			return null;
		MediaCache cache=MediaCache.getInstance();
		try{
			MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.get(icon.url);
			if(item!=null){
				return new CachedRemoteImage(item, getAvatarCropRegion(), icon.url);
			}else{
				SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
				if(icon.width>0 && icon.height>0){
					size=new SizedImage.Dimensions(icon.width, icon.height);
				}
				return new NonCachedRemoteImage(getAvatarArgs(), size, icon.url);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return null;
	}

	protected NonCachedRemoteImage.Args getAvatarArgs(){
		throw new IllegalStateException("Should never happen");
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);

		String userURL=activityPubID.toString();
		obj.addProperty("preferredUsername", username);
		obj.addProperty("inbox", userURL+"/inbox");
		obj.addProperty("outbox", userURL+"/outbox");
		if(canBeFollowed())
			obj.addProperty("followers", getFollowersURL().toString());
		if(canFollowOtherActors())
			obj.addProperty("following", userURL+"/following");

		JsonObject endpoints=new JsonObject();
		endpoints.addProperty("sharedInbox", Config.localURI("/activitypub/sharedInbox").toString());
		endpoints.addProperty("collectionSimpleQuery", userURL+"/collectionQuery");
		obj.add("endpoints", endpoints);

		JsonObject pubkey=new JsonObject();
		pubkey.addProperty("id", userURL+"#main-key");
		pubkey.addProperty("owner", userURL);
		StringBuilder pkey=new StringBuilder("-----BEGIN PUBLIC KEY-----\n");
		String encodedKey=Base64.getEncoder().encodeToString(getFirstRsaPublicKey().key.getEncoded());
		for(int i=0;i<encodedKey.length();i+=64){
			pkey.append(encodedKey, i, Math.min(encodedKey.length(), i+64));
			pkey.append('\n');
		}
		pkey.append("-----END PUBLIC KEY-----\n");
		pubkey.addProperty("publicKeyPem", pkey.toString());
		obj.add("publicKey", pubkey);

		URI wallUrl=getWallURL();
		if(wallUrl!=null){
			obj.addProperty("wall", wallUrl.toString());
			serializerContext.addType("wall", "sm:wall", "@id");
		}
		URI wallCommentsUrl=getWallCommentsURL();
		if(wallCommentsUrl!=null){
			obj.addProperty("wallComments", wallCommentsUrl.toString());
			serializerContext.addType("wallComments", "sm:wallComments", "@id");
		}
		serializerContext.addAlias("collectionSimpleQuery", "sm:collectionSimpleQuery");
		serializerContext.addAlias("sm", JLD.SMITHEREEN);
		serializerContext.addAlias("toot", JLD.MASTODON);
		serializerContext.addAlias("discoverable", "toot:discoverable");
		serializerContext.addAlias("indexable", "toot:indexable");
		obj.addProperty("discoverable", true);
		obj.addProperty("indexable", true);

		serializerContext.addSchema(JLD.W3_SECURITY);

		if(status!=null){
			serializerContext.addSmIdType("status");
			obj.add("status", ActivityPubActorStatus.fromNativeStatus(status, this).asActivityPubObject(new JsonObject(), serializerContext));
		}

		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		boolean skipChecks=this instanceof ActivityPubApplication;

		if(activityPubID==null && !skipChecks)
			throw new IllegalArgumentException("id is required for actors");

		username=optString(obj, "preferredUsername");
		if(!skipChecks){
			if(username==null && activityPubID!=null){
				username=Utils.getLastPathSegment(activityPubID);
			}
			if(StringUtils.isEmpty(username)){
				throw new FederationException("Unable to determine actor username: preferredUsername not present and last path segment of ID is blank");
			}
			if(!Utils.isValidUsername(username)){
				username="_"+username.replaceAll("[^a-zA-Z0-9\\u0080-\\uffff._-]", "_"); // First, replace all disallowed ASCII characters with '_'
				username=TextProcessor.transliterate(username); // Then, transliterate non-ASCII characters, if any
			}
			if(username.length()>USERNAME_MAX_LENGTH)
				username=username.substring(0, USERNAME_MAX_LENGTH);
		}
		if(activityPubID!=null){
			domain=activityPubID.getHost();
			if(activityPubID.getPort()!=-1)
				domain+=":"+activityPubID.getPort();
			if(url==null)
				url=activityPubID;
		}

		JsonObject pkey=obj.getAsJsonObject("publicKey");
		if(pkey==null && !skipChecks)
			throw new IllegalArgumentException("The actor is missing a public key (or @context in the actor object doesn't include the namespace \""+JLD.W3_SECURITY+"\")");
		if(pkey!=null){
			URI keyOwner=tryParseURL(optString(pkey, "owner"));
			if(keyOwner!=null && !keyOwner.equals(activityPubID))
				throw new IllegalArgumentException("Key owner ("+keyOwner+") is not equal to user ID ("+activityPubID+")");
			String pkeyEncoded=pkey.get("publicKeyPem").getAsString();
			pkeyEncoded=pkeyEncoded.replaceAll("-----(BEGIN|END) (RSA )?PUBLIC KEY-----", "").replaceAll("[^A-Za-z0-9+/=]", "").trim();
			byte[] key=Base64.getDecoder().decode(pkeyEncoded);
			URI keyId=tryParseURL(optString(pkey, "id"));
			if(keyId==null)
				throw new IllegalArgumentException("The actor's public key is missing an id");
			if(!keyId.isAbsolute())
				keyId=activityPubID.resolve(keyId);
			publicKeys=new ArrayList<>();
			try{
				X509EncodedKeySpec spec=new X509EncodedKeySpec(key);
				PublicKey pk=KeyFactory.getInstance("RSA").generatePublic(spec);
				publicKeys.add(new SigningKey(keyId, pk, SigningKey.Algorithm.RSA));
			}catch(InvalidKeySpecException x){
				// a simpler RSA key format, used at least by Misskey
				// FWIW, Misskey user objects also contain a key "isCat" which I ignore
				try{
					RSAPublicKeySpec spec=decodeSimpleRSAKey(key);
					PublicKey pk=KeyFactory.getInstance("RSA").generatePublic(spec);
					publicKeys.add(new SigningKey(keyId, pk, SigningKey.Algorithm.RSA));
				}catch(NoSuchAlgorithmException ignore){
				}catch(InvalidKeySpecException | IOException xx){
					throw new BadRequestException(xx);
				}
			}catch(NoSuchAlgorithmException ignore){}
		}

		inbox=tryParseURL(optString(obj, "inbox"));
		ensureHostMatchesID(inbox, "inbox");
		outbox=tryParseURL(optString(obj, "outbox"));
		ensureHostMatchesID(outbox, "outbox");
		followers=tryParseURL(optString(obj, "followers"));
		ensureHostMatchesID(followers, "followers");
		following=tryParseURL(optString(obj, "following"));
		ensureHostMatchesID(following, "following");
		if(obj.has("endpoints")){
			JsonObject endpoints=obj.getAsJsonObject("endpoints");
			sharedInbox=tryParseURL(optString(endpoints, "sharedInbox"));
			ensureHostMatchesID(sharedInbox, "sharedInbox");
			collectionQueryEndpoint=tryParseURL(optString(endpoints, "collectionSimpleQuery"));
			ensureHostMatchesID(collectionQueryEndpoint, "collectionSimpleQuery");
		}

		if(summary!=null)
			summary=TextProcessor.sanitizeHTML(summary);

		if(obj.get("status") instanceof JsonObject jstatus){
			ActivityPubObject rawStatus=ActivityPubObject.parse(jstatus, parserContext);
			if(rawStatus instanceof ActivityPubActorStatus as){
				status=as.asNativeStatus();
			}
		}

		return this;
	}

	public abstract int getLocalID();
	public abstract URI getWallURL();
	public abstract URI getWallCommentsURL();
	public abstract URI getPhotoAlbumsURL();
	public abstract String getTypeAndIdForURL();
	public abstract String getName();
	public abstract String serializeProfileFields();

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
		return Math.min(length, in.available());
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		try{
			publicKeys=deserializePublicKeys(res.getBytes("public_key"));
		}catch(RuntimeException x){
			LOG.warn("Failed to deserialize public keys for actor", x);
		}
		byte[] key=res.getBytes("private_key");
		if(key!=null){
			try{
				PKCS8EncodedKeySpec spec=new PKCS8EncodedKeySpec(key);
				privateKey=KeyFactory.getInstance("RSA").generatePrivate(spec);
			}catch(Exception ignore){}
		}

		String _ava=res.getString("avatar");
		if(_ava!=null){
			if(_ava.startsWith("{")){
				try{
					icon=Collections.singletonList((Image)ActivityPubObject.parse(JsonParser.parseString(_ava).getAsJsonObject(), ParserContext.LOCAL));
				}catch(Exception ignore){}
			}
		}

		username=res.getString("username");
		aboutSource=res.getString("about_source");
	}

	public boolean hasWall(){
		return getWallURL()!=null;
	}

	public boolean hasPhotoAlbums(){
		return getPhotoAlbumsURL()!=null;
	}

	public void ensureLocal(){
		if(StringUtils.isNotEmpty(domain))
			throw new IllegalArgumentException("Local actor is required here (got "+activityPubID+")");
	}

	public void ensureRemote(){
		if(StringUtils.isEmpty(domain))
			throw new IllegalArgumentException("Remote actor is required here (got "+activityPubID+")");
	}

	protected boolean canFollowOtherActors(){
		return true;
	}

	protected boolean canBeFollowed(){
		return true;
	}

	public String getAboutSource(){
		return StringUtils.isNotEmpty(aboutSource) ? aboutSource : summary;
	}

	public EndpointsStorageWrapper getEndpointsForStorage(){
		if(StringUtils.isEmpty(domain))
			return null;
		EndpointsStorageWrapper ep=new EndpointsStorageWrapper();
		if(followers!=null)
			ep.followers=followers.toString();
		if(following!=null)
			ep.following=following.toString();
		if(outbox!=null)
			ep.outbox=outbox.toString();
		URI wall=getWallURL();
		if(wall!=null)
			ep.wall=wall.toString();
		URI wallComments=getWallCommentsURL();
		if(wallComments!=null)
			ep.wallComments=wallComments.toString();
		if(collectionQueryEndpoint!=null)
			ep.collectionQuery=collectionQueryEndpoint.toString();
		URI photoAlbums=getPhotoAlbumsURL();
		if(photoAlbums!=null)
			ep.photoAlbums=photoAlbums.toString();
		return ep;
	}

	public String serializeEndpoints(){
		EndpointsStorageWrapper endpoints=getEndpointsForStorage();
		return endpoints!=null ? Utils.gson.toJson(endpoints) : null;
	}

	public int getOwnerID(){
		throw new UnsupportedOperationException();
	}

	public String getStatusText(){
		return status!=null && !status.isExpired() ? status.text() : null;
	}

	public byte[] serializePublicKeys(){
		return serializePublicKeys(publicKeys);
	}

	public static byte[] serializeLocalActorRsaKey(PublicKey key){
		return serializePublicKeys(List.of(new SigningKey(URI.create("#"), key, SigningKey.Algorithm.RSA)));
	}

	public static byte[] serializePublicKeys(List<SigningKey> publicKeys){
		if(publicKeys.isEmpty())
			return null;
		try{
			ByteArrayOutputStream buf=new ByteArrayOutputStream();
			DataOutputStream out=new DataOutputStream(buf);
			for(SigningKey sk:publicKeys){
				out.write(sk.algorithm.ordinal());
				out.writeUTF(sk.id.toString());
				byte[] key=sk.key.getEncoded();
				out.writeShort(key.length);
				out.write(key);
			}
			return buf.toByteArray();
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}

	public static List<SigningKey> deserializePublicKeys(byte[] serialized){
		if(serialized==null || serialized.length==0)
			return new ArrayList<>();
		try{
			DataInputStream in=new DataInputStream(new ByteArrayInputStream(serialized));
			ArrayList<SigningKey> publicKeys=new ArrayList<>();
			while(in.available()>0){
				SigningKey.Algorithm alg=SigningKey.Algorithm.values()[in.read()];
				URI keyID=URI.create(in.readUTF());
				int len=in.readUnsignedShort();
				byte[] buf=new byte[len];
				in.readFully(buf);
				X509EncodedKeySpec spec=new X509EncodedKeySpec(buf);
				PublicKey pk=KeyFactory.getInstance(switch(alg){
					case RSA -> "RSA";
					case EdDSA -> "Ed25519";
				}).generatePublic(spec);
				publicKeys.add(new SigningKey(keyID, pk, alg));
			}
			return publicKeys;
		}catch(Exception x){
			throw new RuntimeException(x);
		}
	}

	public SigningKey getFirstRsaPublicKey(){
		for(SigningKey sk:publicKeys){
			if(sk.algorithm==SigningKey.Algorithm.RSA)
				return sk;
		}
		throw new NoSuchElementException("This actor does not have an RSA public key");
	}

	public SigningKey getPublicKey(URI id){
		for(SigningKey sk:publicKeys){
			if(sk.id.equals(id))
				return sk;
		}
		throw new NoSuchElementException("This actor does not have a public key with id "+id);
	}

	public static class EndpointsStorageWrapper{
		@SerializedName("fs")
		public String followers;
		@SerializedName("fg")
		public String following;
		@SerializedName("ob")
		public String outbox;
		@SerializedName("wl")
		public String wall;
		@SerializedName("fr")
		public String friends;
		@SerializedName("gr")
		public String groups;
		@SerializedName("at")
		public String actorToken;
		@SerializedName("cq")
		public String collectionQuery;
		@SerializedName("gm")
		public String groupMembers;
		@SerializedName("tm")
		public String tentativeGroupMembers;
		@SerializedName("pa")
		public String photoAlbums;
		@SerializedName("tp")
		public String taggedPhotos;
		@SerializedName("wc")
		public String wallComments;
		@SerializedName("bt")
		public String boardTopics;
		@SerializedName("pp")
		public String pinnedPosts;
		@SerializedName("ap")
		public String apps;
	}

	public record SigningKey(URI id, PublicKey key, Algorithm algorithm){
		public enum Algorithm{
			RSA,
			EdDSA,
		}
	}
}
