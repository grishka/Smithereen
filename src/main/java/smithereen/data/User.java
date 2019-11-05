package smithereen.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Image;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class User extends ActivityPubObject{
	public int id;
	public String firstName;
	public String lastName;
	public String username;
	public java.sql.Date birthDate;
	public Gender gender;
	public Avatar avatar;

	transient public PublicKey publicKey;
	transient public PrivateKey privateKey;

	public String getFullName(){
		if(lastName==null || lastName.length()==0)
			return firstName;
		return firstName+" "+lastName;
	}

	public String getProfileURL(String action){
		return "/"+getFullUsername()+"/"+action;
	}

	public boolean hasAvatar(){
		return avatar!=null;
	}

	public String getSmallAvatar(){
		if(avatar!=null){
			return avatar.hasSizes ? avatar.jpeg50 : avatar.jpegWhatever;
		}
		return null;
	}

	public String getSmallAvatarSrcset(){
		if(avatar!=null){
			if(avatar.hasSizes){
				return avatar.jpeg50+", "+avatar.jpeg100+" 2x";
			}
			return avatar.jpegWhatever;
		}
		return null;
	}

	public String getMediumAvatar(){
		if(avatar!=null){
			return avatar.hasSizes ? avatar.jpeg100 : avatar.jpegWhatever;
		}
		return null;
	}

	public String getMediumAvatarSrcset(){
		if(avatar!=null){
			if(avatar.hasSizes){
				return avatar.jpeg100+", "+avatar.jpeg200+" 2x";
			}
			return avatar.jpegWhatever;
		}
		return null;
	}

	public String getBigAvatar(){
		if(avatar!=null){
			return avatar.hasSizes ? avatar.jpeg200 : avatar.jpegWhatever;
		}
		return null;
	}

	public String getBigAvatarSrcset(){
		if(avatar!=null){
			if(avatar.hasSizes){
				return avatar.jpeg200+", "+avatar.jpeg400+" 2x";
			}
			return avatar.jpegWhatever;
		}
		return null;
	}

	public String getBiggestAvatar(){
		if(avatar!=null){
			return avatar.hasSizes ? avatar.jpeg400 : avatar.jpegWhatever;
		}
		return null;
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
		if(avatar!=null){
			sb.append(", avatar=");
			sb.append(avatar);
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
		username=res.getString("username");
		birthDate=res.getDate("bdate");
		gender=Gender.valueOf(res.getInt("gender"));
		summary=res.getString("about");

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
			try{
				JSONObject ava=new JSONObject(_ava);
				avatar=new Avatar();
				if(ava.has("j50")){
					avatar.jpeg50=ava.getString("j50");
					avatar.jpeg100=ava.getString("j100");
					avatar.jpeg200=ava.getString("j200");
					avatar.jpeg400=ava.getString("j400");
					avatar.hasSizes=true;
				}else{
					avatar.jpegWhatever=ava.getString("jw");
				}
				image=avatar.asImageList();
			}catch(JSONException ignore){
			}
		}

		activityPubID=Config.localURI(username);
		url=activityPubID;
	}

	public String getFullUsername(){
		return username;
	}

	public URI getFollowersURL(){
		return Config.localURI(username+"/activitypub/followers");
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

		root.put("inbox", userURL+"/activitypub/inbox");
		root.put("outbox", userURL+"/activitypub/outbox");
		root.put("followers", userURL+"/activitypub/followers");
		root.put("following", userURL+"/activitypub/following");

		root.put("firstName", firstName);
		if(StringUtils.isNotEmpty(lastName)){
			root.put("lastName", lastName);
		}
		if(birthDate!=null){
			root.put("birthDate", birthDate);
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
		endpoints.put("sharedInbox", Config.localURI("/activitypub/sharedInbox"));
		root.put("endpoints", endpoints);

		JSONObject pubkey=new JSONObject();
		pubkey.put("id", userURL+"#main-key");
		pubkey.put("owner", userURL);
		String pkey="-----BEGIN PUBLIC KEY-----\n";
		pkey+=Base64.getEncoder().encodeToString(publicKey.getEncoded());
		pkey+="\n-----END PUBLIC KEY-----\n";
		pubkey.put("publicKeyPem", pkey);
		root.put("publicKey", pubkey);

		contextCollector.addAlias("sc", JLD.SCHEMA_ORG);
		contextCollector.addType("firstName", "sc:givenName", "sc:Text");
		contextCollector.addType("lastName", "sc:familyName", "sc:Text");
		contextCollector.addType("gender", "sc:gender", "sc:GenderType");
		contextCollector.addType("birthDate", "sc:birthDate", "sc:Date");
		contextCollector.addSchema(JLD.W3_SECURITY);

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

	public static class Avatar{
		public boolean hasSizes;
		public String jpeg50, jpeg100, jpeg200, jpeg400;
		public String jpegWhatever;

		public Avatar(){}

		public Avatar(List<Image> images){
			if(images.size()==1){
				jpegWhatever=images.get(0).url.toString();
			}else{
				for(Image img:images){
					if(img.width==img.height){
						if(img.width==50)
							jpeg50=img.url.toString();
						else if(img.width==100)
							jpeg100=img.url.toString();
						else if(img.width==200)
							jpeg200=img.url.toString();
						else if(img.width==400)
							jpeg400=img.url.toString();
					}
				}
				if(jpeg50!=null && jpeg100!=null && jpeg200!=null && jpeg400!=null)
					hasSizes=true;
				else
					jpegWhatever=images.get(0).url.toString();
			}
		}

		@Override
		public String toString(){
			StringBuilder sb=new StringBuilder("Avatar{");
			sb.append("hasSizes=");
			sb.append(hasSizes);
			if(jpeg50!=null){
				sb.append(", jpeg50='");
				sb.append(jpeg50);
				sb.append('\'');
			}
			if(jpeg100!=null){
				sb.append(", jpeg100='");
				sb.append(jpeg100);
				sb.append('\'');
			}
			if(jpeg200!=null){
				sb.append(", jpeg200='");
				sb.append(jpeg200);
				sb.append('\'');
			}
			if(jpeg400!=null){
				sb.append(", jpeg400='");
				sb.append(jpeg400);
				sb.append('\'');
			}
			if(jpegWhatever!=null){
				sb.append(", jpegWhatever='");
				sb.append(jpegWhatever);
				sb.append('\'');
			}
			sb.append('}');
			return sb.toString();
		}

		public String asJSON(){
			JSONObject j=new JSONObject();
			j.put("v", 1);
			if(hasSizes){
				j.put("j50", jpeg50);
				j.put("j100", jpeg100);
				j.put("j200", jpeg200);
				j.put("j400", jpeg400);
			}else{
				j.put("jw", jpegWhatever);
			}
			return j.toString();
		}

		public List<Image> asImageList(){
			try{
				if(!hasSizes){
					Image img=new Image();
					img.mediaType="image/jpeg";
					img.url=Config.localURI(jpegWhatever);
					return Collections.singletonList(img);
				}else{
					ArrayList<Image> imgs=new ArrayList<>();
					String[] urls={jpeg400, jpeg200, jpeg100, jpeg50};
					int[] sizes={400, 200, 100, 50};
					for(int i=0;i<urls.length;i++){
						Image img=new Image();
						img.mediaType="image/jpeg";
						img.url=Config.localURI(urls[i]);
						img.width=img.height=sizes[i];
						imgs.add(img);
					}
					return imgs;
				}
			}catch(Exception x){
				x.printStackTrace();
				return null;
			}
		}
	}
}
