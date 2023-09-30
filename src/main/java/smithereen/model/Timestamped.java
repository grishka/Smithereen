package smithereen.model;

public class Timestamped<T>{
	public final long timestamp=System.currentTimeMillis();
	public final T object;

	public Timestamped(T object){
		this.object=object;
	}
}
