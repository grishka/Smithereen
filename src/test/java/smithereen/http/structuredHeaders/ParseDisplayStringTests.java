package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseDisplayStringTests {
	@Test
	@DisplayName("basic display string (ascii content)")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"foo bar\"");
		assertEquals(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"foo bar\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("all printable ascii")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\" !%22#$%25&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\"");
		assertEquals(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\" !\\\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("non-ascii display string (uppercase escaping)")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"f%C3%BC%C3%BC\"");
		} );
	}

	@Test
	@DisplayName("non-ascii display string (lowercase escaping)")
	public void test3() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"f%c3%bc%c3%bc\"");
		assertEquals(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"füü\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("non-ascii display string (unescaped)")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"füü\"");
		} );
	}

	@Test
	@DisplayName("tab in display string")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"\t\"");
		} );
	}

	@Test
	@DisplayName("newline in display string")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"\n"
							+ "\"");
		} );
	}

	@Test
	@DisplayName("single quoted display string")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%'foo'");
		} );
	}

	@Test
	@DisplayName("unquoted display string")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%foo");
		} );
	}

	@Test
	@DisplayName("display string missing initial quote")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%foo\"");
		} );
	}

	@Test
	@DisplayName("unbalanced display string")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"foo");
		} );
	}

	@Test
	@DisplayName("display string quoting")
	public void test11() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"foo %22bar%22 \\ baz\"");
		assertEquals(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"foo \\\"bar\\\" \\\\ baz\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("bad display string escaping")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"foo %a");
		} );
	}

	@Test
	@DisplayName("bad display string utf-8 (invalid 2-byte seq)")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"%c3%28\"");
		} );
	}

	@Test
	@DisplayName("bad display string utf-8 (invalid sequence id)")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"%a0%a1\"");
		} );
	}

	@Test
	@DisplayName("bad display string utf-8 (invalid hex)")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"%g0%1w\"");
		} );
	}

	@Test
	@DisplayName("bad display string utf-8 (invalid 3-byte seq)")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"%e2%28%a1\"");
		} );
	}

	@Test
	@DisplayName("bad display string utf-8 (invalid 4-byte seq)")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"%f0%28%8c%28\"");
		} );
	}

	@Test
	@DisplayName("BOM in display string")
	public void test18() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%\"BOM: %ef%bb%bf\"");
		assertEquals(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"BOM: ﻿\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
