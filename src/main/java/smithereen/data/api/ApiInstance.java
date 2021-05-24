package smithereen.data.api;

public class ApiInstance{
	public String uri;
	public String title;
	public String shortDescription;
	public String description;
	public String email;
	public String version;
	public boolean registrations;
	public boolean approvalRequired;
	public boolean invitesEnabled;
	public Stats stats=new Stats();

	public static class Stats{
		public int userCount;
		public int statusCount;
		public int domainCount;
	}
}
