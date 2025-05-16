package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeOnlyNumberTests {
	@Test
	@DisplayName("too big positive integer - serialize")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1000000000000000,[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("too big negative integer - serialize")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-1000000000000000,[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("too big positive decimal - serialize")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1000000000000.1,[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("too big negative decimal - serialize")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-1000000000000.1,[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("round positive odd decimal - serialize")
	public void test4() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[0.0015,[]]"));
		assertEquals("0.002", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("round positive even decimal - serialize")
	public void test5() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[0.0025,[]]"));
		assertEquals("0.002", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("round negative odd decimal - serialize")
	public void test6() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-0.0015,[]]"));
		assertEquals("-0.002", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("round negative even decimal - serialize")
	public void test7() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-0.0025,[]]"));
		assertEquals("-0.002", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("decimal round up to integer part - serialize")
	public void test8() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[9.9995,[]]"));
		assertEquals("10.0", StructuredHttpHeaders.serialize(source));
	}
}
