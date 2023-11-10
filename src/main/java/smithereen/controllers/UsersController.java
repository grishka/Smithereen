package smithereen.controllers;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.SignupInvitation;
import smithereen.model.SignupRequest;
import smithereen.model.User;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.UserPermissions;
import smithereen.model.UserRole;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.util.FloodControl;
import spark.Request;

public class UsersController{
	private final ApplicationContext context;

	public UsersController(ApplicationContext context){
		this.context=context;
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
			DatabaseUtils.runWithUniqueUsername(()->{
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
			});
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
			DatabaseUtils.runWithUniqueUsername(()->{
				if(SessionStorage.getAccountByEmail(email)!=null || SessionStorage.isThereInviteRequestWithEmail(email) || SessionStorage.isEmailInvited(email))
					throw new UserErrorException("err_reg_email_taken");
				FloodControl.OPEN_SIGNUP_OR_INVITE_REQUEST.incrementOrThrow(Utils.getRequestIP(req));
				SessionStorage.putInviteRequest(email, firstName, lastName, reason);
			});
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
		if(ids.isEmpty())
			return Map.of();
		try{
			return UserStorage.getById(ids);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteForeignUser(ForeignUser user){
		try{
			UserStorage.deleteUser(user);
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
}
