package smithereen.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A collection of named mutexes.
 * Useful for when you want only one copy of some task to run at a time.
 * For example, when downloading files in response to a user request, you want any other potential tasks to download the same file to wait
 * for the first one to complete, and then use the already downloaded file rather than download it multiple times in parallel.
 */
public class NamedMutexCollection{
	private final LinkedList<RefCountedReentrantLock> reusePool=new LinkedList<>();
	private final HashMap<String, RefCountedReentrantLock> heldLocks=new HashMap<>();

	public void acquire(String name){
		ReentrantLock lock;
		synchronized(this){
			lock=heldLocks.computeIfAbsent(name, k->reusePool.isEmpty() ? new RefCountedReentrantLock() : reusePool.pop());
		}
		lock.lock();
	}

	public synchronized void release(String name){
		RefCountedReentrantLock lock=heldLocks.get(name);
		if(lock==null)
			throw new IllegalStateException("Mutex for name '"+name+"' not held");
		lock.unlock();
		if(lock.getRefCount()==0){
			heldLocks.remove(name);
			reusePool.push(lock);
		}
	}

	public synchronized int getHeldLockCount(){
		return heldLocks.size();
	}

	private static class RefCountedReentrantLock extends ReentrantLock{
		private AtomicInteger refCount=new AtomicInteger();

		@Override
		public void lock(){
			refCount.incrementAndGet();
			super.lock();
		}

		@Override
		public void unlock(){
			super.unlock();
			refCount.decrementAndGet();
		}

		public int getRefCount(){
			return refCount.get();
		}
	}
}
