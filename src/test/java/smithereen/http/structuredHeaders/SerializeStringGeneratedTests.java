package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeStringGeneratedTests {
	@Test
	@DisplayName("0x20 in string")
	public void test32() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"   \",[]]"));
		assertEquals("\"   \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x21 in string")
	public void test33() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ! \",[]]"));
		assertEquals("\" ! \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x23 in string")
	public void test35() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" # \",[]]"));
		assertEquals("\" # \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x24 in string")
	public void test36() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" $ \",[]]"));
		assertEquals("\" $ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x25 in string")
	public void test37() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" % \",[]]"));
		assertEquals("\" % \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x26 in string")
	public void test38() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" & \",[]]"));
		assertEquals("\" & \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x27 in string")
	public void test39() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ' \",[]]"));
		assertEquals("\" ' \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x28 in string")
	public void test40() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ( \",[]]"));
		assertEquals("\" ( \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x29 in string")
	public void test41() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ) \",[]]"));
		assertEquals("\" ) \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a in string")
	public void test42() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" * \",[]]"));
		assertEquals("\" * \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2b in string")
	public void test43() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" + \",[]]"));
		assertEquals("\" + \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2c in string")
	public void test44() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" , \",[]]"));
		assertEquals("\" , \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2d in string")
	public void test45() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" - \",[]]"));
		assertEquals("\" - \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2e in string")
	public void test46() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" . \",[]]"));
		assertEquals("\" . \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2f in string")
	public void test47() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" / \",[]]"));
		assertEquals("\" / \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x30 in string")
	public void test48() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 0 \",[]]"));
		assertEquals("\" 0 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x31 in string")
	public void test49() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 1 \",[]]"));
		assertEquals("\" 1 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x32 in string")
	public void test50() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 2 \",[]]"));
		assertEquals("\" 2 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x33 in string")
	public void test51() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 3 \",[]]"));
		assertEquals("\" 3 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x34 in string")
	public void test52() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 4 \",[]]"));
		assertEquals("\" 4 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x35 in string")
	public void test53() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 5 \",[]]"));
		assertEquals("\" 5 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x36 in string")
	public void test54() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 6 \",[]]"));
		assertEquals("\" 6 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x37 in string")
	public void test55() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 7 \",[]]"));
		assertEquals("\" 7 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x38 in string")
	public void test56() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 8 \",[]]"));
		assertEquals("\" 8 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x39 in string")
	public void test57() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" 9 \",[]]"));
		assertEquals("\" 9 \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3a in string")
	public void test58() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" : \",[]]"));
		assertEquals("\" : \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3b in string")
	public void test59() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ; \",[]]"));
		assertEquals("\" ; \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3c in string")
	public void test60() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" < \",[]]"));
		assertEquals("\" < \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3d in string")
	public void test61() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" = \",[]]"));
		assertEquals("\" = \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3e in string")
	public void test62() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" > \",[]]"));
		assertEquals("\" > \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3f in string")
	public void test63() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ? \",[]]"));
		assertEquals("\" ? \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x40 in string")
	public void test64() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" @ \",[]]"));
		assertEquals("\" @ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x41 in string")
	public void test65() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" A \",[]]"));
		assertEquals("\" A \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x42 in string")
	public void test66() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" B \",[]]"));
		assertEquals("\" B \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x43 in string")
	public void test67() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" C \",[]]"));
		assertEquals("\" C \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x44 in string")
	public void test68() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" D \",[]]"));
		assertEquals("\" D \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x45 in string")
	public void test69() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" E \",[]]"));
		assertEquals("\" E \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x46 in string")
	public void test70() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" F \",[]]"));
		assertEquals("\" F \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x47 in string")
	public void test71() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" G \",[]]"));
		assertEquals("\" G \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x48 in string")
	public void test72() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" H \",[]]"));
		assertEquals("\" H \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x49 in string")
	public void test73() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" I \",[]]"));
		assertEquals("\" I \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4a in string")
	public void test74() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" J \",[]]"));
		assertEquals("\" J \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4b in string")
	public void test75() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" K \",[]]"));
		assertEquals("\" K \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4c in string")
	public void test76() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" L \",[]]"));
		assertEquals("\" L \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4d in string")
	public void test77() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" M \",[]]"));
		assertEquals("\" M \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4e in string")
	public void test78() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" N \",[]]"));
		assertEquals("\" N \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x4f in string")
	public void test79() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" O \",[]]"));
		assertEquals("\" O \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x50 in string")
	public void test80() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" P \",[]]"));
		assertEquals("\" P \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x51 in string")
	public void test81() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" Q \",[]]"));
		assertEquals("\" Q \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x52 in string")
	public void test82() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" R \",[]]"));
		assertEquals("\" R \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x53 in string")
	public void test83() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" S \",[]]"));
		assertEquals("\" S \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x54 in string")
	public void test84() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" T \",[]]"));
		assertEquals("\" T \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x55 in string")
	public void test85() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" U \",[]]"));
		assertEquals("\" U \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x56 in string")
	public void test86() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" V \",[]]"));
		assertEquals("\" V \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x57 in string")
	public void test87() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" W \",[]]"));
		assertEquals("\" W \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x58 in string")
	public void test88() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" X \",[]]"));
		assertEquals("\" X \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x59 in string")
	public void test89() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" Y \",[]]"));
		assertEquals("\" Y \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5a in string")
	public void test90() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" Z \",[]]"));
		assertEquals("\" Z \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5b in string")
	public void test91() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" [ \",[]]"));
		assertEquals("\" [ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5d in string")
	public void test93() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ] \",[]]"));
		assertEquals("\" ] \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5e in string")
	public void test94() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ^ \",[]]"));
		assertEquals("\" ^ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5f in string")
	public void test95() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" _ \",[]]"));
		assertEquals("\" _ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x60 in string")
	public void test96() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ` \",[]]"));
		assertEquals("\" ` \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 in string")
	public void test97() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" a \",[]]"));
		assertEquals("\" a \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 in string")
	public void test98() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" b \",[]]"));
		assertEquals("\" b \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 in string")
	public void test99() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" c \",[]]"));
		assertEquals("\" c \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 in string")
	public void test100() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" d \",[]]"));
		assertEquals("\" d \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 in string")
	public void test101() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" e \",[]]"));
		assertEquals("\" e \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 in string")
	public void test102() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" f \",[]]"));
		assertEquals("\" f \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 in string")
	public void test103() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" g \",[]]"));
		assertEquals("\" g \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 in string")
	public void test104() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" h \",[]]"));
		assertEquals("\" h \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 in string")
	public void test105() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" i \",[]]"));
		assertEquals("\" i \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a in string")
	public void test106() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" j \",[]]"));
		assertEquals("\" j \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b in string")
	public void test107() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" k \",[]]"));
		assertEquals("\" k \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c in string")
	public void test108() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" l \",[]]"));
		assertEquals("\" l \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d in string")
	public void test109() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" m \",[]]"));
		assertEquals("\" m \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e in string")
	public void test110() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" n \",[]]"));
		assertEquals("\" n \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f in string")
	public void test111() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" o \",[]]"));
		assertEquals("\" o \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 in string")
	public void test112() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" p \",[]]"));
		assertEquals("\" p \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 in string")
	public void test113() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" q \",[]]"));
		assertEquals("\" q \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 in string")
	public void test114() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" r \",[]]"));
		assertEquals("\" r \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 in string")
	public void test115() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" s \",[]]"));
		assertEquals("\" s \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 in string")
	public void test116() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" t \",[]]"));
		assertEquals("\" t \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 in string")
	public void test117() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" u \",[]]"));
		assertEquals("\" u \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 in string")
	public void test118() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" v \",[]]"));
		assertEquals("\" v \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 in string")
	public void test119() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" w \",[]]"));
		assertEquals("\" w \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 in string")
	public void test120() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" x \",[]]"));
		assertEquals("\" x \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 in string")
	public void test121() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" y \",[]]"));
		assertEquals("\" y \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a in string")
	public void test122() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" z \",[]]"));
		assertEquals("\" z \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7b in string")
	public void test123() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" { \",[]]"));
		assertEquals("\" { \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7c in string")
	public void test124() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" | \",[]]"));
		assertEquals("\" | \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7d in string")
	public void test125() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" } \",[]]"));
		assertEquals("\" } \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7e in string")
	public void test126() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\" ~ \",[]]"));
		assertEquals("\" ~ \"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Escaped 0x22 in string")
	public void test162() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\\"\",[]]"));
		assertEquals("\"\\\"\"", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("Escaped 0x5c in string")
	public void test220() {
		StructuredHttpHeaders.Item source=StructuredHeadersTestUtils.itemFromJson(JsonParser.parseString("[\"\\\\\",[]]"));
		assertEquals("\"\\\\\"", StructuredHttpHeaders.serialize(source));
	}
}
