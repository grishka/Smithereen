package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.String;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeParamDictTests {
	@Test
	@DisplayName("basic parameterised dict")
	public void test0() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"abc\",[123,[[\"a\",1],[\"b\",2]]]],[\"def\",[456,[]]],[\"ghi\",[789,[[\"q\",9],[\"r\",\"+w\"]]]]]"));
		assertEquals("abc=123;a=1;b=2, def=456, ghi=789;q=9;r=\"+w\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single item parameterised dict")
	public void test1() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"q\",1.0]]]]]"));
		assertEquals("a=b;q=1.0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("list item parameterised dictionary")
	public void test2() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[[\"q\",1.0]]]]]"));
		assertEquals("a=(1 2);q=1.0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("missing parameter value parameterised dict")
	public void test3() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[3,[[\"c\",true],[\"d\",5]]]]]"));
		assertEquals("a=3;c;d=5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("terminal missing parameter value parameterised dict")
	public void test4() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[3,[[\"c\",5],[\"d\",true]]]]]"));
		assertEquals("a=3;c=5;d", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("no whitespace parameterised dict")
	public void test5() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2]]]]]"));
		assertEquals("a=b;c=1, d=e;f=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("whitespace after ; parameterised dict")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"q\",0.5]]]]]"));
		assertEquals("a=b;q=0.5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("extra whitespace parameterised dict")
	public void test10() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2],[\"g\",3]]]]]"));
		assertEquals("a=b;c=1, d=e;f=2;g=3", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("two lines parameterised list")
	public void test11() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[{\"__type\":\"token\",\"value\":\"b\"},[[\"c\",1]]]],[\"d\",[{\"__type\":\"token\",\"value\":\"e\"},[[\"f\",2]]]]]"));
		assertEquals("a=b;c=1, d=e;f=2", StructuredHttpHeaders.serialize(source));
	}
}
