package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeTokenTests {
	@Test
	@DisplayName("basic token - item")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a_b-c.d3:f%00/*\"},[]]"));
		assertEquals("a_b-c.d3:f%00/*", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("token with capitals - item")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"fooBar\"},[]]"));
		assertEquals("fooBar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("token starting with capitals - item")
	public void test2() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"FooBar\"},[]]"));
		assertEquals("FooBar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("basic token - list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"a_b-c3/*\"},[]]]"));
		assertEquals("a_b-c3/*", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("token with capitals - list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"fooBar\"},[]]]"));
		assertEquals("fooBar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("token starting with capitals - list")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"FooBar\"},[]]]"));
		assertEquals("FooBar", StructuredHttpHeaders.serialize(source));
	}
}
