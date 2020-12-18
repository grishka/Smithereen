package smithereen.data;

import org.json.JSONObject;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.jsonld.JLD;
import smithereen.storage.MediaCache;
import spark.utils.StringUtils;

public class User extends ActivityPubObject implements Actor{
	public static final long FLAG_SUPPORTS_FRIEND_REQS=1;


	public int id;
	public String firstName;
	public String lastName;
	public String middleName;
	public String maidenName;
	public String username;
	public java.sql.Date birthDate;
	public Gender gender;
	public long flags;

	transient public PublicKey publicKey;
	transient public PrivateKey privateKey;

	// additional profile fields
	public boolean manuallyApprovesFollowers;

	public String getFullName(){
		if(StringUtils.isEmpty(lastName))
			return firstName.isEmpty() ? ('@'+username) : firstName;
		return firstName+" "+lastName;
	}

	public String getCompleteName(){
		if(StringUtils.isEmpty(middleName) && StringUtils.isEmpty(maidenName))
			return getFullName();
		StringBuilder sb=new StringBuilder(firstName);
		if(StringUtils.isNotEmpty(middleName)){
			sb.append(' ');
			sb.append(middleName);
		}
		if(StringUtils.isNotEmpty(lastName)){
			sb.append(' ');
			sb.append(lastName);
		}
		if(StringUtils.isNotEmpty(maidenName)){
			sb.append(" (");
			sb.append(maidenName);
			sb.append(')');
		}
		return sb.toString();
	}

	public String getProfileURL(String action){
		return "/"+getFullUsername()+"/"+action;
	}

	public String getProfileURL(){
		return "/"+getFullUsername();
	}

	public boolean hasAvatar(){
		return icon!=null;
	}

	public SizedImage getAvatar(){
		Image icon=getBestAvatarImage();
		if(icon==null)
			return null;
		if(icon instanceof LocalImage){
			return (LocalImage) icon;
		}
		MediaCache cache=MediaCache.getInstance();
		try{
			MediaCache.PhotoItem item=(MediaCache.PhotoItem) cache.get(icon.url);
			if(item!=null){
				return new CachedRemoteImage(item, getAvatarCropRegion());
			}else{
				SizedImage.Dimensions size=SizedImage.Dimensions.UNKNOWN;
				if(icon.width>0 && icon.height>0){
					size=new SizedImage.Dimensions(icon.width, icon.height);
				}
				return new NonCachedRemoteImage(new NonCachedRemoteImage.ProfilePictureArgs(id), size);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return null;
	}

	public Image getBestAvatarImage(){
		Image icon=this.icon!=null ? this.icon.get(0) : null;
		if(icon==null)
			return null;
		if(icon instanceof LocalImage)
			return icon;
		if(icon.image!=null && !icon.image.isEmpty() && icon.image.get(0).width>0 && icon.image.get(0).height>0)
			return icon.image.get(0);
		return icon;
	}

	public float[] getAvatarCropRegion(){
		Image icon=this.icon!=null ? this.icon.get(0) : null;
		if(icon==null)
			return null;
		return icon.cropRegion;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("User{");
		sb.append(super.toString());
		sb.append("id=");
		sb.append(id);
		if(firstName!=null){
			sb.append(", firstName='");
			sb.append(firstName);
			sb.append('\'');
		}
		if(lastName!=null){
			sb.append(", lastName='");
			sb.append(lastName);
			sb.append('\'');
		}
		if(username!=null){
			sb.append(", username='");
			sb.append(username);
			sb.append('\'');
		}
		if(birthDate!=null){
			sb.append(", birthDate=");
			sb.append(birthDate);
		}
		if(gender!=null){
			sb.append(", gender=");
			sb.append(gender);
		}
		if(publicKey!=null){
			sb.append(", publicKey=");
			sb.append(publicKey);
		}
		if(privateKey!=null){
			sb.append(", privateKey=");
			sb.append(privateKey);
		}
		sb.append('}');
		return sb.toString();
	}

	public static User fromResultSet(ResultSet res) throws SQLException{
		if(res.getString("domain").length()>0)
			return ForeignUser.fromResultSet(res);
		User user=new User();
		user.fillFromResultSet(res);
		return user;
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt("id");
		firstName=res.getString("fname");
		lastName=res.getString("lname");
		middleName=res.getString("middle_name");
		maidenName=res.getString("maiden_name");
		username=res.getString("username");
		birthDate=res.getDate("bdate");
		gender=Gender.valueOf(res.getInt("gender"));
		summary=res.getString("about");
		flags=res.getLong("flags");

		byte[] key=res.getBytes("public_key");
		try{
			X509EncodedKeySpec spec=new X509EncodedKeySpec(key);
			publicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
		}catch(Exception ignore){}
		key=res.getBytes("private_key");
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
					icon=Collections.singletonList((Image)ActivityPubObject.parse(new JSONObject(_ava), ParserContext.LOCAL));
				}catch(Exception ignore){}
			}else{
				LocalImage ava=new LocalImage();
				SizedImage.Type[] sizes={SizedImage.Type.SMALL, SizedImage.Type.MEDIUM, SizedImage.Type.LARGE, SizedImage.Type.XLARGE};
				int[] sizeDimens={50, 100, 200, 400};
				for(SizedImage.Format format : SizedImage.Format.values()){
					for(SizedImage.Type size : sizes){
//						ava.sizes.add(new PhotoSize(Config.localURI(Config.uploadURLPath+"/avatars/"+_ava+"_"+size.suffix()+"."+format.fileExtension()), sizeDimens[size.ordinal()], sizeDimens[size.ordinal()], size, format));
					}
				}
				icon=Collections.singletonList(ava);
			}
		}

		activityPubID=Config.localURI("/users/"+id);
		url=Config.localURI(username);

		String fields=res.getString("profile_fields");
		if(StringUtils.isNotEmpty(fields)){
			JSONObject o=new JSONObject(fields);
			manuallyApprovesFollowers=o.optBoolean("manuallyApprovesFollowers", false);
		}
	}

	public String getFullUsername(){
		return username;
	}

	public URI getFollowersURL(){
		String userURL=activityPubID.toString();
		return URI.create(userURL+"/followers");
	}

	@Override
	public String getType(){
		return "Person";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		name=getFullName();
		String userURL=activityPubID.toString();
		JSONObject root=super.asActivityPubObject(obj, contextCollector);
		root.put("preferredUsername", username);

		root.put("inbox", userURL+"/inbox");
		root.put("outbox", userURL+"/outbox");
		root.put("followers", userURL+"/followers");
		root.put("following", userURL+"/following");

		root.put("firstName", firstName);
		if(StringUtils.isNotEmpty(lastName)){
			root.put("lastName", lastName);
		}
		if(StringUtils.isNotEmpty(middleName)){
			root.put("middleName", middleName);
		}
		if(StringUtils.isNotEmpty(maidenName)){
			root.put("maidenName", maidenName);
		}
		if(birthDate!=null){
			root.put("birthDate", birthDate.toString());
		}
		switch(gender){
			case MALE:
				root.put("gender", "http://schema.org#Male");
				break;
			case FEMALE:
				root.put("gender", "http://schema.org#Female");
				break;
		}

		JSONObject endpoints=new JSONObject();
		endpoints.put("sharedInbox", Config.localURI("/activitypub/sharedInbox").toString());
		root.put("endpoints", endpoints);

		JSONObject pubkey=new JSONObject();
		pubkey.put("id", userURL+"#main-key");
		pubkey.put("owner", userURL);
		String pkey="-----BEGIN PUBLIC KEY-----\n";
		pkey+=Base64.getEncoder().encodeToString(publicKey.getEncoded());
		pkey+="\n-----END PUBLIC KEY-----\n";
		pubkey.put("publicKeyPem", pkey);
		root.put("publicKey", pubkey);
		root.put("supportsFriendRequests", true);

		contextCollector.addAlias("sc", JLD.SCHEMA_ORG);
		contextCollector.addType("firstName", "sc:givenName", "sc:Text");
		contextCollector.addType("lastName", "sc:familyName", "sc:Text");
		contextCollector.addType("middleName", "sc:additionalName", "sc:Text");
		contextCollector.addType("gender", "sc:gender", "sc:GenderType");
		contextCollector.addType("birthDate", "sc:birthDate", "sc:Date");
		contextCollector.addSchema(JLD.W3_SECURITY);
		contextCollector.addAlias("sm", JLD.SMITHEREEN);
		contextCollector.addAlias("supportsFriendRequests", "sm:supportsFriendRequests");
		contextCollector.addAlias("maidenName", "sm:maidenName");

		return root;
	}

	@Override
	public boolean equals(Object other){
		if(other==null)
			return false;
		if(other instanceof User){
			return ((User) other).id==id && ((User) other).activityPubID.equals(activityPubID);
		}
		return false;
	}

	public String serializeProfileFields(){
		JSONObject o=new JSONObject();
		if(manuallyApprovesFollowers)
			o.put("manuallyApprovesFollowers", true);
		return o.toString();
	}

	public boolean supportsFriendRequests(){
		return true;
	}

	public String getNameForReply(){
		if(StringUtils.isNotEmpty(firstName))
			return firstName;
		return username;
	}

	public enum Gender{
		UNKNOWN,
		MALE,
		FEMALE;

		public static Gender valueOf(int v){
			switch(v){
				case 0:
					return UNKNOWN;
				case 1:
					return MALE;
				case 2:
					return FEMALE;
			}
			throw new IllegalArgumentException("Invalid gender "+v);
		}
	}
}
