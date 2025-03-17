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

public class ParseExamplesTests {
	@Test
	@DisplayName("Foo-Example")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("2; foourl=\"https://foo.example.com/\"");
		assertEquals(JsonParser.parseString("[2,[[\"foourl\",\"https://foo.example.com/\"]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-StrListHeader")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("\"foo\", \"bar\", \"It was the best of times.\"");
		assertEquals(JsonParser.parseString("[[\"foo\",[]],[\"bar\",[]],[\"It was the best of times.\",[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-Hdr (list on one line)")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo, bar");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[]],[{\"__type\":\"token\",\"value\":\"bar\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-Hdr (list on two lines)")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo,bar");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[]],[{\"__type\":\"token\",\"value\":\"bar\"},[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-StrListListHeader")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(\"foo\" \"bar\"), (\"baz\"), (\"bat\" \"one\"), ()");
		assertEquals(JsonParser.parseString("[[[[\"foo\",[]],[\"bar\",[]]],[]],[[[\"baz\",[]]],[]],[[[\"bat\",[]],[\"one\",[]]],[]],[[],[]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-ListListParam")
	public void test5() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("(\"foo\"; a=1;b=2);lvl=5, (\"bar\" \"baz\");lvl=1");
		assertEquals(JsonParser.parseString("[[[[\"foo\",[[\"a\",1],[\"b\",2]]]],[[\"lvl\",5]]],[[[\"bar\",[]],[\"baz\",[]]],[[\"lvl\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-ParamListHeader")
	public void test6() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("abc;a=1;b=2; cde_456, (ghi;jk=4 l);q=\"9\";r=w");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"abc\"},[[\"a\",1],[\"b\",2],[\"cde_456\",true]]],[[[{\"__type\":\"token\",\"value\":\"ghi\"},[[\"jk\",4]]],[{\"__type\":\"token\",\"value\":\"l\"},[]]],[[\"q\",\"9\"],[\"r\",{\"__type\":\"token\",\"value\":\"w\"}]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-IntHeader")
	public void test7() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1; a; b=?0");
		assertEquals(JsonParser.parseString("[1,[[\"a\",true],[\"b\",false]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-DictHeader")
	public void test8() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("en=\"Applepie\", da=:w4ZibGV0w6ZydGU=:");
		assertEquals(JsonParser.parseString("[[\"en\",[\"Applepie\",[]]],[\"da\",[{\"__type\":\"binary\",\"value\":\"YODGE3DFOTB2M4TUMU======\"},[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-DictHeader (boolean values)")
	public void test9() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=?0, b, c; foo=bar");
		assertEquals(JsonParser.parseString("[[\"a\",[false,[]]],[\"b\",[true,[]]],[\"c\",[true,[[\"foo\",{\"__type\":\"token\",\"value\":\"bar\"}]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-DictListHeader")
	public void test10() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("rating=1.5, feelings=(joy sadness)");
		assertEquals(JsonParser.parseString("[[\"rating\",[1.5,[]]],[\"feelings\",[[[{\"__type\":\"token\",\"value\":\"joy\"},[]],[{\"__type\":\"token\",\"value\":\"sadness\"},[]]],[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-MixDict")
	public void test11() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=(1 2), b=3, c=4;aa=bb, d=(5 6);valid");
		assertEquals(JsonParser.parseString("[[\"a\",[[[1,[]],[2,[]]],[]]],[\"b\",[3,[]]],[\"c\",[4,[[\"aa\",{\"__type\":\"token\",\"value\":\"bb\"}]]]],[\"d\",[[[5,[]],[6,[]]],[[\"valid\",true]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-Hdr (dictionary on one line)")
	public void test12() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("foo=1, bar=2");
		assertEquals(JsonParser.parseString("[[\"foo\",[1,[]]],[\"bar\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-Hdr (dictionary on two lines)")
	public void test13() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("foo=1,bar=2");
		assertEquals(JsonParser.parseString("[[\"foo\",[1,[]]],[\"bar\",[2,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-IntItemHeader")
	public void test14() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("5");
		assertEquals(JsonParser.parseString("[5,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-IntItemHeader (params)")
	public void test15() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("5; foo=bar");
		assertEquals(JsonParser.parseString("[5,[[\"foo\",{\"__type\":\"token\",\"value\":\"bar\"}]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-IntegerHeader")
	public void test16() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("42");
		assertEquals(JsonParser.parseString("[42,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-FloatHeader")
	public void test17() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("4.5");
		assertEquals(JsonParser.parseString("[4.5,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-StringHeader")
	public void test18() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"hello world\"");
		assertEquals(JsonParser.parseString("[\"hello world\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-BinaryHdr")
	public void test19() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":cHJldGVuZCB0aGlzIGlzIGJpbmFyeSBjb250ZW50Lg==:");
		assertEquals(JsonParser.parseString("[{\"__type\":\"binary\",\"value\":\"OBZGK5DFNZSCA5DINFZSA2LTEBRGS3TBOJ4SAY3PNZ2GK3TUFY======\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Example-BoolHdr")
	public void test20() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?1");
		assertEquals(JsonParser.parseString("[true,[]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
