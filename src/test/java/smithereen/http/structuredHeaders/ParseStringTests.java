package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseStringTests {
	@Test
	@DisplayName("basic string")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo bar\"");
		assertEquals(JsonParser.parseString("[\"foo bar\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty string")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\"");
		assertEquals(JsonParser.parseString("[\"\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("long string")
	public void test2() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \"");
		assertEquals(JsonParser.parseString("[\"foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("whitespace string")
	public void test3() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"   \"");
		assertEquals(JsonParser.parseString("[\"   \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("non-ascii string")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"füü\"");
		} );
	}

	@Test
	@DisplayName("tab in string")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\t\"");
		} );
	}

	@Test
	@DisplayName("newline in string")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \n"
							+ " \"");
		} );
	}

	@Test
	@DisplayName("single quoted string")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("'foo'");
		} );
	}

	@Test
	@DisplayName("unbalanced string")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo");
		} );
	}

	@Test
	@DisplayName("string quoting")
	public void test9() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo \\\"bar\\\" \\\\ baz\"");
		assertEquals(JsonParser.parseString("[\"foo \\\"bar\\\" \\\\ baz\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("bad string quoting")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo \\,\"");
		} );
	}

	@Test
	@DisplayName("ending string quote")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo \\\"");
		} );
	}

	@Test
	@DisplayName("abruptly ending string quote")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"foo \\");
		} );
	}
}
