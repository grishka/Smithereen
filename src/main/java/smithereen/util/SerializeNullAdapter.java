package smithereen.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class SerializeNullAdapter implements TypeAdapterFactory{
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type){
		return new NullSerializer<>(gson.getDelegateAdapter(this, type));
	}

	private static class NullSerializer<T> extends TypeAdapter<T>{
		private final TypeAdapter<T> delegate;

		private NullSerializer(TypeAdapter<T> delegate){
			this.delegate=delegate;
		}

		@Override
		public void write(JsonWriter out, T value) throws IOException{
			if(value==null){
				boolean serializeNulls=out.getSerializeNulls();
				if(!serializeNulls)
					out.setSerializeNulls(true);
				out.nullValue();
				if(!serializeNulls)
					out.setSerializeNulls(false);
			}else{
				delegate.write(out, value);
			}
		}

		@Override
		public T read(JsonReader in) throws IOException{
			return delegate.read(in);
		}
	}
}
