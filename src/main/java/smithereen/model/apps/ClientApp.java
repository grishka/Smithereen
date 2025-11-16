package smithereen.model.apps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.CachedRemoteImage;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.SizedImage;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.MediaCache;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import spark.utils.StringUtils;

public class ClientApp{
	public long id;
	public URI apID;
	public String username;
	public String domain;
	transient public PublicKey publicKey;
	transient public PrivateKey privateKey;
	public String name;
	public ClientAppType type;
	public String description;
	public Image logo;
	public int developerID;
	public URI apInbox, apSharedInbox;
	public Instant lastUpdated;
	public Set<String> allowedRedirectURIs=Set.of();

	public static ClientApp fromResultSet(ResultSet res) throws SQLException{
		ClientApp a=new ClientApp();
		a.id=res.getLong("id");
		String apID=res.getString("ap_id");
		a.apID=apID==null ? null : URI.create(apID);
		a.username=res.getString("username");
		a.domain=res.getString("domain");
		byte[] key=res.getBytes("public_key");
		try{
			X509EncodedKeySpec spec=new X509EncodedKeySpec(key);
			a.publicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
		}catch(Exception ignore){}
		key=res.getBytes("private_key");
		if(key!=null){
			try{
				PKCS8EncodedKeySpec spec=new PKCS8EncodedKeySpec(key);
				a.privateKey=KeyFactory.getInstance("RSA").generatePrivate(spec);
			}catch(Exception ignore){}
		}
		a.type=ClientAppType.values()[res.getInt("type")];
		a.name=res.getString("name");
		a.description=res.getString("description");

		String _ava=res.getString("logo");
		if(_ava!=null){
			if(_ava.startsWith("{")){
				try{
					a.logo=(Image) ActivityPubObject.parse(JsonParser.parseString(_ava).getAsJsonObject(), ParserContext.LOCAL);
				}catch(Exception ignore){}
			}
		}

		a.developerID=res.getInt("developer_id");

		String inbox=res.getString("ap_inbox");
		if(inbox!=null)
			a.apInbox=URI.create(inbox);
		String sharedInbox=res.getString("ap_shared_inbox");
		if(sharedInbox!=null)
			a.apSharedInbox=URI.create(sharedInbox);

		a.lastUpdated=DatabaseUtils.getInstant(res, "last_updated");

		String extra=res.getString("extra");
		if(extra!=null){
			JsonObject e=JsonParser.parseString(extra).getAsJsonObject();
			a.allowedRedirectURIs=e.get("redirectURIs").getAsJsonArray().asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
		}

		return a;
	}

	public String serializeExtraFields(){
		return new JsonObjectBuilder()
				.add("redirectURIs", allowedRedirectURIs.stream().collect(JsonArrayBuilder.COLLECTOR))
				.build()
				.toString();
	}

	public String getURL(){
		if(StringUtils.isNotEmpty(username)){
			if(StringUtils.isNotEmpty(domain))
				return "/"+username+"@"+domain;
			return "/"+username;
		}
		return "/app"+id;
	}

	public SizedImage getLogo(){
		if(logo==null)
			return null;
		if(logo instanceof LocalImage){
			return (LocalImage) logo;
		}
		if(logo.url==null)
			return null;
		MediaCache cache=MediaCache.getInstance();
		try{
			MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.get(logo.url);
			if(item!=null){
				return new CachedRemoteImage(item, logo.url);
			}else{
				SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
				if(logo.width>0 && logo.height>0){
					size=new SizedImage.Dimensions(logo.width, logo.height);
				}
				return new NonCachedRemoteImage(new NonCachedRemoteImage.AppLogoArgs(id), size, logo.url);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public URI getActivityPubID(){
		return apID==null ? Config.localURI("/apps/"+id) : apID;
	}

	public boolean isLocal(){
		return apID==null;
	}
}
