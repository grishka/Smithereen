package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class SerializeOnlyKeyGeneratedTests {
	@Test
	@DisplayName("0x00 in dictionary key - serialise only")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0000a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 in dictionary key - serialise only")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0001a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 in dictionary key - serialise only")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0002a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 in dictionary key - serialise only")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0003a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 in dictionary key - serialise only")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0004a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 in dictionary key - serialise only")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0005a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 in dictionary key - serialise only")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0006a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 in dictionary key - serialise only")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0007a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 in dictionary key - serialise only")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 in dictionary key - serialise only")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\ta\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a in dictionary key - serialise only")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\na\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b in dictionary key - serialise only")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u000ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c in dictionary key - serialise only")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d in dictionary key - serialise only")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\ra\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e in dictionary key - serialise only")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u000ea\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f in dictionary key - serialise only")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u000fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 in dictionary key - serialise only")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0010a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 in dictionary key - serialise only")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0011a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 in dictionary key - serialise only")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0012a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 in dictionary key - serialise only")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0013a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 in dictionary key - serialise only")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0014a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 in dictionary key - serialise only")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0015a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 in dictionary key - serialise only")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0016a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 in dictionary key - serialise only")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0017a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 in dictionary key - serialise only")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0018a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 in dictionary key - serialise only")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u0019a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a in dictionary key - serialise only")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001aa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b in dictionary key - serialise only")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c in dictionary key - serialise only")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001ca\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d in dictionary key - serialise only")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001da\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e in dictionary key - serialise only")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001ea\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f in dictionary key - serialise only")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\u001fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 in dictionary key - serialise only")
	public void test32() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x21 in dictionary key - serialise only")
	public void test33() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a!a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 in dictionary key - serialise only")
	public void test34() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\\"a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x23 in dictionary key - serialise only")
	public void test35() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a#a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x24 in dictionary key - serialise only")
	public void test36() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a$a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x25 in dictionary key - serialise only")
	public void test37() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a%a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x26 in dictionary key - serialise only")
	public void test38() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a&a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x27 in dictionary key - serialise only")
	public void test39() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a'a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 in dictionary key - serialise only")
	public void test40() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a(a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 in dictionary key - serialise only")
	public void test41() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a)a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2b in dictionary key - serialise only")
	public void test42() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a+a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c in dictionary key - serialise only")
	public void test43() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a,a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2f in dictionary key - serialise only")
	public void test44() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a/a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3a in dictionary key - serialise only")
	public void test45() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a:a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b in dictionary key - serialise only")
	public void test46() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a;a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c in dictionary key - serialise only")
	public void test47() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a<a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d in dictionary key - serialise only")
	public void test48() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a=a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e in dictionary key - serialise only")
	public void test49() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a>a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f in dictionary key - serialise only")
	public void test50() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a?a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 in dictionary key - serialise only")
	public void test51() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a@a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x41 in dictionary key - serialise only")
	public void test52() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aAa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x42 in dictionary key - serialise only")
	public void test53() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aBa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x43 in dictionary key - serialise only")
	public void test54() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aCa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x44 in dictionary key - serialise only")
	public void test55() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aDa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x45 in dictionary key - serialise only")
	public void test56() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aEa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x46 in dictionary key - serialise only")
	public void test57() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aFa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x47 in dictionary key - serialise only")
	public void test58() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aGa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x48 in dictionary key - serialise only")
	public void test59() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aHa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x49 in dictionary key - serialise only")
	public void test60() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aIa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4a in dictionary key - serialise only")
	public void test61() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aJa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4b in dictionary key - serialise only")
	public void test62() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aKa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4c in dictionary key - serialise only")
	public void test63() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aLa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4d in dictionary key - serialise only")
	public void test64() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aMa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4e in dictionary key - serialise only")
	public void test65() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aNa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4f in dictionary key - serialise only")
	public void test66() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aOa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x50 in dictionary key - serialise only")
	public void test67() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aPa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x51 in dictionary key - serialise only")
	public void test68() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aQa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x52 in dictionary key - serialise only")
	public void test69() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aRa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x53 in dictionary key - serialise only")
	public void test70() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aSa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x54 in dictionary key - serialise only")
	public void test71() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aTa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x55 in dictionary key - serialise only")
	public void test72() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aUa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x56 in dictionary key - serialise only")
	public void test73() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aVa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x57 in dictionary key - serialise only")
	public void test74() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aWa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x58 in dictionary key - serialise only")
	public void test75() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aXa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x59 in dictionary key - serialise only")
	public void test76() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aYa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5a in dictionary key - serialise only")
	public void test77() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"aZa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b in dictionary key - serialise only")
	public void test78() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a[a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c in dictionary key - serialise only")
	public void test79() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\\\\a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d in dictionary key - serialise only")
	public void test80() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a]a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5e in dictionary key - serialise only")
	public void test81() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a^a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x60 in dictionary key - serialise only")
	public void test82() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a`a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b in dictionary key - serialise only")
	public void test83() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a{a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7c in dictionary key - serialise only")
	public void test84() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a|a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d in dictionary key - serialise only")
	public void test85() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a}a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7e in dictionary key - serialise only")
	public void test86() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a~a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f in dictionary key - serialise only")
	public void test87() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"a\u007fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x00 starting a dictionary key - serialise only")
	public void test88() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0000a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 starting a dictionary key - serialise only")
	public void test89() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0001a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 starting a dictionary key - serialise only")
	public void test90() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0002a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 starting a dictionary key - serialise only")
	public void test91() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0003a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 starting a dictionary key - serialise only")
	public void test92() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0004a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 starting a dictionary key - serialise only")
	public void test93() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0005a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 starting a dictionary key - serialise only")
	public void test94() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0006a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 starting a dictionary key - serialise only")
	public void test95() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0007a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 starting a dictionary key - serialise only")
	public void test96() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 starting a dictionary key - serialise only")
	public void test97() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\ta\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a starting a dictionary key - serialise only")
	public void test98() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\na\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b starting a dictionary key - serialise only")
	public void test99() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u000ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c starting a dictionary key - serialise only")
	public void test100() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d starting a dictionary key - serialise only")
	public void test101() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\ra\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e starting a dictionary key - serialise only")
	public void test102() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u000ea\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f starting a dictionary key - serialise only")
	public void test103() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u000fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 starting a dictionary key - serialise only")
	public void test104() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0010a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 starting a dictionary key - serialise only")
	public void test105() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0011a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 starting a dictionary key - serialise only")
	public void test106() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0012a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 starting a dictionary key - serialise only")
	public void test107() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0013a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 starting a dictionary key - serialise only")
	public void test108() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0014a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 starting a dictionary key - serialise only")
	public void test109() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0015a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 starting a dictionary key - serialise only")
	public void test110() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0016a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 starting a dictionary key - serialise only")
	public void test111() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0017a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 starting a dictionary key - serialise only")
	public void test112() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0018a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 starting a dictionary key - serialise only")
	public void test113() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u0019a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a starting a dictionary key - serialise only")
	public void test114() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001aa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b starting a dictionary key - serialise only")
	public void test115() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c starting a dictionary key - serialise only")
	public void test116() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001ca\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d starting a dictionary key - serialise only")
	public void test117() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001da\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e starting a dictionary key - serialise only")
	public void test118() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001ea\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f starting a dictionary key - serialise only")
	public void test119() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\u001fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 starting a dictionary key - serialise only")
	public void test120() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\" a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x21 starting a dictionary key - serialise only")
	public void test121() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"!a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 starting a dictionary key - serialise only")
	public void test122() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\\"a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x23 starting a dictionary key - serialise only")
	public void test123() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"#a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x24 starting a dictionary key - serialise only")
	public void test124() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"$a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x25 starting a dictionary key - serialise only")
	public void test125() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"%a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x26 starting a dictionary key - serialise only")
	public void test126() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"&a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x27 starting a dictionary key - serialise only")
	public void test127() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"'a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 starting a dictionary key - serialise only")
	public void test128() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"(a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 starting a dictionary key - serialise only")
	public void test129() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\")a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2b starting a dictionary key - serialise only")
	public void test130() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"+a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c starting a dictionary key - serialise only")
	public void test131() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\",a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2d starting a dictionary key - serialise only")
	public void test132() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"-a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2e starting a dictionary key - serialise only")
	public void test133() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\".a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2f starting a dictionary key - serialise only")
	public void test134() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"/a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x30 starting a dictionary key - serialise only")
	public void test135() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"0a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x31 starting a dictionary key - serialise only")
	public void test136() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"1a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x32 starting a dictionary key - serialise only")
	public void test137() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"2a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x33 starting a dictionary key - serialise only")
	public void test138() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"3a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x34 starting a dictionary key - serialise only")
	public void test139() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"4a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x35 starting a dictionary key - serialise only")
	public void test140() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"5a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x36 starting a dictionary key - serialise only")
	public void test141() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"6a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x37 starting a dictionary key - serialise only")
	public void test142() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"7a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x38 starting a dictionary key - serialise only")
	public void test143() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"8a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x39 starting a dictionary key - serialise only")
	public void test144() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"9a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3a starting a dictionary key - serialise only")
	public void test145() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\":a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b starting a dictionary key - serialise only")
	public void test146() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\";a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c starting a dictionary key - serialise only")
	public void test147() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"<a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d starting a dictionary key - serialise only")
	public void test148() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"=a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e starting a dictionary key - serialise only")
	public void test149() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\">a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f starting a dictionary key - serialise only")
	public void test150() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"?a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 starting a dictionary key - serialise only")
	public void test151() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"@a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x41 starting a dictionary key - serialise only")
	public void test152() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Aa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x42 starting a dictionary key - serialise only")
	public void test153() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ba\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x43 starting a dictionary key - serialise only")
	public void test154() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ca\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x44 starting a dictionary key - serialise only")
	public void test155() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Da\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x45 starting a dictionary key - serialise only")
	public void test156() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ea\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x46 starting a dictionary key - serialise only")
	public void test157() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x47 starting a dictionary key - serialise only")
	public void test158() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ga\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x48 starting a dictionary key - serialise only")
	public void test159() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ha\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x49 starting a dictionary key - serialise only")
	public void test160() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ia\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4a starting a dictionary key - serialise only")
	public void test161() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ja\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4b starting a dictionary key - serialise only")
	public void test162() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ka\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4c starting a dictionary key - serialise only")
	public void test163() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"La\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4d starting a dictionary key - serialise only")
	public void test164() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ma\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4e starting a dictionary key - serialise only")
	public void test165() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Na\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4f starting a dictionary key - serialise only")
	public void test166() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Oa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x50 starting a dictionary key - serialise only")
	public void test167() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Pa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x51 starting a dictionary key - serialise only")
	public void test168() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Qa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x52 starting a dictionary key - serialise only")
	public void test169() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ra\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x53 starting a dictionary key - serialise only")
	public void test170() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Sa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x54 starting a dictionary key - serialise only")
	public void test171() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ta\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x55 starting a dictionary key - serialise only")
	public void test172() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ua\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x56 starting a dictionary key - serialise only")
	public void test173() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Va\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x57 starting a dictionary key - serialise only")
	public void test174() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Wa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x58 starting a dictionary key - serialise only")
	public void test175() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Xa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x59 starting a dictionary key - serialise only")
	public void test176() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Ya\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5a starting a dictionary key - serialise only")
	public void test177() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"Za\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b starting a dictionary key - serialise only")
	public void test178() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"[a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c starting a dictionary key - serialise only")
	public void test179() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\\\\a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d starting a dictionary key - serialise only")
	public void test180() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"]a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5e starting a dictionary key - serialise only")
	public void test181() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"^a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5f starting a dictionary key - serialise only")
	public void test182() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"_a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x60 starting a dictionary key - serialise only")
	public void test183() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"`a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b starting a dictionary key - serialise only")
	public void test184() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"{a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7c starting a dictionary key - serialise only")
	public void test185() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"|a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d starting a dictionary key - serialise only")
	public void test186() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"}a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7e starting a dictionary key - serialise only")
	public void test187() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"~a\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f starting a dictionary key - serialise only")
	public void test188() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.dictionaryFromJson(JsonParser.parseString("[[\"\u007fa\",[1,[]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x00 in parameterised list key - serialise only")
	public void test189() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0000a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 in parameterised list key - serialise only")
	public void test190() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0001a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 in parameterised list key - serialise only")
	public void test191() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0002a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 in parameterised list key - serialise only")
	public void test192() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0003a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 in parameterised list key - serialise only")
	public void test193() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0004a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 in parameterised list key - serialise only")
	public void test194() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0005a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 in parameterised list key - serialise only")
	public void test195() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0006a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 in parameterised list key - serialise only")
	public void test196() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0007a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 in parameterised list key - serialise only")
	public void test197() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 in parameterised list key - serialise only")
	public void test198() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\ta\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a in parameterised list key - serialise only")
	public void test199() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\na\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b in parameterised list key - serialise only")
	public void test200() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u000ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c in parameterised list key - serialise only")
	public void test201() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d in parameterised list key - serialise only")
	public void test202() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\ra\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e in parameterised list key - serialise only")
	public void test203() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u000ea\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f in parameterised list key - serialise only")
	public void test204() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u000fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 in parameterised list key - serialise only")
	public void test205() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0010a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 in parameterised list key - serialise only")
	public void test206() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0011a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 in parameterised list key - serialise only")
	public void test207() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0012a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 in parameterised list key - serialise only")
	public void test208() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0013a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 in parameterised list key - serialise only")
	public void test209() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0014a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 in parameterised list key - serialise only")
	public void test210() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0015a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 in parameterised list key - serialise only")
	public void test211() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0016a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 in parameterised list key - serialise only")
	public void test212() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0017a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 in parameterised list key - serialise only")
	public void test213() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0018a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 in parameterised list key - serialise only")
	public void test214() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u0019a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a in parameterised list key - serialise only")
	public void test215() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001aa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b in parameterised list key - serialise only")
	public void test216() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c in parameterised list key - serialise only")
	public void test217() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001ca\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d in parameterised list key - serialise only")
	public void test218() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001da\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e in parameterised list key - serialise only")
	public void test219() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001ea\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f in parameterised list key - serialise only")
	public void test220() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\u001fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 in parameterised list key - serialise only")
	public void test221() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x21 in parameterised list key - serialise only")
	public void test222() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a!a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 in parameterised list key - serialise only")
	public void test223() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\\"a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x23 in parameterised list key - serialise only")
	public void test224() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a#a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x24 in parameterised list key - serialise only")
	public void test225() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a$a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x25 in parameterised list key - serialise only")
	public void test226() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a%a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x26 in parameterised list key - serialise only")
	public void test227() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a&a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x27 in parameterised list key - serialise only")
	public void test228() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a'a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 in parameterised list key - serialise only")
	public void test229() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a(a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 in parameterised list key - serialise only")
	public void test230() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a)a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2b in parameterised list key - serialise only")
	public void test231() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a+a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c in parameterised list key - serialise only")
	public void test232() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a,a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2f in parameterised list key - serialise only")
	public void test233() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a/a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3a in parameterised list key - serialise only")
	public void test234() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a:a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b in parameterised list key - serialise only")
	public void test235() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a;a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c in parameterised list key - serialise only")
	public void test236() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a<a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d in parameterised list key - serialise only")
	public void test237() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a=a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e in parameterised list key - serialise only")
	public void test238() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a>a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f in parameterised list key - serialise only")
	public void test239() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a?a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 in parameterised list key - serialise only")
	public void test240() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a@a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x41 in parameterised list key - serialise only")
	public void test241() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aAa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x42 in parameterised list key - serialise only")
	public void test242() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aBa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x43 in parameterised list key - serialise only")
	public void test243() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aCa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x44 in parameterised list key - serialise only")
	public void test244() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aDa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x45 in parameterised list key - serialise only")
	public void test245() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aEa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x46 in parameterised list key - serialise only")
	public void test246() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aFa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x47 in parameterised list key - serialise only")
	public void test247() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aGa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x48 in parameterised list key - serialise only")
	public void test248() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aHa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x49 in parameterised list key - serialise only")
	public void test249() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aIa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4a in parameterised list key - serialise only")
	public void test250() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aJa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4b in parameterised list key - serialise only")
	public void test251() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aKa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4c in parameterised list key - serialise only")
	public void test252() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aLa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4d in parameterised list key - serialise only")
	public void test253() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aMa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4e in parameterised list key - serialise only")
	public void test254() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aNa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4f in parameterised list key - serialise only")
	public void test255() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aOa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x50 in parameterised list key - serialise only")
	public void test256() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aPa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x51 in parameterised list key - serialise only")
	public void test257() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aQa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x52 in parameterised list key - serialise only")
	public void test258() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aRa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x53 in parameterised list key - serialise only")
	public void test259() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aSa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x54 in parameterised list key - serialise only")
	public void test260() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aTa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x55 in parameterised list key - serialise only")
	public void test261() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aUa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x56 in parameterised list key - serialise only")
	public void test262() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aVa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x57 in parameterised list key - serialise only")
	public void test263() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aWa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x58 in parameterised list key - serialise only")
	public void test264() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aXa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x59 in parameterised list key - serialise only")
	public void test265() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aYa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5a in parameterised list key - serialise only")
	public void test266() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aZa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b in parameterised list key - serialise only")
	public void test267() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a[a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c in parameterised list key - serialise only")
	public void test268() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\\\\a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d in parameterised list key - serialise only")
	public void test269() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a]a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5e in parameterised list key - serialise only")
	public void test270() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a^a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x60 in parameterised list key - serialise only")
	public void test271() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a`a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b in parameterised list key - serialise only")
	public void test272() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a{a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7c in parameterised list key - serialise only")
	public void test273() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a|a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d in parameterised list key - serialise only")
	public void test274() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a}a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7e in parameterised list key - serialise only")
	public void test275() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a~a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f in parameterised list key - serialise only")
	public void test276() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\u007fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x00 starting a parameterised list key")
	public void test277() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0000a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x01 starting a parameterised list key")
	public void test278() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0001a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x02 starting a parameterised list key")
	public void test279() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0002a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x03 starting a parameterised list key")
	public void test280() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0003a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x04 starting a parameterised list key")
	public void test281() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0004a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x05 starting a parameterised list key")
	public void test282() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0005a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x06 starting a parameterised list key")
	public void test283() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0006a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x07 starting a parameterised list key")
	public void test284() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0007a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x08 starting a parameterised list key")
	public void test285() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x09 starting a parameterised list key")
	public void test286() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\ta\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0a starting a parameterised list key")
	public void test287() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\na\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0b starting a parameterised list key")
	public void test288() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u000ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0c starting a parameterised list key")
	public void test289() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0d starting a parameterised list key")
	public void test290() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\ra\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0e starting a parameterised list key")
	public void test291() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u000ea\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x0f starting a parameterised list key")
	public void test292() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u000fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x10 starting a parameterised list key")
	public void test293() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0010a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x11 starting a parameterised list key")
	public void test294() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0011a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x12 starting a parameterised list key")
	public void test295() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0012a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x13 starting a parameterised list key")
	public void test296() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0013a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x14 starting a parameterised list key")
	public void test297() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0014a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x15 starting a parameterised list key")
	public void test298() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0015a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x16 starting a parameterised list key")
	public void test299() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0016a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x17 starting a parameterised list key")
	public void test300() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0017a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x18 starting a parameterised list key")
	public void test301() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0018a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x19 starting a parameterised list key")
	public void test302() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u0019a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1a starting a parameterised list key")
	public void test303() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001aa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1b starting a parameterised list key")
	public void test304() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1c starting a parameterised list key")
	public void test305() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001ca\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1d starting a parameterised list key")
	public void test306() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001da\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1e starting a parameterised list key")
	public void test307() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001ea\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x1f starting a parameterised list key")
	public void test308() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\u001fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x20 starting a parameterised list key")
	public void test309() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\" a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x21 starting a parameterised list key")
	public void test310() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"!a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x22 starting a parameterised list key")
	public void test311() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\\"a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x23 starting a parameterised list key")
	public void test312() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"#a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x24 starting a parameterised list key")
	public void test313() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"$a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x25 starting a parameterised list key")
	public void test314() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"%a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x26 starting a parameterised list key")
	public void test315() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"&a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x27 starting a parameterised list key")
	public void test316() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"'a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x28 starting a parameterised list key")
	public void test317() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"(a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x29 starting a parameterised list key")
	public void test318() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\")a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2b starting a parameterised list key")
	public void test319() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"+a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2c starting a parameterised list key")
	public void test320() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\",a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2d starting a parameterised list key")
	public void test321() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"-a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2e starting a parameterised list key")
	public void test322() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\".a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x2f starting a parameterised list key")
	public void test323() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"/a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x30 starting a parameterised list key")
	public void test324() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"0a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x31 starting a parameterised list key")
	public void test325() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"1a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x32 starting a parameterised list key")
	public void test326() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"2a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x33 starting a parameterised list key")
	public void test327() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"3a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x34 starting a parameterised list key")
	public void test328() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"4a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x35 starting a parameterised list key")
	public void test329() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"5a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x36 starting a parameterised list key")
	public void test330() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"6a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x37 starting a parameterised list key")
	public void test331() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"7a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x38 starting a parameterised list key")
	public void test332() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"8a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x39 starting a parameterised list key")
	public void test333() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"9a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3a starting a parameterised list key")
	public void test334() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\":a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3b starting a parameterised list key")
	public void test335() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\";a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3c starting a parameterised list key")
	public void test336() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"<a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3d starting a parameterised list key")
	public void test337() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"=a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3e starting a parameterised list key")
	public void test338() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\">a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x3f starting a parameterised list key")
	public void test339() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"?a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x40 starting a parameterised list key")
	public void test340() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"@a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x41 starting a parameterised list key")
	public void test341() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Aa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x42 starting a parameterised list key")
	public void test342() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ba\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x43 starting a parameterised list key")
	public void test343() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ca\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x44 starting a parameterised list key")
	public void test344() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Da\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x45 starting a parameterised list key")
	public void test345() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ea\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x46 starting a parameterised list key")
	public void test346() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x47 starting a parameterised list key")
	public void test347() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ga\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x48 starting a parameterised list key")
	public void test348() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ha\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x49 starting a parameterised list key")
	public void test349() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ia\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4a starting a parameterised list key")
	public void test350() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ja\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4b starting a parameterised list key")
	public void test351() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ka\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4c starting a parameterised list key")
	public void test352() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"La\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4d starting a parameterised list key")
	public void test353() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ma\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4e starting a parameterised list key")
	public void test354() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Na\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x4f starting a parameterised list key")
	public void test355() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Oa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x50 starting a parameterised list key")
	public void test356() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Pa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x51 starting a parameterised list key")
	public void test357() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Qa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x52 starting a parameterised list key")
	public void test358() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ra\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x53 starting a parameterised list key")
	public void test359() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Sa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x54 starting a parameterised list key")
	public void test360() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ta\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x55 starting a parameterised list key")
	public void test361() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ua\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x56 starting a parameterised list key")
	public void test362() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Va\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x57 starting a parameterised list key")
	public void test363() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Wa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x58 starting a parameterised list key")
	public void test364() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Xa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x59 starting a parameterised list key")
	public void test365() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Ya\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5a starting a parameterised list key")
	public void test366() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"Za\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5b starting a parameterised list key")
	public void test367() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"[a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5c starting a parameterised list key")
	public void test368() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\\\\a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5d starting a parameterised list key")
	public void test369() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"]a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5e starting a parameterised list key")
	public void test370() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"^a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x5f starting a parameterised list key")
	public void test371() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"_a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x60 starting a parameterised list key")
	public void test372() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"`a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7b starting a parameterised list key")
	public void test373() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"{a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7c starting a parameterised list key")
	public void test374() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"|a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7d starting a parameterised list key")
	public void test375() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"}a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7e starting a parameterised list key")
	public void test376() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"~a\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}

	@Test
	@DisplayName("0x7f starting a parameterised list key")
	public void test377() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> source=StructuredHeadersTestUtils.listFromJson(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"\u007fa\",1]]]]"));
			StructuredHttpHeaders.serialize(source);
		} );
	}
}
