package smithereen.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.Set;

import smithereen.Utils;
import smithereen.exceptions.BadRequestException;

public class PrivacySetting{
	public static final PrivacySetting DEFAULT=new PrivacySetting();

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
