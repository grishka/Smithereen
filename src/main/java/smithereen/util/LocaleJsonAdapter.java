package smithereen.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;

public class LocaleJsonAdapter extends TypeAdapter<Locale>{
	@Override
	public void write(JsonWriter jsonWriter, Locale locale) throws IOException{
		if(locale==null){
			jsonWriter.nullValue();
			return;
		}
		jsonWriter.value(locale.toLanguageTag());
	}

	@Override
	public Locale read(JsonReader jsonReader) throws IOException{
		if(jsonReader.peek()==JsonToken.NULL)
			return null;
		return Locale.forLanguageTag(jsonReader.nextString());
	}
}
