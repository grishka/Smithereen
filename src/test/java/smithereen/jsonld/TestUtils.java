package smithereen.jsonld;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtils{
	public static @NotNull JsonElement readResourceAsJSON(String name){
		try(InputStream in=TestUtils.class.getResourceAsStream(name)){
			return JsonParser.parseReader(new InputStreamReader(in));
		}catch(IOException ignore){}
		throw new IllegalStateException("Failed to load resource: "+name);
	}

	public static List<String> readResourceAsLines(String name){
		try(InputStream in=TestUtils.class.getResourceAsStream(name)){
			byte[] buf=new byte[in.available()];
			in.read(buf);
			in.close();
			String s=new String(buf, StandardCharsets.UTF_8);
			return s.isEmpty() ? Collections.EMPTY_LIST : Arrays.asList(s.split("\n"));
		}catch(IOException ignore){}
		return null;
	}

	public static List<String> eachToString(List<?> list){
		return list.stream().map(Object::toString).collect(Collectors.toList());
	}

	public static List<RDFTriple> parseRDF(List<String> lines){
		return lines.stream().map(RDFTriple::parse).collect(Collectors.toList());
	}
}
