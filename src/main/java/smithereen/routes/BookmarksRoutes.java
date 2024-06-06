package smithereen.routes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class BookmarksRoutes{
	public static Object users(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("bookmarks_people", req)
				.pageTitle(lang(req).get("bookmarks_title"))
				.with("urlPath", req.raw().getPathInfo());
		String query=req.queryParams("q");
		model.with("query", query);
		if(StringUtils.isNotEmpty(query)){
			model.paginate(ctx.getBookmarksController().searchBookmarkedUsers(self.user, query, offset(req), 100));
		}else{
			model.paginate(ctx.getBookmarksController().getBookmarkedUsers(self.user, offset(req), 100));
		}
		if(isAjax(req)){
			String newURL="/my/bookmarks";
			if(StringUtils.isNotEmpty(query))
				newURL+="?q="+URLEncoder.encode(query, StandardCharsets.UTF_8);
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"))
					.setURL(newURL);
		}
		return model;
	}

	public static Object addUserBookmark(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		ctx.getBookmarksController().addUserBookmark(info.account.user, user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("profileBookmarkButtonText", lang(req).get("remove_bookmark"))
					.setAttribute("profileBookmarkButton", "href", "/users/"+user.id+"/removeBookmark?csrf="+info.csrfToken);
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object removeUserBookmark(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		ctx.getBookmarksController().removeUserBookmark(info.account.user, user);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("profileBookmarkButtonText", lang(req).get("add_bookmark"))
					.setAttribute("profileBookmarkButton", "href", "/users/"+user.id+"/addBookmark?csrf="+info.csrfToken);
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object groups(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("bookmarks_groups", req)
				.pageTitle(lang(req).get("bookmarks_title"))
				.with("urlPath", req.raw().getPathInfo());
		String query=req.queryParams("q");
		model.with("query", query);
		if(StringUtils.isNotEmpty(query)){
			model.paginate(ctx.getBookmarksController().searchBookmarkedGroups(self.user, query, offset(req), 100));
		}else{
			model.paginate(ctx.getBookmarksController().getBookmarkedGroups(self.user, offset(req), 100));
		}
		if(isAjax(req)){
			String newURL="/my/bookmarks/groups";
			if(StringUtils.isNotEmpty(query))
				newURL+="?q="+URLEncoder.encode(query, StandardCharsets.UTF_8);
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"))
					.setURL(newURL);
		}
		return model;
	}

	public static Object addGroupBookmark(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		Group group=ctx.getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
		ctx.getBookmarksController().addGroupBookmark(info.account.user, group);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("profileBookmarkButtonText", lang(req).get("remove_bookmark"))
					.setAttribute("profileBookmarkButton", "href", "/groups/"+group.id+"/removeBookmark?csrf="+info.csrfToken);
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object removeGroupBookmark(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		Group group=ctx.getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
		ctx.getBookmarksController().removeGroupBookmark(info.account.user, group);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("profileBookmarkButtonText", lang(req).get("add_bookmark"))
					.setAttribute("profileBookmarkButton", "href", "/groups/"+group.id+"/addBookmark?csrf="+info.csrfToken);
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object posts(Request req, Response resp, Account self, ApplicationContext ctx){
		PaginatedList<PostViewModel> posts=PostViewModel.wrap(ctx.getUserInteractionsController().getLikedPosts(self.user, true, offset(req), 20));
		RenderedTemplateResponse model=new RenderedTemplateResponse("bookmarks_posts", req)
				.pageTitle(lang(req).get("bookmarks_title"))
				.paginate(posts);

		ctx.getWallController().populateReposts(self.user, posts.list, 2);
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(self.user, posts.list, self.prefs.commentViewType);
		}
		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(posts.list, self.user);
		model.with("postInteractions", interactions);

		PostRoutes.preparePostList(ctx, posts.list, model, self);

		return model;
	}
}
