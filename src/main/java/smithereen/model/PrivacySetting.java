package smithereen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.exceptions.BadRequestException;
import smithereen.jsonld.JLD;

public class PrivacySetting{
	public static final PrivacySetting DEFAULT=new PrivacySetting();
	public static final PrivacySetting DEFAULT_FRIENDS_ONLY=new PrivacySetting();

	static{
		DEFAULT_FRIENDS_ONLY.baseRule=Rule.FRIENDS;
	}

	@SerializedName("r")
	public Rule baseRule=Rule.EVERYONE;
	@SerializedName("au")
	public Set<Integer> allowUsers=Set.of();
	@SerializedName("xu")
	public Set<Integer> exceptUsers=Set.of();

	@Override
	public String toString(){
		return "PrivacySetting{"+
				"baseRule="+baseRule+
				", allowUsers="+allowUsers+
				", exceptUsers="+exceptUsers+
				'}';
	}

	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(!(o instanceof PrivacySetting that)) return false;
		return baseRule==that.baseRule && Objects.equals(allowUsers, that.allowUsers) && Objects.equals(exceptUsers, that.exceptUsers);
	}

	@Override
	public int hashCode(){
		return Objects.hash(baseRule, allowUsers, exceptUsers);
	}

	public boolean isFullyPrivate(){
		return baseRule==Rule.NONE && allowUsers.isEmpty();
	}

	public boolean isFullyPublic(){
		return baseRule==Rule.EVERYONE && exceptUsers.isEmpty();
	}

	public JsonObject serializeForActivityPub(User owner, SerializerContext serializerContext){
		JsonArray allowed=new JsonArray();
		JsonArray except=new JsonArray();

		switch(baseRule){
			case EVERYONE -> allowed.add(ActivityPub.AS_PUBLIC.toString());
			case FRIENDS -> allowed.add(owner.getFriendsURL().toString());
			case FRIENDS_OF_FRIENDS -> {
				allowed.add(owner.getFriendsURL().toString());
				allowed.add("sm:FriendsOfFriends");
			}
		}
		if(!allowUsers.isEmpty() || !exceptUsers.isEmpty()){
			String domain=serializerContext.getRequesterDomain();
			if(domain!=null){
				HashSet<Integer> needUsers=new HashSet<>();
				needUsers.addAll(allowUsers);
				needUsers.addAll(exceptUsers);
				Map<Integer, User> users=serializerContext.appContext.getUsersController().getUsers(needUsers);
				for(int id:allowUsers){
					User user=users.get(id);
					if(user!=null && user.domain.equalsIgnoreCase(domain))
						allowed.add(user.activityPubID.toString());
				}
				for(int id:exceptUsers){
					User user=users.get(id);
					if(user!=null && user.domain.equalsIgnoreCase(domain))
						except.add(user.activityPubID.toString());
				}
			}
		}

		JsonObject setting=new JsonObject();
		setting.add("allowedTo", allowed);
		if(!except.isEmpty())
			setting.add("except", except);
		return setting;
	}

	public static PrivacySetting parseFromActivityPub(User owner, JsonObject setting){
		JsonArray allowedTo=ActivityPubObject.optArrayCompact(setting, "allowedTo");
		if(allowedTo==null)
			return null;
		PrivacySetting ps=new PrivacySetting();
		ps.baseRule=PrivacySetting.Rule.NONE;
		ps.allowUsers=new HashSet<>();
		ps.exceptUsers=new HashSet<>();
		for(int i=0;i<allowedTo.size();i++){
			String e=allowedTo.get(i).getAsString();
			if(ActivityPub.AS_PUBLIC.toString().equals(e) || "as:Public".equals(e)){
				ps.baseRule=PrivacySetting.Rule.EVERYONE;
				continue;
			}else if("sm:FriendsOfFriends".equals(e) || (JLD.SMITHEREEN+"sm").equals(e)){
				ps.baseRule=PrivacySetting.Rule.FRIENDS_OF_FRIENDS;
				continue;
			}
			URI uri;
			try{
				uri=new URI(e);
			}catch(URISyntaxException x){
				continue;
			}
			if(Objects.equals(uri, owner.getFriendsURL()) && ps.baseRule!=PrivacySetting.Rule.FRIENDS_OF_FRIENDS){
				ps.baseRule=PrivacySetting.Rule.FRIENDS;
			}else if(Objects.equals(uri, owner.followers)){
				ps.baseRule=PrivacySetting.Rule.FOLLOWERS;
			}else if(Objects.equals(uri, owner.following)){
				ps.baseRule=PrivacySetting.Rule.FOLLOWING;
			}else if(Config.isLocal(uri)){
				int id=ObjectLinkResolver.getUserIDFromLocalURL(uri);
				if(id>0)
					ps.allowUsers.add(id);
			}
		}
		JsonArray except=ActivityPubObject.optArrayCompact(setting, "except");
		if(except!=null){
			for(int i=0;i<except.size();i++){
				String e=allowedTo.get(i).getAsString();
				URI uri;
				try{
					uri=new URI(e);
				}catch(URISyntaxException x){
					continue;
				}
				if(Config.isLocal(uri)){
					int id=ObjectLinkResolver.getUserIDFromLocalURL(uri);
					if(id>0)
						ps.exceptUsers.add(id);
				}
			}
		}
		return ps;
	}

	public static PrivacySetting fromJson(String json){
		PrivacySetting ps;
		try{
			ps=Utils.gson.fromJson(json, PrivacySetting.class);
		}catch(Exception x){
			throw new BadRequestException(x);
		}
		if(ps.baseRule==null)
			throw new BadRequestException();
		if(ps.allowUsers==null)
			ps.allowUsers=Set.of();
		if(ps.exceptUsers==null)
			ps.exceptUsers=Set.of();
		return ps;
	}

	public enum Rule{
		@SerializedName("e")
		EVERYONE,
		@SerializedName("f")
		FRIENDS,
		@SerializedName("ff")
		FRIENDS_OF_FRIENDS,
		@SerializedName("fl")
		FOLLOWERS,
		@SerializedName("fw")
		FOLLOWING,
		@SerializedName("n")
		NONE
	}
}
