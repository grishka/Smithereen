package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseItemTests {
	@Test
	@DisplayName("empty item")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("");
		} );
	}

	@Test
	@DisplayName("leading space")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(" \t 1");
		} );
	}

	@Test
	@DisplayName("trailing space")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1 \t ");
		} );
	}

	@Test
	@DisplayName("leading and trailing space")
	public void test3() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("  1  ");
		assertEquals(JsonParser.parseString("[1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("leading and trailing whitespace")
	public void test4() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("     1  ");
		assertEquals(JsonParser.parseString("[1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}
}
