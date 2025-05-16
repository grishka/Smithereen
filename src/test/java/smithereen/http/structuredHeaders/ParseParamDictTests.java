package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseParamDictTests {
	@Test
	@DisplayName("basic parameterised dict")
	public void test0() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("abc=123;a=1;b=2, def=456, ghi=789;q=9;r=\"+w\"");
		assertEquals(JsonParser.parseString("[[\"abc\",[123,[[\"a\",1],[\"b\",2]]]],[\"def\",[456,[]]],[\"ghi\",[789,[[\"q\",9],[\"r\",\"+w\"]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single item parameterised dict")
	public void test1() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b; q=1.0");
		assertEquals(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"q\",1.0]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("list item parameterised dictionary")
	public void test2() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=(1 2); q=1.0");
		assertEquals(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[[\"q\",1.0]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("missing parameter value parameterised dict")
	public void test3() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=3;c;d=5");
		assertEquals(JsonParser.parseString("[[\"a\",[3,[[\"c\",true],[\"d\",5]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("terminal missing parameter value parameterised dict")
	public void test4() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=3;c=5;d");
		assertEquals(JsonParser.parseString("[[\"a\",[3,[[\"c\",5],[\"d\",true]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("no whitespace parameterised dict")
	public void test5() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b;c=1,d=e;f=2");
		assertEquals(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("whitespace before = parameterised dict")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b;q =0.5");
		} );
	}

	@Test
	@DisplayName("whitespace after = parameterised dict")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b;q= 0.5");
		} );
	}

	@Test
	@DisplayName("whitespace before ; parameterised dict")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b ;q=0.5");
		} );
	}

	@Test
	@DisplayName("whitespace after ; parameterised dict")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b; q=0.5");
		assertEquals(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"q\",0.5]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("extra whitespace parameterised dict")
	public void test10() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b;  c=1  ,  d=e; f=2; g=3");
		assertEquals(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2],[\"g\",3]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("two lines parameterised list")
	public void test11() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b;c=1,d=e;f=2");
		assertEquals(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("trailing comma parameterised list")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b; q=1.0,");
		} );
	}

	@Test
	@DisplayName("empty item parameterised list")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=b; q=1.0,,c=d");
		} );
	}
}
