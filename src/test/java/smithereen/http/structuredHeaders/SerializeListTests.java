package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeListTests {
	@Test
	@DisplayName("basic list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[1,[]],[42,[]]]"));
		assertEquals("1, 42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty list")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[]"));
		assertEquals("", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading SP list")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[42,[]],[43,[]]]"));
		assertEquals("42, 43", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single item list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[42,[]]]"));
		assertEquals("42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("no whitespace list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[1,[]],[42,[]]]"));
		assertEquals("1, 42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("extra whitespace list")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[1,[]],[42,[]]]"));
		assertEquals("1, 42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("tab separated list")
	public void test6() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[1,[]],[42,[]]]"));
		assertEquals("1, 42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("two line list")
	public void test7() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[1,[]],[42,[]]]"));
		assertEquals("1, 42", StructuredHttpHeaders.serialize(source));
	}
}
