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
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.Account;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.AdminNotifications;
import smithereen.model.AuditLogEntry;
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
import smithereen.model.UserPermissions;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.model.viewmodel.UserRoleViewModel;
import smithereen.storage.ModerationStorage;
import smithereen.storage.SessionStorage;
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
			ModerationStorage.createAuditLogEntry(self.user.id, AuditLogEntry.Action.ASSIGN_ROLE, account.user.id, roleID, AuditLogEntry.ObjectType.ROLE, null);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<UserRoleViewModel> getRoles(UserPermissions ownPermissions){
		try{
			Map<Integer, Integer> roleCounts=ModerationStorage.getRoleAccountCounts();
			boolean canEditAll=ownPermissions.hasPermission(UserRole.Permission.SUPERUSER);
			return Config.userRoles.values()
					.stream()
					.sorted(Comparator.comparingInt(UserRole::id))
					.map(r->new UserRoleViewModel(r, roleCounts.getOrDefault(r.id(), 0), canEditAll || ownPermissions.role.permissions().containsAll(r.permissions())))
					.toList();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateRole(User self, UserPermissions ownPermissions, UserRole role, String name, EnumSet<UserRole.Permission> permissions){
		try{
			// Can only use permissions they have themselves
			if(!ownPermissions.hasPermission(UserRole.Permission.SUPERUSER) && !ownPermissions.role.permissions().containsAll(permissions))
				throw new UserActionNotAllowedException();
			// Can't make role #1 not be superuser role
			if(role.id()==1 && !permissions.contains(UserRole.Permission.SUPERUSER))
				throw new UserActionNotAllowedException();
			// Can't change permissions on user's own role but can change settings and name
			if(ownPermissions.role.id()==role.id()){
				EnumSet<UserRole.Permission> actualPermissions=EnumSet.copyOf(permissions);
				actualPermissions.removeIf(UserRole.Permission::isActuallySetting);
				if(!permissions.containsAll(actualPermissions))
					throw new UserActionNotAllowedException();
			}
			if(permissions.isEmpty())
				throw new BadRequestException();
			// Nothing changed
			if(role.name().equals(name) && role.permissions().equals(permissions))
				return;
			ModerationStorage.updateRole(role.id(), name, permissions);
			UserStorage.resetAccountsCache();
			SessionStorage.resetPermissionsCache();
			Config.reloadRoles();

			HashMap<String, Object> extra=new HashMap<>();
			if(!role.name().equals(name)){
				extra.put("oldName", role.name());
				extra.put("newName", name);
			}
			if(!role.permissions().equals(permissions)){
				extra.put("oldPermissions", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeEnumSetToBytes(role.permissions())));
				extra.put("newPermissions", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeEnumSetToBytes(permissions)));
			}
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.EDIT_ROLE, 0, role.id(), AuditLogEntry.ObjectType.ROLE, extra);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public UserRole createRole(User self, UserPermissions ownPermissions, String name, EnumSet<UserRole.Permission> permissions){
		try{
			// Can only use permissions they have themselves
			if(!ownPermissions.hasPermission(UserRole.Permission.SUPERUSER) && !ownPermissions.role.permissions().containsAll(permissions))
				throw new UserActionNotAllowedException();
			if(permissions.isEmpty())
				throw new BadRequestException();
			int id=ModerationStorage.createRole(name, permissions);
			Config.reloadRoles();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.CREATE_ROLE, 0, id, AuditLogEntry.ObjectType.ROLE, Map.of(
					"name", name,
					"permissions", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeEnumSetToBytes(permissions))
			));
			return Config.userRoles.get(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteRole(User self, UserPermissions ownPermissions, UserRole role){
		try{
			// Can only delete roles with same or lesser permissions as their own role
			if(!ownPermissions.hasPermission(UserRole.Permission.SUPERUSER) && !ownPermissions.role.permissions().containsAll(role.permissions()))
				throw new UserActionNotAllowedException();
			// Can't delete the superuser role
			if(role.id()==1)
				throw new UserActionNotAllowedException();
			// Can't delete their own role because that would be a stupid thing to do
			if(role.id()==ownPermissions.role.id())
				throw new UserActionNotAllowedException();
			ModerationStorage.deleteRole(role.id());
			UserStorage.resetAccountsCache();
			SessionStorage.resetPermissionsCache();
			Config.reloadRoles();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.DELETE_ROLE, 0, role.id(), AuditLogEntry.ObjectType.ROLE, Map.of(
					"name", role.name(),
					"permissions", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeEnumSetToBytes(role.permissions()))
			));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<AuditLogEntry> getGlobalAuditLog(int offset, int count){
		try{
			return ModerationStorage.getGlobalAuditLog(offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
