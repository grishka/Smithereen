package smithereen.controllers;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.Account;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.FriendsNewsfeedTypeFilter;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.GroupsNewsfeedTypeFilter;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.SessionStorage;

public class NewsfeedController{
	private static final Logger LOG=LoggerFactory.getLogger(NewsfeedController.class);

	private final ApplicationContext context;
	private final LruCache<FriendsFeedCacheKey, CachedFeed> friendsNewsFeedCache=new LruCache<>(100);
	private final LruCache<GroupsFeedCacheKey, CachedFeed> groupsNewsFeedCache=new LruCache<>(100);

	public NewsfeedController(ApplicationContext context){
		this.context=context;
	}

	// region Friends feed
	public PaginatedList<NewsfeedEntry> getFriendsFeed(Account self, EnumSet<FriendsNewsfeedTypeFilter> filter, ZoneId timeZone, int startFrom, int offset, int count){
		try{
			FriendsFeedCacheKey cacheKey=new FriendsFeedCacheKey(self.user.id, EnumSet.copyOf(filter));
			CachedFeed cache;
			synchronized(this){
				cache=friendsNewsFeedCache.get(cacheKey);
				if(cache!=null && !cache.timeZone.equals(timeZone))
					cache=null;

				if(cache==null){
					cache=new CachedFeed();
					cache.timeZone=timeZone;
					friendsNewsFeedCache.put(cacheKey, cache);
				}
			}

			int startIndex=-1;
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized(cache){
				if(startFrom>0){
					int i=0;
					outer:
					for(NewsfeedEntry e:cache.feed){
						if(e.id<=startFrom){
							startIndex=i;
							break;
						}
						if(e instanceof GroupedNewsfeedEntry gne){
							for(NewsfeedEntry ce:gne.childEntries){
								if(ce.id<=startFrom){
									startIndex=i;
									break outer;
								}
							}
						}
						i++;
					}
				}else{
					startIndex=0;
				}

				EnumSet<NewsfeedEntry.Type> actualFilter=getFriendsFeedTypesForFilter(filter);
				HashMap<Integer, EnumSet<NewsfeedEntry.Type>> userTypeFilters=new HashMap<>();

				while(startIndex==-1 || startIndex+offset+count>=cache.feed.size()){
					LOG.debug("Getting new feed page from database: userID={}, startFrom={}, offset={}, realOffset={}, count={}, filter={}", self.user.id, startFrom, offset, cache.realOffset, count, actualFilter);
					PaginatedList<NewsfeedEntry> page=NewsfeedStorage.getFriendsFeed(self.user.id, 0, cache.realOffset, 100, actualFilter);
					ArrayList<NewsfeedEntry> newPage=new ArrayList<>(page.list);
					cache.total=page.total;
					cache.realOffset+=newPage.size();
					if(newPage.isEmpty()){
						break;
					}

					Set<Integer> needPosts=newPage.stream().filter(e->e.type==NewsfeedEntry.Type.POST).map(e->(int) e.objectID).collect(Collectors.toSet());
					if(!needPosts.isEmpty()){
						Map<Integer, Post> posts=context.getWallController().getPosts(needPosts);
						Set<Integer> inaccessiblePosts=posts.values().stream().filter(p->!context.getPrivacyController().checkPostPrivacy(self.user, p)).map(p->p.id).collect(Collectors.toSet());
						newPage.removeIf(e->e.type==NewsfeedEntry.Type.POST && inaccessiblePosts.contains((int) e.objectID));
					}

					Set<Integer> needUsersForPrivacy=newPage.stream()
							.filter(e->e.type!=NewsfeedEntry.Type.POST)
							.map(e->e.authorID)
							.filter(id->!userTypeFilters.containsKey(id))
							.collect(Collectors.toSet());

					if(!needUsersForPrivacy.isEmpty()){
						for(User u: context.getUsersController().getUsers(needUsersForPrivacy).values()){
							EnumSet<NewsfeedEntry.Type> userFilter;
							if(u.newsTypesToShow!=null){
								userFilter=getFriendsFeedTypesForFilter(u.newsTypesToShow);
								userFilter.add(NewsfeedEntry.Type.POST); // Posts are always shown
							}else{
								userFilter=EnumSet.allOf(NewsfeedEntry.Type.class);
							}
							userTypeFilters.put(u.id, userFilter);
						}
					}

					newPage.removeIf(e->{
						EnumSet<NewsfeedEntry.Type> userFilter=userTypeFilters.get(e.authorID);
						return userFilter!=null && !userFilter.contains(e.type);
					});

					Set<Long> needPhotos=newPage.stream().filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG).map(e->e.objectID).collect(Collectors.toSet());
					if(!needPhotos.isEmpty()){
						Map<Long, Photo> photos=context.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
						Set<Long> needAlbums=photos.values().stream().map(p->p.albumID).collect(Collectors.toSet());
						Map<Long, PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums);
						Map<Integer, User> owners=context.getUsersController().getUsers(albums.values().stream().map(a->a.ownerID).filter(id->id>0).collect(Collectors.toSet()));
						Set<Long> inaccessibleAlbums=albums.values().stream()
								.filter(a->!context.getPrivacyController().checkUserPrivacy(self.user, owners.get(a.ownerID), a.viewPrivacy))
								.map(a->a.id)
								.collect(Collectors.toSet());
						newPage.removeIf(e->(e.type==NewsfeedEntry.Type.ADD_PHOTO) && (!photos.containsKey(e.objectID) || inaccessibleAlbums.contains(photos.get(e.objectID).albumID)));
						for(NewsfeedEntry e:newPage){
							if(e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG){
								e.extraData=Map.of("album", albums.get(photos.get(e.objectID).albumID));
							}
						}
					}

					for(NewsfeedEntry e:newPage){
						if(e.type==NewsfeedEntry.Type.RELATIONSHIP_STATUS){
							e.extraData=Map.of(
									"relationship", User.RelationshipStatus.values()[(int)(e.objectID >> 56)],
									"partnerID", (int)e.objectID
							);
						}
					}

					int sizeBefore=cache.feed.size();
					cache.add(newPage);
					int i=0;
					for(NewsfeedEntry e:newPage){
						if(e.id>=startFrom){
							startIndex=i+sizeBefore;
							break;
						}
						i++;
					}
				}
			}

			if(startIndex!=-1 && startIndex+offset<cache.feed.size()){
				return new PaginatedList<>(cache.feed.subList(startIndex+offset, Math.min(cache.feed.size(), startIndex+offset+count)), cache.total, offset, count);
			}

			LOG.warn("Returning an empty friends feed for user {}", self.user.id);
			return new PaginatedList<>(List.of(), cache.total, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private static EnumSet<NewsfeedEntry.Type> getFriendsFeedTypesForFilter(EnumSet<FriendsNewsfeedTypeFilter> filter){
		return filter.stream()
				.flatMap(f->switch(f){
					case POSTS -> Stream.of(NewsfeedEntry.Type.POST);
					case PHOTOS -> Stream.of(NewsfeedEntry.Type.ADD_PHOTO);
					case FRIENDS -> Stream.of(NewsfeedEntry.Type.ADD_FRIEND);
					case GROUPS -> Stream.of(NewsfeedEntry.Type.JOIN_GROUP, NewsfeedEntry.Type.CREATE_GROUP);
					case EVENTS -> Stream.of(NewsfeedEntry.Type.JOIN_EVENT, NewsfeedEntry.Type.CREATE_EVENT);
					case PHOTO_TAGS -> Stream.of(NewsfeedEntry.Type.PHOTO_TAG);
					case PERSONAL_INFO -> Stream.of(NewsfeedEntry.Type.RELATIONSHIP_STATUS);
				})
				.collect(Collectors.toCollection(()->EnumSet.noneOf(NewsfeedEntry.Type.class)));
	}

	public void setFriendsFeedFilters(Account self, EnumSet<FriendsNewsfeedTypeFilter> filter){
		if(filter.equals(EnumSet.allOf(FriendsNewsfeedTypeFilter.class)))
			self.prefs.friendFeedFilter=null;
		else
			self.prefs.friendFeedFilter=filter;

		try{
			SessionStorage.updatePreferences(self.id, self.prefs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putFriendsFeedEntry(User user, long objectID, NewsfeedEntry.Type type){
		putFriendsFeedEntry(user, objectID, type, null);
	}

	public void putFriendsFeedEntry(User user, long objectID, NewsfeedEntry.Type type, @Nullable Instant time){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.putFriendsEntry(user.id, objectID, type, time);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFriendsFeedEntry(User user, long objectID, NewsfeedEntry.Type type){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteFriendsEntry(user.id, objectID, type);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFriendsFeedEntriesForObject(long objectID, NewsfeedEntry.Type type){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteAllFriendsEntriesForObject(objectID, type);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void clearFriendsFeedCache(){
		friendsNewsFeedCache.evictAll();
	}

	public void clearFriendsFeedCache(int userID){
		friendsNewsFeedCache.evictAll(); // TODO
	}

	// endregion
	// region Groups feed

	public PaginatedList<NewsfeedEntry> getGroupsFeed(Account self, EnumSet<GroupsNewsfeedTypeFilter> filter, ZoneId timeZone, int startFrom, int offset, int count){
		try{
			GroupsFeedCacheKey cacheKey=new GroupsFeedCacheKey(self.user.id, EnumSet.copyOf(filter));
			CachedFeed cache;
			synchronized(this){
				cache=groupsNewsFeedCache.get(cacheKey);
				if(cache!=null && !cache.timeZone.equals(timeZone))
					cache=null;

				if(cache==null){
					cache=new CachedFeed();
					cache.timeZone=timeZone;
					groupsNewsFeedCache.put(cacheKey, cache);
				}
			}

			int startIndex=-1;
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized(cache){
				if(startFrom>0){
					int i=0;
					outer:
					for(NewsfeedEntry e:cache.feed){
						if(e.id<=startFrom){
							startIndex=i;
							break;
						}
						if(e instanceof GroupedNewsfeedEntry gne){
							for(NewsfeedEntry ce:gne.childEntries){
								if(ce.id<=startFrom){
									startIndex=i;
									break outer;
								}
							}
						}
						i++;
					}
				}else{
					startIndex=0;
				}

				EnumSet<NewsfeedEntry.Type> actualFilter=getGroupsFeedTypesForFilter(filter);

				while(startIndex==-1 || startIndex+offset+count>=cache.feed.size()){
					LOG.debug("Getting new feed page from database: userID={}, startFrom={}, offset={}, realOffset={}, count={}, filter={}", self.user.id, startFrom, offset, cache.realOffset, count, actualFilter);
					PaginatedList<NewsfeedEntry> page=NewsfeedStorage.getGroupsFeed(self.user.id, 0, cache.realOffset, 100, actualFilter);
					ArrayList<NewsfeedEntry> newPage=new ArrayList<>(page.list);
					cache.total=page.total;
					cache.realOffset+=newPage.size();
					if(newPage.isEmpty()){
						break;
					}

					Set<Long> needPhotos=newPage.stream().filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG).map(e->e.objectID).collect(Collectors.toSet());
					if(!needPhotos.isEmpty()){
						Map<Long, Photo> photos=context.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
						Set<Long> needAlbums=photos.values().stream().map(p->p.albumID).collect(Collectors.toSet());
						Map<Long, PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums);
						for(NewsfeedEntry e:newPage){
							if(e.type==NewsfeedEntry.Type.ADD_PHOTO || e.type==NewsfeedEntry.Type.PHOTO_TAG){
								e.extraData=Map.of("album", albums.get(photos.get(e.objectID).albumID));
							}
						}
					}

					int sizeBefore=cache.feed.size();
					cache.add(newPage);
					int i=0;
					for(NewsfeedEntry e:newPage){
						if(e.id>=startFrom){
							startIndex=i+sizeBefore;
							break;
						}
						i++;
					}
				}
			}

			if(startIndex!=-1 && startIndex+offset<cache.feed.size()){
				return new PaginatedList<>(cache.feed.subList(startIndex+offset, Math.min(cache.feed.size(), startIndex+offset+count)), cache.total, offset, count);
			}

			LOG.warn("Returning an empty groups feed for user {}", self.user.id);
			return new PaginatedList<>(List.of(), cache.total, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private static EnumSet<NewsfeedEntry.Type> getGroupsFeedTypesForFilter(EnumSet<GroupsNewsfeedTypeFilter> filter){
		return filter.stream()
				.flatMap(f->switch(f){
					case POSTS -> Stream.of(NewsfeedEntry.Type.POST);
					case PHOTOS -> Stream.of(NewsfeedEntry.Type.ADD_PHOTO);
				})
				.collect(Collectors.toCollection(()->EnumSet.noneOf(NewsfeedEntry.Type.class)));
	}

	public void setGroupsFeedFilters(Account self, EnumSet<GroupsNewsfeedTypeFilter> filter){
		if(filter.equals(EnumSet.allOf(GroupsNewsfeedTypeFilter.class)))
			self.prefs.groupFeedFilter=null;
		else
			self.prefs.groupFeedFilter=filter;

		try{
			SessionStorage.updatePreferences(self.id, self.prefs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putGroupsFeedEntry(Group group, long objectID, NewsfeedEntry.Type type){
		putGroupsFeedEntry(group, objectID, type, null);
	}

	public void putGroupsFeedEntry(Group group, long objectID, NewsfeedEntry.Type type, @Nullable Instant time){
		try{
			groupsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.putGroupsEntry(group.id, objectID, type, time);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteGroupsFeedEntry(Group group, long objectID, NewsfeedEntry.Type type){
		try{
			groupsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteGroupsEntry(group.id, objectID, type);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteGroupsFeedEntriesForObject(long objectID, NewsfeedEntry.Type type){
		try{
			groupsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteAllGroupsEntriesForObject(objectID, type);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void clearGroupsFeedCache(){
		groupsNewsFeedCache.evictAll();
	}

	// endregion
	// region Comments feed

	public PaginatedList<NewsfeedEntry> getCommentsFeed(Account self, int offset, int count, EnumSet<CommentsNewsfeedObjectType> filter){
		try{
			PaginatedList<NewsfeedEntry> feed=PostStorage.getCommentsFeed(self.user.id, offset, count, filter);
			Set<Long> photoIDs=feed.list.stream().filter(e->e.type==NewsfeedEntry.Type.PHOTO).map(e->e.objectID).collect(Collectors.toSet());
			if(!photoIDs.isEmpty()){
				Map<Long, Long> albumsIDs=PhotoStorage.getAlbumIDsForPhotos(photoIDs);
				Map<Long, PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(new HashSet<>(albumsIDs.values()));
				Map<Integer, User> ownerUsers=context.getUsersController().getUsers(albums.values().stream().map(a->a.ownerID).filter(id->id>0).collect(Collectors.toSet()));
				Set<Long> accessibleAlbums=albums.values().stream()
						.filter(a->a.ownerID<0 || context.getPrivacyController().checkUserPrivacy(self.user, ownerUsers.get(a.ownerID), a.viewPrivacy))
						.map(a->a.id)
						.collect(Collectors.toSet());
				feed.list=new ArrayList<>(feed.list);
				feed.list.removeIf(e->e.type==NewsfeedEntry.Type.PHOTO && !accessibleAlbums.contains(albumsIDs.get(e.objectID)));
			}
			return feed;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setCommentsFeedFilters(Account self, EnumSet<CommentsNewsfeedObjectType> filter){
		if(filter.equals(EnumSet.allOf(CommentsNewsfeedObjectType.class)))
			self.prefs.commentsFeedFilter=null;
		else
			self.prefs.commentsFeedFilter=filter;

		try{
			SessionStorage.updatePreferences(self.id, self.prefs);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion

	private static class CachedFeed{
		public ZoneId timeZone;
		public ArrayList<NewsfeedEntry> feed=new ArrayList<>();
		public int realOffset;
		public int total;
		public HashMap<GroupedEntriesKey, GroupedNewsfeedEntry> existingGroupedEntries=new HashMap<>();
		public HashMap<GroupedEntriesKey, NewsfeedEntry> groupableEntries=new HashMap<>();

		public void add(List<NewsfeedEntry> entries){
			for(NewsfeedEntry e:entries){
				if(e.canBeGrouped()){
					LocalDate day=e.time.atZone(timeZone).toLocalDate();
					GroupedEntriesKey key=new GroupedEntriesKey(day, e.type, e.authorID);
					GroupedNewsfeedEntry existingGroup=existingGroupedEntries.get(key);
					if(existingGroup==null){
						NewsfeedEntry existingUngrouped=groupableEntries.get(key);
						if(existingUngrouped==null){
							groupableEntries.put(key, e);
							feed.add(e);
						}else{
							groupableEntries.remove(key);
							GroupedNewsfeedEntry group=new GroupedNewsfeedEntry(List.of(existingUngrouped, e));
							existingGroupedEntries.put(key, group);
							int index=feed.indexOf(existingUngrouped);
							feed.set(index, group);
						}
					}else{
						existingGroup.addChildEntries(List.of(e));
					}
				}else{
					feed.add(e);
				}
			}
		}
	}

	private record FriendsFeedCacheKey(int userID, EnumSet<FriendsNewsfeedTypeFilter> filter){}
	private record GroupsFeedCacheKey(int userID, EnumSet<GroupsNewsfeedTypeFilter> filter){}
	private record GroupedEntriesKey(LocalDate day, NewsfeedEntry.Type type, int authorID){}
}
