package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseListTests {
	@Test
	@DisplayName("basic list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1, 42");
		assertEquals(JsonParser.parseString("[[1,[]],[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty list")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("");
		assertEquals(JsonParser.parseString("[]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading SP list")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("  42, 43");
		assertEquals(JsonParser.parseString("[[42,[]],[43,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single item list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("42");
		assertEquals(JsonParser.parseString("[[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("no whitespace list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1,42");
		assertEquals(JsonParser.parseString("[[1,[]],[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("extra whitespace list")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1 , 42");
		assertEquals(JsonParser.parseString("[[1,[]],[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("tab separated list")
	public void test6() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1\t,\t42");
		assertEquals(JsonParser.parseString("[[1,[]],[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("two line list")
	public void test7() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1,42");
		assertEquals(JsonParser.parseString("[[1,[]],[42,[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("trailing comma list")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1, 42,");
		} );
	}

	@Test
	@DisplayName("empty item list")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1,,42");
		} );
	}

	@Test
	@DisplayName("empty item list (multiple field lines)")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("1,,42");
		} );
	}
}
