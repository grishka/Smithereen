package smithereen.api.methods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiUser;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.libvips.VipsImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.ServerRule;
import smithereen.model.User;
import smithereen.model.admin.ViolationReport;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.reports.ReportableContentObject;
import smithereen.routes.ApiRoutes;
import smithereen.storage.MediaStorageUtils;
import smithereen.util.XTEA;

public class ServerMethods{
	public static Object getInfo(ApplicationContext ctx, ApiCallContext actx){
		return new ServerInfo(
				Config.domain,
				Config.getServerDisplayName(),
				Config.serverDescription,
				Config.serverShortDescription,
				BuildInfo.VERSION,
				Config.serverPolicy,
				ctx.getModerationController().getServerRules().stream().map(r->new ServerInfo.Rule(r.id(), r.title(), r.description())).toList(),
				Config.serverAdminEmail,
				ApiUtils.getUsers(ctx.getModerationController().getPublicServerAdmins(), ctx, actx),
				Config.signupMode.toString().toLowerCase(),
				Map.of(
						"smithereen", ApiRoutes.MAX_VERSION
				),
				new ServerInfo.Uploads(MediaStorageUtils.MAX_IMAGE_SIZE, VipsImage.MAX_SIZE, List.of(
						"image/jpeg",
						"image/png",
						"image/gif",
						"image/webp",
						"image/heic",
						"image/heif"
				)),
				new ServerInfo.Stats(
						ctx.getUsersController().getLocalUserCount(),
						ctx.getUsersController().getActiveLocalUserCount(30, TimeUnit.DAYS),
						ctx.getGroupsController().getLocalGroupCount()
				)
		);
	}
	
	public static Object getRestrictedServers(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<Server> servers=ctx.getModerationController().getAllServers(actx.getOffset(), actx.getCount(Integer.MAX_VALUE, Integer.MAX_VALUE), null, true, null);
		return new ApiPaginatedList<>(servers.total, servers.list.stream().map(s->new RestrictedServer(s.host(), s.restriction().publicComment, "suspension")).toList());
	}

	public static Object report(ApplicationContext ctx, ApiCallContext actx){
		Actor target=ApiUtils.getOwner(ctx, actx, "owner_id");
		if(target instanceof User user && user.id==actx.self.user.id)
			throw actx.paramError("can't report oneself");
		ViolationReport.Reason reason=actx.requireParamEnum("reason", Map.of(
				"spam", ViolationReport.Reason.SPAM,
				"rules", ViolationReport.Reason.SERVER_RULES,
				"illegal", ViolationReport.Reason.ILLEGAL,
				"other", ViolationReport.Reason.OTHER
		));

		Set<Integer> rules;
		if(reason==ViolationReport.Reason.SERVER_RULES){
			Set<Integer> validRuleIDs=ctx.getModerationController()
					.getServerRules()
					.stream()
					.map(ServerRule::id)
					.collect(Collectors.toSet());
			rules=actx.requireCommaSeparatedStringSet("rule_ids")
					.stream()
					.map(Utils::safeParseInt)
					.filter(validRuleIDs::contains)
					.collect(Collectors.toSet());
			if(rules.isEmpty())
				throw new BadRequestException();
		}else{
			rules=Set.of();
		}

		List<ReportableContentObject> content=new ArrayList<>();
		if(actx.hasParam("content")){
			int i=0;
			for(JsonElement _co:actx.requireParamJsonArray("content")){
				if(!(_co instanceof JsonObject co))
					throw actx.paramError("content["+i+"] is not an object");

				if(!co.has("type"))
					throw actx.paramError("content["+i+"].type is undefined");
				if(!(co.get("type") instanceof JsonPrimitive _type) || !_type.isString())
					throw actx.paramError("content["+i+"].type is not a string");
				if(!(co.get("id") instanceof JsonPrimitive _id) || (!_id.isString() && !_id.isNumber()))
					throw actx.paramError("content["+i+"].id is not a string or integer");

				String type=_type.getAsString();
				switch(type){
					case "wall_post", "wall_comment" -> {
						int id;
						try{
							id=_id.getAsInt();
						}catch(NumberFormatException x){
							throw actx.paramError("content["+i+"].id is not an integer");
						}
						try{
							Post post=ctx.getWallController().getPostOrThrow(id);
							if("wall_post".equals(type) && post.getReplyLevel()>0)
								throw actx.error(ApiErrorType.NOT_FOUND, "post with id "+id+" does not exist");
							if("wall_comment".equals(type) && post.getReplyLevel()==0)
								throw actx.error(ApiErrorType.NOT_FOUND, "comment with id "+id+" does not exist");
							content.add(post);
						}catch(ObjectNotFoundException x){
							throw actx.error(ApiErrorType.NOT_FOUND, ("wall_post".equals(type) ? "post" : "comment")+" with id "+id+" does not exist");
						}
					}
					case "comment" -> {
						String id=_id.getAsString();
						try{
							Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(id, ObfuscatedObjectIDType.COMMENT));
							ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, comment);
							content.add(comment);
						}catch(ObjectNotFoundException x){
							throw actx.error(ApiErrorType.NOT_FOUND, "comment with id "+id+" does not exist");
						}
					}
					case "photo" -> {
						String id=_id.getAsString();
						try{
							Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(id, ObfuscatedObjectIDType.PHOTO));
							ctx.getPrivacyController().enforceObjectPrivacy(actx.self.user, photo);
							content.add(photo);
						}catch(ObjectNotFoundException x){
							throw actx.error(ApiErrorType.NOT_FOUND, "photo with id "+id+" does not exist");
						}
					}
					case "message" -> {
						String id=_id.getAsString();
						try{
							content.add(ctx.getMailController().getMessage(actx.self.user, XTEA.decodeObjectID(id, ObfuscatedObjectIDType.MAIL_MESSAGE), false));
						}catch(ObjectNotFoundException x){
							throw actx.error(ApiErrorType.NOT_FOUND, "message with id "+id+" does not exist");
						}
					}
				}

				i++;
			}
		}

		ctx.getModerationController().createViolationReport(actx.self.user, target, content, reason, rules, actx.optParamString("comment", ""), actx.booleanParam("forward"));
		return true;
	}

	private record ServerInfo(
			String domain,
			String name,
			String description,
			String shortDescription,
			String version,
			String policy,
			List<Rule> rules,
			String adminEmail,
			List<ApiUser> admins,
			String signupMode,
			Map<String, String> apiVersions,
			Uploads uploads,
			Stats stats
	){
		private record Rule(int id, String title, String description){}
		private record Uploads(long imageMaxSize, int imageMaxDimensions, List<String> imageTypes){}
		private record Stats(int users, int activeUsers, int groups){}
	}

	private record RestrictedServer(String domain, String reason, String restriction){}
}
