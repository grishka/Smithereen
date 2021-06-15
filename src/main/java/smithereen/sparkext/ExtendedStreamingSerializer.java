package smithereen.sparkext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import spark.serialization.Serializer;

public class ExtendedStreamingSerializer extends Serializer{

	public static final ArrayList<Entry<?>> typeSerializers=new ArrayList<>();

	@Override
	public boolean canProcess(Object element){
		for(Entry<?> e:typeSerializers){
			if(e.type.isInstance(element))
				return true;
		}
		return false;
	}

	@Override
	public void process(OutputStream outputStream, Object element) throws IOException{
		for(Entry<?> e:typeSerializers){
			if(e.type.isInstance(element)){
				e.serialize(outputStream, element);
				return;
			}
		}
	}

	public static class Entry<T>{
		public Class<T> type;
		public ResponseSerializer<T> serializer;

		public Entry(Class<T> type, ResponseSerializer<T> serializer){
			this.type=type;
			this.serializer=serializer;
		}

		private void serialize(OutputStream out, Object obj) throws IOException{
			serializer.serialize(out, type.cast(obj));
		}
	}
}
