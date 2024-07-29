package smithereen.model.feed;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.model.photos.PhotoAlbum;

public class GroupedNewsfeedEntry extends NewsfeedEntry{
	public List<NewsfeedEntry> childEntries=new ArrayList<>();
	public Type childEntriesType;

	public GroupedNewsfeedEntry(@NotNull Collection<NewsfeedEntry> entries){
		if(entries.isEmpty())
			throw new IllegalArgumentException("entries is empty");
		NewsfeedEntry first=entries.iterator().next();
		childEntriesType=first.type;
		authorID=first.authorID;
		if(childEntriesType==Type.ADD_PHOTO){
			extraData=new HashMap<>();
		}
		addChildEntries(entries);
		type=Type.GROUPED;
	}

	public void addChildEntries(Collection<NewsfeedEntry> entries){
		for(NewsfeedEntry e:entries){
			if(e.type!=childEntriesType){
				throw new IllegalArgumentException("Unexpected entry type "+e.type+" added to group of type "+childEntriesType);
			}
			if(e.authorID!=authorID){
				throw new IllegalArgumentException("Unexpected entry author "+e.authorID+" added to group by author "+authorID);
			}
			if(time==null || time.isBefore(e.time))
				time=e.time;
		}
		childEntries.addAll(entries);
		if(childEntriesType==Type.ADD_PHOTO){
			List<PhotoAlbum> albums=(List<PhotoAlbum>) extraData.computeIfAbsent("albums", s->new ArrayList<PhotoAlbum>());
			Set<Long> albumsAlreadyPresent=albums.stream().map(a->a.id).collect(Collectors.toSet());
			for(NewsfeedEntry e:entries){
				PhotoAlbum a=(PhotoAlbum) e.extraData.get("album");
				if(!albumsAlreadyPresent.contains(a.id)){
					albums.add(a);
					albumsAlreadyPresent.add(a.id);
				}
			}
		}
	}

	public List<NewsfeedEntry> getMostRecentEntries(){
		if(childEntries.size()<10)
			return childEntries;
		return childEntries.subList(0, 10);
	}
}
