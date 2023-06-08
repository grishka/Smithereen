package smithereen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import smithereen.Utils;

public class MaintenanceScheduler{
	private static ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();
	private static final Logger LOG=LoggerFactory.getLogger(MaintenanceScheduler.class);

	public static void runDaily(Runnable r){
		executor.scheduleAtFixedRate(r, 0, 1, TimeUnit.DAYS);
	}

	public static void runPeriodically(Runnable r, long time, TimeUnit unit){
		executor.scheduleAtFixedRate(r, time, time, unit);
	}

	public static void shutDown(){
		LOG.info("Stopping thread");
		Utils.stopExecutorBlocking(executor, LOG);
		LOG.info("Stopped");
	}
}
