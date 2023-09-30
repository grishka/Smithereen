package smithereen.model;

import java.util.List;
import java.util.Map;

public class NodeInfo{
	public String version;
	public List<String> protocols;
	public boolean openRegistrations;
	public Software software;
	public Usage usage;
	public Map<String, Object> metadata;

	public static class Software{
		public String name;
		public String version;
		public String repository;
		public String homepage;
	}

	public static class Usage{
		public int localPosts;
		public int localComments;
		public Users users;

		public static class Users{
			public int total;
			public int activeMonth;
			public int activeHalfyear;
		}
	}
}
