package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseParamListlistTests {
	@Test
	@DisplayName("parameterised inner list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(abc_123);a=1;b=2, cdef_456");
		assertEquals(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[]]],[[\"a\",1],[\"b\",2]]],[{\"__type\":\"token\",\"value\":\"cdef_456\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("parameterised inner list item")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(abc_123;a=1;b=2;cdef_456)");
		assertEquals(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2],[\"cdef_456\",true]]]],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("parameterised inner list with parameterised item")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(abc_123;a=1;b=2);cdef_456");
		assertEquals(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2]]]],[[\"cdef_456\",true]]]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
