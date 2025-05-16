package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeTokenGeneratedTests {
	@Test
	@DisplayName("0x21 in token")
	public void test33() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a!a\"},[]]"));
		assertEquals("a!a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x23 in token")
	public void test35() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a#a\"},[]]"));
		assertEquals("a#a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x24 in token")
	public void test36() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a$a\"},[]]"));
		assertEquals("a$a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x25 in token")
	public void test37() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a%a\"},[]]"));
		assertEquals("a%a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x26 in token")
	public void test38() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a&a\"},[]]"));
		assertEquals("a&a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x27 in token")
	public void test39() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a'a\"},[]]"));
		assertEquals("a'a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a in token")
	public void test42() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a*a\"},[]]"));
		assertEquals("a*a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2b in token")
	public void test43() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a+a\"},[]]"));
		assertEquals("a+a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2d in token")
	public void test45() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a-a\"},[]]"));
		assertEquals("a-a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2e in token")
	public void test46() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a.a\"},[]]"));
		assertEquals("a.a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2f in token")
	public void test47() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a/a\"},[]]"));
		assertEquals("a/a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x30 in token")
	public void test48() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a0a\"},[]]"));
		assertEquals("a0a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x31 in token")
	public void test49() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a1a\"},[]]"));
		assertEquals("a1a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x32 in token")
	public void test50() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a2a\"},[]]"));
		assertEquals("a2a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x33 in token")
	public void test51() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a3a\"},[]]"));
		assertEquals("a3a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x34 in token")
	public void test52() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a4a\"},[]]"));
		assertEquals("a4a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x35 in token")
	public void test53() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a5a\"},[]]"));
		assertEquals("a5a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x36 in token")
	public void test54() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a6a\"},[]]"));
		assertEquals("a6a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x37 in token")
	public void test55() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a7a\"},[]]"));
		assertEquals("a7a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x38 in token")
	public void test56() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a8a\"},[]]"));
		assertEquals("a8a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x39 in token")
	public void test57() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a9a\"},[]]"));
		assertEquals("a9a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3a in token")
	public void test58() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a:a\"},[]]"));
		assertEquals("a:a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3b in token")
	public void test59() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\"},[[\"a\",true]]]"));
		assertEquals("a;a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x41 in token")
	public void test65() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aAa\"},[]]"));
		assertEquals("aAa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x42 in token")
	public void test66() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aBa\"},[]]"));
		assertEquals("aBa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x43 in token")
	public void test67() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aCa\"},[]]"));
		assertEquals("aCa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x44 in token")
	public void test68() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aDa\"},[]]"));
		assertEquals("aDa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x45 in token")
	public void test69() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aEa\"},[]]"));
		assertEquals("aEa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x46 in token")
	public void test70() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aFa\"},[]]"));
		assertEquals("aFa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x47 in token")
	public void test71() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aGa\"},[]]"));
		assertEquals("aGa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x48 in token")
	public void test72() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aHa\"},[]]"));
		assertEquals("aHa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x49 in token")
	public void test73() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aIa\"},[]]"));
		assertEquals("aIa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4a in token")
	public void test74() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aJa\"},[]]"));
		assertEquals("aJa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4b in token")
	public void test75() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aKa\"},[]]"));
		assertEquals("aKa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4c in token")
	public void test76() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aLa\"},[]]"));
		assertEquals("aLa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4d in token")
	public void test77() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aMa\"},[]]"));
		assertEquals("aMa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4e in token")
	public void test78() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aNa\"},[]]"));
		assertEquals("aNa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4f in token")
	public void test79() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aOa\"},[]]"));
		assertEquals("aOa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x50 in token")
	public void test80() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aPa\"},[]]"));
		assertEquals("aPa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x51 in token")
	public void test81() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aQa\"},[]]"));
		assertEquals("aQa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x52 in token")
	public void test82() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aRa\"},[]]"));
		assertEquals("aRa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x53 in token")
	public void test83() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aSa\"},[]]"));
		assertEquals("aSa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x54 in token")
	public void test84() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aTa\"},[]]"));
		assertEquals("aTa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x55 in token")
	public void test85() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aUa\"},[]]"));
		assertEquals("aUa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x56 in token")
	public void test86() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aVa\"},[]]"));
		assertEquals("aVa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x57 in token")
	public void test87() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aWa\"},[]]"));
		assertEquals("aWa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x58 in token")
	public void test88() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aXa\"},[]]"));
		assertEquals("aXa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x59 in token")
	public void test89() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aYa\"},[]]"));
		assertEquals("aYa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5a in token")
	public void test90() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aZa\"},[]]"));
		assertEquals("aZa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5e in token")
	public void test94() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a^a\"},[]]"));
		assertEquals("a^a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5f in token")
	public void test95() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a_a\"},[]]"));
		assertEquals("a_a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x60 in token")
	public void test96() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a`a\"},[]]"));
		assertEquals("a`a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 in token")
	public void test97() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aaa\"},[]]"));
		assertEquals("aaa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 in token")
	public void test98() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aba\"},[]]"));
		assertEquals("aba", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 in token")
	public void test99() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aca\"},[]]"));
		assertEquals("aca", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 in token")
	public void test100() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ada\"},[]]"));
		assertEquals("ada", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 in token")
	public void test101() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aea\"},[]]"));
		assertEquals("aea", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 in token")
	public void test102() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"afa\"},[]]"));
		assertEquals("afa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 in token")
	public void test103() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aga\"},[]]"));
		assertEquals("aga", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 in token")
	public void test104() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aha\"},[]]"));
		assertEquals("aha", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 in token")
	public void test105() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aia\"},[]]"));
		assertEquals("aia", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a in token")
	public void test106() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aja\"},[]]"));
		assertEquals("aja", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b in token")
	public void test107() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aka\"},[]]"));
		assertEquals("aka", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c in token")
	public void test108() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ala\"},[]]"));
		assertEquals("ala", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d in token")
	public void test109() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ama\"},[]]"));
		assertEquals("ama", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e in token")
	public void test110() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ana\"},[]]"));
		assertEquals("ana", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f in token")
	public void test111() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aoa\"},[]]"));
		assertEquals("aoa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 in token")
	public void test112() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"apa\"},[]]"));
		assertEquals("apa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 in token")
	public void test113() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aqa\"},[]]"));
		assertEquals("aqa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 in token")
	public void test114() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ara\"},[]]"));
		assertEquals("ara", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 in token")
	public void test115() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"asa\"},[]]"));
		assertEquals("asa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 in token")
	public void test116() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ata\"},[]]"));
		assertEquals("ata", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 in token")
	public void test117() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aua\"},[]]"));
		assertEquals("aua", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 in token")
	public void test118() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ava\"},[]]"));
		assertEquals("ava", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 in token")
	public void test119() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"awa\"},[]]"));
		assertEquals("awa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 in token")
	public void test120() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"axa\"},[]]"));
		assertEquals("axa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 in token")
	public void test121() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aya\"},[]]"));
		assertEquals("aya", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a in token")
	public void test122() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aza\"},[]]"));
		assertEquals("aza", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7c in token")
	public void test124() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a|a\"},[]]"));
		assertEquals("a|a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7e in token")
	public void test126() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a~a\"},[]]"));
		assertEquals("a~a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x20 starting a token")
	public void test160() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"a\"},[]]"));
		assertEquals("a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a starting a token")
	public void test170() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"*a\"},[]]"));
		assertEquals("*a", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x41 starting a token")
	public void test193() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Aa\"},[]]"));
		assertEquals("Aa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x42 starting a token")
	public void test194() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ba\"},[]]"));
		assertEquals("Ba", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x43 starting a token")
	public void test195() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ca\"},[]]"));
		assertEquals("Ca", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x44 starting a token")
	public void test196() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Da\"},[]]"));
		assertEquals("Da", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x45 starting a token")
	public void test197() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ea\"},[]]"));
		assertEquals("Ea", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x46 starting a token")
	public void test198() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Fa\"},[]]"));
		assertEquals("Fa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x47 starting a token")
	public void test199() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ga\"},[]]"));
		assertEquals("Ga", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x48 starting a token")
	public void test200() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ha\"},[]]"));
		assertEquals("Ha", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x49 starting a token")
	public void test201() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ia\"},[]]"));
		assertEquals("Ia", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4a starting a token")
	public void test202() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ja\"},[]]"));
		assertEquals("Ja", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4b starting a token")
	public void test203() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ka\"},[]]"));
		assertEquals("Ka", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4c starting a token")
	public void test204() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"La\"},[]]"));
		assertEquals("La", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4d starting a token")
	public void test205() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ma\"},[]]"));
		assertEquals("Ma", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4e starting a token")
	public void test206() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Na\"},[]]"));
		assertEquals("Na", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4f starting a token")
	public void test207() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Oa\"},[]]"));
		assertEquals("Oa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x50 starting a token")
	public void test208() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Pa\"},[]]"));
		assertEquals("Pa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x51 starting a token")
	public void test209() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Qa\"},[]]"));
		assertEquals("Qa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x52 starting a token")
	public void test210() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ra\"},[]]"));
		assertEquals("Ra", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x53 starting a token")
	public void test211() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Sa\"},[]]"));
		assertEquals("Sa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x54 starting a token")
	public void test212() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ta\"},[]]"));
		assertEquals("Ta", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x55 starting a token")
	public void test213() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ua\"},[]]"));
		assertEquals("Ua", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x56 starting a token")
	public void test214() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Va\"},[]]"));
		assertEquals("Va", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x57 starting a token")
	public void test215() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Wa\"},[]]"));
		assertEquals("Wa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x58 starting a token")
	public void test216() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Xa\"},[]]"));
		assertEquals("Xa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x59 starting a token")
	public void test217() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Ya\"},[]]"));
		assertEquals("Ya", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5a starting a token")
	public void test218() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"Za\"},[]]"));
		assertEquals("Za", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 starting a token")
	public void test225() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"aa\"},[]]"));
		assertEquals("aa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 starting a token")
	public void test226() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ba\"},[]]"));
		assertEquals("ba", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 starting a token")
	public void test227() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ca\"},[]]"));
		assertEquals("ca", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 starting a token")
	public void test228() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"da\"},[]]"));
		assertEquals("da", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 starting a token")
	public void test229() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ea\"},[]]"));
		assertEquals("ea", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 starting a token")
	public void test230() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"fa\"},[]]"));
		assertEquals("fa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 starting a token")
	public void test231() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ga\"},[]]"));
		assertEquals("ga", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 starting a token")
	public void test232() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ha\"},[]]"));
		assertEquals("ha", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 starting a token")
	public void test233() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ia\"},[]]"));
		assertEquals("ia", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a starting a token")
	public void test234() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ja\"},[]]"));
		assertEquals("ja", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b starting a token")
	public void test235() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ka\"},[]]"));
		assertEquals("ka", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c starting a token")
	public void test236() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"la\"},[]]"));
		assertEquals("la", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d starting a token")
	public void test237() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ma\"},[]]"));
		assertEquals("ma", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e starting a token")
	public void test238() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"na\"},[]]"));
		assertEquals("na", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f starting a token")
	public void test239() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"oa\"},[]]"));
		assertEquals("oa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 starting a token")
	public void test240() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"pa\"},[]]"));
		assertEquals("pa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 starting a token")
	public void test241() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"qa\"},[]]"));
		assertEquals("qa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 starting a token")
	public void test242() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ra\"},[]]"));
		assertEquals("ra", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 starting a token")
	public void test243() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"sa\"},[]]"));
		assertEquals("sa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 starting a token")
	public void test244() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ta\"},[]]"));
		assertEquals("ta", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 starting a token")
	public void test245() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ua\"},[]]"));
		assertEquals("ua", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 starting a token")
	public void test246() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"va\"},[]]"));
		assertEquals("va", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 starting a token")
	public void test247() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"wa\"},[]]"));
		assertEquals("wa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 starting a token")
	public void test248() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"xa\"},[]]"));
		assertEquals("xa", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 starting a token")
	public void test249() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"ya\"},[]]"));
		assertEquals("ya", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a starting a token")
	public void test250() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[{\"__type\":\"token\",\"value\":\"za\"},[]]"));
		assertEquals("za", StructuredHttpHeaders.serialize(source));
	}
}
