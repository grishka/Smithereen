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

public class ParseDictionaryTests {
	@Test
	@DisplayName("basic dictionary")
	public void test0() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("en=\"Applepie\", da=:w4ZibGV0w6ZydGUK:");
		assertEquals(JsonParser.parseString("[[\"en\",[\"Applepie\",[]]],[\"da\",[{\"__type\":\"binary\",\"value\":\"YODGE3DFOTB2M4TUMUFA====\"},[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty dictionary")
	public void test1() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("");
		assertEquals(JsonParser.parseString("[]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single item dictionary")
	public void test2() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("list item dictionary")
	public void test3() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=(1 2)");
		assertEquals(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single list item dictionary")
	public void test4() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=(1)");
		assertEquals(JsonParser.parseString("[[\"a\",[[[1,[]]],[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty list item dictionary")
	public void test5() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=()");
		assertEquals(JsonParser.parseString("[[\"a\",[[],[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("no whitespace dictionary")
	public void test6() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,b=2");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("extra whitespace dictionary")
	public void test7() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1 ,  b=2");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("tab separated dictionary")
	public void test8() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1\t,\tb=2");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading whitespace dictionary")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("     a=1 ,  b=2");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("whitespace before = dictionary")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a =1, b=2");
		} );
	}

	@Test
	@DisplayName("whitespace after = dictionary")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b= 2");
		} );
	}

	@Test
	@DisplayName("two lines dictionary")
	public void test12() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,b=2");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("missing value dictionary")
	public void test13() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b, c=3");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[]]],[\"c\",[3,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("all missing value dictionary")
	public void test14() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a, b, c");
		assertEquals(JsonParser.parseString("[[\"a\",[true,[]]],[\"b\",[true,[]]],[\"c\",[true,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("start missing value dictionary")
	public void test15() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a, b=2");
		assertEquals(JsonParser.parseString("[[\"a\",[true,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("end missing value dictionary")
	public void test16() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("missing value with params dictionary")
	public void test17() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b;foo=9, c=3");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[[\"foo\",9]]]],[\"c\",[3,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("explicit true value with params dictionary")
	public void test18() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b=?1;foo=9, c=3");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[[\"foo\",9]]]],[\"c\",[3,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("trailing comma dictionary")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1, b=2,");
		} );
	}

	@Test
	@DisplayName("empty item dictionary")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,,b=2,");
		} );
	}

	@Test
	@DisplayName("duplicate key dictionary")
	public void test21() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,b=2,a=3");
		assertEquals(JsonParser.parseString("[[\"a\",[3,[]]],[\"b\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("numeric key dictionary")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,1b=2,a=1");
		} );
	}

	@Test
	@DisplayName("uppercase key dictionary")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,B=2,a=1");
		} );
	}

	@Test
	@DisplayName("bad key dictionary")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1,b!=2,a=1");
		} );
	}
}
