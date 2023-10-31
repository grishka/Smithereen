package smithereen;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

import smithereen.util.NamedMutexCollection;

public class NamedMutexCollectionTest{
	private static final Logger LOG=LoggerFactory.getLogger(NamedMutexCollectionTest.class);

	@Test
	public void testSingleThread(){
		NamedMutexCollection mutex=new NamedMutexCollection();
		assertTimeoutPreemptively(Duration.ofSeconds(2), ()->{
			CounterTask task=new CounterTask(mutex, "test");
			Thread thread=new Thread(task);
			thread.start();
			thread.join();
			assertEquals(1, task.count);
			assertEquals(0, mutex.getHeldLockCount());
		});
	}

	@Test
	public void testMultipleStaggeredThreads(){
		NamedMutexCollection mutex=new NamedMutexCollection();
		assertTimeoutPreemptively(Duration.ofSeconds(10), ()->{
			CounterTask task=new CounterTask(mutex, "test");
			ArrayList<Thread> threads=new ArrayList<>(10);
			for(int i=0;i<10;i++){
				Thread thread=new Thread(task, "TaskThread-"+i);
				threads.add(thread);
				try{Thread.sleep(100);}catch(InterruptedException ignore){}
				thread.start();
			}
			for(Thread t:threads){
				t.join();
			}
			assertEquals(1, task.count);
			assertEquals(0, mutex.getHeldLockCount());
		});
	}

	@Test
	public void testMultipleStaggeredThreadsMultipleNames(){
		NamedMutexCollection mutex=new NamedMutexCollection();
		assertTimeoutPreemptively(Duration.ofSeconds(10), ()->{
			CounterTask task1=new CounterTask(mutex, "test1");
			CounterTask task2=new CounterTask(mutex, "test2");
			ArrayList<Thread> threads=new ArrayList<>(10);
			for(int i=0;i<20;i++){
				Thread thread=new Thread(i%2==1 ? task1 : task2, "TaskThread-"+i);
				threads.add(thread);
				try{Thread.sleep(50);}catch(InterruptedException ignore){}
				thread.start();
			}
			for(Thread t:threads){
				t.join();
			}
			assertEquals(1, task1.count);
			assertEquals(1, task2.count);
			assertEquals(0, mutex.getHeldLockCount());
		});
	}

	private static class CounterTask implements Runnable{
		private final NamedMutexCollection mutex;
		private final String name;
		private int count=0;

		private CounterTask(NamedMutexCollection mutex, String name){
			this.mutex=mutex;
			this.name=name;
		}

		@Override
		public void run(){
			LOG.debug("Starting thread {}", Thread.currentThread().getName());
			mutex.acquire(name);
			LOG.debug("Thread {} acquired mutex", Thread.currentThread().getName());
			try{
				try{Thread.sleep(500);}catch(InterruptedException ignore){}
				if(count==0){
					try{Thread.sleep(500);}catch(InterruptedException ignore){}
					count++;
				}
			}finally{
				mutex.release(name);
			}
			LOG.debug("Exiting thread {}", Thread.currentThread().getName());
		}
	}
}
