package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeBinaryTests {
	@Test
	@DisplayName("basic binary")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"NBSWY3DP\"},[]]"));
		assertEquals(":aGVsbG8=:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty binary")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"\"},[]]"));
		assertEquals("::", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("bad padding")
	public void test4() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"NBSWY3DP\"},[]]"));
		assertEquals(":aGVsbG8=:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("non-zero pad bits")
	public void test11() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"RE======\"},[]]"));
		assertEquals(":iQ==:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("non-ASCII binary")
	public void test12() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"77QCC===\"},[]]"));
		assertEquals(":/+Ah:", StructuredHttpHeaders.serialize(source));
	}
}
