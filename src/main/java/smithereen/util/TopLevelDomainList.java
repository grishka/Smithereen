package smithereen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;

public class TopLevelDomainList{
	private static final Logger LOG=LoggerFactory.getLogger(TopLevelDomainList.class);

	private static List<String> list=Collections.emptyList();
	public static long lastUpdatedTime;

	public static void updateIfNeeded(){
		if(System.currentTimeMillis()-lastUpdatedTime>3600_000L*24*14){ // update once every 14 days
			BackgroundTaskRunner.getInstance().submit(new Runnable(){
				@Override
				public void run(){
					try{
						Request req=new Request.Builder()
								.url("https://data.iana.org/TLD/tlds-alpha-by-domain.txt")
								.build();
						Call call=ActivityPub.httpClient.newCall(req);
						try(Response resp=call.execute()){
							if(resp.isSuccessful()){
								String file=resp.body().string();
								update(file);
								Config.updateInDatabase(Map.of("TLDList_LastUpdated", (lastUpdatedTime=System.currentTimeMillis())+"", "TLDList_Data", file));
							}
						}
					}catch(IOException|SQLException x){
						LOG.warn("Error loading IANA TLD list", x);
					}
				}
			});
		}
	}

	public static void update(String file){
		list=Arrays.stream(file.split("\n")).filter(s->!s.startsWith("#")).map(String::trim).collect(Collectors.toList());
	}

	public static boolean contains(String tld){
		try{
			return tld!=null && list.contains(Utils.convertIdnToAsciiIfNeeded(tld).toUpperCase());
		}catch(IllegalArgumentException x){
			return false;
		}
	}
}
