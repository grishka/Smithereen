package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeStringTests {
	@Test
	@DisplayName("basic string")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"foo bar\",[]]"));
		assertEquals("\"foo bar\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty string")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\",[]]"));
		assertEquals("\"\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("long string")
	public void test2() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \",[]]"));
		assertEquals("\"foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("whitespace string")
	public void test3() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"   \",[]]"));
		assertEquals("\"   \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("string quoting")
	public void test9() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"foo \\\"bar\\\" \\\\ baz\",[]]"));
		assertEquals("\"foo \\\"bar\\\" \\\\ baz\"", StructuredHttpHeaders.serialize(source));
	}
}
