package smithereen.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Mailer;
import smithereen.SmithereenApplication;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.Account;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.ServerRule;
import smithereen.model.admin.ActorStaffNote;
import smithereen.model.admin.AdminNotifications;
import smithereen.model.admin.AuditLogEntry;
import smithereen.model.admin.EmailDomainBlockRule;
import smithereen.model.admin.EmailDomainBlockRuleFull;
import smithereen.model.FederationRestriction;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.admin.IPBlockRule;
import smithereen.model.admin.IPBlockRuleFull;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OtherSession;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.photos.Photo;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.Server;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.UserPermissions;
import smithereen.model.admin.UserRole;
import smithereen.model.admin.ViolationReport;
import smithereen.model.admin.ViolationReportAction;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.reports.ReportedComment;
import smithereen.model.viewmodel.AdminUserViewModel;
import smithereen.model.viewmodel.UserRoleViewModel;
import smithereen.storage.FederationStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.ModerationStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.text.TextProcessor;
import smithereen.util.InetAddressRange;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.utils.StringUtils;

public class ModerationController{
	private static final Logger LOG=LoggerFactory.getLogger(ModerationController.class);
	private static final int REPORT_FILE_RETENTION_DAYS=7;

	private final ApplicationContext context;
	private final LruCache<String, Server> serversByDomainCache=new LruCache<>(500);
	private List<EmailDomainBlockRule> emailDomainRules;
	private List<IPBlockRule> ipRules;
	private List<ServerRule> serverRules;

	public ModerationController(ApplicationContext context){
		this.context=context;
	}

	// region Reporting

	public void createViolationReport(User self, Actor target, @Nullable List<ReportableContentObject> content, String comment, boolean forward){
		int reportID=createViolationReportInternal(self, target, content, comment, null);
		if(forward && (target instanceof ForeignGroup || target instanceof ForeignUser)){
			ArrayList<URI> objectIDs=new ArrayList<>();
			objectIDs.add(target.activityPubID);
			if(content instanceof ActivityPubRepresentable apr)
				objectIDs.add(apr.getActivityPubID());
			context.getActivityPubWorker().sendViolationReport(reportID, comment, objectIDs, target);
		}
	}

	public void createViolationReport(@Nullable User self, Actor target, @Nullable List<ReportableContentObject> content, String comment, String otherServerDomain){
		createViolationReportInternal(self, target, content, comment, otherServerDomain);
	}

	private int createViolationReportInternal(@Nullable User self, Actor target, @Nullable List<ReportableContentObject> content, String comment, String otherServerDomain){
		try{
			int targetID=target.getOwnerID();

			HashSet<Long> contentFileIDs=new HashSet<>();
			JsonArray contentJson;
			if(content!=null && !content.isEmpty()){
				JsonArrayBuilder ab=new JsonArrayBuilder();
				for(ReportableContentObject obj:content){
					JsonObject jo=obj.serializeForReport(targetID, contentFileIDs);
					if(jo!=null){
						if(obj instanceof Comment c && c.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC){
							BoardTopic topic=context.getBoardController().getTopicIgnoringPrivacy(c.parentObjectID.id());
							jo.addProperty("topicTitle", topic.title);
							if(topic.firstCommentID==c.id)
								jo.addProperty("isFirst", true);
						}
						ab.add(jo);
					}
				}
				contentJson=ab.build();
			}else{
				contentJson=null;
			}

			int id=ModerationStorage.createViolationReport(self!=null ? self.id : 0, targetID, comment, otherServerDomain, contentJson==null ? null : contentJson.toString(), !contentFileIDs.isEmpty());
			updateReportsCounter();
			for(long fid: contentFileIDs){
				// ownerID set to 0 because reports aren't owned by any particular actor
				// and referenced files should stick around regardless of any account deletions
				MediaStorage.createMediaFileReference(fid, id, MediaFileReferenceType.REPORT_OBJECT, 0);
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

	public PaginatedList<ViolationReport> getViolationReportsOfActor(Actor actor, int offset, int count){
		try{
			return ModerationStorage.getViolationReportsOfActor(actor.getLocalID(), offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<ViolationReport> getViolationReportsByUser(User user, int offset, int count){
		try{
			return ModerationStorage.getViolationReportsByUser(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ViolationReport getViolationReportByID(int id, boolean needFiles){
		try{
			ViolationReport report=ModerationStorage.getViolationReportByID(id);
			if(report==null)
				throw new ObjectNotFoundException();
			if(needFiles && !report.content.isEmpty()){
				HashSet<LocalImage> localImages=new HashSet<>();
				List<Photo> photos=new ArrayList<>();
				for(ReportableContentObject rco: report.content){
					List<ActivityPubObject> attachments=switch(rco){
						case Post p -> p.getAttachments();
						case MailMessage m -> m.getAttachments();
						case Photo p -> {
							photos.add(p);
							yield null;
						}
						case Comment c -> c.getAttachments();
					};
					if(attachments==null)
						continue;
					for(ActivityPubObject att: attachments){
						if(att instanceof LocalImage li){
							localImages.add(li);
						}
					}
				}
				Set<Long> fileIDs=localImages.stream().map(li->li.fileID).collect(Collectors.toSet());
				if(!fileIDs.isEmpty()){
					Map<Long, MediaFileRecord> files=MediaStorage.getMediaFileRecords(fileIDs);
					for(LocalImage li: localImages){
						MediaFileRecord mfr=files.get(li.fileID);
						if(mfr!=null)
							li.fillIn(mfr);
					}
				}
				if(!photos.isEmpty()){
					PhotoStorage.postprocessPhotos(photos);
				}
			}
			return report;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static void deleteResolvedViolationReportFiles(){
		try{
			List<ViolationReport> reports=ModerationStorage.getResolvedViolationReportsWithFiles();
			if(reports.isEmpty())
				return;
			Map<Integer, Instant> lastActions=ModerationStorage.getViolationReportLastActionTimes(reports.stream().map(r->r.id).toList());
			Instant deleteBefore=Instant.now().plus(REPORT_FILE_RETENTION_DAYS, ChronoUnit.DAYS);
			for(ViolationReport report:reports){
				Instant lastAction=lastActions.get(report.id);
				if(lastAction!=null && lastAction.isAfter(deleteBefore))
					continue;
				MediaStorage.deleteMediaFileReferences(report.id, MediaFileReferenceType.REPORT_OBJECT);
				ModerationStorage.setViolationReportHasFileRefs(report.id, false);
			}
		}catch(SQLException x){
			LOG.error("Failed to delete file refs for resolved reports", x);
		}
	}

	private void updateReportsCounter(){
		AdminNotifications an=AdminNotifications.getInstance(null);
		if(an!=null){
			an.openReportsCount=getViolationReportsCount(true);
		}
	}

	public List<ViolationReportAction> getViolationReportActions(ViolationReport report){
		try{
			return ModerationStorage.getViolationReportActions(report.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void rejectViolationReport(ViolationReport report, User self){
		try{
			if(report.state!=ViolationReport.State.OPEN)
				throw new IllegalArgumentException("Report is not open");
			ModerationStorage.setViolationReportState(report.id, ViolationReport.State.CLOSED_REJECTED);
			ModerationStorage.createViolationReportAction(report.id, self.id, ViolationReportAction.ActionType.RESOLVE_REJECT, null, null);
			updateReportsCounter();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void markViolationReportUnresolved(ViolationReport report, User self){
		try{
			if(report.state==ViolationReport.State.OPEN)
				throw new IllegalArgumentException("Report is already open");
			ModerationStorage.setViolationReportState(report.id, ViolationReport.State.OPEN);
			ModerationStorage.createViolationReportAction(report.id, self.id, ViolationReportAction.ActionType.REOPEN, null, null);
			updateReportsCounter();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void addViolationReportComment(ViolationReport report, User self, String commentText){
		try{
			ModerationStorage.createViolationReportAction(report.id, self.id, ViolationReportAction.ActionType.COMMENT, TextProcessor.preprocessPostHTML(commentText, null), null);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void resolveViolationReport(ViolationReport report, User self, UserBanStatus status, UserBanInfo banInfo){
		try{
			if(report.state!=ViolationReport.State.OPEN)
				throw new IllegalArgumentException("Report is not open");
			ModerationStorage.setViolationReportState(report.id, ViolationReport.State.CLOSED_ACTION_TAKEN);
			HashMap<String, Object> extra=new HashMap<>();
			extra.put("status", status);
			if(banInfo!=null){
				if(banInfo.expiresAt()!=null)
					extra.put("expiresAt", banInfo.expiresAt().toEpochMilli());
				if(StringUtils.isNotEmpty(banInfo.message()))
					extra.put("message", banInfo.message());
			}
			ModerationStorage.createViolationReportAction(report.id, self.id, ViolationReportAction.ActionType.RESOLVE_WITH_ACTION, null, Utils.gson.toJsonTree(extra).getAsJsonObject());
			updateReportsCounter();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteViolationReportContent(ViolationReport report, SessionInfo session, boolean markResolved){
		if(report.content==null)
			return;
		if(report.state!=ViolationReport.State.OPEN)
			throw new IllegalArgumentException("Report is not open");
		try{
			boolean actuallyDeletedAnything=false;
			for(ReportableContentObject rco: report.content){
				switch(rco){
					case Post post -> {
						try{
							context.getWallController().getPostOrThrow(post.id);
						}catch(ObjectNotFoundException x){
							LOG.debug("Post {} already deleted", post.id);
							continue;
						}
						context.getWallController().deletePostAsServerModerator(session.account.user, post);
					}
					case MailMessage msg -> {
						if(context.getMailController().getMessagesAsModerator(Set.of(XTEA.obfuscateObjectID(msg.id, ObfuscatedObjectIDType.MAIL_MESSAGE))).isEmpty()){
							LOG.debug("Message {} already deleted", msg.id);
							continue;
						}
						User sender=context.getUsersController().getUserOrThrow(msg.senderID);
						context.getMailController().actuallyDeleteMessage(sender, msg, true);
					}
					case Photo photo -> {
						if(photo.ownerID>0){
							context.getPhotosController().deletePhoto(context.getUsersController().getUserOrThrow(photo.ownerID), photo);
						}else{
							context.getPhotosController().deletePhoto(context.getGroupsController().getGroupOrThrow(-photo.ownerID), photo);
						}
					}
					case Comment comment -> {
						if(comment instanceof ReportedComment rc && rc.isFirstInTopic){
							try{
								BoardTopic topic=context.getBoardController().getTopicIgnoringPrivacy(comment.parentObjectID.id());
								context.getBoardController().deleteTopicWithFederation(context.getGroupsController().getGroupOrThrow(topic.groupID), topic);
							}catch(ObjectNotFoundException x){
								LOG.debug("Board topic {} already deleted", rc.parentObjectID.id());
								continue;
							}
						}else{
							context.getCommentsController().deleteComment(context.getWallController().getContentAuthorAndOwner(comment).owner(), comment);
						}
					}
				}
				actuallyDeletedAnything=true;
			}
			if(actuallyDeletedAnything){
				MediaStorage.deleteMediaFileReferences(report.id, MediaFileReferenceType.REPORT_OBJECT);
				ModerationStorage.createViolationReportAction(report.id, session.account.user.id, ViolationReportAction.ActionType.DELETE_CONTENT, null, null);
				if(markResolved){
					ModerationStorage.setViolationReportState(report.id, ViolationReport.State.CLOSED_ACTION_TAKEN);
					updateReportsCounter();
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Federation & federation restrictions

	public PaginatedList<Server> getAllServers(int offset, int count, @Nullable Server.Availability availability, boolean onlyRestricted, String query){
		try{
			if(StringUtils.isNotEmpty(query)){
				query=Utils.convertIdnToAsciiIfNeeded(query);
			}
			return FederationStorage.getAllServers(offset, count, availability, onlyRestricted, query);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Server getServerByDomain(String domain){
		domain=domain.toLowerCase();
		Server server=serversByDomainCache.get(domain);
		if(server!=null)
			return server;

		try{
			server=FederationStorage.getServerByDomain(domain);
			if(server==null)
				throw new ObjectNotFoundException();
			serversByDomainCache.put(domain, server);
			return server;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setServerRestriction(Server server, FederationRestriction restriction){
		try{
			ModerationStorage.setServerRestriction(server.id(), restriction!=null ? Utils.gson.toJson(restriction) : null);
			serversByDomainCache.remove(server.host());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Server getOrAddServer(String domain){
		domain=domain.toLowerCase();
		Server server=serversByDomainCache.get(domain);
		if(server!=null)
			return server;

		try{
			server=FederationStorage.getServerByDomain(domain);
			if(server==null){
				int id=FederationStorage.addServer(domain);
				server=new Server(id, domain, null, null, Instant.now(), null, 0, true, null, EnumSet.noneOf(Server.Feature.class));
			}
			serversByDomainCache.put(domain, server);
			return server;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void addServerFeatures(String domain, EnumSet<Server.Feature> features){
		Server server=getOrAddServer(domain);
		if(server.features().containsAll(features))
			return;
		server.features().addAll(features);
		try{
			FederationStorage.setServerFeatures(server.id(), server.features());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void resetServerAvailability(Server server){
		try{
			FederationStorage.setServerAvailability(server.id(), null, 0, true);
			serversByDomainCache.remove(server.host());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void recordFederationFailure(Server server){
		try{
			LocalDate today=LocalDate.now(ZoneId.systemDefault());
			if(!today.equals(server.lastErrorDay())){
				int dayCount=server.errorDayCount()+1;
				FederationStorage.setServerAvailability(server.id(), today, dayCount, dayCount<7);
				serversByDomainCache.remove(server.host());
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Account roles

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

	// endregion
	// region Audit log

	public PaginatedList<AuditLogEntry> getGlobalAuditLog(int offset, int count){
		try{
			return ModerationStorage.getGlobalAuditLog(offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<AuditLogEntry> getUserAuditLog(User user, int offset, int count){
		try{
			return ModerationStorage.getUserAuditLog(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region User management

	public PaginatedList<AdminUserViewModel> getAllUsers(int offset, int count, String query, Boolean localOnly, String emailDomain, String ipSubnet, int roleID, UserBanStatus banStatus, boolean remoteSuspended){
		try{
			InetAddressRange subnet=ipSubnet!=null ? InetAddressRange.parse(ipSubnet) : null;
			return ModerationStorage.getUsers(query, localOnly, emailDomain, subnet, roleID, banStatus, remoteSuspended, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, Account> getAccounts(Collection<Integer> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			return UserStorage.getAccounts(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setUserEmail(User self, Account account, String newEmail){
		try{
			String oldEmail=account.email;
			SessionStorage.updateActivationInfo(account.id, null);
			SessionStorage.updateEmail(account.id, newEmail);
			account.email=newEmail;
			account.activationInfo=null;
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.SET_USER_EMAIL, account.user.id, 0, null, Map.of("oldEmail", oldEmail, "newEmail", newEmail));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void terminateUserSession(User self, Account account, OtherSession session){
		try{
			SessionStorage.deleteSession(account.id, session.fullID());
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.END_USER_SESSION, account.user.id, 0, null, Map.of("ip", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeInetAddress(session.ip()))));
			SmithereenApplication.invalidateAllSessionsForAccount(account.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setUserBanStatus(User self, User target, Account targetAccount, UserBanStatus status, UserBanInfo info){
		try{
			if(self.id==target.id && status!=UserBanStatus.NONE)
				throw new UserErrorException("You can't ban yourself");
			UserBanStatus prevStatus=target.banStatus;
			UserStorage.setUserBanStatus(target, targetAccount, status, status!=UserBanStatus.NONE ? Utils.gson.toJson(info) : null);
			target.banStatus=status;
			target.banInfo=info;
			HashMap<String, Object> auditLogArgs=new HashMap<>();
			auditLogArgs.put("status", status);
			if(info!=null){
				if(info.expiresAt()!=null)
					auditLogArgs.put("expiresAt", info.expiresAt().toEpochMilli());
				if(StringUtils.isNotEmpty(info.message()))
					auditLogArgs.put("message", info.message());
				if(info.reportID()>0)
					auditLogArgs.put("report", info.reportID());
			}
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.BAN_USER, target.id, 0, null, auditLogArgs);
			if(!(target instanceof ForeignUser) && (status==UserBanStatus.FROZEN || status==UserBanStatus.SUSPENDED)){
				Account account=SessionStorage.getAccountByUserID(target.id);
				Mailer.getInstance().sendAccountBanNotification(account, status, info);
			}
			if(!(target instanceof ForeignUser) && (status==UserBanStatus.SUSPENDED || prevStatus==UserBanStatus.SUSPENDED)){
				context.getActivityPubWorker().sendUpdateUserActivity(target);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void clearUserBanStatus(Account self){
		try{
			UserStorage.setUserBanStatus(self.user, self, UserBanStatus.NONE, null);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region User staff notes

	public int getUserStaffNoteCount(User user){
		try{
			return ModerationStorage.getUserStaffNoteCount(user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<ActorStaffNote> getUserStaffNotes(User user, int offset, int count){
		try{
			return ModerationStorage.getUserStaffNotes(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void createUserStaffNote(User self, User target, String text){
		try{
			ModerationStorage.createUserStaffNote(target.id, self.id, TextProcessor.preprocessPostHTML(text, null));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteUserStaffNote(ActorStaffNote note){
		try{
			ModerationStorage.deleteUserStaffNote(note.id());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ActorStaffNote getUserStaffNoteOrThrow(int id){
		try{
			ActorStaffNote note=ModerationStorage.getUserStaffNote(id);
			if(note==null)
				throw new ObjectNotFoundException();
			return note;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Email blocking

	public List<EmailDomainBlockRule> getEmailDomainBlockRules(){
		try{
			if(emailDomainRules!=null)
				return emailDomainRules;
			emailDomainRules=Collections.unmodifiableList(ModerationStorage.getEmailDomainBlockRules());
			return emailDomainRules;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void reloadEmailDomainBlockCache() throws SQLException{
		emailDomainRules=Collections.unmodifiableList(ModerationStorage.getEmailDomainBlockRules());
	}

	private String normalizeDomain(String domain){
		return Utils.convertIdnToAsciiIfNeeded(domain).toLowerCase();
	}

	public void createEmailDomainBlockRule(User self, String domain, EmailDomainBlockRule.Action action, String note){
		try{
			domain=normalizeDomain(domain);
			EmailDomainBlockRuleFull rule=ModerationStorage.getEmailDomainBlockRuleFull(domain);
			if(rule!=null)
				throw new UserErrorException("err_admin_email_rule_already_exists");
			ModerationStorage.createEmailDomainBlockRule(domain, action, note, self.id);
			reloadEmailDomainBlockCache();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.CREATE_EMAIL_DOMAIN_RULE, 0, 0, null, Map.of("domain", domain, "action", action.toString()));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteEmailDomainBlockRule(User self, EmailDomainBlockRuleFull rule){
		try{
			ModerationStorage.deleteEmailDomainBlockRule(normalizeDomain(rule.rule().domain()));
			reloadEmailDomainBlockCache();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.DELETE_EMAIL_DOMAIN_RULE, 0, 0, null, Map.of("domain", rule.rule().domain(), "action", rule.rule().action().toString()));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateEmailDomainBlockRule(User self, EmailDomainBlockRuleFull rule, EmailDomainBlockRule.Action action, String note){
		try{
			if(action==rule.rule().action() && Objects.equals(rule.note(), note))
				return;
			ModerationStorage.updateEmailDomainBlockRule(rule.rule().domain(), action, note);
			if(action!=rule.rule().action()){
				ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.UPDATE_EMAIL_DOMAIN_RULE, 0, 0, null,
						Map.of("domain", rule.rule().domain(), "oldAction", rule.rule().action().toString(), "newAction", action.toString()));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public EmailDomainBlockRule matchEmailDomainBlockRule(String email){
		if(!Utils.isValidEmail(email))
			throw new IllegalArgumentException("'"+email+"' is not a valid email");
		String domain=normalizeDomain(email.split("@", 2)[1]);
		List<EmailDomainBlockRule> rules=getEmailDomainBlockRules();
		for(EmailDomainBlockRule rule: rules){
			if(rule.matches(domain))
				return rule;
		}
		return null;
	}

	public List<EmailDomainBlockRuleFull> getEmailDomainBlockRulesFull(){
		try{
			return ModerationStorage.getEmailDomainBlockRulesFull();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public EmailDomainBlockRuleFull getEmailDomainBlockRuleOrThrow(String domain){
		domain=normalizeDomain(domain);
		try{
			EmailDomainBlockRuleFull rule=ModerationStorage.getEmailDomainBlockRuleFull(domain);
			if(rule==null)
				throw new ObjectNotFoundException();
			return rule;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region IP blocking

	public List<IPBlockRule> getIPBlockRules(){
		try{
			if(ipRules!=null)
				return ipRules;
			ipRules=Collections.unmodifiableList(ModerationStorage.getIPBlockRules());
			return ipRules;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void reloadIpBlockCache() throws SQLException{
		ipRules=Collections.unmodifiableList(ModerationStorage.getIPBlockRules());
	}

	public void createIPBlockRule(User self, InetAddressRange addressRange, IPBlockRule.Action action, int expiryMinutes, String note){
		try{
			Instant expiry=Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
			ModerationStorage.createIPBlockRule(addressRange, action, expiry, note, self.id);
			reloadIpBlockCache();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.CREATE_IP_RULE, 0, 0, null,
					Map.of("addr", addressRange.toString(), "expiry", expiry.getEpochSecond(), "action", action.toString()));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteIPBlockRule(User self, IPBlockRuleFull rule){
		try{
			ModerationStorage.deleteIPBlockRule(rule.rule().id());
			reloadIpBlockCache();
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.DELETE_IP_RULE, 0, 0, null,
					Map.of("addr", rule.rule().ipRange().toString(), "expiry", rule.rule().expiresAt().getEpochSecond(), "action", rule.rule().action().toString()));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateIPBlockRule(User self, IPBlockRuleFull rule, IPBlockRule.Action action, int newExpiryMinutes, String note){
		try{
			Instant newExpiry=newExpiryMinutes>0 ? Instant.now().plus(newExpiryMinutes, ChronoUnit.MINUTES) : rule.rule().expiresAt();
			HashMap<String, Object> auditLogArgs=new HashMap<>();
			if(newExpiryMinutes!=0){
				auditLogArgs.put("oldExpiry", rule.rule().expiresAt().getEpochSecond());
				auditLogArgs.put("newExpiry", newExpiry.getEpochSecond());
			}
			if(action!=rule.rule().action()){
				auditLogArgs.put("oldRule", rule.rule().action().toString());
				auditLogArgs.put("newRule", rule.toString());
			}
			ModerationStorage.updateIPBlockRule(rule.rule().id(), action, newExpiry, note);
			reloadIpBlockCache();
			if(!auditLogArgs.isEmpty()){
				auditLogArgs.put("addr", rule.rule().ipRange().toString());
				ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.UPDATE_IP_RULE, 0, 0, null, auditLogArgs);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public IPBlockRule matchIPBlockRule(InetAddress ip){
		List<IPBlockRule> rules=getIPBlockRules();
		Instant now=Instant.now();
		for(IPBlockRule rule: rules){
			if(rule.ipRange().contains(ip) && rule.expiresAt().isAfter(now))
				return rule;
		}
		return null;
	}

	public List<IPBlockRuleFull> getIPBlockRulesFull(){
		try{
			return ModerationStorage.getIPBlockRulesFull();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public IPBlockRuleFull getIPBlockRuleFull(int id){
		try{
			IPBlockRuleFull rule=ModerationStorage.getIPBlockRuleFull(id);
			if(rule==null)
				throw new ObjectNotFoundException();
			return rule;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Signup invite management

	public Config.SignupMode getEffectiveSignupMode(Request req){
		InetAddress ip=Utils.getRequestIP(req);
		IPBlockRule rule=matchIPBlockRule(ip);
		if(rule!=null){
			if(rule.action()==IPBlockRule.Action.MANUAL_REVIEW_SIGNUPS && Config.signupMode==Config.SignupMode.OPEN){
				return Config.SignupMode.MANUAL_APPROVAL;
			}else if(rule.action()==IPBlockRule.Action.BLOCK_SIGNUPS){
				return Config.SignupMode.CLOSED;
			}
		}
		return Config.signupMode;
	}

	public PaginatedList<SignupInvitation> getAllSignupInvites(int offset, int count){
		try{
			return ModerationStorage.getAllSignupInvites(offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteSignupInvite(User self, int id){
		try{
			SignupInvitation invite=context.getUsersController().getInvite(id);
			if(invite==null)
				throw new ObjectNotFoundException();
			SessionStorage.deleteInvitation(id);
			Map<String, Object> auditLogArgs=new HashMap<>();
			auditLogArgs.put("signups", invite.signupsRemaining);
			if(StringUtils.isNotEmpty(invite.email))
				auditLogArgs.put("email", invite.email);
			if(StringUtils.isNotEmpty(invite.firstName))
				auditLogArgs.put("name", invite.firstName+" "+invite.lastName);
			ModerationStorage.createAuditLogEntry(self.id, AuditLogEntry.Action.DELETE_SIGNUP_INVITE, invite.ownerID, invite.id, AuditLogEntry.ObjectType.SIGNUP_INVITE, auditLogArgs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Server rules

	public List<ServerRule> getServerRules(){
		if(serverRules==null){
			try{
				serverRules=Collections.unmodifiableList(ModerationStorage.getServerRules(false));
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
		return serverRules;
	}

	private void invalidateServerRuleCache(){
		serverRules=null;
	}

	public void createServerRule(User admin, String title, String description, int priority, Map<String, ServerRule.Translation> translations){
		try{
			String translationsJson=Utils.gson.toJson(translations);
			ModerationStorage.createServerRule(title, description, priority, translationsJson);
			ModerationStorage.createAuditLogEntry(admin.id, AuditLogEntry.Action.CREATE_SERVER_RULE, 0, 0, null, Map.of(
					"title", title,
					"description", description,
					"priority", priority,
					"translations", translationsJson
			));
			invalidateServerRuleCache();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateServerRule(User admin, ServerRule rule, String title, String description, int priority, Map<String, ServerRule.Translation> translations){
		if(rule.title().equals(title) && rule.description().equals(description) && rule.priority()==priority && rule.translations().equals(translations))
			return;
		try{
			String translationsJson=Utils.gson.toJson(translations);
			ModerationStorage.updateServerRule(rule.id(), title, description, priority, translationsJson);
			ModerationStorage.createAuditLogEntry(admin.id, AuditLogEntry.Action.UPDATE_SERVER_RULE, 0, 0, null, Map.of(
					"newTitle", title,
					"newDescription", description,
					"newPriority", priority+"",
					"newTranslations", translationsJson,

					"oldTitle", rule.title(),
					"oldDescription", rule.description(),
					"oldPriority", rule.priority(),
					"oldTranslations", Utils.gson.toJson(rule.translations())
			));
			invalidateServerRuleCache();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ServerRule getServerRuleByID(int id){
		try{
			List<ServerRule> rules=ModerationStorage.getServerRulesByIDs(List.of(id));
			if(rules.isEmpty() || rules.getFirst().isDeleted())
				throw new ObjectNotFoundException();
			return rules.getFirst();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteServerRule(User admin, ServerRule rule){
		try{
			ModerationStorage.deleteServerRule(rule.id());
			ModerationStorage.createAuditLogEntry(admin.id, AuditLogEntry.Action.DELETE_SERVER_RULE, 0, 0, null, Map.of(
					"title", rule.title(),
					"description", rule.description(),
					"priority", rule.priority(),
					"translations", Utils.gson.toJson(rule.translations())
			));
			invalidateServerRuleCache();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
}
