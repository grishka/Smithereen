package smithereen.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.model.StatsPoint;
import smithereen.model.StatsType;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.StatsStorage;
import smithereen.util.BackgroundTaskRunner;

public class StatsController{
	private static final Logger LOG=LoggerFactory.getLogger(StatsController.class);

	private final ApplicationContext context;

	public StatsController(ApplicationContext context){
		this.context=context;
	}

	public void incrementDaily(StatsType type, int objectID){
		// TODO batch stats events somehow? Flush them to the DB every N minutes in one query?
		LocalDate now=LocalDate.now(ZoneId.systemDefault());
		BackgroundTaskRunner.getInstance().submit(()->{
			try{
				StatsStorage.incrementDaily(type, objectID, now);
			}catch(SQLException x){
				LOG.error("Error incrementing stats {} object {}", type, objectID, x);
			}
		});
	}

	public List<StatsPoint> getDaily(StatsType type, int objectID){
		try{
			return StatsStorage.getDaily(type, objectID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
