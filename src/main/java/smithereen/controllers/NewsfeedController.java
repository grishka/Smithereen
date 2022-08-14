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
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.activitypub.objects.Actor;
import smithereen.data.Account;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.feed.GroupedNewsfeedEntry;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.NewsfeedStorage;
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
				for(NewsfeedEntry e:cache.feed){
					if(e.id>=startFrom){
						startIndex=i;
						break;
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

	public void putFriendsFeedEntry(User user, int objectID, NewsfeedEntry.Type type){
		putFriendsFeedEntry(user, objectID, type, null);
	}

	public void putFriendsFeedEntry(User user, int objectID, NewsfeedEntry.Type type, @Nullable Instant time){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.putEntry(user.id, objectID, type, time);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFriendsFeedEntry(User user, int objectID, NewsfeedEntry.Type type){
		try{
			friendsNewsFeedCache.evictAll(); // TODO
			NewsfeedStorage.deleteEntry(user.id, objectID, type);
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
