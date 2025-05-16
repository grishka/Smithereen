package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeParamListlistTests {
	@Test
	@DisplayName("parameterised inner list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[]]],[[\"a\",1],[\"b\",2]]],[{\"__type\":\"token\",\"value\":\"cdef_456\"},[]]]"));
		assertEquals("(abc_123);a=1;b=2, cdef_456", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("parameterised inner list item")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2],[\"cdef_456\",true]]]],[]]]"));
		assertEquals("(abc_123;a=1;b=2;cdef_456)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("parameterised inner list with parameterised item")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2]]]],[[\"cdef_456\",true]]]]"));
		assertEquals("(abc_123;a=1;b=2);cdef_456", StructuredHttpHeaders.serialize(source));
	}
}
