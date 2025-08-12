package smithereen.routes.admin;

import com.google.gson.JsonElement;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.ServerRule;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.ViolationReport;
import smithereen.model.admin.ViolationReportAction;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.reports.ReportableContentObjectID;
import smithereen.model.reports.ReportableContentObjectType;
import smithereen.model.reports.ReportedComment;
import smithereen.model.viewmodel.ViolationReportActionViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.TextProcessor;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminReportingRoutes{
	public static Object reportsList(Request req, Response resp, Account self, ApplicationContext ctx){
		boolean resolved=req.queryParams("resolved")!=null;
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_list", req);
		model.with("tab", resolved ? "resolved" : "open");
		model.pageTitle(lang(req).get("menu_reports"));
		PaginatedList<ViolationReport> reports=ctx.getModerationController().getViolationReports(!resolved, offset(req), 50);
		model.paginate(reports);

		Set<Integer> userIDs=reports.list.stream().filter(r->r.targetID>0).map(r->r.targetID).collect(Collectors.toSet());
		userIDs.addAll(reports.list.stream().filter(r->r.reporterID!=0).map(r->r.reporterID).collect(Collectors.toSet()));
		Set<Integer> groupIDs=reports.list.stream().filter(r->r.targetID<0).map(r->-r.targetID).collect(Collectors.toSet());

		model.with("users", ctx.getUsersController().getUsers(userIDs))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(groupIDs));

		return model;
	}

	public static Object viewReport(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		HashSet<Integer> needPosts=new HashSet<>();
		HashSet<Long> needMessages=new HashSet<>(), needPhotos=new HashSet<>(), needComments=new HashSet<>();

		if(report.targetID>0)
			needUsers.add(report.targetID);
		if(report.targetID<0)
			needGroups.add(-report.targetID);
		needUsers.add(report.reporterID);

		ArrayList<Map<String, Object>> contentForTemplate=new ArrayList<>();
		int i=0;
		for(ReportableContentObject co:report.content){
			switch(co){
				case Post p -> {
					needPosts.add(p.id);
					contentForTemplate.add(Map.of("type", p.getReplyLevel()>0 ? "comment" : "post", "id", p.id, "url", "/settings/admin/reports/"+id+"/content/"+i));
				}
				case MailMessage msg -> {
					needMessages.add(msg.id);
					contentForTemplate.add(Map.of("type", "message", "id", msg.id, "url", "/settings/admin/reports/"+id+"/content/"+i));
				}
				case Photo photo -> {
					needPhotos.add(photo.id);
					contentForTemplate.add(Map.of("type", "photo", "id", photo.id, "url", photo.getURL(), "pvData", photo.getSinglePhotoViewerData()));
				}
				case Comment comment -> {
					needComments.add(comment.id);
					Map<String, Object> content=new HashMap<>();
					content.put("type", "actualComment");
					content.put("id", comment.id);
					content.put("url", "/settings/admin/reports/"+id+"/content/"+i);
					if(comment instanceof ReportedComment rc){
						content.put("firstInTopic", rc.isFirstInTopic);
						content.put("topicTitle", rc.topicTitle);
					}
					contentForTemplate.add(content);
				}
			}
			i++;
		}
		List<ViolationReportAction> actions=ctx.getModerationController().getViolationReportActions(report);
		needUsers.addAll(actions.stream().map(ViolationReportAction::userID).collect(Collectors.toSet()));

		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);
		Map<Long, MailMessage> messages=ctx.getMailController().getMessagesAsModerator(needMessages);
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Long, Comment> comments=ctx.getCommentsController().getCommentsIgnoringPrivacy(needComments);
		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(needGroups);

		Actor target=report.targetID>0 ? users.get(report.targetID) : groups.get(-report.targetID);

		Lang l=lang(req);
		List<ViolationReportActionViewModel> actionViewModels=actions.stream().map(a->{
			User adminUser=users.get(a.userID());
			HashMap<String, Object> links=new HashMap<>();
			links.put("adminUser", Map.of("href", adminUser!=null ? adminUser.getProfileURL() : "/id"+a.userID()));
			HashMap<String, Object> langArgs=new HashMap<>();
			langArgs.put("name", adminUser!=null ? adminUser.getFullName() : "DELETED");
			langArgs.put("gender", adminUser!=null ? adminUser.gender : User.Gender.UNKNOWN);
			String mainText=switch(a.actionType()){
				case REOPEN -> l.get("report_log_reopened", langArgs);
				case RESOLVE_REJECT -> l.get("report_log_rejected", langArgs);
				case COMMENT -> l.get("report_log_commented", langArgs);
				case RESOLVE_WITH_ACTION -> {
					if(report.targetID>0){
						User targetUser=(User)target;
						langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
						links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+report.targetID));
					}
					yield l.get("admin_audit_log_changed_user_restrictions", langArgs);
				}
				case DELETE_CONTENT -> l.get("report_log_deleted_content", langArgs);
				case CHANGE_REASON -> l.get("report_log_changed_reason", langArgs);
				case CHANGE_RULES -> l.get("report_log_changed_rules", langArgs);
				case ADD_CONTENT -> l.get("report_log_added_content", langArgs);
				case REMOVE_CONTENT -> l.get("report_log_excluded_content", langArgs);
			};
			return new ViolationReportActionViewModel(a, TextProcessor.substituteLinks(mainText, links), switch(a.actionType()){
				case COMMENT -> TextProcessor.postprocessPostHTMLForDisplay(a.text(), false, false);
				case RESOLVE_WITH_ACTION -> {
					User targetUser=users.get(report.targetID);
					String statusStr=switch(UserBanStatus.valueOf(a.extra().get("status").getAsString())){
						case NONE -> l.get("admin_user_state_no_restrictions");
						case FROZEN -> l.get("admin_user_state_frozen", Map.of("expirationTime", a.extra().has("expiresAt") ? l.formatDate(Instant.ofEpochMilli(a.extra().get("expiresAt").getAsLong()), timeZoneForRequest(req), false) : l.get("email_account_frozen_until_first_login")));
						case SUSPENDED -> {
							if(targetUser instanceof ForeignUser)
								yield l.get("admin_user_state_suspended_foreign");
							else
								yield l.get("admin_user_state_suspended", Map.of("deletionTime", l.formatDate(a.time().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS), timeZoneForRequest(req), false)));
						}
						case HIDDEN -> l.get("admin_user_state_hidden");
						case SELF_DEACTIVATED -> null;
					};
					if(a.extra().has("message")){
						statusStr+="<br/>"+l.get("admin_user_ban_message")+": "+TextProcessor.escapeHTML(a.extra().get("message").getAsString());
					}
					yield statusStr;
				}
				case CHANGE_REASON -> {
					ViolationReport.Reason oldReason=enumValue(a.extra().get("oldReason").getAsString(), ViolationReport.Reason.class);
					ViolationReport.Reason newReason=enumValue(a.extra().get("newReason").getAsString(), ViolationReport.Reason.class);
					yield l.get(oldReason.getLangKey())+" &rarr; "+l.get(newReason.getLangKey());
				}
				case CHANGE_RULES -> {
					Set<Integer> oldRules;
					if(a.extra().get("oldRules")!=null && !a.extra().get("oldRules").isJsonNull())
						oldRules=a.extra().getAsJsonArray("oldRules").asList().stream().map(JsonElement::getAsInt).collect(Collectors.toSet());
					else
						oldRules=Set.of();
					Set<Integer> newRules=a.extra().getAsJsonArray("newRules").asList().stream().map(JsonElement::getAsInt).collect(Collectors.toSet());
					Set<Integer> allRuleIDs=new HashSet<>(oldRules);
					allRuleIDs.addAll(newRules);
					List<ServerRule> allRules=ctx.getModerationController().getServerRules();
					if(!allRules.stream().map(ServerRule::id).collect(Collectors.toSet()).containsAll(allRuleIDs)){
						allRules=ctx.getModerationController().getServerRulesByIDs(allRuleIDs);
					}
					ArrayList<String> lines=new ArrayList<>();
					for(ServerRule rule:allRules){
						if(oldRules.contains(rule.id())==newRules.contains(rule.id()))
							continue;
						String line;
						if(oldRules.contains(rule.id()))
							line="- ";
						else
							line="+ ";
						line+=TextProcessor.escapeHTML(rule.getTranslatedTitle(l.getLocale()));
						lines.add(line);
					}
					yield String.join("<br>", lines);
				}
				case REMOVE_CONTENT -> {
					List<ReportableContentObject> removedContent=a.extra().getAsJsonArray("content").asList().stream()
							.map(el->ViolationReport.deserializeContentObject(report.id, el.getAsJsonObject())).toList();
					ctx.getModerationController().populateFilesInReportableContent(removedContent);
					yield removedContent.stream()
							.map(co->{
								String langKey=switch(co){
									case Post post -> post.getReplyLevel()>0 ? "admin_report_content_comment" : "admin_report_content_post";
									case MailMessage msg -> "admin_report_content_message";
									case Photo photo -> "admin_report_content_photo";
									case Comment comment -> "admin_report_content_comment";
								};
								String extraAttrs;
								if(co instanceof Photo photo){
									extraAttrs=" data-pv=\""+TextProcessor.escapeHTML(gson.toJson(photo.getSinglePhotoViewerData()))+"\" onclick=\"return openPhotoViewer(this)\" data-pv-url=\"/photos/ajaxViewerInfoForReport?action="+a.id()+"\"";
								}else{
									extraAttrs=" data-ajax-box";
								}
								return "<a href=\"/settings/admin/reports/"+report.id+"/pastContent/"+a.id()+"/"+co.getReportableObjectID().type()+"/"+co.getReportableObjectID().id()
										+"\""+extraAttrs+">"+l.get(langKey, Map.of("id", co.getReportableObjectID().id()))+"</a>";
							})
							.collect(Collectors.joining("<br/>"));
				}
				case ADD_CONTENT -> {
					List<ReportableContentObject> addedContent=a.extra().getAsJsonArray("content").asList().stream()
							.map(el->ViolationReport.deserializeContentObject(report.id, el.getAsJsonObject())).toList();
					yield addedContent.stream()
							.map(co->{
								String langKey=switch(co){
									case Post post -> post.getReplyLevel()>0 ? "admin_report_content_comment" : "admin_report_content_post";
									case MailMessage msg -> "admin_report_content_message";
									case Photo photo -> "admin_report_content_photo";
									case Comment comment -> "admin_report_content_comment";
								};
								return l.get(langKey, Map.of("id", co.getReportableObjectID().id()));
							})
							.collect(Collectors.joining("<br/>"));
				}
				default -> null;
			});
		}).toList();

		RenderedTemplateResponse model=new RenderedTemplateResponse("report", req);
		model.pageTitle(lang(req).get("admin_report_title_X", Map.of("id", id)));
		model.with("report", report);
		model.with("users", users).with("groups", groups);
		model.with("posts", posts).with("messages", messages).with("photos", photos).with("comments", comments);
		model.with("canDeleteContent", (!posts.isEmpty() || !messages.isEmpty() || !photos.isEmpty() || !comments.isEmpty()) && target!=null);
		model.with("actions", actionViewModels);
		model.with("content", contentForTemplate);
		model.with("isLocalTarget", target!=null && StringUtils.isEmpty(target.domain));
		model.with("toolbarTitle", l.get("menu_reports"));
		if(report.rules!=null && !report.rules.isEmpty()){
			model.with("rules", ctx.getModerationController().getServerRulesByIDs(report.rules));
		}
		model.addMessage(req, "reportMessage"+report.id, "message");
		return model;
	}

	public static Object reportMarkResolved(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		if(report.state!=ViolationReport.State.OPEN)
			throw new BadRequestException();
		ctx.getModerationController().rejectViolationReport(report, self.user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportMarkUnresolved(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		if(report.state==ViolationReport.State.OPEN)
			throw new BadRequestException();
		ctx.getModerationController().markViolationReportUnresolved(report, self.user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportAddComment(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "text");
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		String text=req.queryParams("text");
		ctx.getModerationController().addViolationReportComment(report, self.user, text);
		if(isAjax(req))
			return new WebDeltaResponse(resp).setContent("commentText", "").refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportShowContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		int index=safeParseInt(req.params(":index"));
		if(index<0 || index>=report.content.size())
			throw new BadRequestException();
		ReportableContentObject cobj=report.content.get(index);
		return reportShowContent(req, resp, ctx, cobj, id);
	}

	public static Object reportShowPastContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		int actionID=safeParseInt(req.params(":actionID"));
		ReportableContentObjectID objID=new ReportableContentObjectID(enumValue(req.params(":contentType"), ReportableContentObjectType.class), safeParseLong(req.params(":contentID")));
		ViolationReportAction action=ctx.getModerationController().getViolationReportAction(report, actionID);
		if(action.actionType()!=ViolationReportAction.ActionType.REMOVE_CONTENT && action.actionType()!=ViolationReportAction.ActionType.ADD_CONTENT)
			throw new ObjectNotFoundException();
		List<ReportableContentObject> content=action.extra().getAsJsonArray("content").asList().stream()
				.map(el->ViolationReport.deserializeContentObject(report.id, el.getAsJsonObject())).toList();
		ctx.getModerationController().populateFilesInReportableContent(content);
		for(ReportableContentObject obj:content){
			if(obj.getReportableObjectID().equals(objID))
				return reportShowContent(req, resp, ctx, obj, id);
		}
		throw new ObjectNotFoundException();
	}

	private static Object reportShowContent(Request req, Response resp, ApplicationContext ctx, ReportableContentObject cobj, int id){
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_content", req);
		Lang l=lang(req);
		String title;
		model.with("content", cobj);
		model.with("reportID", id);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		switch(cobj){
			case Post post -> {
				title=l.get(post.getReplyLevel()>0 ? "admin_report_content_comment" : "admin_report_content_post", Map.of("id", post.id));
				model.with("contentType", "post");
				needUsers.add(post.authorID);
				if(post.ownerID>0)
					needUsers.add(post.ownerID);
				else
					needGroups.add(-post.ownerID);
			}
			case MailMessage msg -> {
				title=l.get("admin_report_content_message", Map.of("id", msg.id));
				model.with("contentType", "message");
				needUsers.addAll(msg.to);
				if(msg.cc!=null)
					needUsers.addAll(msg.cc);
				needUsers.add(msg.senderID);
			}
			case Photo photo -> throw new ObjectNotFoundException();
			case Comment comment -> {
				title=l.get("admin_report_content_comment", Map.of("id", comment.id));
				model.with("contentType", "comment");
				needUsers.add(comment.authorID);
				if(comment.ownerID>0)
					needUsers.add(comment.ownerID);
				else
					needGroups.add(-comment.ownerID);
			}
		}
		model.with("users", ctx.getUsersController().getUsers(needUsers));
		model.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).box(title, model.renderBlock("content"), null, !isMobile(req));
		}
		model.pageTitle(title);
		return model;
	}

	public static Object reportConfirmDeleteContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ctx.getModerationController().getViolationReportByID(id, false);
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("report_delete_content_title"), l.get("report_confirm_delete_content"), "/settings/admin/reports/"+id+"/deleteContent");
	}

	public static Object reportDeleteContent(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		ctx.getModerationController().deleteViolationReportContent(report, info, true);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportsOfUser(Request req, Response resp, Account self, ApplicationContext ctx){
		return userReports(req, resp, self, ctx, true);
	}

	public static Object reportsByUser(Request req, Response resp, Account self, ApplicationContext ctx){
		return userReports(req, resp, self, ctx, false);
	}

	private static Object userReports(Request req, Response resp, Account self, ApplicationContext ctx, boolean ofUser){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_list", req);
		model.pageTitle(lang(req).get("menu_reports"));
		PaginatedList<ViolationReport> reports;
		if(ofUser){
			model.with("tab", "reportsOf");
			reports=ctx.getModerationController().getViolationReportsOfActor(user, offset(req), 50);
		}else{
			model.with("tab", "reportsBy");
			reports=ctx.getModerationController().getViolationReportsByUser(user, offset(req), 50);
		}
		model.paginate(reports);

		Set<Integer> userIDs=reports.list.stream().filter(r->r.targetID>0).map(r->r.targetID).collect(Collectors.toSet());
		userIDs.addAll(reports.list.stream().filter(r->r.reporterID!=0).map(r->r.reporterID).collect(Collectors.toSet()));
		Set<Integer> groupIDs=reports.list.stream().filter(r->r.targetID<0).map(r->-r.targetID).collect(Collectors.toSet());

		model.with("users", ctx.getUsersController().getUsers(userIDs))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(groupIDs))
				.with("filteredByUser", user)
				.headerBack(user);
		model.with("staffNoteCount", ctx.getModerationController().getUserStaffNoteCount(user));

		return model;
	}

	public static Object createReportForm(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "ids", "uid");
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.queryParams("uid")));
		String type=req.queryParams("type");
		String ids=req.queryParams("ids");
		Lang l=lang(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("report_form", req);
		model.with("actorForAvatar", user)
				.with("otherServerDomain", user instanceof ForeignUser fu ? fu.domain : null)
				.with("serverRules", ctx.getModerationController().getServerRules());
		return wrapForm(req, resp, "report_form", "/settings/admin/createReport?type="+type+"&ids="+ids+"&uid="+user.id, l.get("admin_create_report_title"), "create", model);
	}

	private static List<ReportableContentObject> getReportableObjects(Request req, User user, ApplicationContext ctx){
		List<Long> ids=Arrays.stream(req.queryParams("ids").split(",")).map(Utils::safeParseLong).filter(id->id!=0).toList();
		String type=req.queryParams("type");
		List<ReportableContentObject> content=switch(type){
			case "wall" -> ctx.getWallController().getPosts(ids.stream().map(Long::intValue).collect(Collectors.toSet())).values().stream().map(p->(ReportableContentObject)p).toList();
			case "comments" -> ctx.getCommentsController().getCommentsIgnoringPrivacy(ids).values().stream().map(p->(ReportableContentObject)p).toList();
			case "photos" -> ctx.getPhotosController().getPhotosIgnoringPrivacy(ids).values().stream().map(p->(ReportableContentObject)p).toList();
			default -> throw new IllegalStateException("Unexpected value: " + type);
		};

		for(ReportableContentObject obj:content){
			int authorID=switch(obj){
				case Post p -> p.authorID;
				case Comment c -> c.authorID;
				case MailMessage m -> m.senderID;
				case Photo p -> p.authorID;
			};
			if(authorID!=user.id)
				throw new BadRequestException("Author ID "+authorID+" for "+obj.getReportableObjectID()+" does not match expected "+user.id);
		}
		return content;
	}

	public static Object createReport(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "ids", "uid", "reason");
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.queryParams("uid")));
		String comment=req.queryParamOrDefault("reportText", "");
		ViolationReport.Reason reason=enumValue(req.queryParams("reason"), ViolationReport.Reason.class);
		boolean forward="on".equals(req.queryParams("forward"));
		Set<Integer> rules;
		if(reason==ViolationReport.Reason.SERVER_RULES){
			QueryParamsMap rulesMap=req.queryMap("rules");
			if(rulesMap==null)
				throw new BadRequestException();
			Set<Integer> validRuleIDs=ctx.getModerationController()
					.getServerRules()
					.stream()
					.map(ServerRule::id)
					.collect(Collectors.toSet());
			rules=rulesMap.toMap()
					.keySet()
					.stream()
					.map(Utils::safeParseInt)
					.filter(validRuleIDs::contains)
					.collect(Collectors.toSet());
			if(rules.isEmpty())
				throw new BadRequestException();
		}else{
			rules=Set.of();
		}

		List<ReportableContentObject> content=getReportableObjects(req, user, ctx);

		int reportID=ctx.getModerationController().createViolationReport(self.user, user, content, reason, rules, comment, forward);
		return ajaxAwareRedirect(req, resp, "/settings/admin/reports/"+reportID);
	}

	public static Object addContentToReport(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "ids");
		ViolationReport report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.params(":id")), true);
		User user=ctx.getUsersController().getUserOrThrow(report.targetID);

		List<ReportableContentObject> content=getReportableObjects(req, user, ctx);
		ctx.getModerationController().addContentToViolationReport(self.user, report, content);
		return ajaxAwareRedirect(req, resp, "/settings/admin/reports/"+report.id);
	}

	public static Object addLinksToReportForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "report_add_urls_form", "/settings/admin/reports/"+req.params(":id")+"/addLinks",
				lang(req).get("admin_report_add_content_link"), "save", "report_add_urls", List.of(), s->null, null);
	}

	public static Object addLinksToReport(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "urls");
		ViolationReport report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.params(":id")), true);
		User user=ctx.getUsersController().getUserOrThrow(report.targetID);
		List<URI> urls=Arrays.stream(req.queryParams("urls").split("\n"))
				.map(String::trim)
				.filter(s->!s.isEmpty())
				.map(URI::create)
				.toList();
		ArrayList<ReportableContentObject> content=new ArrayList<>();
		ArrayList<String> errors=new ArrayList<>();
		for(URI uri:urls){
			try{
				ReportableContentObject obj=ctx.getObjectLinkResolver().resolveNative(uri, ReportableContentObject.class, true, true, false, user, true);
				int authorID=switch(obj){
					case Post p -> p.authorID;
					case Comment c -> c.authorID;
					case MailMessage m -> m.senderID;
					case Photo p -> p.authorID;
				};
				if(authorID!=user.id)
					errors.add(uri+": "+lang(req).get("admin_report_link_wrong_author"));
				else
					content.add(obj);
			}catch(ObjectNotFoundException x){
				errors.add(uri+": "+lang(req).get("err_not_found"));
			}
		}
		if(!content.isEmpty()){
			ctx.getModerationController().addContentToViolationReport(self.user, report, content);
		}
		if(!errors.isEmpty()){
			req.session().attribute("reportMessage"+report.id, String.join("\n", errors));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/reports/"+report.id);
	}

	public static Object setReportReason(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "reason");
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		ViolationReport.Reason newReason=enumValue(req.queryParams("reason"), ViolationReport.Reason.class);
		if(newReason!=report.reason){
			if(newReason==ViolationReport.Reason.SERVER_RULES){
				RenderedTemplateResponse model=new RenderedTemplateResponse("admin_report_choose_rules", req)
						.with("serverRules", ctx.getModerationController().getServerRules())
						.with("selectedRules", Set.of());
				return wrapForm(req, resp, "admin_report_choose_rules", "/settings/admin/reports/"+report.id+"/setRules", lang(req).get("admin_report_change_rules_title"), "save", model);
			}else{
				ctx.getModerationController().setViolationReportReason(self.user, report, newReason);
			}
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object setReportRules(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);

		Set<Integer> rules;
		QueryParamsMap rulesMap=req.queryMap("rules");
		if(rulesMap==null)
			throw new BadRequestException();
		Set<Integer> validRuleIDs=ctx.getModerationController()
				.getServerRules()
				.stream()
				.map(ServerRule::id)
				.collect(Collectors.toSet());
		rules=rulesMap.toMap()
				.keySet()
				.stream()
				.map(Utils::safeParseInt)
				.filter(validRuleIDs::contains)
				.collect(Collectors.toSet());
		if(rules.isEmpty())
			throw new BadRequestException();

		if(report.reason!=ViolationReport.Reason.SERVER_RULES)
			ctx.getModerationController().setViolationReportReason(self.user, report, ViolationReport.Reason.SERVER_RULES);
		ctx.getModerationController().setViolationReportRules(self.user, report, rules);

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportRulesForm(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_report_choose_rules", req)
				.with("serverRules", ctx.getModerationController().getServerRules())
				.with("selectedRules", report.rules);
		return wrapForm(req, resp, "admin_report_choose_rules", "/settings/admin/reports/"+report.id+"/setRules", lang(req).get("admin_report_change_rules_title"), "save", model);
	}

	public static Object removeReportContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		requireQueryParams(req, "id", "type");
		long contentID=safeParseLong(req.queryParams("id"));
		ReportableContentObjectType contentType=enumValue(req.queryParams("type").toUpperCase(), ReportableContentObjectType.class);

		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		ctx.getModerationController().removeContentFromViolationReport(self.user, report, List.of(new ReportableContentObjectID(contentType, contentID)));
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}
}
