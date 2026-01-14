package smithereen.api.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import smithereen.model.PrivacySetting;

public class ApiPrivacySetting{
	public String baseRule;
	public List<Integer> allowUsers;
	public List<Integer> allowLists;
	public List<Integer> exceptUsers;
	public List<Integer> exceptLists;

	public ApiPrivacySetting(PrivacySetting ps){
		baseRule=switch(ps.baseRule){
			case EVERYONE -> "everyone";
			case FRIENDS -> "friends";
			case FRIENDS_OF_FRIENDS -> "friends_of_friends";
			case FOLLOWERS -> "followers";
			case FOLLOWING -> "following";
			case NONE -> "none";
		};
		allowUsers=new ArrayList<>(ps.allowUsers);
		allowLists=new ArrayList<>(ps.allowLists);
		exceptUsers=new ArrayList<>(ps.exceptUsers);
		exceptLists=new ArrayList<>(ps.exceptLists);
	}

	public ApiPrivacySetting(){}

	public PrivacySetting toNativePrivacySetting(){
		PrivacySetting ps=new PrivacySetting();
		ps.baseRule=switch(baseRule){
			case "friends" -> PrivacySetting.Rule.FRIENDS;
			case "friends_of_friends" -> PrivacySetting.Rule.FRIENDS_OF_FRIENDS;
			case "followers" -> PrivacySetting.Rule.FOLLOWERS;
			case "following" -> PrivacySetting.Rule.FOLLOWING;
			case "none" -> PrivacySetting.Rule.NONE;
			case null, default -> PrivacySetting.Rule.EVERYONE;
		};
		if(allowUsers!=null)
			ps.allowUsers=new HashSet<>(allowUsers);
		if(allowLists!=null)
			ps.allowLists=new HashSet<>(allowLists);
		if(exceptUsers!=null)
			ps.exceptUsers=new HashSet<>(exceptUsers);
		if(exceptLists!=null)
			ps.exceptLists=new HashSet<>(exceptLists);
		return ps;
	}
}
