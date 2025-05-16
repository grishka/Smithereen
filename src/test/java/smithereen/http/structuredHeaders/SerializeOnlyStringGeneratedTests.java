package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeOnlyStringGeneratedTests {
	@Test
	@DisplayName("0x00 in string - serialise only")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0000\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 in string - serialise only")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0001\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 in string - serialise only")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0002\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 in string - serialise only")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0003\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 in string - serialise only")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0004\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 in string - serialise only")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0005\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 in string - serialise only")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0006\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 in string - serialise only")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0007\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 in string - serialise only")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\b\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 in string - serialise only")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\t\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a in string - serialise only")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\n\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b in string - serialise only")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u000b\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c in string - serialise only")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\f\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d in string - serialise only")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\r\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e in string - serialise only")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u000e\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f in string - serialise only")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u000f\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 in string - serialise only")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0010\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 in string - serialise only")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0011\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 in string - serialise only")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0012\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 in string - serialise only")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0013\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 in string - serialise only")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0014\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 in string - serialise only")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0015\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 in string - serialise only")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0016\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 in string - serialise only")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0017\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 in string - serialise only")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0018\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 in string - serialise only")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u0019\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a in string - serialise only")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001a\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b in string - serialise only")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001b\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c in string - serialise only")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001c\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d in string - serialise only")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001d\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e in string - serialise only")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001e\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f in string - serialise only")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\u001f\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f in string - serialise only")
	public void test32() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\u007f\",[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}
}
