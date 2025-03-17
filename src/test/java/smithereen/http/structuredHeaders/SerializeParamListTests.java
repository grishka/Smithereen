package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeParamListTests {
	@Test
	@DisplayName("basic parameterised list")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"abc_123\"},[[\"a\",1],[\"b\",2],[\"cdef_456\",true]]],[{\"__type\":\"token\",\"value\":\"ghi\"},[[\"q\",9],[\"r\",\"+w\"]]]]"));
		assertEquals("abc_123;a=1;b=2;cdef_456, ghi;q=9;r=\"+w\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single item parameterised list")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"q\",1.0]]]]"));
		assertEquals("text/html;q=1.0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("missing parameter value parameterised list")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"a\",true],[\"q\",1.0]]]]"));
		assertEquals("text/html;a;q=1.0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("missing terminal parameter value parameterised list")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[[\"q\",1.0],[\"a\",true]]]]"));
		assertEquals("text/html;q=1.0;a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("no whitespace parameterised list")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"));
		assertEquals("text/html, text/plain;q=0.5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("whitespace after ; parameterised list")
	public void test8() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"));
		assertEquals("text/html, text/plain;q=0.5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("extra whitespace parameterised list")
	public void test9() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5],[\"charset\",{\"__type\":\"token\",\"value\":\"utf-8\"}]]]]"));
		assertEquals("text/html, text/plain;q=0.5;charset=utf-8", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("two lines parameterised list")
	public void test10() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"text/html\"},[]],[{\"__type\":\"token\",\"value\":\"text/plain\"},[[\"q\",0.5]]]]"));
		assertEquals("text/html, text/plain;q=0.5", StructuredHttpHeaders.serialize(source));
	}
}
