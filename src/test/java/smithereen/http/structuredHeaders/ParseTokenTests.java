package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseTokenTests {
	@Test
	@DisplayName("basic token - item")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a_b-c.d3:f%00/*");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a_b-c.d3:f%00/*\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("token with capitals - item")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("fooBar");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"fooBar\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("token starting with capitals - item")
	public void test2() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("FooBar");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"FooBar\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("basic token - list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("a_b-c3/*");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"a_b-c3/*\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("token with capitals - list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("fooBar");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"fooBar\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("token starting with capitals - list")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("FooBar");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"FooBar\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
