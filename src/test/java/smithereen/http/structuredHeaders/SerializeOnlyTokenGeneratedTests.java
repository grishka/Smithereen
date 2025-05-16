package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeOnlyTokenGeneratedTests {
	@Test
	@DisplayName("0x00 in token - serialise only")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0000a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 in token - serialise only")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0001a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 in token - serialise only")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0002a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 in token - serialise only")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0003a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 in token - serialise only")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0004a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 in token - serialise only")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0005a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 in token - serialise only")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0006a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 in token - serialise only")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0007a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 in token - serialise only")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 in token - serialise only")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\ta\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a in token - serialise only")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\na\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b in token - serialise only")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u000ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c in token - serialise only")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d in token - serialise only")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\ra\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e in token - serialise only")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u000ea\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f in token - serialise only")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u000fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 in token - serialise only")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0010a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 in token - serialise only")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0011a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 in token - serialise only")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0012a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 in token - serialise only")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0013a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 in token - serialise only")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0014a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 in token - serialise only")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0015a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 in token - serialise only")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0016a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 in token - serialise only")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0017a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 in token - serialise only")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0018a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 in token - serialise only")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u0019a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a in token - serialise only")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001aa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b in token - serialise only")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c in token - serialise only")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001ca\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d in token - serialise only")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001da\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e in token - serialise only")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001ea\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f in token - serialise only")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\u001fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 in token - serialise only")
	public void test32() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 in token - serialise only")
	public void test33() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\\"a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 in token - serialise only")
	public void test34() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a(a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 in token - serialise only")
	public void test35() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a)a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c in token - serialise only")
	public void test36() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a,a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b in token - serialise only")
	public void test37() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a;a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c in token - serialise only")
	public void test38() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a<a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d in token - serialise only")
	public void test39() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a=a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e in token - serialise only")
	public void test40() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a>a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f in token - serialise only")
	public void test41() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a?a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 in token - serialise only")
	public void test42() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a@a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b in token - serialise only")
	public void test43() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a[a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c in token - serialise only")
	public void test44() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\\\\a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d in token - serialise only")
	public void test45() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a]a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b in token - serialise only")
	public void test46() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a{a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d in token - serialise only")
	public void test47() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a}a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f in token - serialise only")
	public void test48() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\u007fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x00 starting a token - serialise only")
	public void test49() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0000a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 starting a token - serialise only")
	public void test50() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0001a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 starting a token - serialise only")
	public void test51() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0002a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 starting a token - serialise only")
	public void test52() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0003a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 starting a token - serialise only")
	public void test53() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0004a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 starting a token - serialise only")
	public void test54() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0005a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 starting a token - serialise only")
	public void test55() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0006a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 starting a token - serialise only")
	public void test56() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0007a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 starting a token - serialise only")
	public void test57() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 starting a token - serialise only")
	public void test58() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\ta\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a starting a token - serialise only")
	public void test59() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\na\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b starting a token - serialise only")
	public void test60() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u000ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c starting a token - serialise only")
	public void test61() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d starting a token - serialise only")
	public void test62() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\ra\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e starting a token - serialise only")
	public void test63() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u000ea\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f starting a token - serialise only")
	public void test64() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u000fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 starting a token - serialise only")
	public void test65() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0010a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 starting a token - serialise only")
	public void test66() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0011a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 starting a token - serialise only")
	public void test67() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0012a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 starting a token - serialise only")
	public void test68() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0013a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 starting a token - serialise only")
	public void test69() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0014a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 starting a token - serialise only")
	public void test70() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0015a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 starting a token - serialise only")
	public void test71() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0016a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 starting a token - serialise only")
	public void test72() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0017a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 starting a token - serialise only")
	public void test73() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0018a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 starting a token - serialise only")
	public void test74() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u0019a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a starting a token - serialise only")
	public void test75() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001aa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b starting a token - serialise only")
	public void test76() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001ba\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c starting a token - serialise only")
	public void test77() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001ca\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d starting a token - serialise only")
	public void test78() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001da\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e starting a token - serialise only")
	public void test79() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001ea\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f starting a token - serialise only")
	public void test80() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\u001fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 starting a token - serialise only")
	public void test81() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\" a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x21 starting a token - serialise only")
	public void test82() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"!a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 starting a token - serialise only")
	public void test83() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\\"a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x23 starting a token - serialise only")
	public void test84() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"#a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x24 starting a token - serialise only")
	public void test85() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"$a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x25 starting a token - serialise only")
	public void test86() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"%a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x26 starting a token - serialise only")
	public void test87() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"&a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x27 starting a token - serialise only")
	public void test88() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"'a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 starting a token - serialise only")
	public void test89() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"(a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 starting a token - serialise only")
	public void test90() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\")a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2b starting a token - serialise only")
	public void test91() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"+a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c starting a token - serialise only")
	public void test92() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\",a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2d starting a token - serialise only")
	public void test93() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"-a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2e starting a token - serialise only")
	public void test94() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\".a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2f starting a token - serialise only")
	public void test95() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"/a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x30 starting a token - serialise only")
	public void test96() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"0a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x31 starting a token - serialise only")
	public void test97() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"1a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x32 starting a token - serialise only")
	public void test98() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"2a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x33 starting a token - serialise only")
	public void test99() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"3a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x34 starting a token - serialise only")
	public void test100() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"4a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x35 starting a token - serialise only")
	public void test101() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"5a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x36 starting a token - serialise only")
	public void test102() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"6a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x37 starting a token - serialise only")
	public void test103() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"7a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x38 starting a token - serialise only")
	public void test104() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"8a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x39 starting a token - serialise only")
	public void test105() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"9a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3a starting a token - serialise only")
	public void test106() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\":a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b starting a token - serialise only")
	public void test107() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\";a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c starting a token - serialise only")
	public void test108() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"<a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d starting a token - serialise only")
	public void test109() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"=a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e starting a token - serialise only")
	public void test110() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\">a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f starting a token - serialise only")
	public void test111() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"?a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 starting a token - serialise only")
	public void test112() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"@a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b starting a token - serialise only")
	public void test113() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"[a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c starting a token - serialise only")
	public void test114() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\\\\a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d starting a token - serialise only")
	public void test115() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"]a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5e starting a token - serialise only")
	public void test116() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"^a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5f starting a token - serialise only")
	public void test117() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"_a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x60 starting a token - serialise only")
	public void test118() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"`a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b starting a token - serialise only")
	public void test119() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"{a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7c starting a token - serialise only")
	public void test120() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"|a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d starting a token - serialise only")
	public void test121() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"}a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7e starting a token - serialise only")
	public void test122() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"~a\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f starting a token - serialise only")
	public void test123() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"\u007fa\"},[]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}
}
