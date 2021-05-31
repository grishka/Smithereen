package smithereen.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.TimeZone;

public class TimeZoneJsonAdapter extends TypeAdapter<TimeZone>{
	@Override
	public void write(JsonWriter jsonWriter, TimeZone tz) throws IOException{
		if(tz==null){
			jsonWriter.nullValue();
			return;
		}
		jsonWriter.value(tz.getID());
	}

	@Override
	public TimeZone read(JsonReader jsonReader) throws IOException{
		if(jsonReader.peek()==JsonToken.NULL)
			return null;
		return TimeZone.getTimeZone(jsonReader.nextString());
	}
}
