package smithereen.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MaintenanceScheduler{
	private static ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();

	public static void runDaily(Runnable r){
		executor.scheduleAtFixedRate(r, 0, 1, TimeUnit.DAYS);
	}
}
