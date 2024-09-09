package smithereen.controllers;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.model.Account;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.PostStorage;

public class NewsfeedController{
	private static final Logger LOG=LoggerFactory.getLogger(NewsfeedController.class);

	private final ApplicationContext context;
	private LruCache<Integer, CachedFeed> friendsNewsFeedCache=new LruCache<>(100);

	public NewsfeedController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<NewsfeedEntry> getFriendsFeed(Account self, ZoneId timeZone, int startFrom, int offset, int count){
		try{
			CachedFeed cache;
			synchronized(this){
				cache=friendsNewsFeedCache.get(self.user.id);
				if(cache!=null && !cache.timeZone.equals(timeZone))
					cache=null;

				if(cache==null){
					cache=new CachedFeed();
					cache.timeZone=timeZone;
					friendsNewsFeedCache.put(self.user.id, cache);
				}
			}

			int startIndex=-1;
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

			while(startIndex==-1 || startIndex+offset+count>=cache.feed.size()){
				LOG.debug("Getting new feed page from database: userID={}, startFrom={}, offset={}, realOffset={}, count={}", self.user.id, startFrom, offset, cache.realOffset, count);
				int[] total={0};
				List<NewsfeedEntry> newPage=PostStorage.getFeed(self.user.id, 0, cache.realOffset, 100, total);
				cache.total=total[0];
				cache.realOffset+=newPage.size();
				if(newPage.isEmpty()){
					break;
				}

				Set<Integer> needPosts=newPage.stream().filter(e->e.type==NewsfeedEntry.Type.POST).map(e->(int)e.objectID).collect(Collectors.toSet());
				if(!needPosts.isEmpty()){
					Map<Integer, Post> posts=context.getWallController().getPosts(needPosts);
					Set<Integer> inaccessiblePosts=posts.values().stream().filter(p->!context.getPrivacyController().checkPostPrivacy(self.user, p)).map(p->p.id).collect(Collectors.toSet());
					newPage.removeIf(e->e.type==NewsfeedEntry.Type.POST && inaccessiblePosts.contains((int)e.objectID));
				}

				Set<Long> needPhotos=newPage.stream().filter(e->e.type==NewsfeedEntry.Type.ADD_PHOTO).map(e->e.objectID).collect(Collectors.toSet());
				if(!needPhotos.isEmpty()){
					Map<Long, Photo> photos=context.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
					Set<Long> needAlbums=photos.values().stream().map(p->p.albumID).collect(Collectors.toSet());
					Map<Long, PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums);
					Map<Integer, User> owners=context.getUsersController().getUsers(albums.values().stream().map(a->a.ownerID).filter(id->id>0).collect(Collectors.toSet()));
					Set<Long> inaccessibleAlbums=albums.values().stream()
							.filter(a->!context.getPrivacyController().checkUserPrivacy(self.user, owners.get(a.ownerID), a.viewPrivacy))
							.map(a->a.id)
							.collect(Collectors.toSet());
					newPage.removeIf(e->e.type==NewsfeedEntry.Type.ADD_PHOTO && (!photos.containsKey(e.objectID) || inaccessibleAlbums.contains(photos.get(e.objectID).albumID)));
					for(NewsfeedEntry e:newPage){
						if(e.type==NewsfeedEntry.Type.ADD_PHOTO){
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

			if(startIndex!=-1 && startIndex+offset+1<cache.feed.size()){
				return new PaginatedList<>(cache.feed.subList(startIndex+offset, Math.min(cache.feed.size(), startIndex+offset+count)), cache.total, offset, count);
			}

			LOG.warn("Returning an empty feed for user {}", self.user.id);
			return new PaginatedList<>(List.of(), cache.total, offset, count);
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
			NewsfeedStorage.putEntry(user.id, objectID, type, time);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFriendsFeedEntry(User user, long objectID, NewsfeedEntry.Type type){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteEntry(user.id, objectID, type);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void clearFriendsFeedCache(){ // TODO only cache objects like posts in one place, use IDs everywhere else
		friendsNewsFeedCache.evictAll();
	}

	public PaginatedList<NewsfeedEntry> getCommentsFeed(Account self, int offset, int count){
		try{
			PaginatedList<NewsfeedEntry> feed=PostStorage.getCommentsFeed(self.user.id, offset, count);
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

	private record GroupedEntriesKey(LocalDate day, NewsfeedEntry.Type type, int authorID){}
}
