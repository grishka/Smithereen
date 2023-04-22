package smithereen.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.data.StatsPoint;
import smithereen.data.StatsType;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.StatsStorage;

public class StatsController{
	private static final Logger LOG=LoggerFactory.getLogger(StatsController.class);

	private final ApplicationContext context;

	public StatsController(ApplicationContext context){
		this.context=context;
	}

	public void incrementDaily(StatsType type, int objectID){
		try{
			StatsStorage.incrementDaily(type, objectID, LocalDate.now(ZoneId.systemDefault()));
		}catch(SQLException x){
			LOG.error("Error incrementing stats {} object {}", type, objectID, x);
		}
	}

	public List<StatsPoint> getDaily(StatsType type, int objectID){
		try{
			return StatsStorage.getDaily(type, objectID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
