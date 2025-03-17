package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseBooleanTests {
	@Test
	@DisplayName("basic true boolean")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?1");
		assertEquals(JsonParser.parseString("[true,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("basic false boolean")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?0");
		assertEquals(JsonParser.parseString("[false,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("unknown boolean")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?Q");
		} );
	}

	@Test
	@DisplayName("whitespace boolean")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("? 1");
		} );
	}

	@Test
	@DisplayName("negative zero boolean")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?-0");
		} );
	}

	@Test
	@DisplayName("T boolean")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?T");
		} );
	}

	@Test
	@DisplayName("F boolean")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?F");
		} );
	}

	@Test
	@DisplayName("t boolean")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?t");
		} );
	}

	@Test
	@DisplayName("f boolean")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?f");
		} );
	}

	@Test
	@DisplayName("spelled-out True boolean")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?True");
		} );
	}

	@Test
	@DisplayName("spelled-out False boolean")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?False");
		} );
	}
}
