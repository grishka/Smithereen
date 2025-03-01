package smithereen.routes;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.libvips.VipsImage;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.OtherSession;
import smithereen.model.PrivacySetting;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.UserRole;
import smithereen.model.WebDeltaResponse;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.filtering.FilterContext;
import smithereen.model.filtering.WordFilter;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.media.MediaFileType;
import smithereen.model.notifications.RealtimeNotificationSettingType;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.ImageRect;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.storage.utils.Pair;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;
import smithereen.util.FloodControl;
import smithereen.util.UriBuilder;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SettingsRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(SettingsRoutes.class);

	public static Object settings(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings", req);
		Lang l=lang(req);
		model.with("languages", Lang.list).with("selectedLang", Utils.lang(req));
		model.addMessage(req, "settings.passwordMessage", "passwordMessage")
				.addMessage(req, "settings.profilePicMessage", "profilePicMessage")
				.addMessage(req, "settings.emailMessage", "emailMessage")
				.addMessage(req, "settings.appearanceBehaviorMessage", "appearanceBehaviorMessage")
				.addMessage(req, "settings.usernameMessage", "changeUsernameMessage");
		model.with("activationInfo", self.activationInfo);
		model.with("currentEmailMasked", self.getCurrentEmailMasked());
		model.with("textFormat", self.prefs.textFormat)
				.with("commentView", self.prefs.commentViewType)
				.with("countLikesInUnread", self.prefs.countLikesInUnread);
		model.with("title", l.get("settings"));
		OtherSession session=ctx.getUsersController().getAccountMostRecentSession(self);
		if(session!=null){
			model.with("lastActivityDescription", l.get("settings_activity_web_short", Map.of(
					"time", l.formatDate(session.lastActive(), timeZoneForRequest(req), false),
					"ip", session.ip().getHostAddress(),
					"browserName", session.browserInfo().name()
			)));
		}
		if(req.queryParams("sessionsTerminated")!=null){
			model.with("accountActivityMessage", l.get("settings_sessions_ended"));
		}
		return model;
	}

	public static Object createInvite(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		if(Config.signupMode==Config.SignupMode.OPEN){
			throw new BadRequestException();
		}
		if(Config.signupMode==Config.SignupMode.CLOSED && !sessionInfo(req).permissions.hasPermission(UserRole.Permission.MANAGE_INVITES))
			return wrapError(req, resp, "err_access");
		byte[] code=new byte[16];
		new Random().nextBytes(code);
		UserStorage.putInvite(self.id, code, 1, null, null);
		req.session().attribute("settings.inviteMessage", Utils.lang(req).get("invitation_created"));
		resp.redirect("/settings/");
		return "";
	}

	public static Object updatePassword(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		requireQueryParams(req, "current", "new", "new2");
		String current=req.queryParams("current");
		String new1=req.queryParams("new");
		String new2=req.queryParams("new2");
		String message;
		if(!new1.equals(new2)){
			message=Utils.lang(req).get("err_passwords_dont_match");
		}else{
			try{
				ctx.getUsersController().changePassword(self, current, new1);
				message=Utils.lang(req).get("password_changed");
			}catch(UserErrorException x){
				message=lang(req).get(x.getMessage());
			}
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_changePassword").setContent("formMessage_changePassword", message);
		}
		req.session().attribute("settings.passwordMessage", message);
		resp.redirect("/settings/");
		return "";
	}

	public static Object updateProfileGeneral(Request req, Response resp, Account self, ApplicationContext ctx){
		String first=req.queryParams("first_name");
		String last=req.queryParams("last_name");
		String middle=req.queryParams("middle_name");
		String maiden=req.queryParams("maiden_name");
		User.Gender gender=enumValue(req.queryParamOrDefault("gender", "UNKNOWN"), User.Gender.class);
		LocalDate bdate=self.user.birthDate;
		String _bdate=req.queryParams("bdate");
		if(_bdate!=null){
			try{
				bdate=LocalDate.parse(_bdate);
			}catch(DateTimeParseException ignore){}
		}
		String hometown=req.queryParams("hometown");
		User.RelationshipStatus relation=enumValueOpt(req.queryParams("relationship"), User.RelationshipStatus.class);
		User partner;
		if(relation==null || !relation.canHavePartner()){
			partner=null;
		}else{
			int partnerID=safeParseInt(req.queryParams("partner"));
			if(partnerID!=0){
				partner=ctx.getUsersController().getUserOrThrow(partnerID);
			}else{
				partner=null;
			}
		}
		String message;
		try{
			ctx.getUsersController().updateBasicProfileInfo(self.user, first, last, middle, maiden, gender, bdate, hometown, relation, partner);
			message=lang(req).get("profile_info_updated");
		}catch(UserErrorException x){
			message=lang(req).get(x.getMessage());
		}
		self.user=ctx.getUsersController().getUserOrThrow(self.user.id);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_profileEdit").setContent("formMessage_profileEdit", message);
		}
		req.session().attribute("settings.profileEditMessage", message);
		resp.redirect("/settings/profile/general");
		return "";
	}

	public static Object updateProfilePicture(Request req, Response resp, Account self, ApplicationContext ctx){
		int groupID=parseIntOrDefault(req.queryParams("group"), 0);
		Group group=null;
		if(groupID!=0){
			group=ctx.getGroupsController().getGroupOrThrow(groupID);
			ctx.getGroupsController().enforceUserAdminLevel(group, self.user, Group.AdminLevel.ADMIN);
		}

		AvatarCropRects cropRects=AvatarCropRects.fromString(req.queryParams("crop"));

		SizedImage.Rotation rotation;
		try{
			rotation=SizedImage.Rotation.valueOf(safeParseInt(req.queryParams("rotation")));
		}catch(IllegalArgumentException x){
			rotation=null;
		}

		LocalImage img=MediaStorageUtils.saveUploadedImage(req, resp, self, false);
		img.rotation=rotation;
		try{
			ctx.getPhotosController().updateAvatar(self, group, img, cropRects);
		}catch(UserErrorException x){
			LOG.error("Exception while processing a profile picture upload", x);
			if(isAjax(req)){
				Lang l=lang(req);
				return new WebDeltaResponse(resp).messageBox(l.get("error"), l.get("image_upload_error"), l.get("close"));
			}

			req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"));
			resp.redirect("/settings/");
			return "";
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();

		req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("avatar_updated"));
		resp.redirect("/settings/");
		return "";
	}

	public static Object setLanguage(Request req, Response resp) throws SQLException{
		String lang=req.queryParams("lang");
		SessionInfo info=req.session().attribute("info");
		if(info==null){
			req.session().attribute("info", info=new SessionInfo());
		}
		if(info.account!=null){
			info.account.prefs.locale=Locale.forLanguageTag(lang);
			SessionStorage.updatePreferences(info.account.id, info.account.prefs);
		}else{
			info.preferredLocale=Locale.forLanguageTag(lang);
		}
		resp.redirect("/settings/");
		return "";
	}

	public static Object setTimezone(Request req, Response resp) throws SQLException{
		String tz=req.queryParams("tz");
		SessionInfo info=req.session().attribute("info");
		if(info==null){
			req.session().attribute("info", info=new SessionInfo());
		}
		if(info.account!=null){
			info.account.prefs.timeZone=ZoneId.of(tz);
			SessionStorage.updatePreferences(info.account.id, info.account.prefs);
		}else{
			info.timeZone=ZoneId.of(tz);
		}
		if(req.queryParams("_ajax")!=null)
			return "";
		resp.redirect("/settings/");
		return "";
	}

	public static Object profileEditGeneral(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("profile_edit_general", req);
		model.with("todayDate", new java.sql.Date(System.currentTimeMillis()).toString());
		model.with("title", lang(req).get("edit_profile"));
		model.with("relationshipOptions", User.RelationshipStatus.values());
		List<User> friends=ctx.getFriendsController().getFriends(self.user, 0, 100, FriendsController.SortOrder.ID_ASCENDING).list;
		ArrayList<Map<String, Object>> friendList=new ArrayList<>();
		friendList.add(Map.of("id", 0, "title", lang(req).get("profile_field_none_selected")));
		if(self.user.relationshipPartnerID!=0){
			boolean foundCurrentPartner=false;
			for(User u: friends){
				if(u.id==self.user.relationshipPartnerID){
					foundCurrentPartner=true;
					break;
				}
			}
			if(!foundCurrentPartner){
				try{
					User partner=ctx.getUsersController().getUserOrThrow(self.user.relationshipPartnerID);
					friendList.add(Map.of("id", partner.id, "title", TextProcessor.escapeHTML(partner.getFullName())));
				}catch(ObjectNotFoundException ignore){}
			}
		}
		friendList.addAll(friends.stream().map(u->Map.of("id", (Object)u.id, "title", TextProcessor.escapeHTML(u.getFullName()))).toList());
		model.with("friendList", friendList);
		jsLangKey(req, "profile_edit_relationship_partner", "profile_edit_relationship_spouse", "profile_edit_relationship_in_love_partner");
		Session s=req.session();
		if(s.attribute("settings.profileEditMessage")!=null){
			model.with("profileEditMessage", s.attribute("settings.profileEditMessage"));
			s.removeAttribute("settings.profileEditMessage");
		}
		return model;
	}

	public static Object confirmRemoveProfilePicture(Request req, Response resp, Account self, ApplicationContext ctx){
		req.attribute("noHistory", true);
		String back=Utils.back(req);
		String groupParam=req.queryParams("group")!=null ? ("&group="+req.queryParams("group")) : "";
		return new RenderedTemplateResponse("generic_confirm", req).with("message", Utils.lang(req).get("confirm_remove_profile_picture")).with("formAction", Config.localURI("/settings/removeProfilePicture?_redir="+URLEncoder.encode(back)+groupParam)).with("back", back);
	}

	public static Object removeProfilePicture(Request req, Response resp, Account self, ApplicationContext ctx){
		int groupID=parseIntOrDefault(req.queryParams("group"), 0);
		Actor owner=self.user;
		if(groupID!=0){
			Group group=ctx.getGroupsController().getGroupOrThrow(groupID);
			ctx.getGroupsController().enforceUserAdminLevel(group, self.user, Group.AdminLevel.ADMIN);
			owner=group;
		}

		if(owner.getAvatar() instanceof LocalImage li){
			if(li.photoID!=0)
				ctx.getPhotosController().deletePhoto(self.user, ctx.getPhotosController().getPhotoIgnoringPrivacy(li.photoID));
			else
				ctx.getPhotosController().deleteAvatar(owner);
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect("/settings/");
		return "";
	}

	public static Object blocking(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_blocking", req).pageTitle(lang(req).get("settings_blocking")).mobileToolbarTitle(lang(req).get("settings"));
		model.with("blockedUsers", UserStorage.getBlockedUsers(self.user.id));
		model.with("blockedDomains", UserStorage.getBlockedDomains(self.user.id));
		jsLangKey(req, "unblock", "yes", "no", "cancel");
		return model;
	}

	public static Object blockDomainForm(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("block_domain", req);
		return wrapForm(req, resp, "block_domain", "/settings/blockDomain", lang(req).get("block_a_domain"), "block", model);
	}

	public static Object blockDomain(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String domain=req.queryParams("domain");
		if(domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$")){
			if(UserStorage.isDomainBlocked(self.user.id, domain))
				return wrapError(req, resp, "err_domain_already_blocked");
			UserStorage.blockDomain(self.user.id, domain);
		}
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmUnblockDomain(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.queryParams("domain");
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_domain_X", Map.of("domain", domain))).with("formAction", "/settings/unblockDomain?domain="+domain+"_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object unblockDomain(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String domain=req.queryParams("domain");
		if(StringUtils.isNotEmpty(domain))
			UserStorage.unblockDomain(self.user.id, domain);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object updateEmail(Request req, Response resp, Account self, ApplicationContext ctx){
		String email=req.queryParams("email");
		Lang l=lang(req);
		String message;
		if(!isValidEmail(email)){
			message=l.get("err_invalid_email");
		}else{
			try{
				if(email.equals(self.email) || email.equals(self.getUnconfirmedEmail())){
					message=null;
				}else if(Config.signupConfirmEmail){
					self.activationInfo=new Account.ActivationInfo();
					self.activationInfo.emailConfirmationKey=Mailer.generateConfirmationKey();
					self.activationInfo.emailState=Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING;
					self.activationInfo.newEmail=email;
					SessionStorage.updateActivationInfo(self.id, self.activationInfo);
					FloodControl.EMAIL_RESEND.incrementOrThrow(self.getUnconfirmedEmail());
					Mailer.getInstance().sendEmailChange(req, self);
					message=l.get("change_email_sent");
				}else{
					SessionStorage.updateEmail(self.id, email);
					message=l.get("email_confirmed_changed");
				}
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
		if(message==null)
			return "";
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_changeEmail").setContent("formMessage_changeEmail", message);
		}
		Session s=req.session();
		s.attribute("settings.emailMessage", message);
		resp.redirect(back(req));
		return "";
	}

	public static Object cancelEmailChange(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.activationInfo==null || self.activationInfo.emailState!=Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING)
			throw new BadRequestException();
		try{
			SessionStorage.updateActivationInfo(self.id, null);
			self.activationInfo=null;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object resendEmailConfirmation(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.activationInfo==null || self.activationInfo.emailState!=Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING)
			throw new BadRequestException();
		FloodControl.EMAIL_RESEND.incrementOrThrow(self.getUnconfirmedEmail());
		Mailer.getInstance().sendEmailChange(req, self);
		if(isAjax(req)){
			Lang l=lang(req);
			return new WebDeltaResponse(resp).messageBox(l.get("change_email_title"), l.get("email_confirmation_resent_short", Map.of("address", self.getUnconfirmedEmail())), l.get("close"));
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object invites(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_invites", req).pageTitle(lang(req).get("my_invites"));
		model.with("tab", "invites");
		model.paginate(ctx.getUsersController().getUserInvites(self, offset(req), 100));
		jsLangKey(req, "yes", "no");
		String msg=req.session().attribute("invites.message");
		if(msg!=null){
			req.session().removeAttribute("invites.message");
			model.with("message", msg);
		}
		return model;
	}

	public static Object createEmailInviteForm(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!sessionInfo(req).permissions.canInviteNewUsers)
			throw new UserActionNotAllowedException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_email_invite_form", req);
		return wrapForm(req, resp, "settings_email_invite_form", "/settings/invites/createEmailInvite", lang(req).get("invite_by_email"), "send", model);
	}

	public static Object createEmailInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!sessionInfo(req).permissions.canInviteNewUsers)
			throw new UserActionNotAllowedException();

		try{
			String email=requireFormField(req, "email", "err_invalid_email");
			String firstName=requireFormFieldLength(req, "first_name", 2, "err_name_too_short");
			String lastName=req.queryParams("last_name");
			boolean addFriend="on".equals(req.queryParams("add_friend"));
			ctx.getUsersController().sendEmailInvite(req, self, email, firstName, lastName, addFriend, 0);
			req.session().attribute("invites.message", lang(req).get("email_invite_sent"));
			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();
			resp.redirect(back(req));
			return "";
		}catch(UserErrorException x){
			return wrapForm(req, resp, "settings_email_invite_form", "/settings/invites/createEmailInvite", lang(req).get("invite_by_email"), "send", "createEmailInvite", List.of("email", "first_name", "last_name", "add_friend"), null, lang(req).get(x.getMessage()));
		}
	}

	public static Object resendEmailInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		if(id<=0)
			throw new BadRequestException();
		String msg;
		try{
			ctx.getUsersController().resendEmailInvite(req, self, id);
			msg=lang(req).get("email_invite_resent");
		}catch(UserErrorException x){
			msg=lang(req).get(x.getMessage());
		}
		if(isAjax(req))
			return new WebDeltaResponse(resp).show("invitesMessage").setContent("invitesMessage", TextProcessor.escapeHTML(msg));
		req.session().attribute("invites.message", msg);
		resp.redirect(back(req));
		return "";
	}

	public static Object deleteInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		if(id<=0)
			throw new BadRequestException();
		SignupInvitation invite=ctx.getUsersController().getInvite(id);
		if(invite==null || invite.ownerID!=self.id)
			throw new ObjectNotFoundException();
		ctx.getUsersController().deleteInvite(id);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object createInviteLinkForm(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!sessionInfo(req).permissions.canInviteNewUsers)
			throw new UserActionNotAllowedException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_link_invite_form", req);
		return wrapForm(req, resp, "settings_link_invite_form", "/settings/invites/createInviteLink", lang(req).get("invite_create_link_title"), "create", model);
	}

	public static Object createInviteLink(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!sessionInfo(req).permissions.canInviteNewUsers)
			throw new UserActionNotAllowedException();
		int signupCount=Math.max(1, parseIntOrDefault(req.queryParams("signups"), 1));
		String code=ctx.getUsersController().createInviteCode(self, signupCount, "on".equals(req.queryParams("add_friend")));
		String link=UriBuilder.local().path("account", "register").queryParam("invite", code).build().toString();
		req.session().attribute("invites.message", lang(req).get("invite_link_created")+"<br/><a herf=\"#\" onclick=\"copyText('"+link+"', '"+lang(req).get("link_copied")+"')\">"+lang(req).get("copy_link")+"</a>");
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object invitedUsers(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("settings_invited_users", req).paginate(ctx.getUsersController().getInvitedUsers(self, offset(req), 100)).pageTitle(lang(req).get("invited_people_title"));
	}

	public static Object privacySettings(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_privacy", req);
		model.with("settingsKeys", UserPrivacySettingKey.values()).pageTitle(lang(req).get("privacy_settings_title")).mobileToolbarTitle(lang(req).get("settings"));

		HashMap<UserPrivacySettingKey, PrivacySetting> settings=new HashMap<>(self.user.privacySettings);
		for(UserPrivacySettingKey key:UserPrivacySettingKey.values()){
			if(!settings.containsKey(key)){
				settings.put(key, key.getDefaultValue());
			}
		}
		model.with("privacySettings", settings);
		Set<Integer> needUsers=new HashSet<>();
		for(PrivacySetting ps:settings.values()){
			needUsers.addAll(ps.exceptUsers);
			needUsers.addAll(ps.allowUsers);
		}
		model.with("users", ctx.getUsersController().getUsers(needUsers));
		model.with("allFeedTypes", EnumSet.complementOf(EnumSet.of(FriendsNewsfeedTypeFilter.POSTS)));

		Templates.addJsLangForPrivacySettings(req);
		return model;
	}

	public static Object savePrivacySettings(Request req, Response resp, Account self, ApplicationContext ctx){
		HashMap<UserPrivacySettingKey, PrivacySetting> settings=new HashMap<>();
		for(UserPrivacySettingKey key:UserPrivacySettingKey.values()){
			if(req.queryParams(key.toString())==null)
				continue;
			String json=req.queryParams(key.toString());
			settings.put(key, PrivacySetting.fromJson(json));
		}
		EnumSet<FriendsNewsfeedTypeFilter> feedTypes;
		if(req.queryParams("needUpdateFeedTypes")!=null){
			if(StringUtils.isNotEmpty(req.queryParams("allFeedTypes"))){
				feedTypes=null;
			}else{
				feedTypes=EnumSet.noneOf(FriendsNewsfeedTypeFilter.class);
				for(FriendsNewsfeedTypeFilter type:FriendsNewsfeedTypeFilter.values()){
					if(type==FriendsNewsfeedTypeFilter.POSTS)
						continue;
					if(req.queryParams("feedTypes_"+type)!=null)
						feedTypes.add(type);
				}
			}
		}else{
			feedTypes=self.user.newsTypesToShow;
		}
		ctx.getPrivacyController().updateUserPrivacySettings(self.user, settings, feedTypes);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_privacy").setContent("formMessage_privacy", lang(req).get("privacy_settings_saved"));
		}
		resp.redirect("/settings/privacy");
		return "";
	}

	public static Object mobileEditPrivacy(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "key");
		if(!isMobile(req)){
			resp.redirect("/settings/privacy");
			return "";
		}
		UserPrivacySettingKey key;
		try{
			key=UserPrivacySettingKey.valueOf(req.queryParams("key"));
		}catch(IllegalArgumentException x){
			resp.redirect("/settings/privacy");
			return "";
		}
		PrivacySetting ps=self.user.privacySettings.computeIfAbsent(key, k->new PrivacySetting());
		Set<Integer> needUsers=new HashSet<>();
		needUsers.addAll(ps.exceptUsers);
		needUsers.addAll(ps.allowUsers);
		jsLangKey(req, "save", "select_friends_title", "friends_search_placeholder");
		return new RenderedTemplateResponse("settings_privacy_edit", req)
				.with("key", key)
				.with("setting", ps)
				.with("users", ctx.getUsersController().getUsers(needUsers))
				.pageTitle(lang(req).get("privacy_settings_title"));
	}

	public static Object mobileFeedTypes(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isMobile(req)){
			resp.redirect("/settings/privacy");
			return "";
		}
		return new RenderedTemplateResponse("settings_privacy_feed_types", req)
				.with("allFeedTypes", EnumSet.complementOf(EnumSet.of(FriendsNewsfeedTypeFilter.POSTS)))
				.pageTitle(lang(req).get("privacy_settings_title"));
	}

	public static Object mobilePrivacyBox(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isMobile(req))
			return ajaxAwareRedirect(req, resp, back(req));
		requireQueryParams(req, "value", "onlyMe");
		PrivacySetting setting=PrivacySetting.fromJson(req.queryParams("value"));
		boolean onlyMe=Boolean.parseBoolean(req.queryParams("onlyMe"));
		Set<Integer> needUsers=new HashSet<>();
		needUsers.addAll(setting.exceptUsers);
		needUsers.addAll(setting.allowUsers);
		return new RenderedTemplateResponse("privacy_setting_selector", req)
				.with("setting", setting)
				.with("onlyMe", onlyMe)
				.with("users", ctx.getUsersController().getUsers(needUsers));
	}

	public static Object deactivateAccountForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "deactivate_account_form", "/settings/deactivateAccount", lang(req).get("admin_user_delete_account_title"), "delete", "deactivateAccount", List.of(), null, null);
	}

	public static Object deactivateAccount(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "password");
		String password=req.queryParams("password");
		if(!ctx.getUsersController().checkPassword(self, password)){
			return wrapForm(req, resp, "deactivate_account_form", "/settings/deactivateAccount", lang(req).get("admin_user_delete_account_title"), "delete", "deactivateAccount", List.of(), null, lang(req).get("err_old_password_incorrect"));
		}
		ctx.getUsersController().selfDeactivateAccount(self);
		return ajaxAwareRedirect(req, resp, "/feed");
	}

	public static Object saveAppearanceBehaviorSettings(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "textFormat", "commentView");
		self.prefs.textFormat=enumValue(req.queryParams("textFormat"), FormattedTextFormat.class);
		self.prefs.commentViewType=enumValue(req.queryParams("commentView"), CommentViewType.class);
		boolean newCountLikes="on".equals(req.queryParams("countLikesInUnread"));
		boolean needResetCounters=newCountLikes!=self.prefs.countLikesInUnread;
		self.prefs.countLikesInUnread=newCountLikes;
		try{
			SessionStorage.updatePreferences(self.id, self.prefs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		if(needResetCounters){
			ctx.getNotificationsController().recountCounters(self.user);
		}

		String msg=lang(req).get("settings_saved");
		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_appearanceBehavior").setContent("formMessage_appearanceBehavior", msg);
		req.session().attribute("settings.appearanceBehaviorMessage", msg);
		resp.redirect(back(req));
		return "";
	}

	public static Object updateUsername(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "username");
		String username=req.queryParams("username").trim();
		String msg;
		boolean success=false;
		try{
			ctx.getUsersController().updateUsername(self.user, username);
			msg=lang(req).get("settings_username_changed");
			success=true;
		}catch(UserErrorException x){
			msg=lang(req).get(x.getMessage());
		}
		if(isAjax(req)){
			WebDeltaResponse wdr=new WebDeltaResponse(resp).show("formMessage_changeUsername").setContent("formMessage_changeUsername", msg);
			if(success)
				wdr.setAttribute("myProfileLink", "href", "/"+username);
			return wdr;
		}
		req.session().attribute("settings.usernameMessage", msg);
		resp.redirect(back(req));
		return "";
	}

	public static Object sessions(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "settings_activity_history" : "content_wrap", req);
		model.with("sessions", ctx.getUsersController().getAccountSessions(info.account))
				.with("currentSessionID", Base64.getDecoder().decode(req.cookie("psid")))
				.with("isAjax", isAjax(req));
		Lang l=lang(req);
		if(isAjax(req)){
			String auxLink="<a href=\"/settings/confirmEndOtherSessions\" data-confirm-action=\"/settings/endOtherSessions\" data-confirm-title=\""+
					TextProcessor.escapeHTML(l.get("settings_end_all_sessions"))+"\" data-confirm-message=\""+
					TextProcessor.escapeHTML(l.get("settings_confirm_end_all_sessions"))+"\">"+
					TextProcessor.escapeHTML(l.get("settings_end_all_sessions"))+"</a>";
			return new WebDeltaResponse(resp)
					.box(l.get("settings_sessions"), model.renderToString(), "settingsSessions", true, auxLink);
		}
		model.with("contentTemplate", "settings_activity_history").pageTitle(l.get("settings_sessions"));
		return model;
	}

	public static Object confirmEndOtherSessions(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("settings_end_all_sessions"), l.get("settings_confirm_end_all_sessions"), "/settings/endOtherSessions");
	}

	public static Object endOtherSessions(Request req, Response resp, Account self, ApplicationContext ctx){
		ctx.getUsersController().terminateSessionsExcept(self, req.cookie("psid"));
		if(isAjax(req))
			return new WebDeltaResponse(resp).replaceLocation("/settings/?sessionsTerminated");
		resp.redirect("/settings/?sessionsTerminated");
		return "";
	}

	public static Object profileEditInterests(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("profile_edit_interests", req)
				.pageTitle(lang(req).get("edit_profile"))
				.addMessage(req, "settings.profileEditInterestsMessage", "profileInterestsMessage");
	}

	public static Object updateProfileInterests(Request req, Response resp, Account self, ApplicationContext ctx){
		ctx.getUsersController().updateProfileInterests(self.user, req.queryParams("about"), req.queryParams("activities"), req.queryParams("interests"), req.queryParams("music"),
				req.queryParams("movies"), req.queryParams("tv"), req.queryParams("books"), req.queryParams("games"), req.queryParams("quotes"));
		self.user=ctx.getUsersController().getUserOrThrow(self.user.id);
		String message=lang(req).get("profile_info_updated");
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_profileInterests").setContent("formMessage_profileInterests", message);
		}
		req.session().attribute("settings.profileEditInterestsMessage", message);
		resp.redirect("/settings/profile/interests");
		return "";
	}

	public static Object profileEditPersonal(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("profile_edit_personal", req)
				.pageTitle(lang(req).get("edit_profile"))
				.with("politicalOptions", User.PoliticalViews.values())
				.with("personalPriorityOptions", User.PersonalPriority.values())
				.with("peoplePriorityOptions", User.PeoplePriority.values())
				.with("habitsOptions", User.HabitsViews.values())
				.addMessage(req, "settings.profileEditPersonalMessage", "profilePersonalMessage");
	}

	public static Object updateProfilePersonal(Request req, Response resp, Account self, ApplicationContext ctx){
		ctx.getUsersController().updateProfilePersonal(self.user, enumValueOpt(req.queryParams("politicalViews"), User.PoliticalViews.class),
				req.queryParams("religion"), enumValueOpt(req.queryParams("personalPriority"), User.PersonalPriority.class),
				enumValueOpt(req.queryParams("peoplePriority"), User.PeoplePriority.class), enumValueOpt(req.queryParams("smokingViews"), User.HabitsViews.class),
				enumValueOpt(req.queryParams("alcoholViews"), User.HabitsViews.class), req.queryParams("inspiredBy"));
		self.user=ctx.getUsersController().getUserOrThrow(self.user.id);
		String message=lang(req).get("profile_info_updated");
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_profilePersonal").setContent("formMessage_profilePersonal", message);
		}
		req.session().attribute("settings.profileEditPersonalMessage", message);
		resp.redirect("/settings/profile/personal");
		return "";
	}

	public static Object profileEditContacts(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("profile_edit_contacts", req)
				.pageTitle(lang(req).get("edit_profile"))
				.with("contactInfoKeys", User.ContactInfoKey.values())
				.addMessage(req, "settings.profileEditContactsMessage", "profileContactsMessage");
	}

	public static Object updateProfileContacts(Request req, Response resp, Account self, ApplicationContext ctx){
		Map<User.ContactInfoKey, String> contactInfo=req.queryMap("contactInfo")
				.toMap()
				.entrySet()
				.stream()
				.filter(e->e.getValue().length>0 && StringUtils.isNotEmpty(e.getValue()[0]))
				.map(e->{
					User.ContactInfoKey key=enumValue(e.getKey(), User.ContactInfoKey.class);
					return new Pair<>(key, TextProcessor.normalizeContactInfoValue(key, e.getValue()[0]));
				})
				.collect(HashMap::new, (m,v)->m.put(v.first(), v.second()), HashMap::putAll);
		String location=req.queryParams("location");
		String website=req.queryParams("website");
		Lang l=lang(req);
		String message;
		if(contactInfo.containsValue(null)){
			ArrayList<String> invalidKeyNames=new ArrayList<>();
			for(User.ContactInfoKey key:User.ContactInfoKey.values()){
				if(contactInfo.containsKey(key) && contactInfo.get(key)==null){
					invalidKeyNames.add(key.isLocalizable() ? l.get(key.getLangKey()) : key.getFieldName());
				}
			}
			if(invalidKeyNames.size()==1){
				message=l.get("profile_edit_invalid_field", Map.of("fieldName", invalidKeyNames.getFirst()));
			}else{
				message=l.get("profile_edit_invalid_field_multiple", Map.of("fieldList", String.join(", ")));
			}
		}else{
			ctx.getUsersController().updateProfileContacts(self.user, contactInfo, location, website);
			self.user=ctx.getUsersController().getUserOrThrow(self.user.id);
			message=l.get("profile_info_updated");
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_profileContacts").setContent("formMessage_profileContacts", message);
		}
		req.session().attribute("settings.profileEditContactsMessage", message);
		resp.redirect("/settings/profile/contacts");
		return "";
	}

	public static Object notificationsSettings(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings_notifications", req)
				.pageTitle(lang(req).get("notifications"))
				.with("notifierTypes", self.prefs.notifierTypes)
				.with("notifierEnableSound", self.prefs.notifierEnableSound)
				.with("notifierShowMessageText", self.prefs.notifierShowMessageText)
				.with("allNotifierTypes", RealtimeNotificationSettingType.values())
				.addMessage(req, "settings.notifierMessage", "notificationsMessage");
		return model;
	}

	public static Object updateNotifierSettings(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<RealtimeNotificationSettingType> notifierTypes;
		if(StringUtils.isNotEmpty(req.queryParams("allNotifierTypes"))){
			notifierTypes=null;
		}else{
			notifierTypes=EnumSet.noneOf(RealtimeNotificationSettingType.class);
			for(RealtimeNotificationSettingType type:RealtimeNotificationSettingType.values()){
				if(req.queryParams("notifierTypes_"+type)!=null)
					notifierTypes.add(type);
			}
		}
		self.prefs.notifierTypes=notifierTypes;
		self.prefs.notifierEnableSound="on".equals(req.queryParams("notifierEnableSound"));
		self.prefs.notifierShowMessageText="on".equals(req.queryParams("notifierShowMessageText"));
		ctx.getUsersController().updateUserPreferences(self);
		req.session().attribute("settings.notifierMessage", lang(req).get("settings_saved"));
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.refresh();
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object filters(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("settings_filters", req)
				.with("filters", ctx.getNewsfeedController().getWordFilters(self.user, true))
				.pageTitle(lang(req).get("feed_filters"));
	}

	public static Object createFilterForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("settings_filter_create", req)
				.pageTitle(lang(req).get("feed_filters"));
	}

	public static Object createFilter(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "name", "word", "expire");
		String name=req.queryParams("name");
		List<String> words=Arrays.stream(req.queryParamsValues("word"))
				.map(String::trim)
				.filter(StringUtils::isNotEmpty)
				.toList();
		if(words.isEmpty())
			throw new BadRequestException();
		int expire=safeParseInt(req.queryParams("expire"));
		EnumSet<FilterContext> contexts=Arrays.stream(FilterContext.values())
				.filter(c->"on".equals(req.queryParams("contexts_"+c)))
				.collect(Collectors.toCollection(()->EnumSet.noneOf(FilterContext.class)));
		if(contexts.isEmpty())
			throw new BadRequestException();

		ctx.getNewsfeedController().createWordFilter(self.user, name, words, contexts, expire!=0 ? Instant.now().plusSeconds(expire) : null);

		return ajaxAwareRedirect(req, resp, "/settings/filters");
	}

	public static Object editFilterForm(Request req, Response resp, Account self, ApplicationContext ctx){
		WordFilter filter=ctx.getNewsfeedController().getWordFilter(self.user, safeParseInt(req.params(":id")));
		return new RenderedTemplateResponse("settings_filter_create", req)
				.with("filter", filter)
				.with("contexts", filter.contexts.stream().map(Object::toString).collect(Collectors.toSet()))
				.pageTitle(lang(req).get("feed_filters"));
	}

	public static Object editFilter(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "name", "word", "expire");

		String name=req.queryParams("name");
		List<String> words=Arrays.stream(req.queryParamsValues("word"))
				.map(String::trim)
				.filter(StringUtils::isNotEmpty)
				.toList();
		if(words.isEmpty())
			throw new BadRequestException();
		EnumSet<FilterContext> contexts=Arrays.stream(FilterContext.values())
				.filter(c->"on".equals(req.queryParams("contexts_"+c)))
				.collect(Collectors.toCollection(()->EnumSet.noneOf(FilterContext.class)));
		if(contexts.isEmpty())
			throw new BadRequestException();

		WordFilter filter=ctx.getNewsfeedController().getWordFilter(self.user, safeParseInt(req.params(":id")));
		String expireInput=req.queryParams("expire");
		Instant expire;
		if(expireInput.equals("unchanged")){
			expire=filter.expiresAt;
		}else{
			int expireInt=safeParseInt(req.queryParams("expire"));
			expire=expireInt!=0 ? Instant.now().plusSeconds(expireInt) : null;
		}

		ctx.getNewsfeedController().updateWordFilter(self.user, filter, name, words, contexts, expire);
		return ajaxAwareRedirect(req, resp, "/settings/filters");
	}

	public static Object confirmDeleteFilter(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("settings_confirm_delete_filter"), "/settings/filters/"+req.params(":id")+"/delete?csrf="+info.csrfToken);
	}

	public static Object deleteFilter(Request req, Response resp, Account self, ApplicationContext ctx){
		WordFilter filter=ctx.getNewsfeedController().getWordFilter(self.user, safeParseInt(req.params(":id")));
		ctx.getNewsfeedController().deleteWordFilter(self.user, filter);
		if(isAjax(req))
			return new WebDeltaResponse(resp).remove("filter"+filter.id);
		resp.redirect(back(req));
		return "";
	}
}
