package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.String;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeKeyGeneratedTests {
	@Test
	@DisplayName("0x2a as a single-character dictionary key")
	public void test42() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"*\",[1,[]]]]"));
		assertEquals("*=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 as a single-character dictionary key")
	public void test97() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]]]"));
		assertEquals("a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 as a single-character dictionary key")
	public void test98() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"b\",[1,[]]]]"));
		assertEquals("b=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 as a single-character dictionary key")
	public void test99() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"c\",[1,[]]]]"));
		assertEquals("c=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 as a single-character dictionary key")
	public void test100() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"d\",[1,[]]]]"));
		assertEquals("d=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 as a single-character dictionary key")
	public void test101() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"e\",[1,[]]]]"));
		assertEquals("e=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 as a single-character dictionary key")
	public void test102() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"f\",[1,[]]]]"));
		assertEquals("f=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 as a single-character dictionary key")
	public void test103() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"g\",[1,[]]]]"));
		assertEquals("g=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 as a single-character dictionary key")
	public void test104() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"h\",[1,[]]]]"));
		assertEquals("h=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 as a single-character dictionary key")
	public void test105() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"i\",[1,[]]]]"));
		assertEquals("i=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a as a single-character dictionary key")
	public void test106() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"j\",[1,[]]]]"));
		assertEquals("j=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b as a single-character dictionary key")
	public void test107() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"k\",[1,[]]]]"));
		assertEquals("k=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c as a single-character dictionary key")
	public void test108() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"l\",[1,[]]]]"));
		assertEquals("l=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d as a single-character dictionary key")
	public void test109() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"m\",[1,[]]]]"));
		assertEquals("m=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e as a single-character dictionary key")
	public void test110() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"n\",[1,[]]]]"));
		assertEquals("n=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f as a single-character dictionary key")
	public void test111() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"o\",[1,[]]]]"));
		assertEquals("o=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 as a single-character dictionary key")
	public void test112() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"p\",[1,[]]]]"));
		assertEquals("p=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 as a single-character dictionary key")
	public void test113() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"q\",[1,[]]]]"));
		assertEquals("q=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 as a single-character dictionary key")
	public void test114() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"r\",[1,[]]]]"));
		assertEquals("r=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 as a single-character dictionary key")
	public void test115() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"s\",[1,[]]]]"));
		assertEquals("s=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 as a single-character dictionary key")
	public void test116() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"t\",[1,[]]]]"));
		assertEquals("t=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 as a single-character dictionary key")
	public void test117() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"u\",[1,[]]]]"));
		assertEquals("u=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 as a single-character dictionary key")
	public void test118() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"v\",[1,[]]]]"));
		assertEquals("v=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 as a single-character dictionary key")
	public void test119() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"w\",[1,[]]]]"));
		assertEquals("w=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 as a single-character dictionary key")
	public void test120() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"x\",[1,[]]]]"));
		assertEquals("x=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 as a single-character dictionary key")
	public void test121() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"y\",[1,[]]]]"));
		assertEquals("y=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a as a single-character dictionary key")
	public void test122() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"z\",[1,[]]]]"));
		assertEquals("z=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a in dictionary key")
	public void test170() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a*a\",[1,[]]]]"));
		assertEquals("a*a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2c in dictionary key")
	public void test172() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]]]"));
		assertEquals("a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2d in dictionary key")
	public void test173() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a-a\",[1,[]]]]"));
		assertEquals("a-a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2e in dictionary key")
	public void test174() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a.a\",[1,[]]]]"));
		assertEquals("a.a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x30 in dictionary key")
	public void test176() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a0a\",[1,[]]]]"));
		assertEquals("a0a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x31 in dictionary key")
	public void test177() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a1a\",[1,[]]]]"));
		assertEquals("a1a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x32 in dictionary key")
	public void test178() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a2a\",[1,[]]]]"));
		assertEquals("a2a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x33 in dictionary key")
	public void test179() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a3a\",[1,[]]]]"));
		assertEquals("a3a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x34 in dictionary key")
	public void test180() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a4a\",[1,[]]]]"));
		assertEquals("a4a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x35 in dictionary key")
	public void test181() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a5a\",[1,[]]]]"));
		assertEquals("a5a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x36 in dictionary key")
	public void test182() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a6a\",[1,[]]]]"));
		assertEquals("a6a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x37 in dictionary key")
	public void test183() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a7a\",[1,[]]]]"));
		assertEquals("a7a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x38 in dictionary key")
	public void test184() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a8a\",[1,[]]]]"));
		assertEquals("a8a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x39 in dictionary key")
	public void test185() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a9a\",[1,[]]]]"));
		assertEquals("a9a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3b in dictionary key")
	public void test187() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[true,[[\"a\",1]]]]]"));
		assertEquals("a;a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5f in dictionary key")
	public void test223() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a_a\",[1,[]]]]"));
		assertEquals("a_a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 in dictionary key")
	public void test225() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aaa\",[1,[]]]]"));
		assertEquals("aaa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 in dictionary key")
	public void test226() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aba\",[1,[]]]]"));
		assertEquals("aba=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 in dictionary key")
	public void test227() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aca\",[1,[]]]]"));
		assertEquals("aca=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 in dictionary key")
	public void test228() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ada\",[1,[]]]]"));
		assertEquals("ada=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 in dictionary key")
	public void test229() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aea\",[1,[]]]]"));
		assertEquals("aea=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 in dictionary key")
	public void test230() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"afa\",[1,[]]]]"));
		assertEquals("afa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 in dictionary key")
	public void test231() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aga\",[1,[]]]]"));
		assertEquals("aga=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 in dictionary key")
	public void test232() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aha\",[1,[]]]]"));
		assertEquals("aha=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 in dictionary key")
	public void test233() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aia\",[1,[]]]]"));
		assertEquals("aia=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a in dictionary key")
	public void test234() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aja\",[1,[]]]]"));
		assertEquals("aja=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b in dictionary key")
	public void test235() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aka\",[1,[]]]]"));
		assertEquals("aka=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c in dictionary key")
	public void test236() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ala\",[1,[]]]]"));
		assertEquals("ala=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d in dictionary key")
	public void test237() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ama\",[1,[]]]]"));
		assertEquals("ama=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e in dictionary key")
	public void test238() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ana\",[1,[]]]]"));
		assertEquals("ana=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f in dictionary key")
	public void test239() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aoa\",[1,[]]]]"));
		assertEquals("aoa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 in dictionary key")
	public void test240() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"apa\",[1,[]]]]"));
		assertEquals("apa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 in dictionary key")
	public void test241() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aqa\",[1,[]]]]"));
		assertEquals("aqa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 in dictionary key")
	public void test242() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ara\",[1,[]]]]"));
		assertEquals("ara=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 in dictionary key")
	public void test243() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"asa\",[1,[]]]]"));
		assertEquals("asa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 in dictionary key")
	public void test244() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ata\",[1,[]]]]"));
		assertEquals("ata=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 in dictionary key")
	public void test245() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aua\",[1,[]]]]"));
		assertEquals("aua=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 in dictionary key")
	public void test246() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ava\",[1,[]]]]"));
		assertEquals("ava=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 in dictionary key")
	public void test247() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"awa\",[1,[]]]]"));
		assertEquals("awa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 in dictionary key")
	public void test248() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"axa\",[1,[]]]]"));
		assertEquals("axa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 in dictionary key")
	public void test249() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aya\",[1,[]]]]"));
		assertEquals("aya=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a in dictionary key")
	public void test250() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aza\",[1,[]]]]"));
		assertEquals("aza=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x20 starting a dictionary key")
	public void test288() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\",[1,[]]]]"));
		assertEquals("a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a starting a dictionary key")
	public void test298() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"*a\",[1,[]]]]"));
		assertEquals("*a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 starting a dictionary key")
	public void test353() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aa\",[1,[]]]]"));
		assertEquals("aa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 starting a dictionary key")
	public void test354() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ba\",[1,[]]]]"));
		assertEquals("ba=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 starting a dictionary key")
	public void test355() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ca\",[1,[]]]]"));
		assertEquals("ca=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 starting a dictionary key")
	public void test356() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"da\",[1,[]]]]"));
		assertEquals("da=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 starting a dictionary key")
	public void test357() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ea\",[1,[]]]]"));
		assertEquals("ea=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 starting a dictionary key")
	public void test358() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"fa\",[1,[]]]]"));
		assertEquals("fa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 starting a dictionary key")
	public void test359() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ga\",[1,[]]]]"));
		assertEquals("ga=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 starting a dictionary key")
	public void test360() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ha\",[1,[]]]]"));
		assertEquals("ha=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 starting a dictionary key")
	public void test361() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ia\",[1,[]]]]"));
		assertEquals("ia=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a starting a dictionary key")
	public void test362() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ja\",[1,[]]]]"));
		assertEquals("ja=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b starting a dictionary key")
	public void test363() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ka\",[1,[]]]]"));
		assertEquals("ka=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c starting a dictionary key")
	public void test364() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"la\",[1,[]]]]"));
		assertEquals("la=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d starting a dictionary key")
	public void test365() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ma\",[1,[]]]]"));
		assertEquals("ma=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e starting a dictionary key")
	public void test366() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"na\",[1,[]]]]"));
		assertEquals("na=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f starting a dictionary key")
	public void test367() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"oa\",[1,[]]]]"));
		assertEquals("oa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 starting a dictionary key")
	public void test368() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"pa\",[1,[]]]]"));
		assertEquals("pa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 starting a dictionary key")
	public void test369() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"qa\",[1,[]]]]"));
		assertEquals("qa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 starting a dictionary key")
	public void test370() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ra\",[1,[]]]]"));
		assertEquals("ra=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 starting a dictionary key")
	public void test371() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"sa\",[1,[]]]]"));
		assertEquals("sa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 starting a dictionary key")
	public void test372() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ta\",[1,[]]]]"));
		assertEquals("ta=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 starting a dictionary key")
	public void test373() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ua\",[1,[]]]]"));
		assertEquals("ua=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 starting a dictionary key")
	public void test374() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"va\",[1,[]]]]"));
		assertEquals("va=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 starting a dictionary key")
	public void test375() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"wa\",[1,[]]]]"));
		assertEquals("wa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 starting a dictionary key")
	public void test376() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"xa\",[1,[]]]]"));
		assertEquals("xa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 starting a dictionary key")
	public void test377() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"ya\",[1,[]]]]"));
		assertEquals("ya=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a starting a dictionary key")
	public void test378() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"za\",[1,[]]]]"));
		assertEquals("za=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a in parameterised list key")
	public void test426() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a*a\",1]]]]"));
		assertEquals("foo;a*a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2d in parameterised list key")
	public void test429() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a-a\",1]]]]"));
		assertEquals("foo;a-a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2e in parameterised list key")
	public void test430() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a.a\",1]]]]"));
		assertEquals("foo;a.a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x30 in parameterised list key")
	public void test432() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a0a\",1]]]]"));
		assertEquals("foo;a0a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x31 in parameterised list key")
	public void test433() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a1a\",1]]]]"));
		assertEquals("foo;a1a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x32 in parameterised list key")
	public void test434() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a2a\",1]]]]"));
		assertEquals("foo;a2a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x33 in parameterised list key")
	public void test435() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a3a\",1]]]]"));
		assertEquals("foo;a3a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x34 in parameterised list key")
	public void test436() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a4a\",1]]]]"));
		assertEquals("foo;a4a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x35 in parameterised list key")
	public void test437() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a5a\",1]]]]"));
		assertEquals("foo;a5a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x36 in parameterised list key")
	public void test438() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a6a\",1]]]]"));
		assertEquals("foo;a6a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x37 in parameterised list key")
	public void test439() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a7a\",1]]]]"));
		assertEquals("foo;a7a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x38 in parameterised list key")
	public void test440() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a8a\",1]]]]"));
		assertEquals("foo;a8a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x39 in parameterised list key")
	public void test441() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a9a\",1]]]]"));
		assertEquals("foo;a9a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x3b in parameterised list key")
	public void test443() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\",1]]]]"));
		assertEquals("foo;a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x5f in parameterised list key")
	public void test479() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a_a\",1]]]]"));
		assertEquals("foo;a_a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 in parameterised list key")
	public void test481() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aaa\",1]]]]"));
		assertEquals("foo;aaa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 in parameterised list key")
	public void test482() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aba\",1]]]]"));
		assertEquals("foo;aba=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 in parameterised list key")
	public void test483() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aca\",1]]]]"));
		assertEquals("foo;aca=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 in parameterised list key")
	public void test484() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ada\",1]]]]"));
		assertEquals("foo;ada=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 in parameterised list key")
	public void test485() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aea\",1]]]]"));
		assertEquals("foo;aea=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 in parameterised list key")
	public void test486() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"afa\",1]]]]"));
		assertEquals("foo;afa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 in parameterised list key")
	public void test487() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aga\",1]]]]"));
		assertEquals("foo;aga=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 in parameterised list key")
	public void test488() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aha\",1]]]]"));
		assertEquals("foo;aha=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 in parameterised list key")
	public void test489() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aia\",1]]]]"));
		assertEquals("foo;aia=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a in parameterised list key")
	public void test490() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aja\",1]]]]"));
		assertEquals("foo;aja=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b in parameterised list key")
	public void test491() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aka\",1]]]]"));
		assertEquals("foo;aka=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c in parameterised list key")
	public void test492() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ala\",1]]]]"));
		assertEquals("foo;ala=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d in parameterised list key")
	public void test493() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ama\",1]]]]"));
		assertEquals("foo;ama=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e in parameterised list key")
	public void test494() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ana\",1]]]]"));
		assertEquals("foo;ana=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f in parameterised list key")
	public void test495() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aoa\",1]]]]"));
		assertEquals("foo;aoa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 in parameterised list key")
	public void test496() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"apa\",1]]]]"));
		assertEquals("foo;apa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 in parameterised list key")
	public void test497() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aqa\",1]]]]"));
		assertEquals("foo;aqa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 in parameterised list key")
	public void test498() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ara\",1]]]]"));
		assertEquals("foo;ara=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 in parameterised list key")
	public void test499() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"asa\",1]]]]"));
		assertEquals("foo;asa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 in parameterised list key")
	public void test500() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ata\",1]]]]"));
		assertEquals("foo;ata=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 in parameterised list key")
	public void test501() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aua\",1]]]]"));
		assertEquals("foo;aua=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 in parameterised list key")
	public void test502() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ava\",1]]]]"));
		assertEquals("foo;ava=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 in parameterised list key")
	public void test503() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"awa\",1]]]]"));
		assertEquals("foo;awa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 in parameterised list key")
	public void test504() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"axa\",1]]]]"));
		assertEquals("foo;axa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 in parameterised list key")
	public void test505() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aya\",1]]]]"));
		assertEquals("foo;aya=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a in parameterised list key")
	public void test506() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aza\",1]]]]"));
		assertEquals("foo;aza=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x20 starting a parameterised list key")
	public void test544() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\",1]]]]"));
		assertEquals("foo;a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x2a starting a parameterised list key")
	public void test554() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"*a\",1]]]]"));
		assertEquals("foo;*a=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x61 starting a parameterised list key")
	public void test609() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aa\",1]]]]"));
		assertEquals("foo;aa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x62 starting a parameterised list key")
	public void test610() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ba\",1]]]]"));
		assertEquals("foo;ba=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x63 starting a parameterised list key")
	public void test611() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ca\",1]]]]"));
		assertEquals("foo;ca=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x64 starting a parameterised list key")
	public void test612() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"da\",1]]]]"));
		assertEquals("foo;da=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x65 starting a parameterised list key")
	public void test613() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ea\",1]]]]"));
		assertEquals("foo;ea=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x66 starting a parameterised list key")
	public void test614() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"fa\",1]]]]"));
		assertEquals("foo;fa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x67 starting a parameterised list key")
	public void test615() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ga\",1]]]]"));
		assertEquals("foo;ga=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x68 starting a parameterised list key")
	public void test616() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ha\",1]]]]"));
		assertEquals("foo;ha=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x69 starting a parameterised list key")
	public void test617() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ia\",1]]]]"));
		assertEquals("foo;ia=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6a starting a parameterised list key")
	public void test618() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ja\",1]]]]"));
		assertEquals("foo;ja=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6b starting a parameterised list key")
	public void test619() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ka\",1]]]]"));
		assertEquals("foo;ka=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6c starting a parameterised list key")
	public void test620() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"la\",1]]]]"));
		assertEquals("foo;la=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6d starting a parameterised list key")
	public void test621() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ma\",1]]]]"));
		assertEquals("foo;ma=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6e starting a parameterised list key")
	public void test622() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"na\",1]]]]"));
		assertEquals("foo;na=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x6f starting a parameterised list key")
	public void test623() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"oa\",1]]]]"));
		assertEquals("foo;oa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x70 starting a parameterised list key")
	public void test624() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"pa\",1]]]]"));
		assertEquals("foo;pa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x71 starting a parameterised list key")
	public void test625() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"qa\",1]]]]"));
		assertEquals("foo;qa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x72 starting a parameterised list key")
	public void test626() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ra\",1]]]]"));
		assertEquals("foo;ra=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x73 starting a parameterised list key")
	public void test627() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"sa\",1]]]]"));
		assertEquals("foo;sa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x74 starting a parameterised list key")
	public void test628() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ta\",1]]]]"));
		assertEquals("foo;ta=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x75 starting a parameterised list key")
	public void test629() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ua\",1]]]]"));
		assertEquals("foo;ua=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x76 starting a parameterised list key")
	public void test630() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"va\",1]]]]"));
		assertEquals("foo;va=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x77 starting a parameterised list key")
	public void test631() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"wa\",1]]]]"));
		assertEquals("foo;wa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x78 starting a parameterised list key")
	public void test632() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"xa\",1]]]]"));
		assertEquals("foo;xa=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x79 starting a parameterised list key")
	public void test633() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ya\",1]]]]"));
		assertEquals("foo;ya=1", StructuredHttpHeaders.serialize(source));
	}

	@Test
	@DisplayName("0x7a starting a parameterised list key")
	public void test634() {
		List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"za\",1]]]]"));
		assertEquals("foo;za=1", StructuredHttpHeaders.serialize(source));
	}
}
