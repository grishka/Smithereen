package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseNumberTests {
	@Test
	@DisplayName("basic integer")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("42");
		assertEquals(JsonParser.parseString("[42,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("zero integer")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("negative zero")
	public void test2() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-0");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("double negative zero")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("--0");
		} );
	}

	@Test
	@DisplayName("negative integer")
	public void test4() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-42");
		assertEquals(JsonParser.parseString("[-42,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading 0 integer")
	public void test5() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("042");
		assertEquals(JsonParser.parseString("[42,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading 0 negative integer")
	public void test6() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-042");
		assertEquals(JsonParser.parseString("[-42,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading 0 zero")
	public void test7() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("comma")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("2,3");
		} );
	}

	@Test
	@DisplayName("negative non-DIGIT first character")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-a23");
		} );
	}

	@Test
	@DisplayName("sign out of place")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("4-2");
		} );
	}

	@Test
	@DisplayName("whitespace after sign")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("- 42");
		} );
	}

	@Test
	@DisplayName("long integer")
	public void test12() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("123456789012345");
		assertEquals(JsonParser.parseString("[123456789012345,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("long negative integer")
	public void test13() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-123456789012345");
		assertEquals(JsonParser.parseString("[-123456789012345,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("too long integer")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1234567890123456");
		} );
	}

	@Test
	@DisplayName("negative too long integer")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-1234567890123456");
		} );
	}

	@Test
	@DisplayName("simple decimal")
	public void test16() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.23");
		assertEquals(JsonParser.parseString("[1.23,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("negative decimal")
	public void test17() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-1.23");
		assertEquals(JsonParser.parseString("[-1.23,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("decimal, whitespace after decimal")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1. 23");
		} );
	}

	@Test
	@DisplayName("decimal, whitespace before decimal")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1 .23");
		} );
	}

	@Test
	@DisplayName("negative decimal, whitespace after sign")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("- 1.23");
		} );
	}

	@Test
	@DisplayName("tricky precision decimal")
	public void test21() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("123456789012.1");
		assertEquals(JsonParser.parseString("[123456789012.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("double decimal decimal")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.5.4");
		} );
	}

	@Test
	@DisplayName("adjacent double decimal decimal")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1..4");
		} );
	}

	@Test
	@DisplayName("decimal with three fractional digits")
	public void test24() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.123");
		assertEquals(JsonParser.parseString("[1.123,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("negative decimal with three fractional digits")
	public void test25() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-1.123");
		assertEquals(JsonParser.parseString("[-1.123,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("decimal with four fractional digits")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.1234");
		} );
	}

	@Test
	@DisplayName("negative decimal with four fractional digits")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-1.1234");
		} );
	}

	@Test
	@DisplayName("decimal with thirteen integer digits")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1234567890123.0");
		} );
	}

	@Test
	@DisplayName("negative decimal with thirteen integer digits")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-1234567890123.0");
		} );
	}

	@Test
	@DisplayName("decimal with 1 significant digit and 1 insignificant digit")
	public void test30() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.20");
		assertEquals(JsonParser.parseString("[1.2,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("decimal with 1 significant digit and 2 insignificant digits")
	public void test31() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.200");
		assertEquals(JsonParser.parseString("[1.2,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("decimal with 2 significant digits and 1 insignificant digit")
	public void test32() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.230");
		assertEquals(JsonParser.parseString("[1.23,[]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
