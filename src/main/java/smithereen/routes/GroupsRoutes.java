package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.controllers.GroupsController;
import smithereen.data.ActorWithDescription;
import smithereen.data.ForeignUser;
import smithereen.data.PaginatedList;
import smithereen.data.SizedImage;
import smithereen.exceptions.BadRequestException;
import smithereen.Config;
import smithereen.data.GroupAdmin;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.data.Account;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.WebDeltaResponse;
import smithereen.lang.Lang;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class GroupsRoutes{

	private static Group getGroup(Request req){
		int id=parseIntOrDefault(req.params(":id"), 0);
		return context(req).getGroupsController().getGroupOrThrow(id);
	}

	private static Group getGroupAndRequireLevel(Request req, Account self, Group.AdminLevel level){
		Group group=getGroup(req);
		context(req).getGroupsController().enforceUserAdminLevel(group, self.user, level);
		return group;
	}

	public static Object myGroups(Request req, Response resp, Account self){
		return userGroups(req, resp, self.user);
	}

	public static Object userGroups(Request req, Response resp){
		int uid=parseIntOrDefault(req.params(":id"), 0);
		User user=context(req).getUsersController().getUserOrThrow(uid);
		return userGroups(req, resp, user);
	}

	public static Object userGroups(Request req, Response resp, User user){
		jsLangKey(req, "cancel", "create");
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("tab", "groups").with("title", lang(req).get("groups"));
		model.paginate(context(req).getGroupsController().getUserGroups(user, offset(req), 100));
		model.with("owner", user);
		return model;
	}

	public static Object myManagedGroups(Request req, Response resp, Account self){
		jsLangKey(req, "cancel", "create");
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("tab", "managed").with("title", lang(req).get("groups"));
		model.paginate(context(req).getGroupsController().getUserManagedGroups(self.user, offset(req), 100)).with("owner", self.user);
		return model;
	}

	public static Object myEvents(Request req, Response resp, Account self){
		return myEvents(req, resp, self, GroupsController.EventsType.FUTURE);
	}

	public static Object myPastEvents(Request req, Response resp, Account self){
		return myEvents(req, resp, self, GroupsController.EventsType.PAST);
	}

	public static Object myEvents(Request req, Response resp, Account self, GroupsController.EventsType type){
		jsLangKey(req, "cancel", "create");
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("events", true).with("tab", type==GroupsController.EventsType.PAST ? "past" : "events").with("owner", self.user).pageTitle(lang(req).get("events"));
		model.paginate(context(req).getGroupsController().getUserEvents(self.user, type, offset(req), 100));
		return model;
	}

	public static Object createGroup(Request req, Response resp, Account self){
		RenderedTemplateResponse model=new RenderedTemplateResponse("create_group", req);
		return wrapForm(req, resp, "create_group", "/my/groups/create", lang(req).get("create_group_title"), "create", model);
	}

	public static Object createEvent(Request req, Response resp, Account self){
		RenderedTemplateResponse model=new RenderedTemplateResponse("create_event", req);
		return wrapForm(req, resp, "create_event", "/my/groups/create?type=event", lang(req).get("create_event_title"), "create", model);
	}

	private static Object groupCreateError(Request req, Response resp, String errKey){
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_createGroup").setContent("formMessage_createGroup", lang(req).get(errKey));
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("create_group", req);
		model.with("groupName", req.queryParams("name")).with("groupUsername", req.queryParams("username"));
		return wrapForm(req, resp, "create_group", "/my/groups/create", lang(req).get("create_group_title"), "create", model);
	}

	public static Object doCreateGroup(Request req, Response resp, Account self){
		String name=req.queryParams("name");
		String description=req.queryParams("description");
		Group group;
		if("event".equals(req.queryParams("type"))){
			String eventTime=req.queryParams("event_start_time");
			String eventDate=req.queryParams("event_start_date");
			if(StringUtils.isEmpty(eventDate) || StringUtils.isEmpty(eventTime))
				throw new BadRequestException("date/time empty");

			try{
				Instant eventStart=instantFromDateAndTime(req, eventDate, eventTime);
				group=context(req).getGroupsController().createEvent(self.user, name, description, eventStart, null);
			}catch(DateTimeParseException x){
				throw new BadRequestException(x);
			}
		}else{
			group=context(req).getGroupsController().createGroup(self.user, name, description);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).replaceLocation("/"+group.username);
		}else{
			resp.redirect(Config.localURI("/"+group.username).toString());
			return "";
		}
	}

	public static Object groupProfile(Request req, Response resp, Group group){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;

		List<User> members=context(req).getGroupsController().getRandomMembersForProfile(group, false);
		int offset=offset(req);
		PaginatedList<Post> wall=context(req).getWallController().getWallPosts(group, false, offset, 20);

		if(req.attribute("mobile")==null){
			context(req).getWallController().populateCommentPreviews(wall.list);
		}

		Map<Integer, UserInteractions> interactions=context(req).getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);
		Lang l=lang(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("group", req);
		model.with("group", group).with("members", members).with("postCount", wall.total).paginate(wall);
		if(group.isEvent())
			model.with("tentativeMembers", context(req).getGroupsController().getRandomMembersForProfile(group, true));
		model.with("postInteractions", interactions);
		model.with("title", group.name);
		model.with("admins", context(req).getGroupsController().getAdmins(group));
		if(group instanceof ForeignGroup)
			model.with("noindex", true);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "remove_friend", "cancel", "delete", "post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw", "attach_menu_poll", "max_file_size_exceeded", "max_attachment_count_exceeded", "remove_attachment");
		jsLangKey(req, "create_poll_question", "create_poll_options", "create_poll_add_option", "create_poll_delete_option", "create_poll_multi_choice", "create_poll_anonymous", "create_poll_time_limit", "X_days", "X_hours");
		if(self!=null){
			Group.AdminLevel level=context(req).getGroupsController().getMemberAdminLevel(group, self.user);
			Group.MembershipState membershipState=context(req).getGroupsController().getUserMembershipState(group, self.user);
			model.with("membershipState", membershipState);
			model.with("groupAdminLevel", level);
			if(level.isAtLeast(Group.AdminLevel.ADMIN)){
				jsLangKey(req, "update_profile_picture", "save", "profile_pic_select_square_version", "drag_or_choose_file", "choose_file",
						"drop_files_here", "picture_too_wide", "picture_too_narrow", "ok", "error", "error_loading_picture",
						"remove_profile_picture", "confirm_remove_profile_picture", "choose_file_mobile");
			}
			if(group.isEvent()){
				if(membershipState==Group.MembershipState.MEMBER)
					model.with("membershipStateText", l.get("event_joined_certain"));
				else if(membershipState==Group.MembershipState.TENTATIVE_MEMBER)
					model.with("membershipStateText", l.get("event_joined_tentative"));
			}
		}else{
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:type", "profile");
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:title", group.name);
			meta.put("og:url", group.url.toString());
			meta.put("og:username", group.getFullUsername());
			String descr=l.get("X_members", Map.of("count", group.memberCount))+", "+l.get("X_posts", Map.of("count", wall.total));
			if(StringUtils.isNotEmpty(group.summary))
				descr+="\n"+Jsoup.clean(group.summary, Whitelist.none());
			meta.put("og:description", descr);
			if(group.hasAvatar()){
				URI img=group.getAvatar().getUriForSizeAndFormat(SizedImage.Type.LARGE, SizedImage.Format.JPEG);
				if(img!=null){
					SizedImage.Dimensions size=group.getAvatar().getDimensionsForSize(SizedImage.Type.LARGE);
					meta.put("og:image", img.toString());
					meta.put("og:image:width", size.width+"");
					meta.put("og:image:height", size.height+"");
				}
			}
			model.with("metaTags", meta);
			model.with("moreMetaTags", Map.of("description", descr));
		}
		model.with("activityPubURL", group.activityPubID);
		model.addNavBarItem(l.get(switch(group.type){
			case GROUP -> "open_group";
			case EVENT -> "open_event";
		}));
		ArrayList<PropertyValue> profileFields=new ArrayList<>();
		if(StringUtils.isNotEmpty(group.summary))
			profileFields.add(new PropertyValue(l.get(group.type==Group.Type.EVENT ? "about_event" : "about_group"), group.summary));
		if(group.type==Group.Type.EVENT){
			profileFields.add(new PropertyValue(l.get("event_start_time"), l.formatDate(group.eventStartTime, timeZoneForRequest(req), false)));
			if(group.eventEndTime!=null)
				profileFields.add(new PropertyValue(l.get("event_end_time"), l.formatDate(group.eventEndTime, timeZoneForRequest(req), false)));
		}
		model.with("profileFields", profileFields);
		return model;
	}

	public static Object join(Request req, Response resp, Account self){
		Group group=getGroup(req);
		context(req).getGroupsController().joinGroup(group, self.user, "1".equals(req.queryParams("tentative")));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
		return "";
	}

	public static Object leave(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroup(req);
		context(req).getGroupsController().leaveGroup(group, self.user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
		return "";
	}

	public static Object editGeneral(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_general", req);
		model.with("group", group).with("title", group.name);
		Session s=req.session();
		if(s.attribute("settings.groupEditMessage")!=null){
			model.with("groupEditMessage", s.attribute("settings.groupEditMessage"));
			s.removeAttribute("settings.groupEditMessage");
		}
		return model;
	}

	public static Object saveGeneral(Request req, Response resp, Account self){
		Group group=getGroup(req);
		String name=req.queryParams("name"), about=req.queryParams("about");
		String message;
		try{
			if(StringUtils.isEmpty(name) || name.length()<1)
				throw new BadRequestException(lang(req).get("group_name_too_short"));

			Instant eventStart=null, eventEnd=null;
			if(group.isEvent()){
				String startTime=req.queryParams("event_start_time"), startDate=req.queryParams("event_start_date");
				String endTime=req.queryParams("event_end_time"), endDate=req.queryParams("event_end_date");
				if(StringUtils.isEmpty(startTime) || StringUtils.isEmpty(startDate))
					throw new BadRequestException("start date/time empty");
				try{
					eventStart=instantFromDateAndTime(req, startDate, startTime);
					if(StringUtils.isNotEmpty(endDate) && StringUtils.isNotEmpty(endTime))
						eventEnd=instantFromDateAndTime(req, endDate, endTime);
				}catch(DateTimeParseException x){
					throw new BadRequestException(x);
				}
				if(eventEnd!=null && eventStart.isAfter(eventEnd))
					throw new BadRequestException(lang(req).get("err_event_end_time_before_start"));
			}

			if(StringUtils.isEmpty(about))
				about=null;

			context(req).getGroupsController().updateGroupInfo(group, self.user, name, about, eventStart, eventEnd);

			message=lang(req).get(group.isEvent() ? "event_info_updated" : "group_info_updated");
		}catch(BadRequestException x){
			message=x.getMessage();
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).show("formMessage_groupEdit").setContent("formMessage_groupEdit", message);
		}
		req.session().attribute("settings.groupEditMessage", message);
		resp.redirect("/groups/"+group.id+"/edit");
		return "";
	}

	public static Object members(Request req, Response resp){
		return members(req, resp, false);
	}

	public static Object tentativeMembers(Request req, Response resp){
		return members(req, resp, true);
	}

	private static Object members(Request req, Response resp, boolean tentative){
		Group group=getGroup(req);
		if(tentative && !group.isEvent())
			throw new BadRequestException();
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req);
		model.paginate(context(req).getGroupsController().getMembers(group, offset(req), 100, tentative));
		model.with("summary", lang(req).get(tentative ? "summary_event_X_tentative_members" : (group.isEvent() ? "summary_event_X_members" : "summary_group_X_members"), Map.of("count", tentative ? group.tentativeMemberCount : group.memberCount)));
		model.with("contentTemplate", "user_grid").with("title", group.name);
		return model;
	}

	public static Object admins(Request req, Response resp){
		Group group=getGroup(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("actor_list", req);
		model.with("actors", context(req).getGroupsController().getAdmins(group).stream().map(a->new ActorWithDescription(a.user, a.title)).collect(Collectors.toList()));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).box(lang(req).get(group.isEvent() ? "event_organizers" : "group_admins"), model.renderContentBlock(), null, true);
		}
		return model;
	}

	public static Object editAdmins(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_admins", req);
		model.with("group", group).with("title", group.name);
		model.with("admins", context(req).getGroupsController().getAdmins(group));
		jsLangKey(req, "cancel", "group_admin_demote", "yes", "no");
		return model;
	}

	public static Object editMembers(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		Group.AdminLevel level=context(req).getGroupsController().getMemberAdminLevel(group, self.user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_members", req);
		model.paginate(context(req).getGroupsController().getAllMembers(group, offset(req), 100));
		model.with("group", group).with("title", group.name);
		model.with("adminIDs", context(req).getGroupsController().getAdmins(group).stream().map(adm->adm.user.id).collect(Collectors.toList()));
		model.with("canAddAdmins", level.isAtLeast(Group.AdminLevel.ADMIN));
		model.with("adminLevel", level);
		jsLangKey(req, "cancel", "yes", "no");
		return model;
	}

	public static Object editAdminForm(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		User user=UserStorage.getById(userID);
		if(user==null)
			throw new ObjectNotFoundException("user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_admin", req);
		GroupAdmin admin=GroupStorage.getGroupAdmin(group.id, userID);
		model.with("existingAdmin", admin);
		return wrapForm(req, resp, "group_edit_admin", "/groups/"+group.id+"/saveAdmin?id="+userID, user.getFullName(), "save", model);
	}

	public static Object saveAdmin(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		User user=UserStorage.getById(userID);
		if(user==null)
			throw new ObjectNotFoundException("user_not_found");

		String _lvl=req.queryParams("level");
		String title=req.queryParams("title");
		Group.AdminLevel lvl=null;
		if(_lvl!=null){
			try{
				lvl=Group.AdminLevel.valueOf(_lvl);
				if(lvl==Group.AdminLevel.OWNER)
					lvl=null;
			}catch(Exception x){
				throw new BadRequestException(x);
			}
		}

		GroupStorage.addOrUpdateGroupAdmin(group.id, userID, title, lvl);

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object confirmDemoteAdmin(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=safeParseInt(req.queryParams("id"));
		User user=context(req).getUsersController().getUserOrThrow(userID);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", Utils.lang(req).get("group_admin_demote_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", Config.localURI("/groups/"+group.id+"/removeAdmin?_redir="+URLEncoder.encode(back)+"&id="+userID)).with("back", back);
	}

	public static Object removeAdmin(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=safeParseInt(req.queryParams("id"));
		User user=context(req).getUsersController().getUserOrThrow(userID);

		GroupStorage.removeGroupAdmin(group.id, userID);

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object editAdminReorder(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		int order=parseIntOrDefault(req.queryParams("order"), 0);
		if(order<0)
			throw new BadRequestException();

		GroupStorage.setGroupAdminOrder(group.id, userID, order);

		return "";
	}

	public static Object blocking(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		Group.AdminLevel level=GroupStorage.getGroupMemberAdminLevel(group.id, self.user.id);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_blocking", req).with("title", lang(req).get("settings_blocking"));
		model.with("blockedUsers", GroupStorage.getBlockedUsers(group.id));
		model.with("blockedDomains", GroupStorage.getBlockedDomains(group.id));
		model.with("group", group);
		model.with("adminLevel", level);
		jsLangKey(req, "unblock", "yes", "no", "cancel");
		return model;
	}

	public static Object blockDomainForm(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		RenderedTemplateResponse model=new RenderedTemplateResponse("block_domain", req);
		return wrapForm(req, resp, "block_domain", "/groups/"+group.id+"/blockDomain", lang(req).get("block_a_domain"), "block", model);
	}

	public static Object blockDomain(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		if(domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$")){
			if(GroupStorage.isDomainBlocked(group.id, domain))
				return wrapError(req, resp, "err_domain_already_blocked");
			GroupStorage.blockDomain(group.id, domain);
		}
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmUnblockDomain(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_domain_X", Map.of("domain", domain))).with("formAction", "/groups/"+group.id+"/unblockDomain?domain="+domain+"_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object unblockDomain(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		if(StringUtils.isNotEmpty(domain))
			GroupStorage.unblockDomain(group.id, domain);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	private static User getUserOrThrow(Request req){
		int id=parseIntOrDefault(req.queryParams("id"), 0);
		return context(req).getUsersController().getUserOrThrow(id);
	}

	public static Object confirmBlockUser(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_block_user_X", Map.of("user", user.getFirstLastAndGender()))).with("formAction", "/groups/"+group.id+"/blockUser?id="+user.id+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object confirmUnblockUser(Request req, Response resp, Account self){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_user_X", Map.of("user", user.getFirstLastAndGender()))).with("formAction", "/groups/"+group.id+"/unblockUser?id="+user.id+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object blockUser(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		if(GroupStorage.getGroupMemberAdminLevel(group.id, user.id).isAtLeast(Group.AdminLevel.MODERATOR))
			throw new BadRequestException("Can't block a group manager");
		GroupStorage.blockUser(group.id, user.id);
		if(user instanceof ForeignUser)
			ActivityPubWorker.getInstance().sendBlockActivity(group, (ForeignUser) user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object unblockUser(Request req, Response resp, Account self) throws SQLException{
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		GroupStorage.unblockUser(group.id, user.id);
		if(user instanceof ForeignUser)
			ActivityPubWorker.getInstance().sendUndoBlockActivity(group, (ForeignUser) user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

//	public static Object eventCalendar(Request req, Response resp, Account self){
//		if(isMobile(req))
//			return eventCalendarMobile(req, resp, self);
//		else
//			return eventCalendarDesktop(req, resp, self);
//	}

	public static Object eventCalendar(Request req, Response resp, Account self){
		ZoneId timeZone=timeZoneForRequest(req).toZoneId();
		LocalDate today=LocalDate.now(timeZone);
		LocalDate tomorrow=today.plusDays(1);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "events_actual_calendar" : "events_calendar", req);

		if(!isAjax(req) && !isMobile(req)){
			List<User> birthdays=context(req).getUsersController().getFriendsWithBirthdaysWithinTwoDays(self.user, today);
			model.with("birthdays", birthdays);
			if(!birthdays.isEmpty()){
				HashMap<Integer, String> days=new HashMap<>(birthdays.size()), ages=new HashMap<>(birthdays.size());
				for(User user : birthdays){
					days.put(user.id, lang(req).get(today.getDayOfMonth()==user.birthDate.getDayOfMonth() && today.getMonthValue()==user.birthDate.getMonthValue() ? "date_today" : "date_tomorrow"));
					LocalDate birthday=user.birthDate.withYear(today.getYear());
					if(birthday.isBefore(today))
						birthday=birthday.plusYears(1);
					ages.put(user.id, lang(req).get("X_years", Map.of("count", birthday.getYear()-user.birthDate.getYear())));
				}
				model.with("userDays", days).with("userAges", ages);
			}
			PaginatedList<Group> events=context(req).getGroupsController().getUserEvents(self.user, GroupsController.EventsType.FUTURE, 0, 10);
			Instant eventMaxTime=tomorrow.atTime(23, 59, 59).atZone(timeZone).toInstant();
			List<Group> eventsWithinTwoDays=events.list.stream().filter(e->e.eventStartTime.isBefore(eventMaxTime)).toList();
			model.with("events", eventsWithinTwoDays);
		}

		int month=safeParseInt(req.queryParams("month"));
		int year=safeParseInt(req.queryParams("year"));
		LocalDate monthStart;
		if(month<1 || month>12 || year==0){
			monthStart=LocalDate.now(timeZone).withDayOfMonth(1);
			month=monthStart.getMonthValue();
			year=monthStart.getYear();
		}else{
			monthStart=LocalDate.of(year, month, 1);
		}
		model.with("year", year).with("month", month).with("monthLength", monthStart.lengthOfMonth()).with("monthStartWeekday", monthStart.getDayOfWeek().getValue());
		model.with("todayDay", today.getDayOfMonth()).with("todayMonth", today.getMonthValue()).with("todayYear", today.getYear());
		Lang l=lang(req);
		Instant now=Instant.now();

		ArrayList<Actor> eventsInMonth=new ArrayList<>();
		eventsInMonth.addAll(context(req).getUsersController().getFriendsWithBirthdaysInMonth(self.user, month));
		eventsInMonth.addAll(context(req).getGroupsController().getUserEventsInMonth(self.user, year, month, timeZone));
		if(isMobile(req)){
			Map<Integer, List<ActorWithDescription>> eventsByDay=eventsInMonth.stream()
					.map(a->new ActorWithDescription(a, getActorCalendarDescription(a, l, today, monthStart, now, timeZone)))
					.collect(Collectors.groupingBy(a->{
						if(a.actor() instanceof User u)
							return u.birthDate.getDayOfMonth();
						else if(a.actor() instanceof Group g)
							return g.eventStartTime.atZone(timeZone).getDayOfMonth();
						else
							throw new IllegalStateException();
					}));
			model.with("calendarEvents", eventsByDay);
		}else{
			Map<Integer, List<Actor>> eventsByDay=eventsInMonth.stream().collect(Collectors.groupingBy(a->{
				if(a instanceof User u)
					return u.birthDate.getDayOfMonth();
				else if(a instanceof Group g)
					return g.eventStartTime.atZone(timeZone).getDayOfMonth();
				else
					throw new IllegalStateException();
			}));
			model.with("calendarEvents", eventsByDay);
		}
		model.pageTitle(lang(req).get("events_calendar_title"));

		if(isAjax(req)){
			return new WebDeltaResponse(resp).setContent("eventsCalendarW", model.renderToString());
		}

		return model;
	}

	public static Object eventCalendarMobile(Request req, Response resp, Account self){
		RenderedTemplateResponse model=new RenderedTemplateResponse("events_calendar", req);
		Lang l=lang(req);
		Instant now=Instant.now();
		ZoneId timeZone=timeZoneForRequest(req).toZoneId();
		LocalDate today=LocalDate.now(timeZone);
		int month=safeParseInt(req.queryParams("month"));
		int year=safeParseInt(req.queryParams("year"));
		LocalDate monthStart;
		if(month<1 || month>12 || year==0){
			monthStart=LocalDate.now(timeZone).withDayOfMonth(1);
			month=monthStart.getMonthValue();
			year=monthStart.getYear();
		}else{
			monthStart=LocalDate.of(year, month, 1);
		}
		model.with("month", month).with("year", year);
		ArrayList<Actor> eventsInMonth=new ArrayList<>();
		eventsInMonth.addAll(context(req).getUsersController().getFriendsWithBirthdaysInMonth(self.user, month));
		eventsInMonth.addAll(context(req).getGroupsController().getUserEventsInMonth(self.user, year, month, timeZone));
		List<ActorWithDescription> actors=eventsInMonth.stream().sorted((a1, a2)->{
			LocalDate date1, date2;
			if(a1 instanceof User u)
				date1=u.birthDate;
			else if(a1 instanceof Group g)
				date1=g.eventStartTime.atZone(timeZone).toLocalDate();
			else
				throw new IllegalStateException();
			if(a2 instanceof User u)
				date2=u.birthDate;
			else if(a2 instanceof Group g)
				date2=g.eventStartTime.atZone(timeZone).toLocalDate();
			else
				throw new IllegalStateException();
			if(date1.equals(date2)){
				if(a1 instanceof User && a2 instanceof Group){
					return -1;
				}else if(a1 instanceof Group && a2 instanceof User){
					return 1;
				}else if(a1 instanceof User u1 && a2 instanceof User u2){
					return Integer.compare(u1.id, u2.id);
				}else if(a1 instanceof Group g1 && a2 instanceof Group g2){
					return g1.eventStartTime.compareTo(g2.eventStartTime);
				}else{
					throw new IllegalStateException();
				}
			}else{
				return date1.compareTo(date2);
			}
		}).map(a->new ActorWithDescription(a, getActorCalendarDescription(a, l, today, monthStart, now, timeZone))).toList();
		model.with("actors", actors);
		model.pageTitle(lang(req).get("events_calendar_title"));

		return model;
	}

	public static Object eventCalendarDayPopup(Request req, Response resp, Account self){
		LocalDate date;
		try{
			date=LocalDate.parse(Objects.requireNonNullElse(req.queryParams("date"), ""));
		}catch(DateTimeParseException x){
			throw new BadRequestException(x);
		}

		Lang l=lang(req);
		ZoneId timeZone=timeZoneForRequest(req).toZoneId();
		Instant now=Instant.now();
		LocalDate today=LocalDate.now(timeZone);
		RenderedTemplateResponse model=new RenderedTemplateResponse("actor_list", req);
		ArrayList<Actor> actors=new ArrayList<>();
		actors.addAll(context(req).getUsersController().getFriendsWithBirthdaysOnDay(self.user, date.getMonthValue(), date.getDayOfMonth()));
		actors.addAll(context(req).getGroupsController().getUserEventsOnDay(self.user, date, timeZone));
		List<ActorWithDescription> actorsDescr=actors.stream().sorted((a1, a2)->{
			if(a1 instanceof User && a2 instanceof Group){
				return -1;
			}else if(a1 instanceof Group && a2 instanceof User){
				return 1;
			}else if(a1 instanceof User u1 && a2 instanceof User u2){
				return Integer.compare(u1.id, u2.id);
			}else if(a1 instanceof Group g1 && a2 instanceof Group g2){
				return g1.eventStartTime.compareTo(g2.eventStartTime);
			}
			return 0;
		}).map(a->new ActorWithDescription(a, getActorCalendarDescription(a, l, today, date, now, timeZone))).toList();
		model.with("actors", actorsDescr);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).box(l.get("events_for_date", Map.of("date", l.formatDay(date))), model.renderContentBlock(), null, true);
		}
		return model;
	}

	private static String getActorCalendarDescription(Actor a, Lang l, LocalDate today, LocalDate targetDate, Instant now, ZoneId timeZone){
		if(a instanceof User u){
			LocalDate birthday=u.birthDate.withYear(targetDate.getYear());
			if(birthday.isBefore(targetDate))
				birthday=birthday.plusYears(1);
			int age=birthday.getYear()-u.birthDate.getYear();
			String key;
			if(targetDate.isBefore(today))
				key="birthday_descr_past";
			else if(targetDate.isAfter(today))
				key="birthday_descr_future";
			else
				key="birthday_descr_today";
			return l.get(key, Map.of("age", age));
		}else if(a instanceof Group g){
			return l.get(g.eventStartTime.isBefore(now) ? "event_descr_past" : "event_descr_future", Map.of("time", l.formatTime(g.eventStartTime, timeZone)));
		}else{
			throw new IllegalStateException();
		}
	}
}
