package smithereen.routes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.Account;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.TextProcessor;
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
			if(isMobile(req)){
				RenderedTemplateResponse profile=ProfileRoutes.userProfile(req, resp, user);
				return new WebDeltaResponse(resp)
						.setOuterHTML("profileMoreButton", profile.renderBlock("optionsButton"))
						.showSnackbar(TextProcessor.substituteLinks(lang(req).get("user_bookmark_added", Map.of("name", user.firstName, "gender", user.gender)), Map.of(
								"bookmarks", Map.of("href", "/my/bookmarks")
						)));
			}
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
			if(isMobile(req)){
				RenderedTemplateResponse profile=ProfileRoutes.userProfile(req, resp, user);
				return new WebDeltaResponse(resp)
						.setOuterHTML("profileMoreButton", profile.renderBlock("optionsButton"))
						.showSnackbar(TextProcessor.substituteLinks(lang(req).get("user_bookmark_removed", Map.of("name", user.firstName, "gender", user.gender)), Map.of(
								"bookmarks", Map.of("href", "/my/bookmarks")
						)));
			}
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
			if(isMobile(req)){
				RenderedTemplateResponse profile=GroupsRoutes.groupProfile(req, resp, group);
				return new WebDeltaResponse(resp)
						.setOuterHTML("profileMoreButton", profile.renderBlock("optionsButton"))
						.showSnackbar(TextProcessor.substituteLinks(lang(req).get(group.isEvent() ? "event_bookmark_added" : "group_bookmark_added"), Map.of(
								"bookmarks", Map.of("href", "/my/bookmarks")
						)));
			}
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
			if(isMobile(req)){
				RenderedTemplateResponse profile=GroupsRoutes.groupProfile(req, resp, group);
				return new WebDeltaResponse(resp)
						.setOuterHTML("profileMoreButton", profile.renderBlock("optionsButton"))
						.showSnackbar(TextProcessor.substituteLinks(lang(req).get(group.isEvent() ? "event_bookmark_removed" : "group_bookmark_removed"), Map.of(
								"bookmarks", Map.of("href", "/my/bookmarks")
						)));
			}
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

	public static Object photos(Request req, Response resp, Account self, ApplicationContext ctx){
		int offset=offset(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("bookmarks_photos", req)
				.pageTitle(lang(req).get("bookmarks_title"));
		PaginatedList<Long> photoIDs=ctx.getUserInteractionsController().getLikedObjects(self.user, Like.ObjectType.PHOTO, offset, 100);
		Map<Long, Photo> photoObjects=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs.list);
		PaginatedList<Photo> photos=new PaginatedList<>(photoIDs, photoIDs.list.stream().map(photoObjects::get).toList());
		model.paginate(photos);

		Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
		int i=0;
		for(Photo p:photos.list){
			pvData.put(p.id, new PhotoViewerInlineData(offset+i, "liked/0", p.image.getURLsForPhotoViewer()));
			i++;
		}
		model.with("photoViewerData", pvData);

		if(isAjax(req)){
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("photosInner"));
				if(photos.offset+photos.perPage>=photos.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(photos.offset+photos.perPage));
				}
				return r;
			}
		}

		return model;
	}
}
