package smithereen.debug;

public class DebugLog{
	private static final ThreadLocal<DebugLog> threadLocal=new ThreadLocal<>();

	public long startTime;
	public long routeMatchTime;
	public long totalDatabaseQueryDuration;
	public int numDatabaseQueries;

	public static DebugLog get(){
		DebugLog dl=threadLocal.get();
		if(dl==null)
			threadLocal.set(dl=new DebugLog());
		return dl;
	}

	public void start(){
		routeMatchTime=startTime=System.nanoTime();
	}

	public void setRouteMatched(){
		routeMatchTime=System.nanoTime();
	}

	public long getDuration(){
		return System.nanoTime()-startTime;
	}

	public void logDatabaseQuery(String query, long durationNanos){
		totalDatabaseQueryDuration+=durationNanos;
		numDatabaseQueries++;
	}
}
