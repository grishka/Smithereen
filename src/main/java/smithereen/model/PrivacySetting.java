package smithereen.model;

import com.google.gson.annotations.SerializedName;

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
