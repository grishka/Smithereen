package smithereen.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Mailer;
import smithereen.SmithereenApplication;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ActorStatus;
import smithereen.model.AuditLogEntry;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.OtherSession;
import smithereen.model.PaginatedList;
import smithereen.model.SignupInvitation;
import smithereen.model.SignupRequest;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.UserPermissions;
import smithereen.model.UserPresence;
import smithereen.model.UserRole;
import smithereen.model.admin.UserActionLogAction;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.friends.FollowRelationship;
import smithereen.model.viewmodel.UserContentMetrics;
import smithereen.model.viewmodel.UserRelationshipMetrics;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.FederationStorage;
import smithereen.storage.ModerationStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.FloodControl;
import smithereen.util.MaintenanceScheduler;
import smithereen.util.NamedMutexCollection;
import spark.Request;
import spark.utils.StringUtils;

public class UsersController{
	private static final int ONLINE_TIMEOUT_MINUTES=5;
	public static final int FOLLOWERS_TRANSFER_COOLDOWN_DAYS=30;
	private static final Logger LOG=LoggerFactory.getLogger(UsersController.class);
	private final ApplicationContext context;
	private final ConcurrentSkipListSet<CachedUserPresence> onlineLocalUsers=new ConcurrentSkipListSet<>(Comparator.comparing(p->p.presence.lastUpdated()));
	private final ConcurrentHashMap<Integer, CachedUserPresence> onlineLocalUsersByID=new ConcurrentHashMap<>();
	private final NamedMutexCollection onlineMutexes=new NamedMutexCollection();
	private Map<Integer, UserPresence> pendingUserPresenceUpdates=Collections.synchronizedMap(new HashMap<>());
	private final HashSet<Integer> movingUsers=new HashSet<>();

	public UsersController(ApplicationContext context){
		this.context=context;
		MaintenanceScheduler.runPeriodically(this::doPendingPresenceUpdates, 1, TimeUnit.MINUTES);
	}

	public User getUserOrThrow(int id){
		try{
			if(id<=0)
				throw new ObjectNotFoundException("err_user_not_found");
			User user=UserStorage.getById(id);
			if(user==null)
				throw new ObjectNotFoundException("err_user_not_found");
			return user;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public User getLocalUserOrThrow(int id){
		User user=getUserOrThrow(id);
		if(user instanceof ForeignUser)
			throw new ObjectNotFoundException("err_user_not_found");
		return user;
	}

	public int tryGetUserIdByUsername(String username){
		try{
			return UserStorage.getIdByUsername(username);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public User getUserByUsernameOrThrow(String username){
		try{
			User user=UserStorage.getByUsername(username);
			if(user==null)
				throw new ObjectNotFoundException();
			return user;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getFriendsWithBirthdaysWithinTwoDays(User self, LocalDate date){
		try{
			ArrayList<Integer> today=new ArrayList<>(), tomorrow=new ArrayList<>();
			UserStorage.getFriendIdsWithBirthdaysTodayAndTomorrow(self.id, date, today, tomorrow);
			if(today.isEmpty() && tomorrow.isEmpty())
				return Collections.emptyList();
			today.addAll(tomorrow);
			return UserStorage.getByIdAsList(today);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getFriendsWithBirthdaysInMonth(User self, int month){
		try{
			return UserStorage.getByIdAsList(UserStorage.getFriendsWithBirthdaysInMonth(self.id, month));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getFriendsWithBirthdaysOnDay(User self, int month, int day){
		try{
			return UserStorage.getByIdAsList(UserStorage.getFriendsWithBirthdaysOnDay(self.id, month, day));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<SignupInvitation> getUserInvites(Account self, int offset, int count){
		try{
			return UserStorage.getInvites(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendEmailInvite(Request req, Account self, String email, String firstName, String lastName, boolean addFriend, int _requestID){
		try{
			if(!Utils.isValidEmail(email))
				throw new UserErrorException("err_invalid_email");
			if(SessionStorage.getAccountByEmail(email)!=null){
				throw new UserErrorException("err_reg_email_taken");
			}
			if(SessionStorage.isEmailInvited(email)){
				throw new UserErrorException("err_email_already_invited");
			}
			UserPermissions permissions=SessionStorage.getUserPermissions(self);
			if(!permissions.hasPermission(UserRole.Permission.MANAGE_INVITES)){
				FloodControl.EMAIL_INVITE.incrementOrThrow(self);
			}
			int requestID=_requestID;
			if(requestID==0){
				SignupRequest sr=SessionStorage.getInviteRequestByEmail(email);
				if(sr!=null){
					requestID=sr.id;
				}
			}
			byte[] code=Utils.randomBytes(16);
			String codeStr=Utils.byteArrayToHexString(code);
			UserStorage.putInvite(self.id, code, 1, email, SignupInvitation.getExtra(!addFriend, firstName, lastName, requestID>0));
			Mailer.getInstance().sendSignupInvitation(req, self, email, codeStr, firstName, requestID>0);
			if(requestID>0){
				deleteSignupInviteRequest(requestID);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void resendEmailInvite(Request req, Account self, int id){
		try{
			SignupInvitation invite=SessionStorage.getInvitationByID(id);
			if(invite==null || invite.ownerID!=self.id || invite.email==null)
				throw new ObjectNotFoundException();
			UserPermissions permissions=SessionStorage.getUserPermissions(self);
			if(!permissions.hasPermission(UserRole.Permission.MANAGE_INVITES)){
				FloodControl.EMAIL_RESEND.incrementOrThrow(invite.email);
			}
			Mailer.getInstance().sendSignupInvitation(req, self, invite.email, invite.code, invite.firstName, invite.fromRequest);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public SignupInvitation getInvite(String codeStr){
		try{
			if(!codeStr.matches("^[\\da-f]{32}$")){
				throw new UserErrorException("err_invalid_invitation");
			}
			byte[] code=Utils.hexStringToByteArray(codeStr);
			return SessionStorage.getInvitationByCode(code);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public SignupInvitation getInvite(int id){
		try{
			if(id<=0)
				throw new BadRequestException();
			return SessionStorage.getInvitationByID(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteInvite(int id){
		try{
			if(id<=0)
				throw new BadRequestException();
			SessionStorage.deleteInvitation(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public String createInviteCode(Account self, int signupCount, boolean addFriend){
		try{
			byte[] code=Utils.randomBytes(16);
			UserStorage.putInvite(self.id, code, signupCount, null, SignupInvitation.getExtra(!addFriend, null, null, false));
			return Utils.byteArrayToHexString(code);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getInvitedUsers(Account self, int offset, int count){
		try{
			return SessionStorage.getInvitedUsers(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void requestSignupInvite(Request req, String firstName, String lastName, String email, String reason){
		try{
			if(SessionStorage.getAccountByEmail(email)!=null || SessionStorage.isThereInviteRequestWithEmail(email) || SessionStorage.isEmailInvited(email))
				throw new UserErrorException("err_reg_email_taken");
			FloodControl.OPEN_SIGNUP_OR_INVITE_REQUEST.incrementOrThrow(Utils.getRequestIP(req));
			SessionStorage.putInviteRequest(email, firstName, lastName, reason);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getSignupInviteRequestCount(){
		try{
			return SessionStorage.getInviteRequestCount();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<SignupRequest> getSignupInviteRequests(int offset, int count){
		try{
			return SessionStorage.getInviteRequests(offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteSignupInviteRequest(int id){
		try{
			SessionStorage.deleteInviteRequest(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void acceptSignupInviteRequest(Request req, Account self, int id){
		try{
			SignupRequest sr=SessionStorage.getInviteRequest(id);
			if(sr==null)
				throw new ObjectNotFoundException();

			sendEmailInvite(req, self, sr.email, sr.firstName, sr.lastName, false, id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean isUserBlocked(User owner, User target){
		try{
			return UserStorage.isUserBlocked(owner.id, target.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void ensureUserNotBlocked(User self, Actor target){
		try{
			if(target instanceof User u)
				Utils.ensureUserNotBlocked(self, u);
			else if(target instanceof Group g)
				Utils.ensureUserNotBlocked(self, g);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, User> getUsers(Collection<Integer> ids){
		return getUsers(ids, false);
	}

	public Map<Integer, User> getUsers(Collection<Integer> ids, boolean wantDeleted){
		if(ids.isEmpty())
			return Map.of();
		try{
			return UserStorage.getById(ids, wantDeleted);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteForeignUser(ForeignUser user){
		try{
			UserStorage.deleteForeignUser(user);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteLocalUser(User admin, User user){
		if(user instanceof ForeignUser || (user.banStatus!=UserBanStatus.SELF_DEACTIVATED && user.banStatus!=UserBanStatus.SUSPENDED))
			throw new IllegalArgumentException();
		try{
			Account acc=SessionStorage.getAccountByUserID(user.id);
			if(acc==null)
				return;
			context.getActivityPubWorker().sendUserDeleteSelf(user);
			UserStorage.deleteAccount(acc);
			SmithereenApplication.invalidateAllSessionsForAccount(acc.id);
			if(admin!=null)
				ModerationStorage.createAuditLogEntry(admin.id, AuditLogEntry.Action.DELETE_USER, user.id, 0, null, Map.of("name", user.getCompleteName()));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Account getAccountOrThrow(int id){
		try{
			Account acc=UserStorage.getAccount(id);
			if(acc==null)
				throw new ObjectNotFoundException("err_user_not_found");
			return acc;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Account getAccountForUser(User user){
		try{
			Account acc=SessionStorage.getAccountByUserID(user.id);
			if(acc==null)
				throw new ObjectNotFoundException();
			return acc;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public UserRelationshipMetrics getRelationshipMetrics(User user){
		try{
			return new UserRelationshipMetrics(
					UserStorage.getUserFriendsCount(user.id),
					UserStorage.getUserFollowerOrFollowingCount(user.id, true),
					UserStorage.getUserFollowerOrFollowingCount(user.id, false)
			);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public UserContentMetrics getContentMetrics(User user){
		try{
			return new UserContentMetrics(
					PostStorage.getUserPostCount(user.id),
					PostStorage.getUserPostCommentCount(user.id)
			);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<OtherSession> getAccountSessions(Account acc){
		try{
			return SessionStorage.getAccountSessions(acc.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public OtherSession getAccountMostRecentSession(Account acc){
		try{
			return SessionStorage.getAccountMostRecentSession(acc.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void changePassword(Account self, String oldPassword, String newPassword){
		try{
			if(newPassword.length()<4){
				throw new UserErrorException("err_password_short");
			}
			FloodControl.PASSWORD_CHECK.incrementOrThrow(self);
			if(!SessionStorage.updatePassword(self.id, oldPassword, newPassword)){
				throw new UserErrorException("err_old_password_incorrect");
			}
			FloodControl.PASSWORD_CHECK.reset(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean checkPassword(Account self, String password){
		try{
			FloodControl.PASSWORD_CHECK.incrementOrThrow(self);
			boolean valid=SessionStorage.checkPassword(self.id, password);
			if(valid)
				FloodControl.PASSWORD_CHECK.reset(self);
			return valid;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void terminateSessionsExcept(Account self, String psid){
		try{
			byte[] sid=Base64.getDecoder().decode(psid);
			SessionStorage.deleteSessionsExcept(self.id, sid);
			SmithereenApplication.invalidateAllSessionsForAccount(self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void selfDeactivateAccount(Account self){
		try{
			if(self.user.banStatus!=UserBanStatus.NONE)
				throw new IllegalArgumentException("Already banned");
			UserBanInfo info=new UserBanInfo(Instant.now(), null, null, false, 0, 0, false);
			UserStorage.setUserBanStatus(self.user, self, UserBanStatus.SELF_DEACTIVATED, Utils.gson.toJson(info));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void selfReinstateAccount(Account self){
		try{
			if(self.user.banStatus!=UserBanStatus.SELF_DEACTIVATED)
				throw new IllegalArgumentException("Invalid account status");
			UserStorage.setUserBanStatus(self.user, self, UserBanStatus.NONE, null);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static void doPendingAccountDeletions(ApplicationContext ctx){
		try{
			List<User> users=UserStorage.getTerminallyBannedUsers();
			if(users.isEmpty()){
				LOG.trace("No users to delete");
				return;
			}
			Instant deleteBannedBefore=Instant.now().minus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS);
			for(User user:users){
				if(user.banStatus!=UserBanStatus.SUSPENDED && user.banStatus!=UserBanStatus.SELF_DEACTIVATED){
					LOG.warn("Ineligible user {} in pending account deletions - bug likely (banStatus {}, banInfo {})", user.id, user.banStatus, user.banInfo);
					continue;
				}
				if(user.banInfo.bannedAt().isBefore(deleteBannedBefore)){
					LOG.info("Deleting user {}, banStatus {}, banInfo {}", user.id, user.banStatus, user.banInfo);
					ctx.getUsersController().deleteLocalUser(null, user);
				}else{
					LOG.trace("User {} too early to delete", user.id);
				}
			}
		}catch(SQLException x){
			LOG.error("Failed to delete accounts", x);
		}
	}

	public void updateUsername(User self, String username){
		try{
			if(!Utils.isValidUsername(username))
				throw new UserErrorException("err_reg_invalid_username");
			if(Utils.isReservedUsername(username))
				throw new UserErrorException("err_reg_reserved_username");
			boolean result=DatabaseUtils.runWithUniqueUsername(username, ()->{
				UserStorage.updateUsername(self, username);
			});
			if(!result)
				throw new UserErrorException("err_reg_username_taken");
			self.username=username;
			self.url=Config.localURI(username);
			context.getActivityPubWorker().sendUpdateUserActivity(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private static String clampString(String str, int maxLength){
		if(str==null)
			return null;
		return str.length()>maxLength ? str.substring(0, maxLength) : str;
	}

	public void updateBasicProfileInfo(User self, String firstName, String lastName, String middleName, String maidenName, User.Gender gender, LocalDate birthDate, String hometown, User.RelationshipStatus relation, User partner){
		try{
			if(birthDate!=null && birthDate.isAfter(LocalDate.now().plusDays(1))){
				birthDate=self.birthDate;
			}
			if(firstName.length()<2){
				throw new UserErrorException("err_name_too_short");
			}else{
				UserStorage.changeBasicInfo(self, firstName, lastName, middleName, maidenName, gender, birthDate);
			}
			int partnerID=partner==null ? 0 : partner.id;
			if(!Objects.equals(hometown, self.hometown) || self.relationship!=relation || self.relationshipPartnerID!=partnerID){
				if(self.relationship!=relation || self.relationshipPartnerID!=partnerID){
					maybeCreateRelationshipStatusNewsfeedEntry(self, relation, partner);
				}
				self.hometown=hometown;
				self.relationship=relation;
				self.relationshipPartnerID=partnerID;
				self.relationshipPartnerActivityPubID=partner==null ? null : partner.activityPubID;
				UserStorage.updateExtendedFields(self, self.serializeProfileFields());
			}
			self=getUserOrThrow(self.id);
			context.getActivityPubWorker().sendUpdateUserActivity(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateProfileInterests(User self, String aboutSource, String activities, String interests, String music, String movies, String tv, String books, String games, String quotes){
		try{
			boolean anythingChanged=false;
			String about;
			if(StringUtils.isNotEmpty(aboutSource))
				about=TextProcessor.preprocessPostHTML(aboutSource, null);
			else
				about=null;
			if(!Objects.equals(self.summary, about)){
				UserStorage.updateAbout(self, about, aboutSource);
				anythingChanged=true;
			}
			if(!Objects.equals(activities, self.activities) || !Objects.equals(interests, self.interests) || !Objects.equals(music, self.favoriteMusic) || !Objects.equals(movies, self.favoriteMovies)
					|| !Objects.equals(tv, self.favoriteTvShows) || !Objects.equals(books, self.favoriteBooks) || !Objects.equals(games, self.favoriteGames) || !Objects.equals(quotes, self.favoriteQuotes)){
				self.activities=clampString(activities, 1024);
				self.interests=clampString(interests, 1024);
				self.favoriteMusic=clampString(music, 1024);
				self.favoriteMovies=clampString(movies, 1024);
				self.favoriteTvShows=clampString(tv, 1024);
				self.favoriteBooks=clampString(books, 1024);
				self.favoriteGames=clampString(games, 1024);
				self.favoriteQuotes=clampString(quotes, 1024);

				UserStorage.updateExtendedFields(self, self.serializeProfileFields());
				anythingChanged=true;
			}
			if(anythingChanged){
				self=getUserOrThrow(self.id);
				context.getActivityPubWorker().sendUpdateUserActivity(self);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateProfilePersonal(User self, User.PoliticalViews politicalViews, String religion, User.PersonalPriority personalPriority,
									  User.PeoplePriority peoplePriority, User.HabitsViews smokingViews, User.HabitsViews alcoholViews, String inspiredBy){
		try{
			if(self.politicalViews==politicalViews && Objects.equals(self.religion, religion) && self.personalPriority==personalPriority && self.peoplePriority==peoplePriority
				&& self.smokingViews==smokingViews && self.alcoholViews==alcoholViews && Objects.equals(self.inspiredBy, inspiredBy)){
				return;
			}
			self.politicalViews=politicalViews;
			self.religion=clampString(religion, 256);
			self.personalPriority=personalPriority;
			self.peoplePriority=peoplePriority;
			self.smokingViews=smokingViews;
			self.alcoholViews=alcoholViews;
			self.inspiredBy=clampString(inspiredBy, 256);
			UserStorage.updateExtendedFields(self, self.serializeProfileFields());
			context.getActivityPubWorker().sendUpdateUserActivity(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateProfileContacts(User self, Map<User.ContactInfoKey, String> contactInfo, String location, String website){
		try{
			if(StringUtils.isNotEmpty(website) && !website.startsWith("http://") && !website.startsWith("https://"))
				website="http://"+website;
			if(self.contacts.equals(contactInfo) && Objects.equals(self.location, location) && Objects.equals(self.website, website)){
				return;
			}
			self.contacts=contactInfo;
			self.location=location;
			self.website=website;
			UserStorage.updateExtendedFields(self, self.serializeProfileFields());
			context.getActivityPubWorker().sendUpdateUserActivity(self);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void maybeCreateRelationshipStatusNewsfeedEntry(User self, User.RelationshipStatus newStatus, User newPartner){
		if(newStatus==null)
			return;
		int partnerID=newPartner==null || !newStatus.canHavePartner() ? 0 : newPartner.id;
		if(self.relationship==newStatus && self.relationshipPartnerID==partnerID)
			return;
		if(newPartner!=null && newStatus.needsPartnerApproval()){
			if(newPartner.relationshipPartnerID==self.id){
				// If the partner already has this user set as *their* partner, create a newsfeed entry for them too
				if(self.relationshipPartnerID!=partnerID){
					long id=((long)newPartner.relationship.ordinal()) << 56 | (((System.currentTimeMillis()/1000L) & 0xFFFFFFL) << 32) | self.id;
					context.getNewsfeedController().putFriendsFeedEntry(newPartner, id, NewsfeedEntry.Type.RELATIONSHIP_STATUS);
				}
			}else{
				partnerID=0;
			}
		}
		long id=((long)newStatus.ordinal()) << 56 | (((System.currentTimeMillis()/1000L) & 0xFFFFFFL) << 32) | partnerID;
		context.getNewsfeedController().putFriendsFeedEntry(self, id, NewsfeedEntry.Type.RELATIONSHIP_STATUS);
	}

	public void updateUserPreferences(Account self){
		try{
			SessionStorage.updatePreferences(self.id, self.prefs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setOnline(User user, UserPresence.PresenceType type, long sessionID){
		user.ensureLocal();
		UserPresence presence=new UserPresence(true, Instant.now(), type);
		LOG.trace("Setting user {} presence to online, type {}", user.id, type);
		String mutexName=String.valueOf(user.id);

		onlineMutexes.acquire(mutexName);
		CachedUserPresence prevPresence=onlineLocalUsersByID.get(user.id);
		if(prevPresence!=null){
			onlineLocalUsers.remove(prevPresence);
		}
		CachedUserPresence newPresence=new CachedUserPresence(user.id, sessionID, presence);
		onlineLocalUsersByID.put(user.id, newPresence);
		onlineLocalUsers.add(newPresence);
		pendingUserPresenceUpdates.put(user.id, presence);
		onlineMutexes.release(mutexName);
	}

	public void setOffline(User user, long sessionID){
		user.ensureLocal();
		setOfflineInternal(user.id, sessionID);
	}

	private void setOfflineInternal(int userID, long sessionID){
		String mutexName=String.valueOf(userID);

		onlineMutexes.acquire(mutexName);
		CachedUserPresence prevPresence=onlineLocalUsersByID.get(userID);
		if(prevPresence!=null && prevPresence.sessionID==sessionID){
			onlineLocalUsers.remove(prevPresence);
			onlineLocalUsersByID.remove(userID);
			pendingUserPresenceUpdates.put(userID, new UserPresence(false, prevPresence.presence.lastUpdated(), prevPresence.presence.type()));
		}
		onlineMutexes.release(mutexName);
	}

	public void doPendingPresenceUpdates(){
		try{
			Instant onlineThreshold=Instant.now().minus(ONLINE_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
			for(CachedUserPresence p:onlineLocalUsers.reversed()){
				if(p.presence.lastUpdated().isBefore(onlineThreshold)){
					LOG.trace("Setting user {} presence to offline by timeout", p.userID);
					setOfflineInternal(p.userID, p.sessionID);
				}else{
					break;
				}
			}

			if(!pendingUserPresenceUpdates.isEmpty()){
				Map<Integer, UserPresence> updates=pendingUserPresenceUpdates;
				pendingUserPresenceUpdates=Collections.synchronizedMap(new HashMap<>());
				UserStorage.updateUserPresences(updates);
			}
		}catch(SQLException x){
			LOG.error("Failed to do pending user presence updates", x);
		}
	}

	public void loadPresenceFromDatabase(){
		try{
			Set<Integer> onlineUsers=UserStorage.getOnlineLocalUserIDs();
			if(!onlineUsers.isEmpty()){
				Map<Integer, UserPresence> presences=UserStorage.getUserPresences(onlineUsers);
				presences.forEach((id, presence)->{
					CachedUserPresence p=new CachedUserPresence(id, 0, presence);
					onlineLocalUsers.add(p);
					onlineLocalUsersByID.put(id, p);
				});
				doPendingPresenceUpdates();
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<Integer, UserPresence> getUserPresences(Collection<Integer> userIDs){
		if(userIDs.isEmpty())
			return Map.of();
		Set<Integer> remainingIDs=new HashSet<>(userIDs);
		HashMap<Integer, UserPresence> result=new HashMap<>();
		for(int id:remainingIDs){
			CachedUserPresence presence=onlineLocalUsersByID.get(id);
			if(presence!=null)
				result.put(id, presence.presence);
		}
		remainingIDs.removeAll(result.keySet());
		if(!remainingIDs.isEmpty()){
			for(int id:remainingIDs){
				UserPresence presence=pendingUserPresenceUpdates.get(id);
				if(presence!=null)
					result.put(id, presence);
			}
		}
		remainingIDs.removeAll(result.keySet());
		if(!remainingIDs.isEmpty()){
			try{
				result.putAll(UserStorage.getUserPresences(remainingIDs));
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
		return result;
	}

	public UserPresence getUserPresence(User user){
		return getUserPresences(Set.of(user.id)).get(user.id);
	}

	public Map<Integer, UserPresence> getUserPresencesOnlineOnly(Collection<Integer> userIDs){
		HashMap<Integer, UserPresence> result=new HashMap<>();
		for(int id:userIDs){
			CachedUserPresence presence=onlineLocalUsersByID.get(id);
			if(presence!=null)
				result.put(id, presence.presence);
		}
		return result;
	}

	public URI addAlsoKnownAs(User self, String link){
		User target;
		try{
			if(Utils.isUsernameAndDomain(link)){
				ObjectLinkResolver.UsernameResolutionResult res=context.getObjectLinkResolver().resolveUsername(link, true, EnumSet.of(ObjectLinkResolver.UsernameOwnerType.USER));
				target=context.getUsersController().getUserOrThrow(res.localID());
			}else if(Utils.isURL(link)){
				if(!link.startsWith("https://") && !link.startsWith("http://"))
					link="https://"+link;
				ActivityPubObject obj=context.getObjectLinkResolver().resolve(URI.create(link), ActivityPubObject.class, true, false, false);
				if(!(obj instanceof User user)){
					throw new UserErrorException("settings_transfer_link_unsupported");
				}
				target=user;
			}else{
				throw new UserErrorException("settings_transfer_link_not_found");
			}
		}catch(ObjectNotFoundException x){
			throw new UserErrorException("settings_transfer_link_not_found");
		}
		if(!(target instanceof ForeignUser)){
			throw new UserErrorException("settings_transfer_link_this_server", Map.of("domain", Config.domain));
		}
		URI id=target.activityPubID;
		if(self.alsoKnownAs.contains(id))
			return id;
		self.alsoKnownAs.add(id);
		try{
			UserStorage.updateExtendedFields(self, self.serializeProfileFields());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		context.getActivityPubWorker().sendUpdateUserActivity(self);
		return id;
	}

	public void deleteAlsoKnownAs(User self, String link){
		List<String> links=self.alsoKnownAs.stream().map(Object::toString).toList();
		int index=links.indexOf(link);
		if(index==-1)
			return;
		self.alsoKnownAs.remove(index);
		try{
			UserStorage.updateExtendedFields(self, self.serializeProfileFields());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		context.getActivityPubWorker().sendUpdateUserActivity(self);
	}

	public void transferUserFollowers(User oldUser, User newUser){
		synchronized(movingUsers){
			if(movingUsers.contains(oldUser.id)){
				LOG.debug("Not moving {} to {} because its previous Move activity is already being processed", oldUser.activityPubID, newUser.activityPubID);
				return;
			}
			movingUsers.add(oldUser.id);
		}

		boolean success=false;
		try{
			oldUser.movedTo=newUser.id;
			oldUser.movedToApID=newUser.activityPubID;
			oldUser.movedAt=Instant.now();
			newUser.movedFrom=oldUser.id;
			if(oldUser instanceof ForeignUser)
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(oldUser, oldUser);
			else
				UserStorage.updateExtendedFields(oldUser, oldUser.serializeProfileFields());

			if(newUser instanceof ForeignUser)
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(newUser, newUser);
			else
				UserStorage.updateExtendedFields(newUser, newUser.serializeProfileFields());

			success=true;
			BackgroundTaskRunner.getInstance().submit(()->performMove(oldUser, newUser));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}finally{
			if(!success){
				synchronized(movingUsers){
					movingUsers.remove(oldUser.id);
				}
			}
		}
	}

	public Instant getUserLastFollowersTransferTime(User self){
		try{
			return UserStorage.getUserLastFollowersTransferTime(self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void transferLocalUserFollowers(Account self, String link, String password){
		Instant lastTransfer=getUserLastFollowersTransferTime(self.user);
		if(lastTransfer!=null && lastTransfer.isAfter(Instant.now().minus(FOLLOWERS_TRANSFER_COOLDOWN_DAYS, ChronoUnit.DAYS))){
			Lang l=Lang.get(self.prefs.locale);
			throw new UserErrorException("settings_transfer_out_too_often", Map.of("lastTransferDate", l.formatDate(lastTransfer, self.prefs.timeZone, false),
					"days", FOLLOWERS_TRANSFER_COOLDOWN_DAYS,
					"nextTransferDate", l.formatDate(lastTransfer.plus(FOLLOWERS_TRANSFER_COOLDOWN_DAYS, ChronoUnit.DAYS), self.prefs.timeZone, false)));
		}
		if(!checkPassword(self, password))
			throw new UserErrorException("err_old_password_incorrect");

		User target;
		try{
			if(Utils.isUsernameAndDomain(link)){
				ObjectLinkResolver.UsernameResolutionResult res=context.getObjectLinkResolver().resolveUsername(link, true, EnumSet.of(ObjectLinkResolver.UsernameOwnerType.USER));
				target=context.getUsersController().getUserOrThrow(res.localID());
				if(target instanceof ForeignUser && target.lastUpdated.isBefore(Instant.now().minus(10, ChronoUnit.SECONDS)))
					target=context.getObjectLinkResolver().resolve(target.activityPubID, User.class, true, true, true);
			}else if(Utils.isURL(link)){
				if(!link.startsWith("https://") && !link.startsWith("http://"))
					link="https://"+link;
				ActivityPubObject obj=context.getObjectLinkResolver().resolve(URI.create(link), ActivityPubObject.class, true, true, true);
				if(!(obj instanceof User user)){
					throw new UserErrorException("settings_transfer_link_unsupported");
				}
				target=user;
			}else{
				throw new UserErrorException("settings_transfer_out_link_not_found");
			}
		}catch(ObjectNotFoundException x){
			throw new UserErrorException("settings_transfer_out_link_not_found");
		}
		if(!(target instanceof ForeignUser)){
			throw new UserErrorException("settings_transfer_link_this_server", Map.of("domain", Config.domain));
		}
		if(!target.alsoKnownAs.contains(self.user.activityPubID))
			throw new UserErrorException("settings_transfer_out_no_link", Map.of("thisAccountUsername", self.user.username+"@"+Config.domain, "newAccountServer", target.activityPubID.getHost()));

		transferUserFollowers(self.user, target);
		context.getActivityPubWorker().sendUserMoveSelf(self.user, target);
		SmithereenApplication.invalidateAllSessionsForAccount(self.id);
	}

	public void clearMovedTo(Account self){
		try{
			self.user.movedTo=0;
			self.user.movedToApID=null;
			self.user.movedAt=null;
			UserStorage.updateExtendedFields(self.user, self.user.serializeProfileFields());
			context.getActivityPubWorker().sendUpdateUserActivity(self.user);
			SmithereenApplication.invalidateAllSessionsForAccount(self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void performMove(User oldUser, User newUser){
		try{
			ModerationStorage.addUserActionLogEntry(oldUser.id, UserActionLogAction.TRANSFER_FOLLOWERS, Map.of("to", newUser.id, "toApID", newUser.activityPubID.toString()));
			List<FollowRelationship> localFollowers=UserStorage.getUserLocalFollowers(oldUser.id);
			LOG.debug("Started moving {} followers for {} -> {}", localFollowers.size(), oldUser.activityPubID, newUser.activityPubID);
			for(FollowRelationship fr:localFollowers){
				try{
					User follower=context.getUsersController().getUserOrThrow(fr.followerID());
					context.getFriendsController().removeFriend(follower, oldUser);
					context.getFriendsController().followUser(follower, newUser);
					if(fr.muted())
						context.getFriendsController().setUserMuted(newUser, follower, true);
					// TODO carry lists over as well
				}catch(UserErrorException|ObjectNotFoundException ignore){}
			}
			LOG.debug("Done moving followers for {} -> {}", oldUser.activityPubID, newUser.activityPubID);

			List<User> blockingUsers=UserStorage.getBlockingUsers(oldUser.id);
			LOG.debug("Started moving {} blocks for {} -> {}", blockingUsers.size(), oldUser.activityPubID, newUser.activityPubID);
			for(User user:blockingUsers){
				if(newUser instanceof ForeignUser && user instanceof ForeignUser)
					continue;
				context.getFriendsController().blockUser(user, newUser);
			}
			LOG.debug("Done moving blocks for {} -> {}", oldUser.activityPubID, newUser.activityPubID);
		}catch(Exception x){
			LOG.error("Failed to move {} to {}", oldUser.activityPubID, newUser.activityPubID, x);
		}finally{
			synchronized(movingUsers){
				movingUsers.remove(oldUser.id);
			}
		}
	}

	public String updateStatus(User self, String status){
		ActorStatus result=updateStatus(self, new ActorStatus(status, Instant.now(), null, null));
		return result==null ? null : result.text();
	}

	public ActorStatus updateStatus(User self, ActorStatus status){
		ActorStatus prev=self.status;
		if(status!=null && StringUtils.isNotEmpty(status.text()) && !status.isExpired()){
			if(status.text().length()>100)
				status=status.withText(TextProcessor.truncateOnWordBoundary(status.text(), 100)+"...");
			if(self.status!=null && self.status.text().equals(status.text()))
				return status;
			self.status=status;
		}else{
			if(self.status==null)
				return status;
			self.status=null;
		}
		try{
			UserStorage.updateExtendedFields(self, self.serializeProfileFields());
			if(self instanceof ForeignUser){
				if(prev!=null)
					FederationStorage.deleteFromApIdIndex(ObjectLinkResolver.ObjectType.USER_STATUS, self.id);
				if(status!=null && status.apId()!=null)
					FederationStorage.addToApIdIndex(status.apId(), ObjectLinkResolver.ObjectType.USER_STATUS, self.id);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		if(!(self instanceof ForeignUser)){
			if(self.status!=null)
				context.getActivityPubWorker().sendCreateStatusActivity(self, self.status);
			else
				context.getActivityPubWorker().sendClearStatusActivity(self, prev);
		}

		return status;
	}

	private record CachedUserPresence(int userID, long sessionID, UserPresence presence){}
}
