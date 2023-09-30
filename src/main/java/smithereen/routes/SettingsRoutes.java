package smithereen.routes;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;

import smithereen.ApplicationContext;
import smithereen.Config;
import static smithereen.Utils.*;

import smithereen.Mailer;
import smithereen.Utils;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.model.Account;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.PrivacySetting;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.UriBuilder;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.libvips.VipsImage;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.FloodControl;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.StringUtils;

public class SettingsRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(SettingsRoutes.class);

	public static Object settings(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("settings", req);
		model.with("languages", Lang.list).with("selectedLang", Utils.lang(req));
		Session s=req.session();
		if(s.attribute("settings.passwordMessage")!=null){
			model.with("passwordMessage", s.attribute("settings.passwordMessage"));
			s.removeAttribute("settings.passwordMessage");
		}
		if(s.attribute("settings.inviteMessage")!=null){
			model.with("inviteMessage", s.attribute("settings.inviteMessage"));
			s.removeAttribute("settings.inviteMessage");
		}
		if(s.attribute("settings.profilePicMessage")!=null){
			model.with("profilePicMessage", s.attribute("settings.profilePicMessage"));
			s.removeAttribute("settings.profilePicMessage");
		}
		if(s.attribute("settings.emailMessage")!=null){
			model.with("emailMessage", s.attribute("settings.emailMessage"));
			s.removeAttribute("settings.emailMessage");
		}
		model.with("activationInfo", self.activationInfo);
		model.with("currentEmailMasked", self.getCurrentEmailMasked());
		model.with("title", lang(req).get("settings"));
		return model;
	}

	public static Object createInvite(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		if(Config.signupMode==Config.SignupMode.OPEN){
			throw new BadRequestException();
		}
		if(Config.signupMode==Config.SignupMode.CLOSED && self.accessLevel!=Account.AccessLevel.ADMIN)
			return wrapError(req, resp, "err_access");
		byte[] code=new byte[16];
		new Random().nextBytes(code);
		UserStorage.putInvite(self.id, code, 1, null, null);
		req.session().attribute("settings.inviteMessage", Utils.lang(req).get("invitation_created"));
		resp.redirect("/settings/");
		return "";
	}

	public static Object updatePassword(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String current=req.queryParams("current");
		String new1=req.queryParams("new");
		String new2=req.queryParams("new2");
		String message;
		if(!new1.equals(new2)){
			message=Utils.lang(req).get("err_passwords_dont_match");
		}else if(new1.length()<4){
			message=Utils.lang(req).get("err_password_short");
		}else if(!SessionStorage.updatePassword(self.id, current, new1)){
			message=Utils.lang(req).get("err_old_password_incorrect");
		}else{
			message=Utils.lang(req).get("password_changed");
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_changePassword").setContent("formMessage_changePassword", message);
		}
		req.session().attribute("settings.passwordMessage", message);
		resp.redirect("/settings/");
		return "";
	}

	public static Object updateProfileGeneral(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String first=req.queryParams("first_name");
		String last=req.queryParams("last_name");
		String middle=req.queryParams("middle_name");
		String maiden=req.queryParams("maiden_name");
		String about=req.queryParams("about");
		String aboutSource=about;
		if(StringUtils.isNotEmpty(about))
			about=preprocessPostHTML(about, null);
		else
			about=null;
		User.Gender gender=enumValue(req.queryParamOrDefault("gender", "UNKNOWN"), User.Gender.class);
		LocalDate bdate=self.user.birthDate;
		String _bdate=req.queryParams("bdate");
		if(_bdate!=null){
			try{
				bdate=LocalDate.parse(_bdate);
			}catch(DateTimeParseException ignore){}
		}
		String message;
		if(first.length()<2){
			message=Utils.lang(req).get("err_name_too_short");
		}else{
			UserStorage.changeBasicInfo(self.user, first, last, middle, maiden, gender, bdate, about, aboutSource);
			message=Utils.lang(req).get("profile_info_updated");
		}
		self.user=UserStorage.getById(self.user.id);
		if(self.user==null)
			throw new IllegalStateException("?!");
		ctx.getActivityPubWorker().sendUpdateUserActivity(self.user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_profileEdit").setContent("formMessage_profileEdit", message);
		}
		req.session().attribute("settings.profileEditMessage", message);
		resp.redirect("/settings/profile/general");
		return "";
	}

	public static Object updateProfilePicture(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		try{
			int groupID=parseIntOrDefault(req.queryParams("group"), 0);
			Group group=null;
			if(groupID!=0){
				group=GroupStorage.getById(groupID);
				if(group==null || !GroupStorage.getGroupMemberAdminLevel(groupID, self.user.id).isAtLeast(Group.AdminLevel.ADMIN)){
					throw new UserActionNotAllowedException();
				}
			}

			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 5*1024*1024, -1L, 0));
			Part part=req.raw().getPart("pic");
			if(part.getSize()>5*1024*1024){
				throw new IOException("file too large");
			}

			byte[] key=MessageDigest.getInstance("MD5").digest((self.user.username+","+System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
			String keyHex=Utils.byteArrayToHexString(key);
			String mime=part.getContentType();
			if(!mime.startsWith("image/"))
				throw new IOException("incorrect mime type");

			File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			File temp=new File(tmpDir, keyHex);
			part.write(keyHex);
			VipsImage img=new VipsImage(temp.getAbsolutePath());
			if(img.hasAlpha())
				img=img.flatten(1, 1, 1);
			float ratio=(float)img.getWidth()/(float)img.getHeight();
			boolean ratioIsValid=ratio<=2.5f && ratio>=0.25f;
			LocalImage ava=new LocalImage();
			if(ratioIsValid){
				try{
					String _x1=req.queryParams("x1"),
							_x2=req.queryParams("x2"),
							_y1=req.queryParams("y1"),
							_y2=req.queryParams("y2");
					if(_x1!=null && _x2!=null && _y1!=null && _y2!=null){
						float x1=Float.parseFloat(_x1);
						float x2=Float.parseFloat(_x2);
						float y1=Float.parseFloat(_y1);
						float y2=Float.parseFloat(_y2);
						if(x1 >= 0f && x1<=1f && y1 >= 0f && y1<=1f && x2 >= 0f && x2<=1f && y2 >= 0f && y2<=1f && x1<x2 && y1<y2){
							float iw=img.getWidth();
							float ih=img.getHeight();
							int x=Math.round(iw*x1);
							int y=Math.round(ih*y1);
							int size=Math.round(((x2-x1)*iw+(y2-y1)*ih)/2f);
							ava.cropRegion=new float[]{x1, y1, x2, y2};
						}
					}
				}catch(NumberFormatException ignore){}
			}
			if(ava.cropRegion==null && img.getWidth()!=img.getHeight()){
				int cropSize, cropX=0;
				if(img.getHeight()>img.getWidth()){
					cropSize=img.getWidth();
					ava.cropRegion=new float[]{0f, 0f, 1f, (float)img.getWidth()/(float)img.getHeight()};
				}else{
					cropSize=img.getHeight();
					cropX=img.getWidth()/2-img.getHeight()/2;
					ava.cropRegion=new float[]{(float)cropX/(float)img.getWidth(), 0f, (float)(cropX+img.getHeight())/(float)img.getWidth(), 1f};
				}
				if(!ratioIsValid){
					VipsImage cropped=img.crop(cropX, 0, cropSize, cropSize);
					img.release();
					img=cropped;
				}
			}

			File profilePicsDir=new File(Config.uploadPath, "avatars");
			profilePicsDir.mkdirs();
			try{
				int[] size={0, 0};
				MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, 93, keyHex, profilePicsDir, size);
				ava.localID=keyHex;
				ava.path="avatars";
				ava.width=size[0];
				ava.height=size[1];

				if(group==null){
					if(self.user.icon!=null){
						LocalImage li=(LocalImage) self.user.icon.get(0);
						File file=new File(profilePicsDir, li.localID+".webp");
						if(file.exists()){
							LOG.info("Deleting: {}", file.getAbsolutePath());
							file.delete();
						}
					}
					UserStorage.updateProfilePicture(self.user, MediaStorageUtils.serializeAttachment(ava).toString());
					self.user=UserStorage.getById(self.user.id);
					ctx.getActivityPubWorker().sendUpdateUserActivity(self.user);
				}else{
					if(group.icon!=null && !(group instanceof ForeignGroup)){
						LocalImage li=(LocalImage) group.icon.get(0);
						File file=new File(profilePicsDir, li.localID+".webp");
						if(file.exists()){
							LOG.info("Deleting: {}", file.getAbsolutePath());
							file.delete();
						}
					}
					GroupStorage.updateProfilePicture(group, MediaStorageUtils.serializeAttachment(ava).toString());
					group=GroupStorage.getById(group.id);
					ctx.getActivityPubWorker().sendUpdateGroupActivity(group);
				}
				temp.delete();
			}finally{
				img.release();
			}
			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();

			req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("avatar_updated"));
			resp.redirect("/settings/");
		}catch(IOException|ServletException|NoSuchAlgorithmException|IllegalStateException x){
			LOG.error("Exception while processing a profile picture upload", x);
			if(isAjax(req)){
				Lang l=lang(req);
				return new WebDeltaResponse(resp).messageBox(l.get("error"), l.get("image_upload_error")+"<br/>"+x.getMessage(), l.get("ok"));
			}

			req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"));
			resp.redirect("/settings/");
		}
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

	public static Object removeProfilePicture(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		int groupID=parseIntOrDefault(req.queryParams("group"), 0);
		Group group=null;
		if(groupID!=0){
			group=GroupStorage.getById(groupID);
			if(group==null || !GroupStorage.getGroupMemberAdminLevel(groupID, self.user.id).isAtLeast(Group.AdminLevel.ADMIN)){
				throw new UserActionNotAllowedException();
			}
		}

		File profilePicsDir=new File(Config.uploadPath, "avatars");
		List<Image> icon=group!=null ? group.icon : self.user.icon;
		if(icon!=null && !icon.isEmpty() && icon.get(0) instanceof LocalImage){
			LocalImage li=(LocalImage) icon.get(0);
			File file=new File(profilePicsDir, li.localID+".webp");
			if(file.exists()){
				LOG.info("Deleting: {}", file.getAbsolutePath());
				file.delete();
			}
		}

		if(group!=null){
			GroupStorage.updateProfilePicture(group, null);
			group=GroupStorage.getById(groupID);
			ctx.getActivityPubWorker().sendUpdateGroupActivity(group);
		}else{
			UserStorage.updateProfilePicture(self.user, null);
			self.user=UserStorage.getById(self.user.id);
			ctx.getActivityPubWorker().sendUpdateUserActivity(self.user);
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
			return new WebDeltaResponse(resp).show("invitesMessage").setContent("invitesMessage", escapeHTML(msg));
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
				settings.put(key, new PrivacySetting());
			}
		}
		model.with("privacySettings", settings);
		Set<Integer> needUsers=new HashSet<>();
		for(PrivacySetting ps:settings.values()){
			needUsers.addAll(ps.exceptUsers);
			needUsers.addAll(ps.allowUsers);
		}
		model.with("users", ctx.getUsersController().getUsers(needUsers));

		jsLangKey(req,
				"privacy_value_everyone", "privacy_value_friends", "privacy_value_friends_of_friends", "privacy_value_no_one",
				"privacy_value_only_me", "privacy_value_everyone_except", "privacy_value_certain_friends",
				"save", "privacy_settings_title", "privacy_allowed_title", "privacy_denied_title", "privacy_allowed_to_X",
				"privacy_value_to_everyone", "privacy_value_to_friends", "privacy_value_to_friends_of_friends", "privacy_value_to_certain_friends", "delete", "privacy_enter_friend_name",
				"privacy_settings_value_except", "privacy_settings_value_certain_friends_before", "privacy_settings_value_name_separator",
				"select_friends_title", "friends_search_placeholder", "friend_list_your_friends", "friends_in_list", "select_friends_empty_selection");
		return model;
	}

	public static Object savePrivacySettings(Request req, Response resp, Account self, ApplicationContext ctx){
		HashMap<UserPrivacySettingKey, PrivacySetting> settings=new HashMap<>();
		for(UserPrivacySettingKey key:UserPrivacySettingKey.values()){
			if(req.queryParams(key.toString())==null)
				continue;
			PrivacySetting ps;
			try{
				ps=gson.fromJson(req.queryParams(key.toString()), PrivacySetting.class);
			}catch(Exception x){
				throw new BadRequestException(x);
			}
			if(ps.baseRule==null)
				throw new BadRequestException();
			if(ps.allowUsers==null)
				ps.allowUsers=Set.of();
			if(ps.exceptUsers==null)
				ps.exceptUsers=Set.of();
			settings.put(key, ps);
		}
		ctx.getPrivacyController().updateUserPrivacySettings(self.user, settings);
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
}
