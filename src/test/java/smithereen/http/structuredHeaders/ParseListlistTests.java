package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseListlistTests {
	@Test
	@DisplayName("basic list of lists")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(1 2), (42 43)");
		assertEquals(JsonParser.parseString("[[[[1,[]],[2,[]]],[]],[[[42,[]],[43,[]]],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single item list of lists")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(42)");
		assertEquals(JsonParser.parseString("[[[[42,[]]],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty item list of lists")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("()");
		assertEquals(JsonParser.parseString("[[[],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("empty middle item list of lists")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(1),(),(42)");
		assertEquals(JsonParser.parseString("[[[[1,[]]],[]],[[],[]],[[[42,[]]],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("extra whitespace list of lists")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(  1  42  )");
		assertEquals(JsonParser.parseString("[[[[1,[]],[42,[]]],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("wrong whitespace list of lists")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(1\t 42)");
		} );
	}

	@Test
	@DisplayName("no trailing parenthesis list of lists")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(1 42");
		} );
	}

	@Test
	@DisplayName("no trailing parenthesis middle list of lists")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(1 2, (42 43)");
		} );
	}

	@Test
	@DisplayName("no spaces in inner-list")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(abc\"def\"?0123*dXZ3*xyz)");
		} );
	}

	@Test
	@DisplayName("no closing parenthesis")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(");
		} );
	}
}
