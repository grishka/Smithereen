package smithereen.jsonld;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.jupiter.api.Assertions.*;

public class JLDUtilities{
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
}
