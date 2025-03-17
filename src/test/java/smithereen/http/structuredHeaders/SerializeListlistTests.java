package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeListlistTests {
	@Test
	@DisplayName("basic list of lists")
	public void test0() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[1,[]],[2,[]]],[]],[[[42,[]],[43,[]]],[]]]"));
		assertEquals("(1 2), (42 43)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("single item list of lists")
	public void test1() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[42,[]]],[]]]"));
		assertEquals("(42)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty item list of lists")
	public void test2() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[],[]]]"));
		assertEquals("()", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("empty middle item list of lists")
	public void test3() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[1,[]]],[]],[[],[]],[[[42,[]]],[]]]"));
		assertEquals("(1), (), (42)", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("extra whitespace list of lists")
	public void test4() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[[[1,[]],[42,[]]],[]]]"));
		assertEquals("(1 42)", StructuredHttpHeaders.serialize(source));
	}
}
