package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeDisplayStringTests {
	@Test
	@DisplayName("basic display string (ascii content)")
	public void test0() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"foo bar\"},[]]"));
		assertEquals("%\"foo bar\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("all printable ascii")
	public void test1() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\" !\\\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\"},[]]"));
		assertEquals("%\" !%22#$%25&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("non-ascii display string (lowercase escaping)")
	public void test3() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"füü\"},[]]"));
		assertEquals("%\"f%c3%bc%c3%bc\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("display string quoting")
	public void test11() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"foo \\\"bar\\\" \\\\ baz\"},[]]"));
		assertEquals("%\"foo %22bar%22 \\ baz\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("BOM in display string")
	public void test18() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"displaystring\",\"value\":\"BOM: ﻿\"},[]]"));
		assertEquals("%\"BOM: %ef%bb%bf\"", StructuredHttpHeaders.serialize(source));
	}
}
