package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeNumberTests {
	@Test
	@DisplayName("basic integer")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[42,[]]"));
		assertEquals("42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("zero integer")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[0,[]]"));
		assertEquals("0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("negative zero")
	public void test2() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[0,[]]"));
		assertEquals("0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("negative integer")
	public void test4() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-42,[]]"));
		assertEquals("-42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading 0 integer")
	public void test5() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[42,[]]"));
		assertEquals("42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading 0 negative integer")
	public void test6() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-42,[]]"));
		assertEquals("-42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("leading 0 zero")
	public void test7() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[0,[]]"));
		assertEquals("0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("long integer")
	public void test12() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[123456789012345,[]]"));
		assertEquals("123456789012345", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("long negative integer")
	public void test13() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-123456789012345,[]]"));
		assertEquals("-123456789012345", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("simple decimal")
	public void test16() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1.23,[]]"));
		assertEquals("1.23", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("negative decimal")
	public void test17() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-1.23,[]]"));
		assertEquals("-1.23", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("tricky precision decimal")
	public void test21() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[123456789012.1,[]]"));
		assertEquals("123456789012.1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("decimal with three fractional digits")
	public void test24() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1.123,[]]"));
		assertEquals("1.123", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("negative decimal with three fractional digits")
	public void test25() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[-1.123,[]]"));
		assertEquals("-1.123", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("decimal with 1 significant digit and 1 insignificant digit")
	public void test30() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1.2,[]]"));
		assertEquals("1.2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("decimal with 1 significant digit and 2 insignificant digits")
	public void test31() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1.2,[]]"));
		assertEquals("1.2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("decimal with 2 significant digits and 1 insignificant digit")
	public void test32() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1.23,[]]"));
		assertEquals("1.23", StructuredHttpHeaders.serialize(source));
	}
}
