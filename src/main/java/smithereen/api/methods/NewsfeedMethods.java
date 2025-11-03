package smithereen.api.methods;

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
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPhoto;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.photos.Photo;
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
		String rawStartFrom=actx.optParamString("start_from");
		int startFromID=0, offset=0;
		if(StringUtils.isNotEmpty(rawStartFrom)){
			String[] startFromParts=rawStartFrom.split(",");
			if(startFromParts.length==2){
				startFromID=Math.max(0, Utils.safeParseInt(startFromParts[0]));
				offset=Math.max(0, Utils.safeParseInt(startFromParts[1]));
			}
		}

		PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(actx.self, filters, actx.self.prefs.timeZone, startFromID, offset, actx.getCount(25, 100));

		Set<Integer> needPosts=new HashSet<>(), needUsers=new HashSet<>(), needGroups=new HashSet<>();
		for(NewsfeedEntry e:feed.list){
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
				feedPhotos.put(p.id, new ApiPhoto(p, actx, false));
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

		record PhotosInfo(int count, List<ApiPhoto> items, String listId){}
		record RelationInfo(String status, Integer partner){}
		record FriendsFeedItem(String type, int id, int userId, ApiWallPost post, PhotosInfo photos, List<Long> friendIds, List<Long> groupIds, List<ApiBoardTopic> topics, RelationInfo relation){}
		record FriendsFeed(List<FriendsFeedItem> items, List<ApiUser> profiles, List<ApiGroup> groups, String nextFrom){}

		if(!feed.list.isEmpty() && startFromID==0){
			startFromID=switch(feed.list.getFirst()){
				case GroupedNewsfeedEntry gne -> gne.childEntries.getFirst().id;
				case NewsfeedEntry e -> e.id;
			};
		}

		return new FriendsFeed(
				feed.list.stream()
						.map(e->{
							NewsfeedEntry.Type type=e instanceof GroupedNewsfeedEntry gne ? gne.childEntriesType : e.type;
							List<Long> objectIDs=e instanceof GroupedNewsfeedEntry gne ? gne.childEntries.stream().map(ce->ce.objectID).toList() : List.of(e.objectID);
							int entryID=e instanceof GroupedNewsfeedEntry gne ? gne.childEntries.getFirst().id : e.id;
							return switch(type){
								case POST -> new FriendsFeedItem("post", e.id, e.authorID, feedPosts.get((int)e.objectID), null, null, null, null, null);
								case JOIN_GROUP, CREATE_GROUP, JOIN_EVENT, CREATE_EVENT ->
										new FriendsFeedItem(switch(type){
											case JOIN_GROUP -> "group_join";
											case JOIN_EVENT -> "event_join";
											case CREATE_GROUP -> "group_create";
											case CREATE_EVENT -> "event_create";
											default -> throw new IllegalStateException("Unexpected value: " + type);
										}, entryID, e.authorID, null, null, null, objectIDs, null, null);
								case ADD_FRIEND -> new FriendsFeedItem("friend", entryID, e.authorID, null, null, objectIDs, null, null, null);
								case ADD_PHOTO, PHOTO_TAG ->
										new FriendsFeedItem(type==NewsfeedEntry.Type.PHOTO_TAG ? "photo_tag" : "photo",
												entryID, e.authorID, null,
												new PhotosInfo(objectIDs.size(), objectIDs.stream().limit(10).map(feedPhotos::get).toList(), "friends/"+entryID),
												null, null, null, null);
								case BOARD_TOPIC -> new FriendsFeedItem("board", entryID, e.authorID, null, null, null, null, objectIDs.stream().map(feedTopics::get).toList(), null);
								case RELATIONSHIP_STATUS -> {
									User.RelationshipStatus status=User.RelationshipStatus.values()[(int)(e.objectID >> 56)];
									int partnerID=(int)e.objectID;
									yield new FriendsFeedItem("relation", e.id, e.authorID, null, null, null, null, null, new RelationInfo(ApiUser.mapRelationshipStatus(status), partnerID>0 ? partnerID : null));
								}
								default -> throw new IllegalStateException("Unexpected value: " + type);
							};
						})
						.toList(),
				ApiUtils.getUsers(needUsers, ctx, actx),
				ApiUtils.getGroups(needGroups, ctx, actx),
				startFromID+","+(offset+feed.list.size())
		);
	}
}
