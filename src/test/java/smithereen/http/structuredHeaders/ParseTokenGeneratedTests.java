package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseTokenGeneratedTests {
	@Test
	@DisplayName("0x00 in token")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0000a");
		} );
	}

	@Test
	@DisplayName("0x01 in token")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0001a");
		} );
	}

	@Test
	@DisplayName("0x02 in token")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0002a");
		} );
	}

	@Test
	@DisplayName("0x03 in token")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0003a");
		} );
	}

	@Test
	@DisplayName("0x04 in token")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0004a");
		} );
	}

	@Test
	@DisplayName("0x05 in token")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0005a");
		} );
	}

	@Test
	@DisplayName("0x06 in token")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0006a");
		} );
	}

	@Test
	@DisplayName("0x07 in token")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0007a");
		} );
	}

	@Test
	@DisplayName("0x08 in token")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\ba");
		} );
	}

	@Test
	@DisplayName("0x09 in token")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\ta");
		} );
	}

	@Test
	@DisplayName("0x0a in token")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\n"
							+ "a");
		} );
	}

	@Test
	@DisplayName("0x0b in token")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u000ba");
		} );
	}

	@Test
	@DisplayName("0x0c in token")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\fa");
		} );
	}

	@Test
	@DisplayName("0x0d in token")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\ra");
		} );
	}

	@Test
	@DisplayName("0x0e in token")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u000ea");
		} );
	}

	@Test
	@DisplayName("0x0f in token")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u000fa");
		} );
	}

	@Test
	@DisplayName("0x10 in token")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0010a");
		} );
	}

	@Test
	@DisplayName("0x11 in token")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0011a");
		} );
	}

	@Test
	@DisplayName("0x12 in token")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0012a");
		} );
	}

	@Test
	@DisplayName("0x13 in token")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0013a");
		} );
	}

	@Test
	@DisplayName("0x14 in token")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0014a");
		} );
	}

	@Test
	@DisplayName("0x15 in token")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0015a");
		} );
	}

	@Test
	@DisplayName("0x16 in token")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0016a");
		} );
	}

	@Test
	@DisplayName("0x17 in token")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0017a");
		} );
	}

	@Test
	@DisplayName("0x18 in token")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0018a");
		} );
	}

	@Test
	@DisplayName("0x19 in token")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u0019a");
		} );
	}

	@Test
	@DisplayName("0x1a in token")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001aa");
		} );
	}

	@Test
	@DisplayName("0x1b in token")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001ba");
		} );
	}

	@Test
	@DisplayName("0x1c in token")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001ca");
		} );
	}

	@Test
	@DisplayName("0x1d in token")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001da");
		} );
	}

	@Test
	@DisplayName("0x1e in token")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001ea");
		} );
	}

	@Test
	@DisplayName("0x1f in token")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u001fa");
		} );
	}

	@Test
	@DisplayName("0x20 in token")
	public void test32() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a a");
		} );
	}

	@Test
	@DisplayName("0x21 in token")
	public void test33() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a!a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a!a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x22 in token")
	public void test34() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\"a");
		} );
	}

	@Test
	@DisplayName("0x23 in token")
	public void test35() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a#a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a#a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x24 in token")
	public void test36() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a$a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a$a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x25 in token")
	public void test37() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a%a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a%a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x26 in token")
	public void test38() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a&a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a&a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x27 in token")
	public void test39() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a'a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a'a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x28 in token")
	public void test40() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a(a");
		} );
	}

	@Test
	@DisplayName("0x29 in token")
	public void test41() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a)a");
		} );
	}

	@Test
	@DisplayName("0x2a in token")
	public void test42() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a*a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a*a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b in token")
	public void test43() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a+a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a+a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2c in token")
	public void test44() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a,a");
		} );
	}

	@Test
	@DisplayName("0x2d in token")
	public void test45() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a-a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a-a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2e in token")
	public void test46() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a.a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a.a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2f in token")
	public void test47() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a/a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a/a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x30 in token")
	public void test48() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a0a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a0a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x31 in token")
	public void test49() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a1a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a1a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x32 in token")
	public void test50() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a2a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a2a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x33 in token")
	public void test51() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a3a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a3a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x34 in token")
	public void test52() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a4a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a4a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x35 in token")
	public void test53() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a5a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a5a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x36 in token")
	public void test54() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a6a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a6a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x37 in token")
	public void test55() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a7a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a7a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x38 in token")
	public void test56() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a8a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a8a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x39 in token")
	public void test57() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a9a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a9a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3a in token")
	public void test58() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a:a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a:a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3b in token")
	public void test59() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a;a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\"},[[\"a\",true]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3c in token")
	public void test60() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a<a");
		} );
	}

	@Test
	@DisplayName("0x3d in token")
	public void test61() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a=a");
		} );
	}

	@Test
	@DisplayName("0x3e in token")
	public void test62() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a>a");
		} );
	}

	@Test
	@DisplayName("0x3f in token")
	public void test63() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a?a");
		} );
	}

	@Test
	@DisplayName("0x40 in token")
	public void test64() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a@a");
		} );
	}

	@Test
	@DisplayName("0x41 in token")
	public void test65() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aAa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aAa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x42 in token")
	public void test66() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aBa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aBa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x43 in token")
	public void test67() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aCa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aCa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x44 in token")
	public void test68() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aDa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aDa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x45 in token")
	public void test69() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aEa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aEa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x46 in token")
	public void test70() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aFa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aFa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x47 in token")
	public void test71() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aGa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aGa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x48 in token")
	public void test72() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aHa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aHa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x49 in token")
	public void test73() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aIa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aIa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4a in token")
	public void test74() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aJa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aJa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4b in token")
	public void test75() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aKa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aKa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4c in token")
	public void test76() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aLa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aLa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4d in token")
	public void test77() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aMa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aMa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4e in token")
	public void test78() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aNa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aNa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4f in token")
	public void test79() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aOa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aOa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x50 in token")
	public void test80() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aPa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aPa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x51 in token")
	public void test81() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aQa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aQa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x52 in token")
	public void test82() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aRa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aRa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x53 in token")
	public void test83() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aSa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aSa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x54 in token")
	public void test84() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aTa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aTa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x55 in token")
	public void test85() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aUa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aUa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x56 in token")
	public void test86() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aVa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aVa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x57 in token")
	public void test87() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aWa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aWa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x58 in token")
	public void test88() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aXa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aXa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x59 in token")
	public void test89() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aYa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aYa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5a in token")
	public void test90() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aZa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aZa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5b in token")
	public void test91() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a[a");
		} );
	}

	@Test
	@DisplayName("0x5c in token")
	public void test92() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\\a");
		} );
	}

	@Test
	@DisplayName("0x5d in token")
	public void test93() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a]a");
		} );
	}

	@Test
	@DisplayName("0x5e in token")
	public void test94() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a^a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a^a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5f in token")
	public void test95() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a_a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a_a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x60 in token")
	public void test96() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a`a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a`a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x61 in token")
	public void test97() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aaa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aaa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 in token")
	public void test98() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aba");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aba\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 in token")
	public void test99() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aca");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aca\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 in token")
	public void test100() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ada");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ada\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 in token")
	public void test101() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aea");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aea\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 in token")
	public void test102() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("afa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"afa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 in token")
	public void test103() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aga");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aga\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 in token")
	public void test104() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aha");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aha\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 in token")
	public void test105() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aia");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aia\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a in token")
	public void test106() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aja");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aja\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b in token")
	public void test107() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aka");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aka\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c in token")
	public void test108() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ala");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ala\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d in token")
	public void test109() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ama");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ama\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e in token")
	public void test110() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ana");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ana\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f in token")
	public void test111() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aoa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aoa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 in token")
	public void test112() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("apa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"apa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 in token")
	public void test113() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aqa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aqa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 in token")
	public void test114() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ara");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ara\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 in token")
	public void test115() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("asa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"asa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 in token")
	public void test116() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ata");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ata\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 in token")
	public void test117() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aua");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aua\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 in token")
	public void test118() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ava");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ava\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 in token")
	public void test119() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("awa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"awa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 in token")
	public void test120() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("axa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"axa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 in token")
	public void test121() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aya");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aya\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a in token")
	public void test122() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aza");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aza\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b in token")
	public void test123() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a{a");
		} );
	}

	@Test
	@DisplayName("0x7c in token")
	public void test124() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a|a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a|a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7d in token")
	public void test125() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a}a");
		} );
	}

	@Test
	@DisplayName("0x7e in token")
	public void test126() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a~a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a~a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7f in token")
	public void test127() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("a\u007fa");
		} );
	}

	@Test
	@DisplayName("0x00 starting a token")
	public void test128() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0000a");
		} );
	}

	@Test
	@DisplayName("0x01 starting a token")
	public void test129() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0001a");
		} );
	}

	@Test
	@DisplayName("0x02 starting a token")
	public void test130() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0002a");
		} );
	}

	@Test
	@DisplayName("0x03 starting a token")
	public void test131() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0003a");
		} );
	}

	@Test
	@DisplayName("0x04 starting a token")
	public void test132() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0004a");
		} );
	}

	@Test
	@DisplayName("0x05 starting a token")
	public void test133() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0005a");
		} );
	}

	@Test
	@DisplayName("0x06 starting a token")
	public void test134() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0006a");
		} );
	}

	@Test
	@DisplayName("0x07 starting a token")
	public void test135() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0007a");
		} );
	}

	@Test
	@DisplayName("0x08 starting a token")
	public void test136() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\ba");
		} );
	}

	@Test
	@DisplayName("0x09 starting a token")
	public void test137() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\ta");
		} );
	}

	@Test
	@DisplayName("0x0a starting a token")
	public void test138() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\n"
							+ "a");
		} );
	}

	@Test
	@DisplayName("0x0b starting a token")
	public void test139() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u000ba");
		} );
	}

	@Test
	@DisplayName("0x0c starting a token")
	public void test140() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\fa");
		} );
	}

	@Test
	@DisplayName("0x0d starting a token")
	public void test141() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\ra");
		} );
	}

	@Test
	@DisplayName("0x0e starting a token")
	public void test142() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u000ea");
		} );
	}

	@Test
	@DisplayName("0x0f starting a token")
	public void test143() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u000fa");
		} );
	}

	@Test
	@DisplayName("0x10 starting a token")
	public void test144() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0010a");
		} );
	}

	@Test
	@DisplayName("0x11 starting a token")
	public void test145() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0011a");
		} );
	}

	@Test
	@DisplayName("0x12 starting a token")
	public void test146() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0012a");
		} );
	}

	@Test
	@DisplayName("0x13 starting a token")
	public void test147() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0013a");
		} );
	}

	@Test
	@DisplayName("0x14 starting a token")
	public void test148() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0014a");
		} );
	}

	@Test
	@DisplayName("0x15 starting a token")
	public void test149() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0015a");
		} );
	}

	@Test
	@DisplayName("0x16 starting a token")
	public void test150() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0016a");
		} );
	}

	@Test
	@DisplayName("0x17 starting a token")
	public void test151() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0017a");
		} );
	}

	@Test
	@DisplayName("0x18 starting a token")
	public void test152() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0018a");
		} );
	}

	@Test
	@DisplayName("0x19 starting a token")
	public void test153() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u0019a");
		} );
	}

	@Test
	@DisplayName("0x1a starting a token")
	public void test154() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001aa");
		} );
	}

	@Test
	@DisplayName("0x1b starting a token")
	public void test155() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001ba");
		} );
	}

	@Test
	@DisplayName("0x1c starting a token")
	public void test156() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001ca");
		} );
	}

	@Test
	@DisplayName("0x1d starting a token")
	public void test157() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001da");
		} );
	}

	@Test
	@DisplayName("0x1e starting a token")
	public void test158() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001ea");
		} );
	}

	@Test
	@DisplayName("0x1f starting a token")
	public void test159() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u001fa");
		} );
	}

	@Test
	@DisplayName("0x20 starting a token")
	public void test160() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(" a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x21 starting a token")
	public void test161() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("!a");
		} );
	}

	@Test
	@DisplayName("0x22 starting a token")
	public void test162() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\"a");
		} );
	}

	@Test
	@DisplayName("0x23 starting a token")
	public void test163() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("#a");
		} );
	}

	@Test
	@DisplayName("0x24 starting a token")
	public void test164() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("$a");
		} );
	}

	@Test
	@DisplayName("0x25 starting a token")
	public void test165() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("%a");
		} );
	}

	@Test
	@DisplayName("0x26 starting a token")
	public void test166() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("&a");
		} );
	}

	@Test
	@DisplayName("0x27 starting a token")
	public void test167() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("'a");
		} );
	}

	@Test
	@DisplayName("0x28 starting a token")
	public void test168() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("(a");
		} );
	}

	@Test
	@DisplayName("0x29 starting a token")
	public void test169() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(")a");
		} );
	}

	@Test
	@DisplayName("0x2a starting a token")
	public void test170() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("*a");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"*a\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b starting a token")
	public void test171() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("+a");
		} );
	}

	@Test
	@DisplayName("0x2c starting a token")
	public void test172() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(",a");
		} );
	}

	@Test
	@DisplayName("0x2d starting a token")
	public void test173() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("-a");
		} );
	}

	@Test
	@DisplayName("0x2e starting a token")
	public void test174() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(".a");
		} );
	}

	@Test
	@DisplayName("0x2f starting a token")
	public void test175() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("/a");
		} );
	}

	@Test
	@DisplayName("0x30 starting a token")
	public void test176() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0a");
		} );
	}

	@Test
	@DisplayName("0x31 starting a token")
	public void test177() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1a");
		} );
	}

	@Test
	@DisplayName("0x32 starting a token")
	public void test178() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("2a");
		} );
	}

	@Test
	@DisplayName("0x33 starting a token")
	public void test179() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("3a");
		} );
	}

	@Test
	@DisplayName("0x34 starting a token")
	public void test180() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("4a");
		} );
	}

	@Test
	@DisplayName("0x35 starting a token")
	public void test181() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("5a");
		} );
	}

	@Test
	@DisplayName("0x36 starting a token")
	public void test182() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("6a");
		} );
	}

	@Test
	@DisplayName("0x37 starting a token")
	public void test183() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("7a");
		} );
	}

	@Test
	@DisplayName("0x38 starting a token")
	public void test184() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("8a");
		} );
	}

	@Test
	@DisplayName("0x39 starting a token")
	public void test185() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9a");
		} );
	}

	@Test
	@DisplayName("0x3a starting a token")
	public void test186() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(":a");
		} );
	}

	@Test
	@DisplayName("0x3b starting a token")
	public void test187() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(";a");
		} );
	}

	@Test
	@DisplayName("0x3c starting a token")
	public void test188() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("<a");
		} );
	}

	@Test
	@DisplayName("0x3d starting a token")
	public void test189() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("=a");
		} );
	}

	@Test
	@DisplayName("0x3e starting a token")
	public void test190() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem(">a");
		} );
	}

	@Test
	@DisplayName("0x3f starting a token")
	public void test191() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("?a");
		} );
	}

	@Test
	@DisplayName("0x40 starting a token")
	public void test192() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("@a");
		} );
	}

	@Test
	@DisplayName("0x41 starting a token")
	public void test193() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Aa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Aa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x42 starting a token")
	public void test194() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ba");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ba\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x43 starting a token")
	public void test195() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ca");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ca\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x44 starting a token")
	public void test196() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Da");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Da\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x45 starting a token")
	public void test197() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ea");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ea\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x46 starting a token")
	public void test198() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Fa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Fa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x47 starting a token")
	public void test199() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ga");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ga\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x48 starting a token")
	public void test200() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ha");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ha\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x49 starting a token")
	public void test201() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ia");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ia\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4a starting a token")
	public void test202() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ja");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ja\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4b starting a token")
	public void test203() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ka");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ka\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4c starting a token")
	public void test204() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("La");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"La\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4d starting a token")
	public void test205() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ma");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ma\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4e starting a token")
	public void test206() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Na");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Na\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x4f starting a token")
	public void test207() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Oa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Oa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x50 starting a token")
	public void test208() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Pa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Pa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x51 starting a token")
	public void test209() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Qa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Qa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x52 starting a token")
	public void test210() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ra");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ra\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x53 starting a token")
	public void test211() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Sa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Sa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x54 starting a token")
	public void test212() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ta");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ta\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x55 starting a token")
	public void test213() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ua");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ua\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x56 starting a token")
	public void test214() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Va");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Va\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x57 starting a token")
	public void test215() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Wa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Wa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x58 starting a token")
	public void test216() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Xa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Xa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x59 starting a token")
	public void test217() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Ya");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ya\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5a starting a token")
	public void test218() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("Za");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Za\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x5b starting a token")
	public void test219() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("[a");
		} );
	}

	@Test
	@DisplayName("0x5c starting a token")
	public void test220() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\\a");
		} );
	}

	@Test
	@DisplayName("0x5d starting a token")
	public void test221() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("]a");
		} );
	}

	@Test
	@DisplayName("0x5e starting a token")
	public void test222() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("^a");
		} );
	}

	@Test
	@DisplayName("0x5f starting a token")
	public void test223() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("_a");
		} );
	}

	@Test
	@DisplayName("0x60 starting a token")
	public void test224() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("`a");
		} );
	}

	@Test
	@DisplayName("0x61 starting a token")
	public void test225() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("aa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 starting a token")
	public void test226() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ba");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ba\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 starting a token")
	public void test227() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ca");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ca\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 starting a token")
	public void test228() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("da");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"da\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 starting a token")
	public void test229() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ea");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ea\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 starting a token")
	public void test230() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("fa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"fa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 starting a token")
	public void test231() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ga");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ga\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 starting a token")
	public void test232() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ha");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ha\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 starting a token")
	public void test233() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ia");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ia\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a starting a token")
	public void test234() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ja");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ja\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b starting a token")
	public void test235() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ka");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ka\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c starting a token")
	public void test236() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("la");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"la\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d starting a token")
	public void test237() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ma");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ma\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e starting a token")
	public void test238() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("na");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"na\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f starting a token")
	public void test239() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("oa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"oa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 starting a token")
	public void test240() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("pa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"pa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 starting a token")
	public void test241() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("qa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"qa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 starting a token")
	public void test242() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ra");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ra\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 starting a token")
	public void test243() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("sa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"sa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 starting a token")
	public void test244() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ta");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ta\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 starting a token")
	public void test245() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ua");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ua\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 starting a token")
	public void test246() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("va");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"va\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 starting a token")
	public void test247() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("wa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"wa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 starting a token")
	public void test248() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("xa");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"xa\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 starting a token")
	public void test249() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("ya");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ya\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a starting a token")
	public void test250() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("za");
		assertEquals(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"za\"},[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b starting a token")
	public void test251() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("{a");
		} );
	}

	@Test
	@DisplayName("0x7c starting a token")
	public void test252() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("|a");
		} );
	}

	@Test
	@DisplayName("0x7d starting a token")
	public void test253() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("}a");
		} );
	}

	@Test
	@DisplayName("0x7e starting a token")
	public void test254() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("~a");
		} );
	}

	@Test
	@DisplayName("0x7f starting a token")
	public void test255() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("\u007fa");
		} );
	}
}
