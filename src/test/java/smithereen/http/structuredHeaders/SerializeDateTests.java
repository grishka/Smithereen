package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeDateTests {
	@Test
	@DisplayName("date - 1970-01-01 00:00:00")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"date\",\"value\":0},[]]"));
		assertEquals("@0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("date - 2022-08-04 01:57:13")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"date\",\"value\":1659578233},[]]"));
		assertEquals("@1659578233", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("date - 1917-05-30 22:02:47")
	public void test2() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"date\",\"value\":-1659578233},[]]"));
		assertEquals("@-1659578233", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("date - 2^31")
	public void test3() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"date\",\"value\":2147483648},[]]"));
		assertEquals("@2147483648", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("date - 2^32")
	public void test4() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"date\",\"value\":4294967296},[]]"));
		assertEquals("@4294967296", StructuredHttpHeaders.serialize(source));
	}
}
