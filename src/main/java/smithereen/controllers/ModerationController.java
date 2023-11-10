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
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.Account;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.AdminNotifications;
import smithereen.model.FederationRestriction;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.User;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.ModerationStorage;
import smithereen.storage.UserStorage;
import smithereen.util.XTEA;

public class ModerationController{
	private static final Logger LOG=LoggerFactory.getLogger(ModerationController.class);

	private final ApplicationContext context;
	private final LruCache<String, Server> serversByDomainCache=new LruCache<>(500);

	public ModerationController(ApplicationContext context){
		this.context=context;
	}

	public void createViolationReport(User self, Actor target, @Nullable Object content, String comment, boolean forward){
		int reportID=createViolationReportInternal(self, target, content, comment, null);
		if(forward && (target instanceof ForeignGroup || target instanceof ForeignUser)){
			ArrayList<URI> objectIDs=new ArrayList<>();
			objectIDs.add(target.activityPubID);
			if(content instanceof ActivityPubRepresentable apr)
				objectIDs.add(apr.getActivityPubID());
			context.getActivityPubWorker().sendViolationReport(reportID, comment, objectIDs, target);
		}
	}

	public void createViolationReport(@Nullable User self, Actor target, @Nullable Object content, String comment, String otherServerDomain){
		createViolationReportInternal(self, target, content, comment, otherServerDomain);
	}

	private int createViolationReportInternal(@Nullable User self, Actor target, @Nullable Object content, String comment, String otherServerDomain){
		try{
			ViolationReport.TargetType targetType;
			ViolationReport.ContentType contentType;
			int targetID;
			long contentID;
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
			}else if(content instanceof MailMessage msg){
				contentType=ViolationReport.ContentType.MESSAGE;
				contentID=XTEA.deobfuscateObjectID(msg.id, ObfuscatedObjectIDType.MAIL_MESSAGE);
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
			AdminNotifications an=AdminNotifications.getInstance(null);
			if(an!=null){
				an.openReportsCount=getViolationReportsCount(true);
			}
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

	public void setAccountRole(Account self, Account account, int roleID){
		UserRole ownRole=Config.userRoles.get(self.roleID);
		UserRole targetRole=null;
		if(roleID>0){
			targetRole=Config.userRoles.get(roleID);
			if(targetRole==null)
				throw new BadRequestException();
		}
		// If not an owner and the user already has a role, can only change roles for someone you promoted yourself
		if(account.roleID>0 && !ownRole.permissions().contains(UserRole.Permission.SUPERUSER)){
			if(account.promotedBy!=self.id)
				throw new UserActionNotAllowedException();
		}
		// Can only assign one's own role or a lesser one
		if(targetRole!=null && !ownRole.permissions().contains(UserRole.Permission.SUPERUSER) && !ownRole.permissions().containsAll(targetRole.permissions()))
			throw new UserActionNotAllowedException();
		try{
			UserStorage.setAccountRole(account, roleID, targetRole==null ? 0 : self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
