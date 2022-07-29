package smithereen.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import smithereen.data.Account;
import smithereen.exceptions.FloodControlViolationException;

/**
 * A thing that allows some action to be performed no more than X times over a period of time.
 */
public class FloodControl<K>{
	public static final FloodControl<Account> PASSWORD_RESET=FloodControl.ofObjectKey(1, 10, TimeUnit.MINUTES, acc -> "account"+acc.id);
	public static final FloodControl<String> EMAIL_RESEND=FloodControl.ofStringKey(1, 10, TimeUnit.MINUTES);
	public static final FloodControl<Account> EMAIL_INVITE=FloodControl.ofObjectKey(5, 1, TimeUnit.HOURS, acc->"account"+acc.id);

	private long timeout;
	private int count;
	private HashMap<String, ActionTracker> trackers=new HashMap<>();
	private Function<K, String> keyFunction;

	private FloodControl(int count, long time, TimeUnit unit, Function<K, String> keyFunction){
		this.count=count;
		timeout=unit.toMillis(time);
		this.keyFunction=keyFunction;
	}

	public static FloodControl<String> ofStringKey(int count, long time, TimeUnit unit){
		return new FloodControl<>(count, time, unit, Function.identity());
	}

	public static <K> FloodControl<K> ofObjectKey(int count, long time, TimeUnit unit, Function<K, String> keyFunction){
		return new FloodControl<K>(count, time, unit, keyFunction);
	}

	private ActionTracker tracker(K key){
		String k=keyFunction.apply(key);
		return trackers.computeIfAbsent(k, _k -> new ActionTracker());
	}

	public synchronized void incrementOrThrow(K key){
		tracker(key).tryPerformAction();
	}

	public synchronized void gc(){
		Iterator<Map.Entry<String, ActionTracker>> itr=trackers.entrySet().iterator();
		while(itr.hasNext()){
			Map.Entry<String, ActionTracker> e=itr.next();
			e.getValue().removeExpired();
			if(e.getValue().isEmpty())
				itr.remove();
		}
	}

	private class ActionTracker{
		private final ArrayList<Long> actionTimestamps=new ArrayList<>();

		public void tryPerformAction(){
			removeExpired();
			if(actionTimestamps.size()>=count)
				throw new FloodControlViolationException();
			actionTimestamps.add(System.currentTimeMillis());
		}

		public void removeExpired(){
			int removeToIndex=-1;
			long time=System.currentTimeMillis()-timeout;
			for(int i=0;i<actionTimestamps.size();i++){
				if(actionTimestamps.get(i)<time){
					removeToIndex=i;
				}else{
					break;
				}
			}
			if(removeToIndex>=0){
				actionTimestamps.subList(0, removeToIndex+1).clear();
			}
		}

		public boolean isEmpty(){
			return actionTimestamps.isEmpty();
		}
	}
}
