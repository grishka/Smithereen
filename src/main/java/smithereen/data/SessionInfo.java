package smithereen.data;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class SessionInfo{
	public Account account;
	public Locale preferredLocale;
	public PageHistory history=new PageHistory();
	public String csrfToken;
	public TimeZone timeZone;

	public static class PageHistory{
		public ArrayList<String> entries=new ArrayList<>();

		public void add(String path){
			if(last().equals(path)) // don't record page refreshes
				return;
			entries.add(path);
			while(entries.size()>5)
				entries.remove(0);
		}

		public String last(){
			if(entries.isEmpty())
				return "/";
			return entries.get(entries.size()-1);
		}
	}
}
