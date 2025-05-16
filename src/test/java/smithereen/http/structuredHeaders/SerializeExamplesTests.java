package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.String;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeExamplesTests {
	@Test
	@DisplayName("Foo-Example")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[2,[[\"foourl\",\"https://foo.example.com/\"]]]"));
		assertEquals("2;foourl=\"https://foo.example.com/\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-StrListHeader")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[\"foo\",[]],[\"bar\",[]],[\"It was the best of times.\",[]]]"));
		assertEquals("\"foo\", \"bar\", \"It was the best of times.\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-Hdr (list on one line)")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[]],[{\"__type\":\"token\",\"value\":\"bar\"},[]]]"));
		assertEquals("foo, bar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-Hdr (list on two lines)")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[]],[{\"__type\":\"token\",\"value\":\"bar\"},[]]]"));
		assertEquals("foo, bar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-StrListListHeader")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[\"foo\",[]],[\"bar\",[]]],[]],[[[\"baz\",[]]],[]],[[[\"bat\",[]],[\"one\",[]]],[]],[[],[]]]"));
		assertEquals("(\"foo\" \"bar\"), (\"baz\"), (\"bat\" \"one\"), ()", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-ListListParam")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[\"foo\",[[\"a\",1],[\"b\",2]]]],[[\"lvl\",5]]],[[[\"bar\",[]],[\"baz\",[]]],[[\"lvl\",1]]]]"));
		assertEquals("(\"foo\";a=1;b=2);lvl=5, (\"bar\" \"baz\");lvl=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-ParamListHeader")
	public void test6() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"abc\"},[[\"a\",1],[\"b\",2],[\"cde_456\",true]]],[[[{\"__type\":\"token\",\"value\":\"ghi\"},[[\"jk\",4]]],[{\"__type\":\"token\",\"value\":\"l\"},[]]],[[\"q\",\"9\"],[\"r\",{\"__type\":\"token\",\"value\":\"w\"}]]]]"));
		assertEquals("abc;a=1;b=2;cde_456, (ghi;jk=4 l);q=\"9\";r=w", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-IntHeader")
	public void test7() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[1,[[\"a\",true],[\"b\",false]]]"));
		assertEquals("1;a;b=?0", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-DictHeader")
	public void test8() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"en\",[\"Applepie\",[]]],[\"da\",[{\"__type\":\"binary\",\"value\":\"YODGE3DFOTB2M4TUMU======\"},[]]]]"));
		assertEquals("en=\"Applepie\", da=:w4ZibGV0w6ZydGU=:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-DictHeader (boolean values)")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[false,[]]],[\"b\",[true,[]]],[\"c\",[true,[[\"foo\",{\"__type\":\"token\",\"value\":\"bar\"}]]]]]"));
		assertEquals("a=?0, b, c;foo=bar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-DictListHeader")
	public void test10() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"rating\",[1.5,[]]],[\"feelings\",[[[{\"__type\":\"token\",\"value\":\"joy\"},[]],[{\"__type\":\"token\",\"value\":\"sadness\"},[]]],[]]]]"));
		assertEquals("rating=1.5, feelings=(joy sadness)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-MixDict")
	public void test11() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[]]],[\"b\",[3,[]]],[\"c\",[4,[[\"aa\",{\"__type\":\"token\",\"value\":\"bb\"}]]]],[\"d\",[[[5,[]],[6,[]]],[[\"valid\",true]]]]]"));
		assertEquals("a=(1 2), b=3, c=4;aa=bb, d=(5 6);valid", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-Hdr (dictionary on one line)")
	public void test12() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"foo\",[1,[]]],[\"bar\",[2,[]]]]"));
		assertEquals("foo=1, bar=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-Hdr (dictionary on two lines)")
	public void test13() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"foo\",[1,[]]],[\"bar\",[2,[]]]]"));
		assertEquals("foo=1, bar=2", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-IntItemHeader")
	public void test14() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[5,[]]"));
		assertEquals("5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-IntItemHeader (params)")
	public void test15() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[5,[[\"foo\",{\"__type\":\"token\",\"value\":\"bar\"}]]]"));
		assertEquals("5;foo=bar", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-IntegerHeader")
	public void test16() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[42,[]]"));
		assertEquals("42", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-FloatHeader")
	public void test17() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[4.5,[]]"));
		assertEquals("4.5", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-StringHeader")
	public void test18() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"hello world\",[]]"));
		assertEquals("\"hello world\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-BinaryHdr")
	public void test19() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"OBZGK5DFNZSCA5DINFZSA2LTEBRGS3TBOJ4SAY3PNZ2GK3TUFY======\"},[]]"));
		assertEquals(":cHJldGVuZCB0aGlzIGlzIGJpbmFyeSBjb250ZW50Lg==:", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Example-BoolHdr")
	public void test20() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[true,[]]"));
		assertEquals("?1", StructuredHttpHeaders.serialize(source));
	}
}
