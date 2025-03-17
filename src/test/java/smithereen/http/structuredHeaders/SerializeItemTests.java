package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeItemTests {
	@Test
	@DisplayName("leading and trailing space")
	public void test3() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1,[]]"));
		assertEquals("1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading and trailing whitespace")
	public void test4() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1,[]]"));
		assertEquals("1", StructuredHttpHeaders.serialize(source));
	}
}
