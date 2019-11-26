package smithereen.jsonld;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils{
	public static void assertEqualJLD(Object a, Object b){
		if(a==null && b==null)
			return;
		assertNotNull(a);
		assertNotNull(b);
		assertTrue(a.getClass().isInstance(b), a+" and "+b+" are different types");
		if(a instanceof JSONObject){
			JSONObject ja=(JSONObject) a, jb=(JSONObject) b;
			assertEquals(ja.length(), jb.length(), "Different object lengths: "+ja+" vs "+jb);
			for(String key:ja.keySet()){
				assertTrue(jb.has(key), "Keys do not match: "+ja+" vs "+jb);
				assertEqualJLD(ja.get(key), jb.get(key));
			}
		}else if(a instanceof JSONArray){
			JSONArray ja=(JSONArray) a, jb=(JSONArray) b;
			assertEquals(ja.length(), jb.length(), "Different array lengths: "+ja+" vs "+jb);
			for(int i=0;i<ja.length();i++){
				assertEqualJLD(ja.get(i), jb.get(i));
			}
		}else{
			assertEquals(a, b);
		}
	}

	public static Object readResourceAsJSON(String name){
		InputStream in=TestUtils.class.getResourceAsStream(name);
		try{
			byte[] buf=new byte[in.available()];
			in.read(buf);
			in.close();
			String s=new String(buf, StandardCharsets.UTF_8);
			if(s.charAt(0)=='[')
				return new JSONArray(s);
			return new JSONObject(s);
		}catch(IOException ignore){}
		return null;
	}

	public static List<String> readResourceAsLines(String name){
		InputStream in=TestUtils.class.getResourceAsStream(name);
		try{
			byte[] buf=new byte[in.available()];
			in.read(buf);
			in.close();
			String s=new String(buf, StandardCharsets.UTF_8);
			return s.isEmpty() ? Collections.EMPTY_LIST : Arrays.asList(s.split("\n"));
		}catch(IOException ignore){}
		return null;
	}

	public static List<String> eachToString(List<?> list){
		ArrayList<String> result=new ArrayList<>();
		for(Object o:list)
			result.add(o.toString());
		return result;
	}

	public static List<RDFTriple> parseRDF(List<String> lines){
		ArrayList<RDFTriple> result=new ArrayList<>();
		for(String s:lines)
			result.add(RDFTriple.parse(s));
		return result;
	}

	public static JSONArray readResourceAsArray(String name){
		Object r=readResourceAsJSON(name);
		return r instanceof JSONArray ? (JSONArray)r : new JSONArray(Collections.singletonList(r));
	}
}
