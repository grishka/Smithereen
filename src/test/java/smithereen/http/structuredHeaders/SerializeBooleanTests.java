package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeBooleanTests {
	@Test
	@DisplayName("basic true boolean")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[true,[]]"));
		assertEquals("?1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("basic false boolean")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[false,[]]"));
		assertEquals("?0", StructuredHttpHeaders.serialize(source));
	}
}
