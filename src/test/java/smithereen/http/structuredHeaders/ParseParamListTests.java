package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseParamListTests {
	@Test
	@DisplayName("basic parameterised list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("abc_123;a=1;b=2; cdef_456, ghi;q=9;r=\"+w\"");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2],[\"cdef_456\",true]]],[{\"__type\":\"token\",\"value\":\"ghi\"},[[\"q\",9],[\"r\",\"+w\"]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("single item parameterised list")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html;q=1.0");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"q\",1.0]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("missing parameter value parameterised list")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html;a;q=1.0");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"a\",true],[\"q\",1.0]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("missing terminal parameter value parameterised list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html;q=1.0;a");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"q\",1.0],[\"a\",true]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("no whitespace parameterised list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html,text/plain;q=0.5");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("whitespace before = parameterised list")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html, text/plain;q =0.5");
		} );
	}

	@Test
	@DisplayName("whitespace after = parameterised list")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html, text/plain;q= 0.5");
		} );
	}

	@Test
	@DisplayName("whitespace before ; parameterised list")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html, text/plain ;q=0.5");
		} );
	}

	@Test
	@DisplayName("whitespace after ; parameterised list")
	public void test8() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html, text/plain; q=0.5");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("extra whitespace parameterised list")
	public void test9() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html  ,  text/plain;  q=0.5;  charset=utf-8");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5],[\"charset\",{\"__type\":\"token\",\"value\":\"utf-8\"}]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("two lines parameterised list")
	public void test10() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html,text/plain;q=0.5");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("trailing comma parameterised list")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html,text/plain;q=0.5,");
		} );
	}

	@Test
	@DisplayName("empty item parameterised list")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("text/html,,text/plain;q=0.5,");
		} );
	}
}
