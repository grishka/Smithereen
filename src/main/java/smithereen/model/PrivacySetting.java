package smithereen.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.Set;

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
