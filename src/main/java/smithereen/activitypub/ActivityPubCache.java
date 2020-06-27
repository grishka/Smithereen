package smithereen.activitypub;

import org.jetbrains.annotations.Nullable;

import smithereen.LruCache;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.Timestamped;

/**
 * Temporary objects are kept here.
 * For example, it isn't feasible to store Undo{Like} in the database, but some implementations might want to fetch these because they don't support LD-signatures.
 */
public class ActivityPubCache{
	private static LruCache<Integer, Timestamped<Undo>> undoneLikes=new LruCache<>(1000);

	public static synchronized void putUndoneLike(int id, Undo act){
		undoneLikes.put(id, new Timestamped<>(act));
	}

	public static synchronized @Nullable Undo getUndoneLike(int id){
		Timestamped<Undo> t=undoneLikes.get(id);
		if(t==null)
			return null;
		if(System.currentTimeMillis()-t.timestamp>10*60*1000L){
			undoneLikes.remove(id);
			return null;
		}
		return t.object;
	}
}
