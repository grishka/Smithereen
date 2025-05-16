package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseBinaryTests {
	@Test
	@DisplayName("basic binary")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG8=:");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"NBSWY3DP\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty binary")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("::");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("padding at beginning")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":=aGVsbG8=:");
		} );
	}

	@Test
	@DisplayName("padding in middle")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":a=GVsbG8=:");
		} );
	}

	@Test
	@DisplayName("bad padding")
	public void test4() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG8:");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"NBSWY3DP\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("bad padding dot")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG8.:");
		} );
	}

	@Test
	@DisplayName("bad end delimiter")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG8=");
		} );
	}

	@Test
	@DisplayName("extra whitespace")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsb G8=:");
		} );
	}

	@Test
	@DisplayName("all whitespace")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":    :");
		} );
	}

	@Test
	@DisplayName("extra chars")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG!8=:");
		} );
	}

	@Test
	@DisplayName("suffix chars")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":aGVsbG8=!:");
		} );
	}

	@Test
	@DisplayName("non-zero pad bits")
	public void test11() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":iZ==:");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"RE======\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("non-ASCII binary")
	public void test12() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":/+Ah:");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"77QCC===\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("base64url binary")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":_-Ah:");
		} );
	}
}
