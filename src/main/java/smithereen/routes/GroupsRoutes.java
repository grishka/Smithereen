package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.controllers.GroupsController;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.Account;
import smithereen.model.ActorWithDescription;
import smithereen.model.CommentViewType;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.GroupAdmin;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.exceptions.BadRequestException;
import smithereen.lang.Lang;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.text.Whitelist;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class GroupsRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(GroupsRoutes.class);

	private static Group getGroup(Request req){
		int id=parseIntOrDefault(req.params(":id"), 0);
		return context(req).getGroupsController().getGroupOrThrow(id);
	}

	private static Group getGroupAndRequireLevel(Request req, Account self, Group.AdminLevel level){
		Group group=getGroup(req);
		context(req).getGroupsController().enforceUserAdminLevel(group, self.user, level);
		return group;
	}

	public static Object myGroups(Request req, Response resp, Account self, ApplicationContext ctx){
		return userGroups(req, resp, self.user);
	}

	public static Object userGroups(Request req, Response resp){
		int uid=parseIntOrDefault(req.params(":id"), 0);
		User user=context(req).getUsersController().getUserOrThrow(uid);
		return userGroups(req, resp, user);
	}

	public static Object userGroups(Request req, Response resp, User user){
		jsLangKey(req, "cancel", "create");
		ApplicationContext ctx=context(req);
		SessionInfo info=sessionInfo(req);
		ctx.getPrivacyController().enforceUserProfileAccess(info!=null && info.account!=null ? info.account.user : null, user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("tab", "groups").with("title", lang(req).get("groups"));
		String query=req.queryParams("q");
		model.with("query", query);
		if(StringUtils.isNotEmpty(query))
			model.paginate(ctx.getSearchController().searchGroups(info!=null && info.account!=null ? info.account.user : null, query, false, user, offset(req), 100));
		else
			model.paginate(ctx.getGroupsController().getUserGroups(user, info!=null && info.account!=null ? info.account.user : null, offset(req), 100));
		model.with("owner", user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"));
		}
		return model;
	}

	public static Object myManagedGroups(Request req, Response resp, Account self, ApplicationContext ctx){
		jsLangKey(req, "cancel", "create");
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("tab", "managed").with("title", lang(req).get("groups"));
		model.paginate(ctx.getGroupsController().getUserManagedGroups(self.user, offset(req), 100)).with("owner", self.user);
		return model;
	}

	public static Object myEvents(Request req, Response resp, Account self, ApplicationContext ctx){
		return myEvents(req, resp, self, GroupsController.EventsType.FUTURE, ctx);
	}

	public static Object myPastEvents(Request req, Response resp, Account self, ApplicationContext ctx){
		return myEvents(req, resp, self, GroupsController.EventsType.PAST, ctx);
	}

	public static Object myEvents(Request req, Response resp, Account self, GroupsController.EventsType type, ApplicationContext ctx){
		jsLangKey(req, "cancel", "create");
		RenderedTemplateResponse model=new RenderedTemplateResponse("groups", req).with("events", true).with("tab", type==GroupsController.EventsType.PAST ? "past" : "events").with("owner", self.user).pageTitle(lang(req).get("events"));
		String query=req.queryParams("q");
		model.with("query", query);
		if(StringUtils.isNotEmpty(query))
			model.paginate(ctx.getSearchController().searchGroups(self.user, query, true, self.user, offset(req), 100));
		else
			model.paginate(context(req).getGroupsController().getUserEvents(self.user, type, offset(req), 100));
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"));
		}
		return model;
	}

	public static Object createGroup(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("create_group", req);
		return wrapForm(req, resp, "create_group", "/my/groups/create", lang(req).get("create_group_title"), "create", model);
	}

	public static Object createEvent(Request req, Response resp, Account self, ApplicationContext ctx){
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

	public static Object doCreateGroup(Request req, Response resp, Account self, ApplicationContext ctx){
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
				group=ctx.getGroupsController().createEvent(self.user, name, description, eventStart, null);
			}catch(DateTimeParseException x){
				throw new BadRequestException(x);
			}
		}else{
			group=ctx.getGroupsController().createGroup(self.user, name, description);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).replaceLocation("/"+group.username);
		}else{
			resp.redirect(Config.localURI("/"+group.username).toString());
			return "";
		}
	}

	public static RenderedTemplateResponse groupProfile(Request req, Response resp, Group group){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);

		Group.MembershipState membershipState;
		if(self==null){
			membershipState=Group.MembershipState.NONE;
		}else{
			membershipState=ctx.getGroupsController().getUserMembershipState(group, self.user);
		}

		boolean canAccessContent=true;
		if(membershipState!=Group.MembershipState.MEMBER && membershipState!=Group.MembershipState.TENTATIVE_MEMBER){
			if(group.accessType==Group.AccessType.CLOSED){
				canAccessContent=false;
			}else if(group.accessType==Group.AccessType.PRIVATE){
				throw new UserActionNotAllowedException(group.isEvent() ? "event_private_no_access" : "group_private_no_access");
			}
		}

		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group", req);


		// Public info: still visible for non-members in public groups
		List<User> members=ctx.getGroupsController().getRandomMembersForProfile(group, false);
		model.with("group", group).with("members", members);
		if(group.isEvent())
			model.with("tentativeMembers", ctx.getGroupsController().getRandomMembersForProfile(group, true));
		model.with("title", group.name);
		model.with("admins", ctx.getGroupsController().getAdmins(group));
		model.with("canAccessContent", canAccessContent);

		// Wall posts
		int wallPostsCount=0;
		if(canAccessContent){
			int offset=offset(req);
			PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallPosts(self!=null ? self.user : null, group, false, offset, 20));
			wallPostsCount=wall.total;
			ctx.getWallController().populateReposts(self!=null ? self.user : null, wall.list, 2);
			CommentViewType viewType=self!=null ? self.prefs.commentViewType : CommentViewType.THREADED;
			if(req.attribute("mobile")==null){
				ctx.getWallController().populateCommentPreviews(self!=null ? self.user : null, wall.list, viewType);
			}
			Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);
			model.with("postCount", wall.total)
					.paginate(wall, "/groups/"+group.id+"/wall?offset=", null)
					.with("canPostOnWall", self!=null)
					.with("canSeeOthersPosts", true);
			model.with("postInteractions", interactions);
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			PostViewModel.collectActorIDs(wall.list, needUsers, needGroups);
			model.with("users", ctx.getUsersController().getUsers(needUsers));
			model.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)).with("commentViewType", viewType);

			PaginatedList<PhotoAlbum> albums;
			if(isMobile(req))
				albums=ctx.getPhotosController().getMostRecentAlbums(group, self!=null ? self.user : null, 1, true);
			else
				albums=ctx.getPhotosController().getRandomAlbumsForProfile(group, self!=null ? self.user : null, 2);
			model.with("albums", albums.list)
					.with("photoAlbumCount", albums.total)
					.with("covers", ctx.getPhotosController().getPhotosIgnoringPrivacy(albums.list.stream().map(a->a.coverID).filter(id->id!=0).collect(Collectors.toSet())));
		}

		if(group instanceof ForeignGroup)
			model.with("noindex", true);

		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "remove_friend", "cancel", "delete");
		Templates.addJsLangForNewPostForm(req);
		if(self!=null){
			Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, self.user);
			model.with("membershipState", membershipState);
			model.with("groupAdminLevel", level);
			if(level.isAtLeast(Group.AdminLevel.ADMIN)){
				jsLangKey(req, "update_avatar_title", "update_avatar_intro_group", "update_avatar_formats", "update_avatar_footer", "update_avatar_crop_title_group", "update_avatar_crop_explanation1_group",
						"update_avatar_crop_explanation2", "update_avatar_thumb_title", "update_avatar_thumb_explanation1", "update_avatar_thumb_explanation2_group", "choose_file", "save_and_continue", "go_back",
						"remove_profile_picture", "confirm_remove_profile_picture_group");
			}
			if(group.isEvent()){
				if(membershipState==Group.MembershipState.MEMBER)
					model.with("membershipStateText", l.get("event_joined_certain"));
				else if(membershipState==Group.MembershipState.TENTATIVE_MEMBER)
					model.with("membershipStateText", l.get("event_joined_tentative"));
			}
			if(membershipState==Group.MembershipState.REQUESTED)
				model.with("membershipStateText", l.get("requested_to_join"));
			model.with("isBookmarked", ctx.getBookmarksController().isGroupBookmarked(self.user, group));
		}else{
			HashMap<String, String> meta=new LinkedHashMap<>();
			meta.put("og:type", "profile");
			meta.put("og:site_name", Config.serverDisplayName);
			meta.put("og:title", group.name);
			meta.put("og:url", group.url.toString());
			meta.put("og:username", group.getFullUsername());
			String descr=l.get("X_members", Map.of("count", group.memberCount));
			if(wallPostsCount>0)
				descr+=", "+l.get("X_posts", Map.of("count", wallPostsCount));
			if(StringUtils.isNotEmpty(group.summary))
				descr+="\n"+Jsoup.clean(group.summary, Whitelist.none());
			meta.put("og:description", descr);
			if(group.hasAvatar()){
				URI img=group.getAvatar().getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_XLARGE, SizedImage.Format.JPEG);
				if(img!=null){
					SizedImage.Dimensions size=group.getAvatar().getDimensionsForSize(SizedImage.Type.AVA_SQUARE_XLARGE);
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
			case GROUP -> switch(group.accessType){
				case OPEN -> "open_group";
				case CLOSED -> "closed_group";
				case PRIVATE -> "private_group";
			};
			case EVENT -> group.accessType==Group.AccessType.OPEN ? "open_event" : "private_event";
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

		try{
			Photo photo=switch(group.getAvatarImage()){
				case LocalImage li when li.photoID!=0 -> ctx.getPhotosController().getPhotoIgnoringPrivacy(li.photoID);
				case Image img when img.photoApID!=null -> ctx.getObjectLinkResolver().resolveLocally(img.photoApID, Photo.class);
				case null, default -> null;
			};
			if(photo!=null){
				model.with("avatarPvInfo", new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(photo.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), photo.image.getURLsForPhotoViewer()))
						.with("avatarPhoto", photo);
			}
		}catch(ObjectNotFoundException ignore){}

		return model;
	}

	public static Object join(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		ctx.getGroupsController().joinGroup(group, self.user, "1".equals(req.queryParams("tentative")));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
		return "";
	}

	public static Object leave(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		ctx.getGroupsController().leaveGroup(group, self.user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
		return "";
	}

	public static Object editGeneral(Request req, Response resp, Account self, ApplicationContext ctx){
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

	public static Object saveGeneral(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		String name=req.queryParams("name"), about=req.queryParams("about"), username=req.queryParams("username");
		Group.AccessType accessType=enumValue(req.queryParams("access"), Group.AccessType.class);
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

			ctx.getGroupsController().updateGroupInfo(group, self.user, name, about, eventStart, eventEnd, username, accessType);

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
		ApplicationContext ctx=context(req);
		SessionInfo info=sessionInfo(req);
		ctx.getPrivacyController().enforceUserAccessToGroupProfile(info!=null && info.account!=null ? info.account.user : null, group);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "user_grid" : "content_wrap", req);
		PaginatedList<User> members=context(req).getGroupsController().getMembers(group, offset(req), 100, tentative);
		model.paginate(members);
		model.with("summary", lang(req).get(tentative ? "summary_event_X_tentative_members" : (group.isEvent() ? "summary_event_X_members" : "summary_group_X_members"), Map.of("count", tentative ? group.tentativeMemberCount : group.memberCount)));
		model.with("contentTemplate", "user_grid").with("title", group.name);
		if(group instanceof ForeignGroup)
			model.with("noindex", true);
		if(!isMobile(req)){
			Map<Integer, Photo> userPhotos=ctx.getPhotosController().getUserProfilePhotos(members.list);
			model.with("avatarPhotos", userPhotos)
					.with("avatarPvInfos", userPhotos.values()
							.stream()
							.collect(Collectors.toMap(p->p.ownerID, p->new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(p.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), p.image.getURLsForPhotoViewer())))
					);
		}
		return model;
	}

	public static Object admins(Request req, Response resp){
		Group group=getGroup(req);
		SessionInfo info=sessionInfo(req);
		context(req).getPrivacyController().enforceUserAccessToGroupProfile(info!=null && info.account!=null ? info.account.user : null, group);
		RenderedTemplateResponse model=new RenderedTemplateResponse("actor_list", req);
		model.with("actors", context(req).getGroupsController().getAdmins(group).stream().map(a->new ActorWithDescription(a.user, a.title)).collect(Collectors.toList()));
		if(group instanceof ForeignGroup)
			model.with("noindex", true);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).box(lang(req).get(group.isEvent() ? "event_organizers" : "group_admins"), model.renderContentBlock(), null, !isMobile(req));
		}
		return model;
	}

	public static Object editAdmins(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_admins", req);
		model.with("group", group).with("title", group.name);
		model.with("admins", ctx.getGroupsController().getAdmins(group));
		model.with("subtab", "admins");
		model.with("joinRequestCount", ctx.getGroupsController().getJoinRequestCount(self.user, group));
		jsLangKey(req, "cancel", "group_admin_demote", "yes", "no");
		return model;
	}

	public static Object editMembers(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, self.user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_members", req);
		model.paginate(ctx.getGroupsController().getAllMembers(group, offset(req), 100));
		model.with("group", group).with("title", group.name);
		model.with("adminIDs", ctx.getGroupsController().getAdmins(group).stream().map(adm->adm.user.id).collect(Collectors.toList()));
		model.with("canAddAdmins", level.isAtLeast(Group.AdminLevel.ADMIN));
		model.with("adminLevel", level);
		model.with("subtab", "all");
		model.with("summaryKey", group.isEvent() ? "summary_event_X_members" : "summary_group_X_members");
		model.with("joinRequestCount", ctx.getGroupsController().getJoinRequestCount(self.user, group));
		jsLangKey(req, "cancel", "yes", "no");
		return model;
	}

	public static Object editAdminForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		User user=ctx.getUsersController().getUserOrThrow(userID);

		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_admin", req);
		GroupAdmin admin=ctx.getGroupsController().getAdmin(group, userID);
		model.with("existingAdmin", admin);
		return wrapForm(req, resp, "group_edit_admin", "/groups/"+group.id+"/saveAdmin?id="+userID, user.getFullName(), "save", model);
	}

	public static Object saveAdmin(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		User user=ctx.getUsersController().getUserOrThrow(userID);

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

		ctx.getGroupsController().addOrUpdateAdmin(group, user, title, lvl);

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object confirmDemoteAdmin(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=safeParseInt(req.queryParams("id"));
		User user=ctx.getUsersController().getUserOrThrow(userID);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", Utils.lang(req).get("group_admin_demote_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", Config.localURI("/groups/"+group.id+"/removeAdmin?_redir="+URLEncoder.encode(back)+"&id="+userID)).with("back", back);
	}

	public static Object removeAdmin(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=safeParseInt(req.queryParams("id"));
		User user=ctx.getUsersController().getUserOrThrow(userID);

		ctx.getGroupsController().removeAdmin(group, user);

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(Utils.back(req));
		return "";
	}

	public static Object editAdminReorder(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.ADMIN);
		int userID=parseIntOrDefault(req.queryParams("id"), 0);
		int order=parseIntOrDefault(req.queryParams("order"), 0);
		if(order<0)
			throw new BadRequestException();

		ctx.getGroupsController().setAdminOrder(group, ctx.getUsersController().getUserOrThrow(userID), order);

		return "";
	}

	public static Object blocking(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		Group.AdminLevel level=ctx.getGroupsController().getMemberAdminLevel(group, self.user);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_blocking", req).with("title", lang(req).get("settings_blocking"));
		model.with("blockedUsers", ctx.getGroupsController().getBlockedUsers(group));
		model.with("blockedDomains", ctx.getGroupsController().getBlockedDomains(group));
		model.with("group", group);
		model.with("adminLevel", level);
		jsLangKey(req, "unblock", "yes", "no", "cancel");
		return model;
	}

	public static Object blockDomainForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		RenderedTemplateResponse model=new RenderedTemplateResponse("block_domain", req);
		return wrapForm(req, resp, "block_domain", "/groups/"+group.id+"/blockDomain", lang(req).get("block_a_domain"), "block", model);
	}

	public static Object blockDomain(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		ctx.getGroupsController().blockDomain(group, domain);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmUnblockDomain(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_domain_X", Map.of("domain", domain))).with("formAction", "/groups/"+group.id+"/unblockDomain?domain="+domain+"_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object unblockDomain(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		String domain=req.queryParams("domain");
		ctx.getGroupsController().unblockDomain(group, domain);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	private static User getUserOrThrow(Request req){
		int id=parseIntOrDefault(req.queryParams("id"), 0);
		return context(req).getUsersController().getUserOrThrow(id);
	}

	public static Object confirmBlockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_block_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/groups/"+group.id+"/blockUser?id="+user.id+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object confirmUnblockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/groups/"+group.id+"/unblockUser?id="+user.id+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object blockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		ctx.getGroupsController().blockUser(group, user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object unblockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		ctx.getGroupsController().unblockUser(group, user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object eventCalendar(Request req, Response resp, Account self, ApplicationContext ctx){
		ZoneId timeZone=timeZoneForRequest(req);
		LocalDate today=LocalDate.now(timeZone);
		LocalDate tomorrow=today.plusDays(1);
		RenderedTemplateResponse model=new RenderedTemplateResponse(isAjax(req) ? "events_actual_calendar" : "events_calendar", req);

		if(!isAjax(req) && !isMobile(req)){
			List<User> birthdays=ctx.getUsersController().getFriendsWithBirthdaysWithinTwoDays(self.user, today);
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
			PaginatedList<Group> events=ctx.getGroupsController().getUserEvents(self.user, GroupsController.EventsType.FUTURE, 0, 10);
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
		eventsInMonth.addAll(ctx.getUsersController().getFriendsWithBirthdaysInMonth(self.user, month));
		eventsInMonth.addAll(ctx.getGroupsController().getUserEventsInMonth(self.user, year, month, timeZone));
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

	public static Object eventCalendarMobile(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("events_calendar", req);
		Lang l=lang(req);
		Instant now=Instant.now();
		ZoneId timeZone=timeZoneForRequest(req);
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
		eventsInMonth.addAll(ctx.getUsersController().getFriendsWithBirthdaysInMonth(self.user, month));
		eventsInMonth.addAll(ctx.getGroupsController().getUserEventsInMonth(self.user, year, month, timeZone));
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

	public static Object eventCalendarDayPopup(Request req, Response resp, Account self, ApplicationContext ctx){
		LocalDate date;
		try{
			date=LocalDate.parse(Objects.requireNonNullElse(req.queryParams("date"), ""));
		}catch(DateTimeParseException x){
			throw new BadRequestException(x);
		}

		Lang l=lang(req);
		ZoneId timeZone=timeZoneForRequest(req);
		Instant now=Instant.now();
		LocalDate today=LocalDate.now(timeZone);
		RenderedTemplateResponse model=new RenderedTemplateResponse("actor_list", req);
		ArrayList<Actor> actors=new ArrayList<>();
		actors.addAll(ctx.getUsersController().getFriendsWithBirthdaysOnDay(self.user, date.getMonthValue(), date.getDayOfMonth()));
		actors.addAll(ctx.getGroupsController().getUserEventsOnDay(self.user, date, timeZone));
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

	public static Object inviteFriend(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.queryParams("user")));
		String msg=null;
		try{
			ctx.getGroupsController().inviteUserToGroup(self.user, user, group);
		}catch(BadRequestException x){
			msg=lang(req).get(x.getMessage());
		}
		if(msg==null)
			msg=lang(req).get("invitation_sent");
		if(isAjax(req)){
			return new WebDeltaResponse(resp).setContent("frowActions"+user.id, "<div class=\"settingsMessage\">"+TextProcessor.escapeHTML(msg)+"</div>");
		}
		return "";
	}

	public static Object groupInvitations(Request req, Response resp, Account self, ApplicationContext ctx){
		return invitations(req, resp, self, false);
	}

	public static Object eventInvitations(Request req, Response resp, Account self, ApplicationContext ctx){
		return invitations(req, resp, self, true);
	}

	private static Object invitations(Request req, Response resp, Account self, boolean events){
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_invites", req);
		model.paginate(context(req).getGroupsController().getUserInvitations(self, events, offset(req), 25));
		model.pageTitle(lang(req).get("group_invitations"));
		model.with("events", events);
		return model;
	}

	public static Object respondToInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);

		boolean accept, tentative;
		if(req.queryParams("accept")!=null){
			accept=true;
			tentative=false;
		}else if(req.queryParams("tentativeAccept")!=null){
			accept=true;
			tentative=true;
		}else if(req.queryParams("decline")!=null){
			accept=false;
			tentative=false;
		}else{
			throw new BadRequestException();
		}

		if(accept){
			ctx.getGroupsController().joinGroup(group, self.user, tentative);
		}else{
			ctx.getGroupsController().declineInvitation(self.user, group);
		}

		if(isAjax(req)){
			return new WebDeltaResponse(resp).setContent("groupInviteBtns"+group.id,
					"<div class=\"settingsMessage\">"+lang(req).get(accept ? "group_invite_accepted" : "group_invite_declined")+"</div>");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmRemoveUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_remove_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/groups/"+group.id+"/removeUser?id="+user.id+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object removeUser(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		ctx.getGroupsController().removeUser(self.user, group, user);
		if(isAjax(req)){
			if(isMobile(req))
				return new WebDeltaResponse(resp).refresh();
			return new WebDeltaResponse(resp).setContent("groupMemberActions"+user.id, "<b>"+lang(req).get("group_member_removed")+"</b>");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object editJoinRequests(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_members", req);
		model.with("summaryKey", "summary_group_X_join_requests").with("group", group).with("subtab", "requests");
		PaginatedList<User> list=ctx.getGroupsController().getJoinRequests(self.user, group, offset(req), 50);
		model.paginate(list);
		model.with("joinRequestCount", list.total);
		String csrf=sessionInfo(req).csrfToken;
		model.with("memberActions", List.of(
				Map.of("href", "/groups/"+group.id+"/acceptJoinRequest?csrf="+csrf+"&id=", "title", lang(req).get("group_accept_join_request")),
				Map.of("href", "/groups/"+group.id+"/rejectJoinRequest?csrf="+csrf+"&id=", "title", lang(req).get("group_reject_join_request"))
		));
		model.pageTitle(group.name);
		return model;
	}

	public static Object acceptJoinRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		return respondToJoinRequest(req, resp, self, true);
	}

	public static Object rejectJoinRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		return respondToJoinRequest(req, resp, self, false);
	}

	private static Object respondToJoinRequest(Request req, Response resp, Account self, boolean accept){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		if(accept)
			context(req).getGroupsController().acceptJoinRequest(self.user, group, user);
		else
			context(req).getGroupsController().removeUser(self.user, group, user);
		if(isAjax(req)){
			if(isMobile(req))
				return new WebDeltaResponse(resp).refresh();
			return new WebDeltaResponse(resp)
					.show("groupMemberActions"+user.id)
					.hide("groupMemberProgress"+user.id)
					.setContent("groupMemberActions"+user.id, "<b>"+lang(req).get(accept ? "group_join_request_accepted" : "group_join_request_rejected")+"</b>");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object editInvitations(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		RenderedTemplateResponse model=new RenderedTemplateResponse("group_edit_members", req);
		model.with("summaryKey", group.isEvent() ? "summary_event_X_invites" : "summary_group_X_invites").with("group", group).with("subtab", "invites");
		PaginatedList<User> list=ctx.getGroupsController().getGroupInvites(self.user, group, offset(req), 50);
		model.paginate(list);
		model.with("joinRequestCount", ctx.getGroupsController().getJoinRequestCount(self.user, group));
		String csrf=sessionInfo(req).csrfToken;
		model.with("memberActions", List.of(
				Map.of("href", "/groups/"+group.id+"/cancelInvite?csrf="+csrf+"&id=", "title", lang(req).get("cancel_invitation"))
		));
		model.pageTitle(group.name);
		jsLangKey(req, "cancel");
		return model;
	}

	public static Object editCancelInvitation(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroupAndRequireLevel(req, self, Group.AdminLevel.MODERATOR);
		User user=getUserOrThrow(req);
		ctx.getGroupsController().cancelInvitation(self.user, group, user);
		if(isAjax(req)){
			if(isMobile(req))
				return new WebDeltaResponse(resp).refresh();
			return new WebDeltaResponse(resp)
					.show("groupMemberActions"+user.id)
					.hide("groupMemberProgress"+user.id)
					.setContent("groupMemberActions"+user.id, "<b>"+lang(req).get("invitation_canceled")+"</b>");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object syncRelationshipsCollections(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		group.ensureRemote();
		ctx.getActivityPubWorker().fetchActorRelationshipCollections(group);
		Lang l=lang(req);
		return new WebDeltaResponse(resp).messageBox(l.get("sync_members"), l.get("sync_started"), l.get("ok"));
	}

	public static Object syncProfile(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		group.ensureRemote();
		ctx.getObjectLinkResolver().resolve(group.activityPubID, ForeignGroup.class, true, true, true);
		return new WebDeltaResponse(resp).refresh();
	}

	public static Object syncContentCollections(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		group.ensureRemote();
		ctx.getActivityPubWorker().fetchActorContentCollections(group);
		Lang l=lang(req);
		return new WebDeltaResponse(resp).messageBox(l.get("sync_content"), l.get("sync_started"), l.get("ok"));
	}
}
