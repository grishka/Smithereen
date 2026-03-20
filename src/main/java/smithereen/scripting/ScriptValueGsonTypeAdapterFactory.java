package smithereen.scripting;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ScriptValueGsonTypeAdapterFactory implements TypeAdapterFactory{
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type){
		if(ScriptValue.class.isAssignableFrom(type.getRawType())){
			return (TypeAdapter<T>) new Adapter(gson);
		}
		return null;
	}

	private class Adapter extends TypeAdapter<ScriptValue>{
		private final Gson gson;

		private Adapter(Gson gson){
			this.gson=gson;
		}

		@Override
		public void write(JsonWriter out, ScriptValue value) throws IOException{
			switch(value){
				case ScriptValue.Str(String s) -> out.value(s);
				case ScriptValue.Num(double n) -> {
					if(n%1==0)
						out.value((long)n);
					else
						out.value(n);
				}
				case ScriptValue.Bool(boolean b) -> out.value(b);
				case ScriptValue.Obj(Map<String, ScriptValue> o) -> gson.getAdapter(Map.class).write(out, o);
				case ScriptValue.Arr(List<ScriptValue> a) -> gson.getAdapter(List.class).write(out, a);
			}
		}

		@Override
		public ScriptValue read(JsonReader in) throws IOException{
			return null;
		}
	}
}
