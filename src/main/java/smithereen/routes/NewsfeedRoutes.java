package smithereen.routes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.LikeableContentObject;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPresence;
import smithereen.model.WebDeltaResponse;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.GroupsNewsfeedTypeFilter;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.filtering.FilterContext;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class NewsfeedRoutes{
	private static void prepareFeed(ApplicationContext ctx, Request req, Account self, List<NewsfeedEntry> feed, RenderedTemplateResponse model, boolean needNonPostInteractions, FilterContext filterContext){
		Set<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>(), needUserOnlines=new HashSet<>();
		for(NewsfeedEntry e:feed){
			if(e.authorID>0)
				needUsers.add(e.authorID);
			else
				needGroups.add(-e.authorID);
			switch(e.type){
				case GROUPED -> {
					GroupedNewsfeedEntry gne=(GroupedNewsfeedEntry) e;
					switch(gne.childEntriesType){
						case ADD_FRIEND -> {
							for(NewsfeedEntry ce:gne.childEntries){
								needUsers.add((int) ce.objectID);
							}
						}
						case JOIN_GROUP, JOIN_EVENT -> {
							for(NewsfeedEntry ce:gne.childEntries){
								needGroups.add((int) ce.objectID);
							}
						}
						case null, default -> {}
					}
				}
				case POST, RETOOT -> {
					needUserOnlines.add(e.authorID);
					needPosts.add((int) e.objectID);
				}
				case ADD_FRIEND -> needUsers.add((int) e.objectID);
				case JOIN_GROUP, JOIN_EVENT, CREATE_GROUP, CREATE_EVENT -> needGroups.add((int) e.objectID);
				case RELATIONSHIP_STATUS -> {
					int partner=(int)e.extraData.get("partnerID");
					if(partner!=0)
						needUsers.add(partner);
				}

				case null, default -> {}
			}
		}

		List<PostViewModel> feedPosts=ctx.getWallController().getPosts(needPosts).values().stream().map(PostViewModel::new).toList();

		ctx.getWallController().populateReposts(self.user, feedPosts, 2);
		if(req.attribute("mobile")==null && !feedPosts.isEmpty()){
			ctx.getWallController().populateCommentPreviews(self.user, feedPosts, self.prefs.commentViewType);
		}

		if(filterContext!=null){
			ctx.getNewsfeedController().applyFiltersToPosts(self.user, filterContext, feedPosts);
		}

		PostViewModel.collectActorIDs(feedPosts, needUsers, needGroups);

		Set<Long> needPhotos=feed.stream()
				.filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG ||
						(e instanceof GroupedNewsfeedEntry gne && (gne.childEntriesType==NewsfeedEntry.Type.ADD_PHOTO || gne.childEntriesType==NewsfeedEntry.Type.PHOTO_TAG)))
				.flatMap(e->switch(e){
					case GroupedNewsfeedEntry gne -> gne.childEntries.stream().map(ce->ce.objectID);
					default -> Stream.of(e.objectID);
				})
				.collect(Collectors.toSet());

		Set<LikeableContentObject> commentsForInteractions=new HashSet<>();

		if(!needPhotos.isEmpty()){
			Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
			for(Photo photo:photos.values()){
				if(photo.ownerID>0)
					needUsers.add(photo.ownerID);
				else if(photo.ownerID<0)
					needGroups.add(-photo.ownerID);
				needUsers.add(photo.authorID);
			}
			model.with("photos", photos);
			if(needNonPostInteractions)
				model.with("photosInteractions", ctx.getUserInteractionsController().getUserInteractions(photos.values(), self.user));

			if(needNonPostInteractions){
				Map<CommentParentObjectID, PaginatedList<CommentViewModel>> comments=ctx.getCommentsController().getCommentsForFeed(needPhotos.stream()
						.map(id->new CommentParentObjectID(CommentableObjectType.PHOTO, id))
						.collect(Collectors.toSet()), self.prefs.commentViewType==CommentViewType.FLAT, 3);

				CommentViewModel.collectUserIDs(comments.values().stream().flatMap(pl->pl.list.stream()).toList(), needUsers);

				model.with("photosComments", comments.entrySet().stream()
						.filter(e->e.getKey().type()==CommentableObjectType.PHOTO)
						.collect(Collectors.toMap(e->e.getKey().id(), Map.Entry::getValue)));

				comments.values().stream().flatMap(l->l.list.stream().map(cvm->cvm.post)).forEach(commentsForInteractions::add);
			}
		}

		Set<Long> needTopics=feed.stream()
				.filter(e->e.type==NewsfeedEntry.Type.BOARD_TOPIC)
				.map(e->e.objectID)
				.collect(Collectors.toSet());

		if(!needTopics.isEmpty()){
			Map<Long, BoardTopic> topics=ctx.getBoardController().getTopicsIgnoringPrivacy(needTopics);
			for(BoardTopic topic:topics.values()){
				needGroups.add(topic.groupID);
				needUsers.add(topic.authorID);
			}
			model.with("topics", topics);

			if(needNonPostInteractions){
				Map<CommentParentObjectID, PaginatedList<CommentViewModel>> comments=ctx.getCommentsController().getCommentsForFeed(needTopics.stream()
						.map(id->new CommentParentObjectID(CommentableObjectType.BOARD_TOPIC, id))
						.collect(Collectors.toSet()), true, 3);

				CommentViewModel.collectUserIDs(comments.values().stream().flatMap(pl->pl.list.stream()).toList(), needUsers);

				model.with("topicsComments", comments.entrySet().stream()
						.filter(e->e.getKey().type()==CommentableObjectType.BOARD_TOPIC)
						.collect(Collectors.toMap(e->e.getKey().id(), Map.Entry::getValue)));

				comments.values().stream().flatMap(l->l.list.stream().map(cvm->cvm.post)).forEach(commentsForInteractions::add);
			}
		}

		Map<Long, UserInteractions> commentInteractions=ctx.getUserInteractionsController().getUserInteractions(commentsForInteractions, self.user);
		model.with("commentInteractions", commentInteractions);

		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers, true);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(needGroups);

		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(feedPosts, self.user);
		model.with("posts", feedPosts.stream().collect(Collectors.toMap(pvm->pvm.post.id, Function.identity())))
				.with("users", users).with("groups", groups).with("postInteractions", interactions);
		model.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)).with("commentViewType", self.prefs.commentViewType);

		Map<Integer, UserPresence> onlines;
		if(!needUserOnlines.isEmpty()){
			onlines=ctx.getUsersController().getUserPresencesOnlineOnly(needUserOnlines);
		}else{
			onlines=Map.of();
		}
		model.with("onlines", onlines);
	}

	public static Object feed(Request req, Response resp, Account self, ApplicationContext ctx){
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		EnumSet<FriendsNewsfeedTypeFilter> filter=self.prefs.friendFeedFilter;
		if(filter==null)
			filter=EnumSet.allOf(FriendsNewsfeedTypeFilter.class);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(self, filter, timeZoneForRequest(req), startFromID, offset, 25);
		if(!feed.list.isEmpty() && startFromID==0)
			startFromID=feed.list.getFirst().id;
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "feed_filters");
		Templates.addJsLangForNewPostForm(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("feed", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed?startFrom="+startFromID+"&offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationPerPage", 25).with("paginationFirstPageUrl", "/feed")
				.with("draftAttachments", Utils.sessionInfo(req).postDraftAttachments)
				.with("feedFilter", filter.stream().map(Object::toString).collect(Collectors.toSet()));

		prepareFeed(ctx, req, self, feed.list, model, false, FilterContext.FRIENDS_FEED);

		return model;
	}

	public static Object setFeedFilters(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<FriendsNewsfeedTypeFilter> filter=EnumSet.noneOf(FriendsNewsfeedTypeFilter.class);
		for(FriendsNewsfeedTypeFilter type:FriendsNewsfeedTypeFilter.values()){
			if(req.queryParams(type.toString())!=null)
				filter.add(type);
		}
		ctx.getNewsfeedController().setFriendsFeedFilters(self, filter);
		if(isMobile(req)){
			return new WebDeltaResponse(resp).refresh();
		}else{
			RenderedTemplateResponse model=(RenderedTemplateResponse) feed(req, resp, self, ctx);
			return new WebDeltaResponse(resp)
					.setContent("feedContent", model.renderBlock("feedContent"))
					.setContent("feedTopSummary", model.renderBlock("topSummary"))
					.setContent("feedBottomSummary", model.renderBlock("bottomSummary"));
		}
	}

	public static Object commentsFeed(Request req, Response resp, Account self, ApplicationContext ctx){
		int offset=offset(req);
		EnumSet<CommentsNewsfeedObjectType> filter=self.prefs.commentsFeedFilter!=null ? self.prefs.commentsFeedFilter : EnumSet.allOf(CommentsNewsfeedObjectType.class);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getCommentsFeed(self, offset, 25, filter);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "cancel", "feed_filters");
		Templates.addJsLangForNewPostForm(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("feed_comments", req)
				.pageTitle(Utils.lang(req).get("feed"))
				.with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed/comments?offset=")
				.with("totalItems", feed.total)
				.with("paginationOffset", offset)
				.with("paginationFirstPageUrl", "/feed/comments")
				.with("paginationPerPage", 25)
				.with("feedFilter", filter.stream().map(Object::toString).collect(Collectors.toSet()));

		prepareFeed(ctx, req, self, feed.list, model, true, null);

		return model;
	}

	public static Object setCommentsFeedFilters(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<CommentsNewsfeedObjectType> filter=EnumSet.noneOf(CommentsNewsfeedObjectType.class);
		for(CommentsNewsfeedObjectType type:CommentsNewsfeedObjectType.values()){
			if(req.queryParams(type.toString())!=null)
				filter.add(type);
		}
		ctx.getNewsfeedController().setCommentsFeedFilters(self, filter);
		if(isMobile(req)){
			return new WebDeltaResponse(resp).refresh();
		}else{
			RenderedTemplateResponse model=(RenderedTemplateResponse) commentsFeed(req, resp, self, ctx);
			return new WebDeltaResponse(resp)
					.setContent("feedContent", model.renderBlock("feedContent"))
					.setContent("feedTopSummary", model.renderBlock("topSummary"))
					.setContent("feedBottomSummary", model.renderBlock("bottomSummary"));
		}
	}

	public static Object groupsFeed(Request req, Response resp, Account self, ApplicationContext ctx){
		int startFromID=parseIntOrDefault(req.queryParams("startFrom"), 0);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		EnumSet<GroupsNewsfeedTypeFilter> filter=self.prefs.groupFeedFilter;
		if(filter==null)
			filter=EnumSet.allOf(GroupsNewsfeedTypeFilter.class);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getGroupsFeed(self, filter, timeZoneForRequest(req), startFromID, offset, 25);
		if(!feed.list.isEmpty() && startFromID==0)
			startFromID=feed.list.getFirst().id;
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "cancel", "feed_filters");
		Templates.addJsLangForNewPostForm(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("feed_groups", req).with("title", Utils.lang(req).get("feed")).with("feed", feed.list)
				.with("paginationUrlPrefix", "/feed/groups?startFrom="+startFromID+"&offset=").with("totalItems", feed.total).with("paginationOffset", offset).with("paginationPerPage", 25).with("paginationFirstPageUrl", "/feed/groups")
				.with("feedFilter", filter.stream().map(Object::toString).collect(Collectors.toSet()))
				.with("photosList", "groupsFeed")
				.with("groupedPhotosList", "groupsFeedGrouped");

		prepareFeed(ctx, req, self, feed.list, model, false, FilterContext.GROUPS_FEED);

		return model;
	}

	public static Object setGroupsFeedFilters(Request req, Response resp, Account self, ApplicationContext ctx){
		EnumSet<GroupsNewsfeedTypeFilter> filter=EnumSet.noneOf(GroupsNewsfeedTypeFilter.class);
		for(GroupsNewsfeedTypeFilter type:GroupsNewsfeedTypeFilter.values()){
			if(req.queryParams(type.toString())!=null)
				filter.add(type);
		}
		ctx.getNewsfeedController().setGroupsFeedFilters(self, filter);
		if(isMobile(req)){
			return new WebDeltaResponse(resp).refresh();
		}else{
			RenderedTemplateResponse model=(RenderedTemplateResponse) groupsFeed(req, resp, self, ctx);
			return new WebDeltaResponse(resp)
					.setContent("feedContent", model.renderBlock("feedContent"))
					.setContent("feedTopSummary", model.renderBlock("topSummary"))
					.setContent("feedBottomSummary", model.renderBlock("bottomSummary"));
		}
	}
}
