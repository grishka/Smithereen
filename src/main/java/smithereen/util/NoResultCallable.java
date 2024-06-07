package smithereen.util;

import java.util.concurrent.Callable;

public abstract class NoResultCallable implements Callable<Void>{
	@Override
	public Void call() throws Exception{
		compute();
		return null;
	}

	protected abstract void compute() throws Exception;
}
