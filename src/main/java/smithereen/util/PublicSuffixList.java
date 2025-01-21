package smithereen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import spark.utils.StringUtils;

public class PublicSuffixList{
	// https://github.com/publicsuffix/list/wiki/Format
	private static final Logger LOG=LoggerFactory.getLogger(PublicSuffixList.class);

	public static long lastUpdatedTime;
	private static List<Rule> rules=List.of();

	public static void updateIfNeeded(){
		if(System.currentTimeMillis()-lastUpdatedTime>3600_000L*24*7){ // update once every 7 days
			BackgroundTaskRunner.getInstance().submit(()->{
				try{
					HttpRequest req=HttpRequest.newBuilder(URI.create("https://publicsuffix.org/list/public_suffix_list.dat"))
							.timeout(Duration.ofSeconds(30))
							.build();
					HttpResponse<Stream<String>> resp=ActivityPub.httpClient.send(req, HttpResponse.BodyHandlers.ofLines());
					if(resp.statusCode()/100==2){
						List<String> lines=resp.body().filter(l->!l.isBlank() && !l.startsWith("/")).toList();
						update(lines);
						Config.updateInDatabase(Map.of("PSList_LastUpdated", (lastUpdatedTime=System.currentTimeMillis())+"", "PSList_Data", String.join("\n", lines)));
					}
				}catch(IOException|SQLException|InterruptedException x){
					LOG.warn("Error loading public suffix list", x);
				}
			});
		}
	}

	public static void update(List<String> fileLines){
		rules=fileLines.stream()
				.map(l->{
					String[] labels=StringUtils.delimitedListToStringArray(l, ".", "!");
					for(int i=0;i<labels.length;i++){
						if(!"*".equals(labels[i])){
							labels[i]=IDN.toASCII(labels[i]).toLowerCase();
						}
					}
					return new Rule(labels, l.charAt(0)=='!');
				})
				.toList();
	}

	public static String getRegisteredDomain(String domain){
		String[] labels=StringUtils.delimitedListToStringArray(domain, ".");
		for(int i=0;i<labels.length;i++){
			labels[i]=IDN.toASCII(labels[i]).toLowerCase();
		}
		List<Rule> matchingRules=rules.stream()
				.filter(r->r.matches(labels))
				.sorted(Comparator.comparingInt(Rule::priority).reversed())
				.toList();

		int length;
		if(matchingRules.isEmpty()){
			length=Math.min(labels.length, 2);
		}else{
			Rule rule=matchingRules.getFirst();
			if(rule.except)
				length=rule.labels.length;
			else
				length=rule.labels.length+1;
		}
		if(length>labels.length)
			return null;
		StringBuilder sb=new StringBuilder();
		for(int i=labels.length-length;i<labels.length;i++){
			sb.append(labels[i]);
			if(i<labels.length-1)
				sb.append('.');
		}
		return sb.toString();
	}

	public static boolean isSameRegisteredDomain(String domain1, String domain2){
		return Objects.equals(getRegisteredDomain(domain1), getRegisteredDomain(domain2));
	}

	private record Rule(String[] labels, boolean except){
		public boolean matches(String[] domain){
			if(labels.length>domain.length)
				return false;
			for(int i=0;i<labels.length;i++){
				String ruleLabel=labels[labels.length-i-1];
				String domainLabel=domain[domain.length-i-1];
				if(!domainLabel.equals(ruleLabel) && !"*".equals(ruleLabel))
					return false;
			}
			return true;
		}

		public int priority(){
			int length=labels.length;
			if(except)
				length*=100;
			return length;
		}
	}
}
