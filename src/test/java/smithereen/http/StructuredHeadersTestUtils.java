package smithereen.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;

public class StructuredHeadersTestUtils{
	public static JsonElement toJson(List<StructuredHttpHeaders.ItemOrInnerList> list){
		return list.stream()
				.map(StructuredHeadersTestUtils::toJson)
				.collect(JsonArrayBuilder.COLLECTOR);
	}

	public static JsonElement toJson(StructuredHttpHeaders.BareItem item){
		return switch(item){
			case StructuredHttpHeaders.BareItem.IntegerItem(long value) -> new JsonPrimitive(value);
			case StructuredHttpHeaders.BareItem.DecimalItem(double value) -> new JsonPrimitive(value);
			case StructuredHttpHeaders.BareItem.StringItem(String value) -> new JsonPrimitive(value);
			case StructuredHttpHeaders.BareItem.TokenItem(String value) -> new JsonObjectBuilder().add("__type", "token").add("value", value).build();
			case StructuredHttpHeaders.BareItem.ByteSequenceItem(byte[] value) -> new JsonObjectBuilder().add("__type", "binary").add("value", base32Encode(value)).build();
			case StructuredHttpHeaders.BareItem.DateItem(Instant value) -> new JsonObjectBuilder().add("__type", "date").add("value", value.getEpochSecond()).build();
			case StructuredHttpHeaders.BareItem.BooleanItem(boolean value) -> new JsonPrimitive(value);
			case StructuredHttpHeaders.BareItem.DisplayStringItem(String value) -> new JsonObjectBuilder().add("__type", "displaystring").add("value", value).build();
		};
	}

	public static JsonElement toJson(StructuredHttpHeaders.Item item){
		return new JsonArrayBuilder()
				.add(toJson(item.item()))
				.add(parametersToJson(item.parameters()))
				.build();
	}

	public static JsonElement toJson(Map<String, StructuredHttpHeaders.ItemOrInnerList> map){
		return map.entrySet()
				.stream()
				.map(e->new JsonArrayBuilder().add(e.getKey()).add(toJson(e.getValue())).build())
				.collect(JsonArrayBuilder.COLLECTOR);
	}

	private static JsonElement toJson(StructuredHttpHeaders.ItemOrInnerList itemOrList){
		return switch(itemOrList){
			case StructuredHttpHeaders.Item item -> toJson(item);
			case StructuredHttpHeaders.InnerList inner -> new JsonArrayBuilder()
					.add(inner.stream().map(StructuredHeadersTestUtils::toJson).collect(JsonArrayBuilder.COLLECTOR))
					.add(parametersToJson(inner.parameters))
					.build();
		};
	}

	private static JsonArray parametersToJson(Map<String, StructuredHttpHeaders.BareItem> params){
		return params.entrySet()
				.stream()
				.map(e->new JsonArrayBuilder().add(e.getKey()).add(toJson(e.getValue())).build())
				.collect(JsonArrayBuilder.COLLECTOR);
	}

	public static StructuredHttpHeaders.BareItem bareItemFromJson(JsonElement je){
		return switch(je){
			case JsonPrimitive primitive -> {
				if(primitive.isNumber()){
					yield primitive.toString().contains(".") ? StructuredHttpHeaders.BareItem.ofDecimal(primitive.getAsDouble()) : StructuredHttpHeaders.BareItem.ofInteger(primitive.getAsLong());
				}else if(primitive.isBoolean()){
					yield StructuredHttpHeaders.BareItem.ofBoolean(primitive.getAsBoolean());
				}else{
					yield StructuredHttpHeaders.BareItem.ofString(primitive.getAsString());
				}
			}
			case JsonObject obj -> switch(obj.get("__type").getAsString()){
				case "token" -> StructuredHttpHeaders.BareItem.ofToken(obj.get("value").getAsString());
				case "binary" -> StructuredHttpHeaders.BareItem.ofByteSequence(base32Decode(obj.get("value").getAsString()));
				case "date" -> StructuredHttpHeaders.BareItem.ofDate(Instant.ofEpochSecond(obj.get("value").getAsLong()));
				case "displaystring" -> StructuredHttpHeaders.BareItem.ofDisplayString(obj.get("value").getAsString());
				default -> throw new IllegalStateException();
			};
			default -> throw new IllegalStateException("Unexpected value: " + je);
		};
	}

	public static StructuredHttpHeaders.Item itemFromJson(JsonElement el){
		JsonArray arr=el.getAsJsonArray();
		return new StructuredHttpHeaders.Item(bareItemFromJson(arr.get(0)), parametersFromJson(arr.get(1).getAsJsonArray()));
	}

	private static Map<String, StructuredHttpHeaders.BareItem> parametersFromJson(JsonArray arr){
		Map<String, StructuredHttpHeaders.BareItem> params=new LinkedHashMap<>();
		for(JsonElement el:arr){
			JsonArray param=el.getAsJsonArray();
			params.put(param.get(0).getAsString(), bareItemFromJson(param.get(1)));
		}
		return params;
	}

	public static List<StructuredHttpHeaders.ItemOrInnerList> listFromJson(JsonElement el){
		ArrayList<StructuredHttpHeaders.ItemOrInnerList> list=new ArrayList<>();
		for(JsonElement item:el.getAsJsonArray()){
			list.add(itemOrInnerListFromJson(item));
		}
		return list;
	}

	public static StructuredHttpHeaders.ItemOrInnerList itemOrInnerListFromJson(JsonElement el){
		JsonArray ar=el.getAsJsonArray();
		if(ar.get(0) instanceof JsonArray inner){
			StructuredHttpHeaders.InnerList list=new StructuredHttpHeaders.InnerList();
			list.parameters=parametersFromJson(ar.get(1).getAsJsonArray());
			for(JsonElement item:inner){
				list.add(itemFromJson(item));
			}
			return list;
		}
		return itemFromJson(el);
	}

	public static Map<String, StructuredHttpHeaders.ItemOrInnerList> dictionaryFromJson(JsonElement el){
		Map<String, StructuredHttpHeaders.ItemOrInnerList> dict=new LinkedHashMap<>();
		for(JsonElement _item:el.getAsJsonArray()){
			JsonArray item=_item.getAsJsonArray();
			dict.put(item.get(0).getAsString(), itemOrInnerListFromJson(item.get(1)));
		}
		return dict;
	}

	private static String base32Encode(byte[] input){
		final char[] alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
		StringBuilder out=new StringBuilder();
		try{
			DataInputStream in=new DataInputStream(new ByteArrayInputStream(input));
			while(in.available()>=5){
				long chunk=((long)in.readInt() << 8) | in.read();
				out.append(alphabet[(int)(chunk >> 35) & 0x1F]);
				out.append(alphabet[(int)(chunk >> 30) & 0x1F]);
				out.append(alphabet[(int)(chunk >> 25) & 0x1F]);
				out.append(alphabet[(int)(chunk >> 20) & 0x1F]);
				out.append(alphabet[(int)(chunk >> 15) & 0x1F]);
				out.append(alphabet[(int)(chunk >> 10) & 0x1F]);
				out.append(alphabet[(int)(chunk >>  5) & 0x1F]);
				out.append(alphabet[(int)(chunk) & 0x1F]);
			}
			switch(in.available()){
				case 4 -> {
					int chunk=in.readInt();
					out.append(alphabet[(chunk >> 27) & 0x1F]);
					out.append(alphabet[(chunk >> 22) & 0x1F]);
					out.append(alphabet[(chunk >> 17) & 0x1F]);
					out.append(alphabet[(chunk >> 12) & 0x1F]);
					out.append(alphabet[(chunk >>  7) & 0x1F]);
					out.append(alphabet[(chunk >>  2) & 0x1F]);
					out.append(alphabet[(chunk <<  3) & 0x1F]);
					out.append('=');
				}
				case 3 -> {
					int chunk=(in.readUnsignedShort() << 8) | in.read();
					out.append(alphabet[(chunk >> 19) & 0x1F]);
					out.append(alphabet[(chunk >> 14) & 0x1F]);
					out.append(alphabet[(chunk >>  9) & 0x1F]);
					out.append(alphabet[(chunk >>  4) & 0x1F]);
					out.append(alphabet[(chunk <<  1) & 0x1F]);
					out.append("===");
				}
				case 2 -> {
					int chunk=in.readUnsignedShort();
					out.append(alphabet[(chunk >> 11) & 0x1F]);
					out.append(alphabet[(chunk >>  6) & 0x1F]);
					out.append(alphabet[(chunk >>  1) & 0x1F]);
					out.append(alphabet[(chunk <<  4) & 0x1F]);
					out.append("====");
				}
				case 1 -> {
					int chunk=in.read();
					out.append(alphabet[(chunk >>  3) & 0x1F]);
					out.append(alphabet[(chunk <<  2) & 0x1F]);
					out.append("======");
				}
			}
		}catch(IOException ignore){}
		return out.toString();
	}

	private static byte[] base32Decode(String input){
		final String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
		char[] in=input.replace("=", "").toCharArray();
		try{
			ByteArrayOutputStream buf=new ByteArrayOutputStream();
			DataOutputStream out=new DataOutputStream(buf);
			for(int offset=0;in.length-offset>=8;offset+=8){
				long chunk=
						((long)alphabet.indexOf(in[offset]) << 35)
						| ((long)alphabet.indexOf(in[offset+1]) << 30)
						| ((long)alphabet.indexOf(in[offset+2]) << 25)
						| ((long)alphabet.indexOf(in[offset+3]) << 20)
						| ((long)alphabet.indexOf(in[offset+4]) << 15)
						| ((long)alphabet.indexOf(in[offset+5]) << 10)
						| ((long)alphabet.indexOf(in[offset+6]) << 5)
						| ((long)alphabet.indexOf(in[offset+7]));
				out.writeInt((int)(chunk >> 8));
				out.write((int)(chunk & 0xFF));
			}
			int offset=in.length-in.length%8;
			switch(in.length%8){
				case 2 -> out.write((alphabet.indexOf(in[offset]) << 3) | (alphabet.indexOf(in[offset+1]) >> 2));
				case 4 -> out.writeShort((alphabet.indexOf(in[offset]) << 11) | (alphabet.indexOf(in[offset+1]) << 6) | (alphabet.indexOf(in[offset+2]) << 1) | (alphabet.indexOf(in[offset+3]) >> 4));
				case 5 -> {
					int chunk=(alphabet.indexOf(in[offset]) << 19)
							| (alphabet.indexOf(in[offset+1]) << 14)
							| (alphabet.indexOf(in[offset+2]) << 9)
							| (alphabet.indexOf(in[offset+3]) << 4)
							| (alphabet.indexOf(in[offset+4]) >> 1);
					out.writeShort(chunk >> 8);
					out.write(chunk);
				}
				case 7 -> {
					int chunk=(alphabet.indexOf(in[offset]) << 27)
							| (alphabet.indexOf(in[offset+1]) << 22)
							| (alphabet.indexOf(in[offset+2]) << 17)
							| (alphabet.indexOf(in[offset+3]) << 12)
							| (alphabet.indexOf(in[offset+4]) << 7)
							| (alphabet.indexOf(in[offset+5]) << 2)
							| (alphabet.indexOf(in[offset+6]) >> 3);
					out.writeInt(chunk);
				}
			}
			return buf.toByteArray();
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}
}
