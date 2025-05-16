package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseStringGeneratedTests {
	@Test
	@DisplayName("0x00 in string")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0000 \"");
		} );
	}

	@Test
	@DisplayName("0x01 in string")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0001 \"");
		} );
	}

	@Test
	@DisplayName("0x02 in string")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0002 \"");
		} );
	}

	@Test
	@DisplayName("0x03 in string")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0003 \"");
		} );
	}

	@Test
	@DisplayName("0x04 in string")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0004 \"");
		} );
	}

	@Test
	@DisplayName("0x05 in string")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0005 \"");
		} );
	}

	@Test
	@DisplayName("0x06 in string")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0006 \"");
		} );
	}

	@Test
	@DisplayName("0x07 in string")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0007 \"");
		} );
	}

	@Test
	@DisplayName("0x08 in string")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \b \"");
		} );
	}

	@Test
	@DisplayName("0x09 in string")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \t \"");
		} );
	}

	@Test
	@DisplayName("0x0a in string")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \n"
							+ " \"");
		} );
	}

	@Test
	@DisplayName("0x0b in string")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u000b \"");
		} );
	}

	@Test
	@DisplayName("0x0c in string")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \f \"");
		} );
	}

	@Test
	@DisplayName("0x0d in string")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \r \"");
		} );
	}

	@Test
	@DisplayName("0x0e in string")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u000e \"");
		} );
	}

	@Test
	@DisplayName("0x0f in string")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u000f \"");
		} );
	}

	@Test
	@DisplayName("0x10 in string")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0010 \"");
		} );
	}

	@Test
	@DisplayName("0x11 in string")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0011 \"");
		} );
	}

	@Test
	@DisplayName("0x12 in string")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0012 \"");
		} );
	}

	@Test
	@DisplayName("0x13 in string")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0013 \"");
		} );
	}

	@Test
	@DisplayName("0x14 in string")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0014 \"");
		} );
	}

	@Test
	@DisplayName("0x15 in string")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0015 \"");
		} );
	}

	@Test
	@DisplayName("0x16 in string")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0016 \"");
		} );
	}

	@Test
	@DisplayName("0x17 in string")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0017 \"");
		} );
	}

	@Test
	@DisplayName("0x18 in string")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0018 \"");
		} );
	}

	@Test
	@DisplayName("0x19 in string")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u0019 \"");
		} );
	}

	@Test
	@DisplayName("0x1a in string")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001a \"");
		} );
	}

	@Test
	@DisplayName("0x1b in string")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001b \"");
		} );
	}

	@Test
	@DisplayName("0x1c in string")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001c \"");
		} );
	}

	@Test
	@DisplayName("0x1d in string")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001d \"");
		} );
	}

	@Test
	@DisplayName("0x1e in string")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001e \"");
		} );
	}

	@Test
	@DisplayName("0x1f in string")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u001f \"");
		} );
	}

	@Test
	@DisplayName("0x20 in string")
	public void test32() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"   \"");
		assertEquals(JsonParser.parseString("[\"   \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x21 in string")
	public void test33() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ! \"");
		assertEquals(JsonParser.parseString("[\" ! \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x22 in string")
	public void test34() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \" \"");
		} );
	}

	@Test
	@DisplayName("0x23 in string")
	public void test35() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" # \"");
		assertEquals(JsonParser.parseString("[\" # \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x24 in string")
	public void test36() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" $ \"");
		assertEquals(JsonParser.parseString("[\" $ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x25 in string")
	public void test37() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" % \"");
		assertEquals(JsonParser.parseString("[\" % \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x26 in string")
	public void test38() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" & \"");
		assertEquals(JsonParser.parseString("[\" & \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x27 in string")
	public void test39() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ' \"");
		assertEquals(JsonParser.parseString("[\" ' \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x28 in string")
	public void test40() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ( \"");
		assertEquals(JsonParser.parseString("[\" ( \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x29 in string")
	public void test41() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ) \"");
		assertEquals(JsonParser.parseString("[\" ) \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2a in string")
	public void test42() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" * \"");
		assertEquals(JsonParser.parseString("[\" * \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b in string")
	public void test43() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" + \"");
		assertEquals(JsonParser.parseString("[\" + \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2c in string")
	public void test44() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" , \"");
		assertEquals(JsonParser.parseString("[\" , \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2d in string")
	public void test45() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" - \"");
		assertEquals(JsonParser.parseString("[\" - \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2e in string")
	public void test46() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" . \"");
		assertEquals(JsonParser.parseString("[\" . \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2f in string")
	public void test47() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" / \"");
		assertEquals(JsonParser.parseString("[\" / \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x30 in string")
	public void test48() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 0 \"");
		assertEquals(JsonParser.parseString("[\" 0 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x31 in string")
	public void test49() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 1 \"");
		assertEquals(JsonParser.parseString("[\" 1 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x32 in string")
	public void test50() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 2 \"");
		assertEquals(JsonParser.parseString("[\" 2 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x33 in string")
	public void test51() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 3 \"");
		assertEquals(JsonParser.parseString("[\" 3 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x34 in string")
	public void test52() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 4 \"");
		assertEquals(JsonParser.parseString("[\" 4 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x35 in string")
	public void test53() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 5 \"");
		assertEquals(JsonParser.parseString("[\" 5 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x36 in string")
	public void test54() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 6 \"");
		assertEquals(JsonParser.parseString("[\" 6 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x37 in string")
	public void test55() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 7 \"");
		assertEquals(JsonParser.parseString("[\" 7 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x38 in string")
	public void test56() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 8 \"");
		assertEquals(JsonParser.parseString("[\" 8 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x39 in string")
	public void test57() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" 9 \"");
		assertEquals(JsonParser.parseString("[\" 9 \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3a in string")
	public void test58() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" : \"");
		assertEquals(JsonParser.parseString("[\" : \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3b in string")
	public void test59() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ; \"");
		assertEquals(JsonParser.parseString("[\" ; \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3c in string")
	public void test60() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" < \"");
		assertEquals(JsonParser.parseString("[\" < \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3d in string")
	public void test61() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" = \"");
		assertEquals(JsonParser.parseString("[\" = \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3e in string")
	public void test62() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" > \"");
		assertEquals(JsonParser.parseString("[\" > \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3f in string")
	public void test63() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ? \"");
		assertEquals(JsonParser.parseString("[\" ? \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x40 in string")
	public void test64() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" @ \"");
		assertEquals(JsonParser.parseString("[\" @ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x41 in string")
	public void test65() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" A \"");
		assertEquals(JsonParser.parseString("[\" A \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x42 in string")
	public void test66() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" B \"");
		assertEquals(JsonParser.parseString("[\" B \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x43 in string")
	public void test67() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" C \"");
		assertEquals(JsonParser.parseString("[\" C \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x44 in string")
	public void test68() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" D \"");
		assertEquals(JsonParser.parseString("[\" D \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x45 in string")
	public void test69() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" E \"");
		assertEquals(JsonParser.parseString("[\" E \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x46 in string")
	public void test70() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" F \"");
		assertEquals(JsonParser.parseString("[\" F \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x47 in string")
	public void test71() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" G \"");
		assertEquals(JsonParser.parseString("[\" G \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x48 in string")
	public void test72() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" H \"");
		assertEquals(JsonParser.parseString("[\" H \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x49 in string")
	public void test73() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" I \"");
		assertEquals(JsonParser.parseString("[\" I \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4a in string")
	public void test74() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" J \"");
		assertEquals(JsonParser.parseString("[\" J \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4b in string")
	public void test75() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" K \"");
		assertEquals(JsonParser.parseString("[\" K \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4c in string")
	public void test76() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" L \"");
		assertEquals(JsonParser.parseString("[\" L \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4d in string")
	public void test77() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" M \"");
		assertEquals(JsonParser.parseString("[\" M \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4e in string")
	public void test78() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" N \"");
		assertEquals(JsonParser.parseString("[\" N \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4f in string")
	public void test79() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" O \"");
		assertEquals(JsonParser.parseString("[\" O \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x50 in string")
	public void test80() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" P \"");
		assertEquals(JsonParser.parseString("[\" P \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x51 in string")
	public void test81() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" Q \"");
		assertEquals(JsonParser.parseString("[\" Q \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x52 in string")
	public void test82() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" R \"");
		assertEquals(JsonParser.parseString("[\" R \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x53 in string")
	public void test83() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" S \"");
		assertEquals(JsonParser.parseString("[\" S \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x54 in string")
	public void test84() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" T \"");
		assertEquals(JsonParser.parseString("[\" T \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x55 in string")
	public void test85() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" U \"");
		assertEquals(JsonParser.parseString("[\" U \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x56 in string")
	public void test86() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" V \"");
		assertEquals(JsonParser.parseString("[\" V \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x57 in string")
	public void test87() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" W \"");
		assertEquals(JsonParser.parseString("[\" W \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x58 in string")
	public void test88() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" X \"");
		assertEquals(JsonParser.parseString("[\" X \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x59 in string")
	public void test89() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" Y \"");
		assertEquals(JsonParser.parseString("[\" Y \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5a in string")
	public void test90() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" Z \"");
		assertEquals(JsonParser.parseString("[\" Z \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5b in string")
	public void test91() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" [ \"");
		assertEquals(JsonParser.parseString("[\" [ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5c in string")
	public void test92() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \\ \"");
		} );
	}

	@Test
	@DisplayName("0x5d in string")
	public void test93() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ] \"");
		assertEquals(JsonParser.parseString("[\" ] \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5e in string")
	public void test94() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ^ \"");
		assertEquals(JsonParser.parseString("[\" ^ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5f in string")
	public void test95() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" _ \"");
		assertEquals(JsonParser.parseString("[\" _ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x60 in string")
	public void test96() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ` \"");
		assertEquals(JsonParser.parseString("[\" ` \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x61 in string")
	public void test97() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" a \"");
		assertEquals(JsonParser.parseString("[\" a \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 in string")
	public void test98() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" b \"");
		assertEquals(JsonParser.parseString("[\" b \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 in string")
	public void test99() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" c \"");
		assertEquals(JsonParser.parseString("[\" c \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 in string")
	public void test100() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" d \"");
		assertEquals(JsonParser.parseString("[\" d \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 in string")
	public void test101() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" e \"");
		assertEquals(JsonParser.parseString("[\" e \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 in string")
	public void test102() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" f \"");
		assertEquals(JsonParser.parseString("[\" f \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 in string")
	public void test103() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" g \"");
		assertEquals(JsonParser.parseString("[\" g \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 in string")
	public void test104() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" h \"");
		assertEquals(JsonParser.parseString("[\" h \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 in string")
	public void test105() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" i \"");
		assertEquals(JsonParser.parseString("[\" i \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a in string")
	public void test106() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" j \"");
		assertEquals(JsonParser.parseString("[\" j \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b in string")
	public void test107() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" k \"");
		assertEquals(JsonParser.parseString("[\" k \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c in string")
	public void test108() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" l \"");
		assertEquals(JsonParser.parseString("[\" l \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d in string")
	public void test109() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" m \"");
		assertEquals(JsonParser.parseString("[\" m \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e in string")
	public void test110() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" n \"");
		assertEquals(JsonParser.parseString("[\" n \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f in string")
	public void test111() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" o \"");
		assertEquals(JsonParser.parseString("[\" o \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 in string")
	public void test112() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" p \"");
		assertEquals(JsonParser.parseString("[\" p \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 in string")
	public void test113() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" q \"");
		assertEquals(JsonParser.parseString("[\" q \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 in string")
	public void test114() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" r \"");
		assertEquals(JsonParser.parseString("[\" r \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 in string")
	public void test115() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" s \"");
		assertEquals(JsonParser.parseString("[\" s \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 in string")
	public void test116() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" t \"");
		assertEquals(JsonParser.parseString("[\" t \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 in string")
	public void test117() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" u \"");
		assertEquals(JsonParser.parseString("[\" u \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 in string")
	public void test118() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" v \"");
		assertEquals(JsonParser.parseString("[\" v \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 in string")
	public void test119() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" w \"");
		assertEquals(JsonParser.parseString("[\" w \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 in string")
	public void test120() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" x \"");
		assertEquals(JsonParser.parseString("[\" x \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 in string")
	public void test121() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" y \"");
		assertEquals(JsonParser.parseString("[\" y \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a in string")
	public void test122() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" z \"");
		assertEquals(JsonParser.parseString("[\" z \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b in string")
	public void test123() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" { \"");
		assertEquals(JsonParser.parseString("[\" { \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7c in string")
	public void test124() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" | \"");
		assertEquals(JsonParser.parseString("[\" | \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7d in string")
	public void test125() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" } \"");
		assertEquals(JsonParser.parseString("[\" } \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7e in string")
	public void test126() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" ~ \"");
		assertEquals(JsonParser.parseString("[\" ~ \",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7f in string")
	public void test127() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\" \u007f \"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x00 in string")
	public void test128() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0000\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x01 in string")
	public void test129() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0001\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x02 in string")
	public void test130() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0002\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x03 in string")
	public void test131() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0003\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x04 in string")
	public void test132() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0004\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x05 in string")
	public void test133() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0005\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x06 in string")
	public void test134() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0006\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x07 in string")
	public void test135() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0007\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x08 in string")
	public void test136() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\b\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x09 in string")
	public void test137() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\t\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0a in string")
	public void test138() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\n"
							+ "\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0b in string")
	public void test139() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u000b\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0c in string")
	public void test140() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\f\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0d in string")
	public void test141() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\r\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0e in string")
	public void test142() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u000e\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x0f in string")
	public void test143() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u000f\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x10 in string")
	public void test144() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0010\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x11 in string")
	public void test145() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0011\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x12 in string")
	public void test146() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0012\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x13 in string")
	public void test147() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0013\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x14 in string")
	public void test148() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0014\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x15 in string")
	public void test149() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0015\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x16 in string")
	public void test150() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0016\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x17 in string")
	public void test151() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0017\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x18 in string")
	public void test152() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0018\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x19 in string")
	public void test153() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u0019\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1a in string")
	public void test154() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001a\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1b in string")
	public void test155() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001b\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1c in string")
	public void test156() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001c\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1d in string")
	public void test157() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001d\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1e in string")
	public void test158() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001e\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x1f in string")
	public void test159() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u001f\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x20 in string")
	public void test160() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\ \"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x21 in string")
	public void test161() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\!\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x22 in string")
	public void test162() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\"\"");
		assertEquals(JsonParser.parseString("[\"\\\"\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Escaped 0x23 in string")
	public void test163() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\#\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x24 in string")
	public void test164() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\$\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x25 in string")
	public void test165() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\%\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x26 in string")
	public void test166() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\&\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x27 in string")
	public void test167() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\'\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x28 in string")
	public void test168() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\(\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x29 in string")
	public void test169() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\)\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2a in string")
	public void test170() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\*\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2b in string")
	public void test171() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\+\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2c in string")
	public void test172() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\,\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2d in string")
	public void test173() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\-\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2e in string")
	public void test174() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\.\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x2f in string")
	public void test175() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\/\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x30 in string")
	public void test176() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\0\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x31 in string")
	public void test177() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\1\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x32 in string")
	public void test178() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\2\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x33 in string")
	public void test179() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\3\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x34 in string")
	public void test180() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\4\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x35 in string")
	public void test181() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\5\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x36 in string")
	public void test182() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\6\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x37 in string")
	public void test183() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\7\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x38 in string")
	public void test184() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\8\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x39 in string")
	public void test185() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\9\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3a in string")
	public void test186() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\:\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3b in string")
	public void test187() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\;\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3c in string")
	public void test188() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\<\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3d in string")
	public void test189() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\=\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3e in string")
	public void test190() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\>\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x3f in string")
	public void test191() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\?\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x40 in string")
	public void test192() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\@\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x41 in string")
	public void test193() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\A\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x42 in string")
	public void test194() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\B\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x43 in string")
	public void test195() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\C\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x44 in string")
	public void test196() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\D\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x45 in string")
	public void test197() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\E\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x46 in string")
	public void test198() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\F\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x47 in string")
	public void test199() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\G\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x48 in string")
	public void test200() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\H\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x49 in string")
	public void test201() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\I\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4a in string")
	public void test202() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\J\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4b in string")
	public void test203() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\K\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4c in string")
	public void test204() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\L\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4d in string")
	public void test205() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\M\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4e in string")
	public void test206() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\N\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x4f in string")
	public void test207() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\O\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x50 in string")
	public void test208() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\P\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x51 in string")
	public void test209() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\Q\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x52 in string")
	public void test210() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\R\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x53 in string")
	public void test211() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\S\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x54 in string")
	public void test212() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\T\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x55 in string")
	public void test213() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\U\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x56 in string")
	public void test214() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\V\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x57 in string")
	public void test215() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\W\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x58 in string")
	public void test216() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\X\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x59 in string")
	public void test217() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\Y\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x5a in string")
	public void test218() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\Z\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x5b in string")
	public void test219() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\[\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x5c in string")
	public void test220() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\\\"");
		assertEquals(JsonParser.parseString("[\"\\\\\",[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("Escaped 0x5d in string")
	public void test221() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\]\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x5e in string")
	public void test222() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\^\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x5f in string")
	public void test223() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\_\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x60 in string")
	public void test224() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\`\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x61 in string")
	public void test225() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\a\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x62 in string")
	public void test226() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\b\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x63 in string")
	public void test227() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\c\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x64 in string")
	public void test228() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\d\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x65 in string")
	public void test229() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\e\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x66 in string")
	public void test230() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\f\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x67 in string")
	public void test231() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\g\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x68 in string")
	public void test232() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\h\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x69 in string")
	public void test233() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\i\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6a in string")
	public void test234() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\j\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6b in string")
	public void test235() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\k\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6c in string")
	public void test236() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\l\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6d in string")
	public void test237() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\m\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6e in string")
	public void test238() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\n\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x6f in string")
	public void test239() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\o\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x70 in string")
	public void test240() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\p\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x71 in string")
	public void test241() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\q\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x72 in string")
	public void test242() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\r\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x73 in string")
	public void test243() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\s\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x74 in string")
	public void test244() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\t\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x75 in string")
	public void test245() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\u\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x76 in string")
	public void test246() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\v\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x77 in string")
	public void test247() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\w\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x78 in string")
	public void test248() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\x\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x79 in string")
	public void test249() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\y\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7a in string")
	public void test250() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\z\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7b in string")
	public void test251() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\{\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7c in string")
	public void test252() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\|\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7d in string")
	public void test253() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\}\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7e in string")
	public void test254() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\~\"");
		} );
	}

	@Test
	@DisplayName("Escaped 0x7f in string")
	public void test255() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"\\\u007f\"");
		} );
	}
}
