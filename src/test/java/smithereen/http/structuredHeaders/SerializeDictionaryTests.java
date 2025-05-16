package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.String;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeDictionaryTests {
	@Test
	@DisplayName("basic dictionary")
	public void test0() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"en\",[\"Applepie\",[]]],[\"da\",[{\"__type\":\"binary\",\"value\":\"YODGE3DFOTB2M4TUMUFA====\"},[]]]]"));
		assertEquals("en=\"Applepie\", da=:w4ZibGV0w6ZydGUK:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty dictionary")
	public void test1() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[]"));
		assertEquals("", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single item dictionary")
	public void test2() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]]]"));
		assertEquals("a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("list item dictionary")
	public void test3() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[]]]]"));
		assertEquals("a=(1 2)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single list item dictionary")
	public void test4() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[[[1,[]]],[]]]]"));
		assertEquals("a=(1)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty list item dictionary")
	public void test5() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[[],[]]]]"));
		assertEquals("a=()", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("no whitespace dictionary")
	public void test6() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=1, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("extra whitespace dictionary")
	public void test7() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=1, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("tab separated dictionary")
	public void test8() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=1, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading whitespace dictionary")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=1, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("two lines dictionary")
	public void test12() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=1, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("missing value dictionary")
	public void test13() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[]]],[\"c\",[3,[]]]]"));
		assertEquals("a=1, b, c=3", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("all missing value dictionary")
	public void test14() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[true,[]]],[\"b\",[true,[]]],[\"c\",[true,[]]]]"));
		assertEquals("a, b, c", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("start missing value dictionary")
	public void test15() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[true,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a, b=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("end missing value dictionary")
	public void test16() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[]]]]"));
		assertEquals("a=1, b", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("missing value with params dictionary")
	public void test17() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[[\"foo\",9]]]],[\"c\",[3,[]]]]"));
		assertEquals("a=1, b;foo=9, c=3", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("explicit true value with params dictionary")
	public void test18() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]],[\"b\",[true,[[\"foo\",9]]]],[\"c\",[3,[]]]]"));
		assertEquals("a=1, b;foo=9, c=3", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("duplicate key dictionary")
	public void test21() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[3,[]]],[\"b\",[2,[]]]]"));
		assertEquals("a=3, b=2", StructuredHttpHeaders.serialize(source));
	}
}
