package smithereen.api.methods;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import smithereen.ApplicationContext;
import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiUser;
import smithereen.libvips.VipsImage;
import smithereen.model.PaginatedList;
import smithereen.model.Server;
import smithereen.routes.ApiRoutes;
import smithereen.storage.MediaStorageUtils;

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
