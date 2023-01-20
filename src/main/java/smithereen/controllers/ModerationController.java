package smithereen.controllers;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.data.AdminNotifications;
import smithereen.data.FederationRestriction;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.Post;
import smithereen.data.Server;
import smithereen.data.User;
import smithereen.data.ViolationReport;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.ModerationStorage;

public class ModerationController{
	private static final Logger LOG=LoggerFactory.getLogger(ModerationController.class);

	private final ApplicationContext context;
	private final LruCache<String, Server> serversByDomainCache=new LruCache<>(500);

	public ModerationController(ApplicationContext context){
		this.context=context;
	}

	public void createViolationReport(User self, Actor target, @Nullable ActivityPubObject content, String comment, boolean forward){
		int reportID=createViolationReportInternal(self, target, content, comment, null);
		if(forward && (target instanceof ForeignGroup || target instanceof ForeignUser)){
			ArrayList<URI> objectIDs=new ArrayList<>();
			objectIDs.add(target.activityPubID);
			if(content!=null)
				objectIDs.add(content.activityPubID);
			context.getActivityPubWorker().sendViolationReport(reportID, comment, objectIDs, target);
		}
	}

	public void createViolationReport(@Nullable User self, Actor target, @Nullable ActivityPubObject content, String comment, String otherServerDomain){
		createViolationReportInternal(self, target, content, comment, otherServerDomain);
	}

	private int createViolationReportInternal(@Nullable User self, Actor target, @Nullable ActivityPubObject content, String comment, String otherServerDomain){
		try{
			ViolationReport.TargetType targetType;
			ViolationReport.ContentType contentType;
			int targetID;
			int contentID;
			if(target instanceof User u){
				targetType=ViolationReport.TargetType.USER;
				targetID=u.id;
			}else if(target instanceof Group g){
				targetType=ViolationReport.TargetType.GROUP;
				targetID=g.id;
			}else{
				throw new IllegalArgumentException();
			}

			if(content instanceof Post p){
				contentType=ViolationReport.ContentType.POST;
				contentID=p.id;
			}else{
				contentType=null;
				contentID=0;
			}

			int id=ModerationStorage.createViolationReport(self!=null ? self.id : 0, targetType, targetID, contentType, contentID, comment, otherServerDomain);
			AdminNotifications an=AdminNotifications.getInstance(null);
			if(an!=null){
				an.openReportsCount=getViolationReportsCount(true);
			}
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getViolationReportsCount(boolean open){
		try{
			return ModerationStorage.getViolationReportsCount(open);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<ViolationReport> getViolationReports(boolean open, int offset, int count){
		try{
			return ModerationStorage.getViolationReports(open, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ViolationReport getViolationReportByID(int id){
		try{
			ViolationReport report=ModerationStorage.getViolationReportByID(id);
			if(report==null)
				throw new ObjectNotFoundException();
			return report;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setViolationReportResolved(ViolationReport report, User moderator){
		try{
			ModerationStorage.setViolationReportResolved(report.id, moderator.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Server> getAllServers(int offset, int count, @Nullable Server.Availability availability, boolean onlyRestricted, String query){
		try{
			return ModerationStorage.getAllServers(offset, count, availability, onlyRestricted, query);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Server getServerByDomain(String domain){
		domain=domain.toLowerCase();
		synchronized(serversByDomainCache){
			Server server=serversByDomainCache.get(domain);
			if(server!=null)
				return server;

			try{
				server=ModerationStorage.getServerByDomain(domain);
				if(server==null)
					throw new ObjectNotFoundException();
				serversByDomainCache.put(domain, server);
				return server;
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
	}

	public void setServerRestriction(Server server, FederationRestriction restriction){
		try{
			ModerationStorage.setServerRestriction(server.id(), restriction!=null ? Utils.gson.toJson(restriction) : null);
			synchronized(serversByDomainCache){
				serversByDomainCache.remove(server.host());
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Server getOrAddServer(String domain){
		domain=domain.toLowerCase();
		synchronized(serversByDomainCache){
			Server server=serversByDomainCache.get(domain);
			if(server!=null)
				return server;

			try{
				server=ModerationStorage.getServerByDomain(domain);
				if(server==null){
					int id=ModerationStorage.addServer(domain);
					server=new Server(id, domain, null, null, Instant.now(), null, 0, true, null);
				}
				serversByDomainCache.put(domain, server);
				return server;
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
	}

	public void resetServerAvailability(Server server){
		try{
			ModerationStorage.setServerAvailability(server.id(), null, 0, true);
			synchronized(serversByDomainCache){
				serversByDomainCache.remove(server.host());
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void recordFederationFailure(Server server){
		try{
			LocalDate today=LocalDate.now(ZoneId.systemDefault());
			synchronized(serversByDomainCache){
				if(!today.equals(server.lastErrorDay())){
					int dayCount=server.errorDayCount()+1;
					ModerationStorage.setServerAvailability(server.id(), today, dayCount, dayCount<7);
					serversByDomainCache.remove(server.host());
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
