package smithereen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;

public class TopLevelDomainList{
	private static final Logger LOG=LoggerFactory.getLogger(TopLevelDomainList.class);

	private static Set<String> list=Collections.emptySet();
	public static long lastUpdatedTime;

	public static void updateIfNeeded(){
		if(System.currentTimeMillis()-lastUpdatedTime>3600_000L*24*14){ // update once every 14 days
			BackgroundTaskRunner.getInstance().submit(()->{
				try{
					HttpRequest req=HttpRequest.newBuilder(URI.create("https://data.iana.org/TLD/tlds-alpha-by-domain.txt"))
							.timeout(Duration.ofSeconds(30))
							.build();
					HttpResponse<String> resp=ActivityPub.httpClient.send(req, HttpResponse.BodyHandlers.ofString());
					if(resp.statusCode()/100==2){
						String file=resp.body();
						update(file);
						Config.updateInDatabase(Map.of("TLDList_LastUpdated", (lastUpdatedTime=System.currentTimeMillis())+"", "TLDList_Data", file));
					}
				}catch(IOException|SQLException|InterruptedException x){
					LOG.warn("Error loading IANA TLD list", x);
				}
			});
		}
	}

	public static void update(String file){
		list=Arrays.stream(file.split("\n")).filter(s->!s.startsWith("#")).map(String::trim).collect(Collectors.toSet());
	}

	public static boolean contains(String tld){
		try{
			return tld!=null && list.contains(Utils.convertIdnToAsciiIfNeeded(tld).toUpperCase());
		}catch(IllegalArgumentException x){
			return false;
		}
	}
}
