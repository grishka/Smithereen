package smithereen.data.feed;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupedNewsfeedEntry extends NewsfeedEntry{
	public List<NewsfeedEntry> childEntries=new ArrayList<>();
	public Type childEntriesType;

	public GroupedNewsfeedEntry(@NotNull Collection<NewsfeedEntry> entries){
		if(entries.isEmpty())
			throw new IllegalArgumentException("entries is empty");
		NewsfeedEntry first=entries.iterator().next();
		childEntriesType=first.type;
		authorID=first.authorID;
		author=first.author;
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
	}

	public List<NewsfeedEntry> getMostRecentEntries(){
		if(childEntries.size()<10)
			return childEntries;
		return childEntries.subList(0, 10);
	}
}
