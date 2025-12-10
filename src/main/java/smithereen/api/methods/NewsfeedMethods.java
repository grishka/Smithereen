package smithereen.api.methods;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiBoardTopic;
import smithereen.api.model.ApiComment;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPhoto;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.model.CommentViewType;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.GroupsNewsfeedTypeFilter;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import spark.utils.StringUtils;

public class NewsfeedMethods{
	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		EnumSet<FriendsNewsfeedTypeFilter> filters=EnumSet.noneOf(FriendsNewsfeedTypeFilter.class);
		if(actx.hasParam("filters")){
			for(String filter:actx.requireCommaSeparatedStringSet("filters")){
				switch(filter){
					case "post" -> filters.add(FriendsNewsfeedTypeFilter.POSTS);
					case "photo" -> filters.add(FriendsNewsfeedTypeFilter.PHOTOS);
					case "photo_tag" -> filters.add(FriendsNewsfeedTypeFilter.PHOTO_TAGS);
					case "friend" -> filters.add(FriendsNewsfeedTypeFilter.FRIENDS);
					case "group" -> filters.add(FriendsNewsfeedTypeFilter.GROUPS);
					case "event" -> filters.add(FriendsNewsfeedTypeFilter.EVENTS);
					case "board" -> filters.add(FriendsNewsfeedTypeFilter.TOPICS);
					case "relation" -> filters.add(FriendsNewsfeedTypeFilter.PERSONAL_INFO);
				}
			}
		}
		if(filters.isEmpty())
			filters=EnumSet.allOf(FriendsNewsfeedTypeFilter.class);
		boolean returnBanned=actx.booleanParam("return_banned");
		StartFrom sf=getStartFrom(actx);

		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(actx.self, filters, actx.self.prefs.timeZone, sf.id, sf.offset, actx.getCount(25, 100), returnBanned);

		return makeFeed(ctx, actx, feed, sf.id, sf.offset, false);
	}

	public static Object getGroups(ApplicationContext ctx, ApiCallContext actx){
		EnumSet<GroupsNewsfeedTypeFilter> filters=EnumSet.noneOf(GroupsNewsfeedTypeFilter.class);
		if(actx.hasParam("filters")){
			for(String filter:actx.requireCommaSeparatedStringSet("filters")){
				switch(filter){
					case "post" -> filters.add(GroupsNewsfeedTypeFilter.POSTS);
					case "photo" -> filters.add(GroupsNewsfeedTypeFilter.PHOTOS);
					case "board" -> filters.add(GroupsNewsfeedTypeFilter.TOPICS);
				}
			}
		}
		if(filters.isEmpty())
			filters=EnumSet.allOf(GroupsNewsfeedTypeFilter.class);

		StartFrom sf=getStartFrom(actx);
		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getGroupsFeed(actx.self, filters, actx.self.prefs.timeZone, sf.id, sf.offset, actx.getCount(25, 100));
		return makeFeed(ctx, actx, feed, sf.id, sf.offset, true);
	}

	private static Feed makeFeed(ApplicationContext ctx, ApiCallContext actx, PaginatedList<NewsfeedEntry> feed, int startFromID, int offset, boolean isGroups){
		Set<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>();
		for(NewsfeedEntry e: feed.list){
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

		List<PostViewModel> feedPostsList=ctx.getWallController().getPosts(needPosts).values().stream().map(PostViewModel::new).toList();
		Map<Integer, ApiWallPost> feedPosts=ApiUtils.getPosts(feedPostsList, ctx, actx, true, true, false)
				.stream()
				.collect(Collectors.toMap(p->p.id, Function.identity()));

		PostViewModel.collectActorIDs(feedPostsList, needUsers, needGroups);

		Set<Long> needPhotos=feed.list.stream()
				.filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG ||
						(e instanceof GroupedNewsfeedEntry gne && (gne.childEntriesType==NewsfeedEntry.Type.ADD_PHOTO || gne.childEntriesType==NewsfeedEntry.Type.PHOTO_TAG)))
				.flatMap(e->switch(e){
					case GroupedNewsfeedEntry gne -> gne.childEntries.stream().map(ce->ce.objectID);
					default -> Stream.of(e.objectID);
				})
				.collect(Collectors.toSet());

		Map<Long, ApiPhoto> feedPhotos;
		if(!needPhotos.isEmpty()){
			Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
			for(Photo photo:photos.values()){
				if(photo.ownerID>0)
					needUsers.add(photo.ownerID);
				else if(photo.ownerID<0)
					needGroups.add(-photo.ownerID);
				needUsers.add(photo.authorID);
			}
			feedPhotos=new HashMap<>();
			for(Photo p:photos.values()){
				feedPhotos.put(p.id, new ApiPhoto(p, actx, null, null));
			}
		}else{
			feedPhotos=Map.of();
		}

		Set<Long> needTopics=feed.list.stream()
				.filter(e->e.type==NewsfeedEntry.Type.BOARD_TOPIC || (e instanceof GroupedNewsfeedEntry gne && gne.childEntriesType==NewsfeedEntry.Type.BOARD_TOPIC))
				.flatMap(e->switch(e){
					case GroupedNewsfeedEntry gne -> gne.childEntries.stream().map(ce->ce.objectID);
					default -> Stream.of(e.objectID);
				})
				.collect(Collectors.toSet());

		Map<Long, ApiBoardTopic> feedTopics;
		if(!needTopics.isEmpty()){
			Map<Long, BoardTopic> topics=ctx.getBoardController().getTopicsIgnoringPrivacy(needTopics);
			for(BoardTopic topic:topics.values()){
				needGroups.add(topic.groupID);
				needUsers.add(topic.authorID);
			}
			feedTopics=new HashMap<>();
			for(BoardTopic t:topics.values()){
				feedTopics.put(t.id, new ApiBoardTopic(t));
			}
		}else{
			feedTopics=Map.of();
		}

		if(!feed.list.isEmpty() && startFromID==0){
			startFromID=switch(feed.list.getFirst()){
				case GroupedNewsfeedEntry gne -> gne.childEntries.getFirst().id;
				case NewsfeedEntry e -> e.id;
			};
		}

		return new Feed(
				feed.list.stream()
						.map(e->{
							NewsfeedEntry.Type type=e instanceof GroupedNewsfeedEntry gne ? gne.childEntriesType : e.type;
							List<Long> objectIDs=e instanceof GroupedNewsfeedEntry gne ? gne.childEntries.stream().map(ce->ce.objectID).toList() : List.of(e.objectID);
							int entryID=e instanceof GroupedNewsfeedEntry gne ? gne.childEntries.getFirst().id : e.id;
							return switch(type){
								case POST -> new FeedItem("post", e.id, e.authorID>0 ? e.authorID : null, e.authorID<0 ? -e.authorID : null, feedPosts.get((int) e.objectID), null, null, null, null, null);
								case JOIN_GROUP, CREATE_GROUP, JOIN_EVENT, CREATE_EVENT -> new FeedItem(switch(type){
									case JOIN_GROUP -> "group_join";
									case JOIN_EVENT -> "event_join";
									case CREATE_GROUP -> "group_create";
									case CREATE_EVENT -> "event_create";
									default -> throw new IllegalStateException("Unexpected value: "+type);
								}, entryID, e.authorID, null, null, null, null, objectIDs, null, null);
								case ADD_FRIEND -> new FeedItem("friend", entryID, e.authorID>0 ? e.authorID : null, e.authorID<0 ? -e.authorID : null, null, null, objectIDs, null, null, null);
								case ADD_PHOTO, PHOTO_TAG -> new FeedItem(type==NewsfeedEntry.Type.PHOTO_TAG ? "photo_tag" : "photo",
										entryID, e.authorID, null, null,
										new PhotosInfo(objectIDs.size(), objectIDs.stream().limit(10).map(feedPhotos::get).toList(), (isGroups ? "groups/" : "friends/")+entryID),
										null, null, null, null);
								case BOARD_TOPIC -> new FeedItem("board", entryID, e.authorID>0 ? e.authorID : null, e.authorID<0 ? -e.authorID : null, null, null, null, null, objectIDs.stream().map(feedTopics::get).toList(), null);
								case RELATIONSHIP_STATUS -> {
									User.RelationshipStatus status=User.RelationshipStatus.values()[(int) (e.objectID >> 56)];
									int partnerID=(int) e.objectID;
									yield new FeedItem("relation", e.id, e.authorID, null, null, null, null, null, null, new RelationInfo(ApiUser.mapRelationshipStatus(status), partnerID>0 ? partnerID : null));
								}
								default -> throw new IllegalStateException("Unexpected value: "+type);
							};
						})
						.toList(),
				ApiUtils.getUsers(needUsers, ctx, actx),
				ApiUtils.getGroups(needGroups, ctx, actx),
				startFromID+","+(offset+feed.list.size())
		);
	}

	private static StartFrom getStartFrom(ApiCallContext actx){
		String rawStartFrom=actx.optParamString("start_from");
		int startFromID=0, offset=0;
		if(StringUtils.isNotEmpty(rawStartFrom)){
			String[] startFromParts=rawStartFrom.split(",");
			if(startFromParts.length==2){
				startFromID=Math.max(0, Utils.safeParseInt(startFromParts[0]));
				offset=Math.max(0, Utils.safeParseInt(startFromParts[1]));
			}
		}
		return new StartFrom(startFromID, offset);
	}

	public static Object getComments(ApplicationContext ctx, ApiCallContext actx){
		EnumSet<CommentsNewsfeedObjectType> filters=EnumSet.noneOf(CommentsNewsfeedObjectType.class);
		if(actx.hasParam("filters")){
			for(String filter:actx.requireCommaSeparatedStringSet("filters")){
				switch(filter){
					case "post" -> filters.add(CommentsNewsfeedObjectType.POST);
					case "photo" -> filters.add(CommentsNewsfeedObjectType.PHOTO);
					case "board" -> filters.add(CommentsNewsfeedObjectType.BOARD_TOPIC);
				}
			}
		}
		if(filters.isEmpty())
			filters=EnumSet.allOf(CommentsNewsfeedObjectType.class);
		int lastCommentCount=Math.min(actx.optParamIntPositive("last_comments"), 3);
		CommentViewType commentViewType=switch(actx.optParamString("comment_view_type")){
			case "threaded" -> CommentViewType.THREADED;
			case "two_level" -> CommentViewType.TWO_LEVEL;
			case "flat" -> CommentViewType.FLAT;
			case null, default -> actx.self.prefs.commentViewType;
		};

		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getCommentsFeed(actx.self, actx.getOffset(), actx.getCount(25, 100), filters);

		HashSet<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>();
		HashSet<Long> needPhotos=new HashSet<>(), needTopics=new HashSet<>();
		for(NewsfeedEntry e:feed.list){
			switch(e.type){
				case POST -> needPosts.add((int)e.objectID);
				case PHOTO -> needPhotos.add(e.objectID);
				case BOARD_TOPIC -> needTopics.add(e.objectID);
			}
		}

		List<PostViewModel> feedPostsList=ctx.getWallController().getPosts(needPosts).values().stream().map(PostViewModel::new).toList();
		Map<Integer, ApiWallPost> feedPosts=ApiUtils.getPosts(feedPostsList, ctx, actx, true, true, false)
				.stream()
				.collect(Collectors.toMap(p->p.id, Function.identity()));
		Map<Integer, List<Integer>> postCommentIDs;
		Map<Integer, ApiWallPost> allPostComments;
		if(lastCommentCount>0){
			ctx.getWallController().populateCommentPreviews(actx.self.user, feedPostsList, commentViewType, lastCommentCount);
			allPostComments=ApiUtils.getPosts(feedPostsList.stream()
						.flatMap(pvm->pvm.repliesObjects.stream())
						.toList(), ctx, actx, false, false, false)
					.stream()
					.collect(Collectors.toMap(p->p.id, Function.identity()));
			postCommentIDs=new HashMap<>();
			for(PostViewModel pvm:feedPostsList){
				postCommentIDs.put(pvm.post.id, pvm.repliesObjects.stream().map(r->r.post.id).toList());
			}
		}else{
			postCommentIDs=Map.of();
			allPostComments=Map.of();
		}
		PostViewModel.collectActorIDs(feedPostsList, needUsers, needGroups);

		HashSet<CommentParentObjectID> needNonWallComments=new HashSet<>();

		Map<Long, ApiPhoto> feedPhotos;
		if(!needPhotos.isEmpty()){
			Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
			for(Photo photo:photos.values()){
				if(photo.ownerID>0)
					needUsers.add(photo.ownerID);
				else if(photo.ownerID<0)
					needGroups.add(-photo.ownerID);
				needUsers.add(photo.authorID);
			}
			feedPhotos=new HashMap<>();
			for(Photo p:photos.values()){
				feedPhotos.put(p.id, new ApiPhoto(p, actx, null, null));
				if(lastCommentCount>0)
					needNonWallComments.add(p.getCommentParentID());
			}
		}else{
			feedPhotos=Map.of();
		}

		Map<Long, ApiBoardTopic> feedTopics;
		if(!needTopics.isEmpty()){
			Map<Long, BoardTopic> topics=ctx.getBoardController().getTopicsIgnoringPrivacy(needTopics);
			for(BoardTopic topic:topics.values()){
				needGroups.add(topic.groupID);
				needUsers.add(topic.authorID);
				needUsers.add(topic.lastCommentAuthorID);
			}
			feedTopics=new HashMap<>();
			for(BoardTopic t:topics.values()){
				feedTopics.put(t.id, new ApiBoardTopic(t));
				if(lastCommentCount>0)
					needNonWallComments.add(t.getCommentParentID());
			}
		}else{
			feedTopics=Map.of();
		}

		Map<CommentParentObjectID, List<ApiComment>> nonWallComments;
		if(lastCommentCount>0 && !needNonWallComments.isEmpty()){
			Map<CommentParentObjectID, PaginatedList<CommentViewModel>> rawNonWallComments=ctx.getCommentsController().getCommentsForFeed(needNonWallComments, commentViewType==CommentViewType.FLAT, lastCommentCount);
			List<CommentViewModel> allComments=rawNonWallComments.values().stream().flatMap(l->l.list.stream()).toList();
			CommentViewModel.collectUserIDs(allComments, needUsers);
			Map<Long, ApiComment> comments=ApiUtils.getComments(allComments, ctx, actx, false)
					.stream()
					.collect(Collectors.toMap(c->c.rawID, Function.identity()));
			nonWallComments=new HashMap<>();
			rawNonWallComments.forEach((id, rawComments)->
					nonWallComments.put(id, rawComments.list.stream().map(cvm->comments.get(cvm.post.id)).toList()));
		}else{
			nonWallComments=Map.of();
		}

		ArrayList<CommentsFeedItem> items=new ArrayList<>();
		for(NewsfeedEntry e:feed.list){
			items.add(switch(e.type){
				case POST -> new CommentsFeedItem("post", feedPosts.get((int)e.objectID), null, null,
						lastCommentCount==0 ? null : postCommentIDs.get((int)e.objectID).stream().map(allPostComments::get).toList());
				case PHOTO -> new CommentsFeedItem("photo", null, feedPhotos.get(e.objectID), null,
						lastCommentCount==0 ? null : nonWallComments.get(new CommentParentObjectID(CommentableObjectType.PHOTO, e.objectID)));
				case BOARD_TOPIC -> new CommentsFeedItem("topic", null, null, feedTopics.get(e.objectID),
						lastCommentCount==0 ? null : nonWallComments.get(new CommentParentObjectID(CommentableObjectType.BOARD_TOPIC, e.objectID)));
				default -> throw new IllegalStateException("Unexpected value: " + e.type);
			});
		}

		return new CommentsFeed(feed.total, items, ApiUtils.getUsers(needUsers, ctx, actx), ApiUtils.getGroups(needGroups, ctx, actx));
	}

	record PhotosInfo(int count, List<ApiPhoto> items, String listId){}
	record RelationInfo(String status, Integer partner){}
	record FeedItem(String type, int id, Integer userId, Integer groupId, ApiWallPost post, PhotosInfo photos, List<Long> friendIds, List<Long> groupIds, List<ApiBoardTopic> topics, RelationInfo relation){}
	record Feed(List<FeedItem> items, List<ApiUser> profiles, List<ApiGroup> groups, String nextFrom){}
	record CommentsFeedItem(String type, ApiWallPost post, ApiPhoto photo, ApiBoardTopic topic, List<?> comments){}
	record CommentsFeed(int count, List<CommentsFeedItem> items, List<ApiUser> profiles, List<ApiGroup> groups){}

	private record StartFrom(int id, int offset){}
}
