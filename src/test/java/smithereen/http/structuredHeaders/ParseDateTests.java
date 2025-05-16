package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseDateTests {
	@Test
	@DisplayName("date - 1970-01-01 00:00:00")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@0");
		assertEquals(JsonParser.parseString("[{\"__type\":\"date\",\"value\":0},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("date - 2022-08-04 01:57:13")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@1659578233");
		assertEquals(JsonParser.parseString("[{\"__type\":\"date\",\"value\":1659578233},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("date - 1917-05-30 22:02:47")
	public void test2() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@-1659578233");
		assertEquals(JsonParser.parseString("[{\"__type\":\"date\",\"value\":-1659578233},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("date - 2^31")
	public void test3() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@2147483648");
		assertEquals(JsonParser.parseString("[{\"__type\":\"date\",\"value\":2147483648},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("date - 2^32")
	public void test4() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@4294967296");
		assertEquals(JsonParser.parseString("[{\"__type\":\"date\",\"value\":4294967296},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("date - decimal")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@1659578233.12");
		} );
	}
}
