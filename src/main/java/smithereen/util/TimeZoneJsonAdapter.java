package smithereen.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.TimeZone;

public class TimeZoneJsonAdapter extends TypeAdapter<ZoneId>{
	@Override
	public void write(JsonWriter jsonWriter, ZoneId tz) throws IOException{
		if(tz==null){
			jsonWriter.nullValue();
			return;
		}
		jsonWriter.value(tz.getId());
	}

	@Override
	public ZoneId read(JsonReader jsonReader) throws IOException{
		if(jsonReader.peek()==JsonToken.NULL)
			return null;
		return ZoneId.of(jsonReader.nextString());
	}
}
