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

public class ParseKeyGeneratedTests {
	@Test
	@DisplayName("0x00 as a single-character dictionary key")
	public void test0() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0000=1");
		} );
	}

	@Test
	@DisplayName("0x01 as a single-character dictionary key")
	public void test1() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0001=1");
		} );
	}

	@Test
	@DisplayName("0x02 as a single-character dictionary key")
	public void test2() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0002=1");
		} );
	}

	@Test
	@DisplayName("0x03 as a single-character dictionary key")
	public void test3() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0003=1");
		} );
	}

	@Test
	@DisplayName("0x04 as a single-character dictionary key")
	public void test4() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0004=1");
		} );
	}

	@Test
	@DisplayName("0x05 as a single-character dictionary key")
	public void test5() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0005=1");
		} );
	}

	@Test
	@DisplayName("0x06 as a single-character dictionary key")
	public void test6() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0006=1");
		} );
	}

	@Test
	@DisplayName("0x07 as a single-character dictionary key")
	public void test7() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0007=1");
		} );
	}

	@Test
	@DisplayName("0x08 as a single-character dictionary key")
	public void test8() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\b=1");
		} );
	}

	@Test
	@DisplayName("0x09 as a single-character dictionary key")
	public void test9() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\t=1");
		} );
	}

	@Test
	@DisplayName("0x0a as a single-character dictionary key")
	public void test10() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\n"
							+ "=1");
		} );
	}

	@Test
	@DisplayName("0x0b as a single-character dictionary key")
	public void test11() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000b=1");
		} );
	}

	@Test
	@DisplayName("0x0c as a single-character dictionary key")
	public void test12() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\f=1");
		} );
	}

	@Test
	@DisplayName("0x0d as a single-character dictionary key")
	public void test13() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\r=1");
		} );
	}

	@Test
	@DisplayName("0x0e as a single-character dictionary key")
	public void test14() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000e=1");
		} );
	}

	@Test
	@DisplayName("0x0f as a single-character dictionary key")
	public void test15() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000f=1");
		} );
	}

	@Test
	@DisplayName("0x10 as a single-character dictionary key")
	public void test16() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0010=1");
		} );
	}

	@Test
	@DisplayName("0x11 as a single-character dictionary key")
	public void test17() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0011=1");
		} );
	}

	@Test
	@DisplayName("0x12 as a single-character dictionary key")
	public void test18() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0012=1");
		} );
	}

	@Test
	@DisplayName("0x13 as a single-character dictionary key")
	public void test19() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0013=1");
		} );
	}

	@Test
	@DisplayName("0x14 as a single-character dictionary key")
	public void test20() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0014=1");
		} );
	}

	@Test
	@DisplayName("0x15 as a single-character dictionary key")
	public void test21() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0015=1");
		} );
	}

	@Test
	@DisplayName("0x16 as a single-character dictionary key")
	public void test22() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0016=1");
		} );
	}

	@Test
	@DisplayName("0x17 as a single-character dictionary key")
	public void test23() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0017=1");
		} );
	}

	@Test
	@DisplayName("0x18 as a single-character dictionary key")
	public void test24() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0018=1");
		} );
	}

	@Test
	@DisplayName("0x19 as a single-character dictionary key")
	public void test25() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0019=1");
		} );
	}

	@Test
	@DisplayName("0x1a as a single-character dictionary key")
	public void test26() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001a=1");
		} );
	}

	@Test
	@DisplayName("0x1b as a single-character dictionary key")
	public void test27() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001b=1");
		} );
	}

	@Test
	@DisplayName("0x1c as a single-character dictionary key")
	public void test28() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001c=1");
		} );
	}

	@Test
	@DisplayName("0x1d as a single-character dictionary key")
	public void test29() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001d=1");
		} );
	}

	@Test
	@DisplayName("0x1e as a single-character dictionary key")
	public void test30() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001e=1");
		} );
	}

	@Test
	@DisplayName("0x1f as a single-character dictionary key")
	public void test31() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001f=1");
		} );
	}

	@Test
	@DisplayName("0x20 as a single-character dictionary key")
	public void test32() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("=1");
		} );
	}

	@Test
	@DisplayName("0x21 as a single-character dictionary key")
	public void test33() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("!=1");
		} );
	}

	@Test
	@DisplayName("0x22 as a single-character dictionary key")
	public void test34() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\"=1");
		} );
	}

	@Test
	@DisplayName("0x23 as a single-character dictionary key")
	public void test35() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("#=1");
		} );
	}

	@Test
	@DisplayName("0x24 as a single-character dictionary key")
	public void test36() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("$=1");
		} );
	}

	@Test
	@DisplayName("0x25 as a single-character dictionary key")
	public void test37() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("%=1");
		} );
	}

	@Test
	@DisplayName("0x26 as a single-character dictionary key")
	public void test38() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("&=1");
		} );
	}

	@Test
	@DisplayName("0x27 as a single-character dictionary key")
	public void test39() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("'=1");
		} );
	}

	@Test
	@DisplayName("0x28 as a single-character dictionary key")
	public void test40() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("(=1");
		} );
	}

	@Test
	@DisplayName("0x29 as a single-character dictionary key")
	public void test41() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(")=1");
		} );
	}

	@Test
	@DisplayName("0x2a as a single-character dictionary key")
	public void test42() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("*=1");
		assertEquals(JsonParser.parseString("[[\"*\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b as a single-character dictionary key")
	public void test43() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("+=1");
		} );
	}

	@Test
	@DisplayName("0x2c as a single-character dictionary key")
	public void test44() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(",=1");
		} );
	}

	@Test
	@DisplayName("0x2d as a single-character dictionary key")
	public void test45() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("-=1");
		} );
	}

	@Test
	@DisplayName("0x2e as a single-character dictionary key")
	public void test46() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(".=1");
		} );
	}

	@Test
	@DisplayName("0x2f as a single-character dictionary key")
	public void test47() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("/=1");
		} );
	}

	@Test
	@DisplayName("0x30 as a single-character dictionary key")
	public void test48() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("0=1");
		} );
	}

	@Test
	@DisplayName("0x31 as a single-character dictionary key")
	public void test49() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("1=1");
		} );
	}

	@Test
	@DisplayName("0x32 as a single-character dictionary key")
	public void test50() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("2=1");
		} );
	}

	@Test
	@DisplayName("0x33 as a single-character dictionary key")
	public void test51() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("3=1");
		} );
	}

	@Test
	@DisplayName("0x34 as a single-character dictionary key")
	public void test52() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("4=1");
		} );
	}

	@Test
	@DisplayName("0x35 as a single-character dictionary key")
	public void test53() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("5=1");
		} );
	}

	@Test
	@DisplayName("0x36 as a single-character dictionary key")
	public void test54() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("6=1");
		} );
	}

	@Test
	@DisplayName("0x37 as a single-character dictionary key")
	public void test55() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("7=1");
		} );
	}

	@Test
	@DisplayName("0x38 as a single-character dictionary key")
	public void test56() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("8=1");
		} );
	}

	@Test
	@DisplayName("0x39 as a single-character dictionary key")
	public void test57() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("9=1");
		} );
	}

	@Test
	@DisplayName("0x3a as a single-character dictionary key")
	public void test58() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(":=1");
		} );
	}

	@Test
	@DisplayName("0x3b as a single-character dictionary key")
	public void test59() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(";=1");
		} );
	}

	@Test
	@DisplayName("0x3c as a single-character dictionary key")
	public void test60() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("<=1");
		} );
	}

	@Test
	@DisplayName("0x3d as a single-character dictionary key")
	public void test61() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("==1");
		} );
	}

	@Test
	@DisplayName("0x3e as a single-character dictionary key")
	public void test62() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(">=1");
		} );
	}

	@Test
	@DisplayName("0x3f as a single-character dictionary key")
	public void test63() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("?=1");
		} );
	}

	@Test
	@DisplayName("0x40 as a single-character dictionary key")
	public void test64() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("@=1");
		} );
	}

	@Test
	@DisplayName("0x41 as a single-character dictionary key")
	public void test65() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("A=1");
		} );
	}

	@Test
	@DisplayName("0x42 as a single-character dictionary key")
	public void test66() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("B=1");
		} );
	}

	@Test
	@DisplayName("0x43 as a single-character dictionary key")
	public void test67() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("C=1");
		} );
	}

	@Test
	@DisplayName("0x44 as a single-character dictionary key")
	public void test68() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("D=1");
		} );
	}

	@Test
	@DisplayName("0x45 as a single-character dictionary key")
	public void test69() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("E=1");
		} );
	}

	@Test
	@DisplayName("0x46 as a single-character dictionary key")
	public void test70() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("F=1");
		} );
	}

	@Test
	@DisplayName("0x47 as a single-character dictionary key")
	public void test71() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("G=1");
		} );
	}

	@Test
	@DisplayName("0x48 as a single-character dictionary key")
	public void test72() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("H=1");
		} );
	}

	@Test
	@DisplayName("0x49 as a single-character dictionary key")
	public void test73() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("I=1");
		} );
	}

	@Test
	@DisplayName("0x4a as a single-character dictionary key")
	public void test74() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("J=1");
		} );
	}

	@Test
	@DisplayName("0x4b as a single-character dictionary key")
	public void test75() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("K=1");
		} );
	}

	@Test
	@DisplayName("0x4c as a single-character dictionary key")
	public void test76() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("L=1");
		} );
	}

	@Test
	@DisplayName("0x4d as a single-character dictionary key")
	public void test77() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("M=1");
		} );
	}

	@Test
	@DisplayName("0x4e as a single-character dictionary key")
	public void test78() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("N=1");
		} );
	}

	@Test
	@DisplayName("0x4f as a single-character dictionary key")
	public void test79() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("O=1");
		} );
	}

	@Test
	@DisplayName("0x50 as a single-character dictionary key")
	public void test80() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("P=1");
		} );
	}

	@Test
	@DisplayName("0x51 as a single-character dictionary key")
	public void test81() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Q=1");
		} );
	}

	@Test
	@DisplayName("0x52 as a single-character dictionary key")
	public void test82() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("R=1");
		} );
	}

	@Test
	@DisplayName("0x53 as a single-character dictionary key")
	public void test83() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("S=1");
		} );
	}

	@Test
	@DisplayName("0x54 as a single-character dictionary key")
	public void test84() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("T=1");
		} );
	}

	@Test
	@DisplayName("0x55 as a single-character dictionary key")
	public void test85() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("U=1");
		} );
	}

	@Test
	@DisplayName("0x56 as a single-character dictionary key")
	public void test86() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("V=1");
		} );
	}

	@Test
	@DisplayName("0x57 as a single-character dictionary key")
	public void test87() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("W=1");
		} );
	}

	@Test
	@DisplayName("0x58 as a single-character dictionary key")
	public void test88() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("X=1");
		} );
	}

	@Test
	@DisplayName("0x59 as a single-character dictionary key")
	public void test89() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Y=1");
		} );
	}

	@Test
	@DisplayName("0x5a as a single-character dictionary key")
	public void test90() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Z=1");
		} );
	}

	@Test
	@DisplayName("0x5b as a single-character dictionary key")
	public void test91() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("[=1");
		} );
	}

	@Test
	@DisplayName("0x5c as a single-character dictionary key")
	public void test92() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\\=1");
		} );
	}

	@Test
	@DisplayName("0x5d as a single-character dictionary key")
	public void test93() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("]=1");
		} );
	}

	@Test
	@DisplayName("0x5e as a single-character dictionary key")
	public void test94() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("^=1");
		} );
	}

	@Test
	@DisplayName("0x5f as a single-character dictionary key")
	public void test95() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("_=1");
		} );
	}

	@Test
	@DisplayName("0x60 as a single-character dictionary key")
	public void test96() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("`=1");
		} );
	}

	@Test
	@DisplayName("0x61 as a single-character dictionary key")
	public void test97() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=1");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 as a single-character dictionary key")
	public void test98() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("b=1");
		assertEquals(JsonParser.parseString("[[\"b\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 as a single-character dictionary key")
	public void test99() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("c=1");
		assertEquals(JsonParser.parseString("[[\"c\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 as a single-character dictionary key")
	public void test100() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("d=1");
		assertEquals(JsonParser.parseString("[[\"d\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 as a single-character dictionary key")
	public void test101() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("e=1");
		assertEquals(JsonParser.parseString("[[\"e\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 as a single-character dictionary key")
	public void test102() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("f=1");
		assertEquals(JsonParser.parseString("[[\"f\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 as a single-character dictionary key")
	public void test103() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("g=1");
		assertEquals(JsonParser.parseString("[[\"g\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 as a single-character dictionary key")
	public void test104() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("h=1");
		assertEquals(JsonParser.parseString("[[\"h\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 as a single-character dictionary key")
	public void test105() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("i=1");
		assertEquals(JsonParser.parseString("[[\"i\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a as a single-character dictionary key")
	public void test106() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("j=1");
		assertEquals(JsonParser.parseString("[[\"j\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b as a single-character dictionary key")
	public void test107() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("k=1");
		assertEquals(JsonParser.parseString("[[\"k\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c as a single-character dictionary key")
	public void test108() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("l=1");
		assertEquals(JsonParser.parseString("[[\"l\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d as a single-character dictionary key")
	public void test109() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("m=1");
		assertEquals(JsonParser.parseString("[[\"m\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e as a single-character dictionary key")
	public void test110() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("n=1");
		assertEquals(JsonParser.parseString("[[\"n\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f as a single-character dictionary key")
	public void test111() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("o=1");
		assertEquals(JsonParser.parseString("[[\"o\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 as a single-character dictionary key")
	public void test112() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("p=1");
		assertEquals(JsonParser.parseString("[[\"p\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 as a single-character dictionary key")
	public void test113() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("q=1");
		assertEquals(JsonParser.parseString("[[\"q\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 as a single-character dictionary key")
	public void test114() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("r=1");
		assertEquals(JsonParser.parseString("[[\"r\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 as a single-character dictionary key")
	public void test115() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("s=1");
		assertEquals(JsonParser.parseString("[[\"s\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 as a single-character dictionary key")
	public void test116() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("t=1");
		assertEquals(JsonParser.parseString("[[\"t\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 as a single-character dictionary key")
	public void test117() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("u=1");
		assertEquals(JsonParser.parseString("[[\"u\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 as a single-character dictionary key")
	public void test118() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("v=1");
		assertEquals(JsonParser.parseString("[[\"v\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 as a single-character dictionary key")
	public void test119() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("w=1");
		assertEquals(JsonParser.parseString("[[\"w\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 as a single-character dictionary key")
	public void test120() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("x=1");
		assertEquals(JsonParser.parseString("[[\"x\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 as a single-character dictionary key")
	public void test121() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("y=1");
		assertEquals(JsonParser.parseString("[[\"y\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a as a single-character dictionary key")
	public void test122() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("z=1");
		assertEquals(JsonParser.parseString("[[\"z\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b as a single-character dictionary key")
	public void test123() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("{=1");
		} );
	}

	@Test
	@DisplayName("0x7c as a single-character dictionary key")
	public void test124() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("|=1");
		} );
	}

	@Test
	@DisplayName("0x7d as a single-character dictionary key")
	public void test125() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("}=1");
		} );
	}

	@Test
	@DisplayName("0x7e as a single-character dictionary key")
	public void test126() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("~=1");
		} );
	}

	@Test
	@DisplayName("0x7f as a single-character dictionary key")
	public void test127() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u007f=1");
		} );
	}

	@Test
	@DisplayName("0x00 in dictionary key")
	public void test128() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0000a=1");
		} );
	}

	@Test
	@DisplayName("0x01 in dictionary key")
	public void test129() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0001a=1");
		} );
	}

	@Test
	@DisplayName("0x02 in dictionary key")
	public void test130() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0002a=1");
		} );
	}

	@Test
	@DisplayName("0x03 in dictionary key")
	public void test131() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0003a=1");
		} );
	}

	@Test
	@DisplayName("0x04 in dictionary key")
	public void test132() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0004a=1");
		} );
	}

	@Test
	@DisplayName("0x05 in dictionary key")
	public void test133() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0005a=1");
		} );
	}

	@Test
	@DisplayName("0x06 in dictionary key")
	public void test134() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0006a=1");
		} );
	}

	@Test
	@DisplayName("0x07 in dictionary key")
	public void test135() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0007a=1");
		} );
	}

	@Test
	@DisplayName("0x08 in dictionary key")
	public void test136() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\ba=1");
		} );
	}

	@Test
	@DisplayName("0x09 in dictionary key")
	public void test137() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\ta=1");
		} );
	}

	@Test
	@DisplayName("0x0a in dictionary key")
	public void test138() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\n"
							+ "a=1");
		} );
	}

	@Test
	@DisplayName("0x0b in dictionary key")
	public void test139() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u000ba=1");
		} );
	}

	@Test
	@DisplayName("0x0c in dictionary key")
	public void test140() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\fa=1");
		} );
	}

	@Test
	@DisplayName("0x0d in dictionary key")
	public void test141() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\ra=1");
		} );
	}

	@Test
	@DisplayName("0x0e in dictionary key")
	public void test142() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u000ea=1");
		} );
	}

	@Test
	@DisplayName("0x0f in dictionary key")
	public void test143() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u000fa=1");
		} );
	}

	@Test
	@DisplayName("0x10 in dictionary key")
	public void test144() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0010a=1");
		} );
	}

	@Test
	@DisplayName("0x11 in dictionary key")
	public void test145() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0011a=1");
		} );
	}

	@Test
	@DisplayName("0x12 in dictionary key")
	public void test146() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0012a=1");
		} );
	}

	@Test
	@DisplayName("0x13 in dictionary key")
	public void test147() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0013a=1");
		} );
	}

	@Test
	@DisplayName("0x14 in dictionary key")
	public void test148() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0014a=1");
		} );
	}

	@Test
	@DisplayName("0x15 in dictionary key")
	public void test149() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0015a=1");
		} );
	}

	@Test
	@DisplayName("0x16 in dictionary key")
	public void test150() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0016a=1");
		} );
	}

	@Test
	@DisplayName("0x17 in dictionary key")
	public void test151() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0017a=1");
		} );
	}

	@Test
	@DisplayName("0x18 in dictionary key")
	public void test152() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0018a=1");
		} );
	}

	@Test
	@DisplayName("0x19 in dictionary key")
	public void test153() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u0019a=1");
		} );
	}

	@Test
	@DisplayName("0x1a in dictionary key")
	public void test154() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001aa=1");
		} );
	}

	@Test
	@DisplayName("0x1b in dictionary key")
	public void test155() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001ba=1");
		} );
	}

	@Test
	@DisplayName("0x1c in dictionary key")
	public void test156() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001ca=1");
		} );
	}

	@Test
	@DisplayName("0x1d in dictionary key")
	public void test157() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001da=1");
		} );
	}

	@Test
	@DisplayName("0x1e in dictionary key")
	public void test158() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001ea=1");
		} );
	}

	@Test
	@DisplayName("0x1f in dictionary key")
	public void test159() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u001fa=1");
		} );
	}

	@Test
	@DisplayName("0x20 in dictionary key")
	public void test160() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a a=1");
		} );
	}

	@Test
	@DisplayName("0x21 in dictionary key")
	public void test161() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a!a=1");
		} );
	}

	@Test
	@DisplayName("0x22 in dictionary key")
	public void test162() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\"a=1");
		} );
	}

	@Test
	@DisplayName("0x23 in dictionary key")
	public void test163() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a#a=1");
		} );
	}

	@Test
	@DisplayName("0x24 in dictionary key")
	public void test164() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a$a=1");
		} );
	}

	@Test
	@DisplayName("0x25 in dictionary key")
	public void test165() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a%a=1");
		} );
	}

	@Test
	@DisplayName("0x26 in dictionary key")
	public void test166() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a&a=1");
		} );
	}

	@Test
	@DisplayName("0x27 in dictionary key")
	public void test167() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a'a=1");
		} );
	}

	@Test
	@DisplayName("0x28 in dictionary key")
	public void test168() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a(a=1");
		} );
	}

	@Test
	@DisplayName("0x29 in dictionary key")
	public void test169() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a)a=1");
		} );
	}

	@Test
	@DisplayName("0x2a in dictionary key")
	public void test170() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a*a=1");
		assertEquals(JsonParser.parseString("[[\"a*a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b in dictionary key")
	public void test171() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a+a=1");
		} );
	}

	@Test
	@DisplayName("0x2c in dictionary key")
	public void test172() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a,a=1");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2d in dictionary key")
	public void test173() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a-a=1");
		assertEquals(JsonParser.parseString("[[\"a-a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2e in dictionary key")
	public void test174() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a.a=1");
		assertEquals(JsonParser.parseString("[[\"a.a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2f in dictionary key")
	public void test175() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a/a=1");
		} );
	}

	@Test
	@DisplayName("0x30 in dictionary key")
	public void test176() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a0a=1");
		assertEquals(JsonParser.parseString("[[\"a0a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x31 in dictionary key")
	public void test177() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a1a=1");
		assertEquals(JsonParser.parseString("[[\"a1a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x32 in dictionary key")
	public void test178() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a2a=1");
		assertEquals(JsonParser.parseString("[[\"a2a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x33 in dictionary key")
	public void test179() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a3a=1");
		assertEquals(JsonParser.parseString("[[\"a3a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x34 in dictionary key")
	public void test180() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a4a=1");
		assertEquals(JsonParser.parseString("[[\"a4a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x35 in dictionary key")
	public void test181() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a5a=1");
		assertEquals(JsonParser.parseString("[[\"a5a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x36 in dictionary key")
	public void test182() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a6a=1");
		assertEquals(JsonParser.parseString("[[\"a6a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x37 in dictionary key")
	public void test183() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a7a=1");
		assertEquals(JsonParser.parseString("[[\"a7a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x38 in dictionary key")
	public void test184() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a8a=1");
		assertEquals(JsonParser.parseString("[[\"a8a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x39 in dictionary key")
	public void test185() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a9a=1");
		assertEquals(JsonParser.parseString("[[\"a9a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3a in dictionary key")
	public void test186() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a:a=1");
		} );
	}

	@Test
	@DisplayName("0x3b in dictionary key")
	public void test187() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a;a=1");
		assertEquals(JsonParser.parseString("[[\"a\",[true,[[\"a\",1]]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3c in dictionary key")
	public void test188() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a<a=1");
		} );
	}

	@Test
	@DisplayName("0x3d in dictionary key")
	public void test189() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a=a=1");
		} );
	}

	@Test
	@DisplayName("0x3e in dictionary key")
	public void test190() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a>a=1");
		} );
	}

	@Test
	@DisplayName("0x3f in dictionary key")
	public void test191() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a?a=1");
		} );
	}

	@Test
	@DisplayName("0x40 in dictionary key")
	public void test192() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a@a=1");
		} );
	}

	@Test
	@DisplayName("0x41 in dictionary key")
	public void test193() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aAa=1");
		} );
	}

	@Test
	@DisplayName("0x42 in dictionary key")
	public void test194() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aBa=1");
		} );
	}

	@Test
	@DisplayName("0x43 in dictionary key")
	public void test195() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aCa=1");
		} );
	}

	@Test
	@DisplayName("0x44 in dictionary key")
	public void test196() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aDa=1");
		} );
	}

	@Test
	@DisplayName("0x45 in dictionary key")
	public void test197() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aEa=1");
		} );
	}

	@Test
	@DisplayName("0x46 in dictionary key")
	public void test198() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aFa=1");
		} );
	}

	@Test
	@DisplayName("0x47 in dictionary key")
	public void test199() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aGa=1");
		} );
	}

	@Test
	@DisplayName("0x48 in dictionary key")
	public void test200() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aHa=1");
		} );
	}

	@Test
	@DisplayName("0x49 in dictionary key")
	public void test201() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aIa=1");
		} );
	}

	@Test
	@DisplayName("0x4a in dictionary key")
	public void test202() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aJa=1");
		} );
	}

	@Test
	@DisplayName("0x4b in dictionary key")
	public void test203() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aKa=1");
		} );
	}

	@Test
	@DisplayName("0x4c in dictionary key")
	public void test204() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aLa=1");
		} );
	}

	@Test
	@DisplayName("0x4d in dictionary key")
	public void test205() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aMa=1");
		} );
	}

	@Test
	@DisplayName("0x4e in dictionary key")
	public void test206() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aNa=1");
		} );
	}

	@Test
	@DisplayName("0x4f in dictionary key")
	public void test207() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aOa=1");
		} );
	}

	@Test
	@DisplayName("0x50 in dictionary key")
	public void test208() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aPa=1");
		} );
	}

	@Test
	@DisplayName("0x51 in dictionary key")
	public void test209() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aQa=1");
		} );
	}

	@Test
	@DisplayName("0x52 in dictionary key")
	public void test210() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aRa=1");
		} );
	}

	@Test
	@DisplayName("0x53 in dictionary key")
	public void test211() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aSa=1");
		} );
	}

	@Test
	@DisplayName("0x54 in dictionary key")
	public void test212() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aTa=1");
		} );
	}

	@Test
	@DisplayName("0x55 in dictionary key")
	public void test213() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aUa=1");
		} );
	}

	@Test
	@DisplayName("0x56 in dictionary key")
	public void test214() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aVa=1");
		} );
	}

	@Test
	@DisplayName("0x57 in dictionary key")
	public void test215() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aWa=1");
		} );
	}

	@Test
	@DisplayName("0x58 in dictionary key")
	public void test216() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aXa=1");
		} );
	}

	@Test
	@DisplayName("0x59 in dictionary key")
	public void test217() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aYa=1");
		} );
	}

	@Test
	@DisplayName("0x5a in dictionary key")
	public void test218() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aZa=1");
		} );
	}

	@Test
	@DisplayName("0x5b in dictionary key")
	public void test219() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a[a=1");
		} );
	}

	@Test
	@DisplayName("0x5c in dictionary key")
	public void test220() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\\a=1");
		} );
	}

	@Test
	@DisplayName("0x5d in dictionary key")
	public void test221() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a]a=1");
		} );
	}

	@Test
	@DisplayName("0x5e in dictionary key")
	public void test222() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a^a=1");
		} );
	}

	@Test
	@DisplayName("0x5f in dictionary key")
	public void test223() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a_a=1");
		assertEquals(JsonParser.parseString("[[\"a_a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x60 in dictionary key")
	public void test224() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a`a=1");
		} );
	}

	@Test
	@DisplayName("0x61 in dictionary key")
	public void test225() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aaa=1");
		assertEquals(JsonParser.parseString("[[\"aaa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 in dictionary key")
	public void test226() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aba=1");
		assertEquals(JsonParser.parseString("[[\"aba\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 in dictionary key")
	public void test227() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aca=1");
		assertEquals(JsonParser.parseString("[[\"aca\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 in dictionary key")
	public void test228() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ada=1");
		assertEquals(JsonParser.parseString("[[\"ada\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 in dictionary key")
	public void test229() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aea=1");
		assertEquals(JsonParser.parseString("[[\"aea\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 in dictionary key")
	public void test230() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("afa=1");
		assertEquals(JsonParser.parseString("[[\"afa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 in dictionary key")
	public void test231() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aga=1");
		assertEquals(JsonParser.parseString("[[\"aga\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 in dictionary key")
	public void test232() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aha=1");
		assertEquals(JsonParser.parseString("[[\"aha\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 in dictionary key")
	public void test233() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aia=1");
		assertEquals(JsonParser.parseString("[[\"aia\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a in dictionary key")
	public void test234() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aja=1");
		assertEquals(JsonParser.parseString("[[\"aja\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b in dictionary key")
	public void test235() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aka=1");
		assertEquals(JsonParser.parseString("[[\"aka\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c in dictionary key")
	public void test236() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ala=1");
		assertEquals(JsonParser.parseString("[[\"ala\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d in dictionary key")
	public void test237() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ama=1");
		assertEquals(JsonParser.parseString("[[\"ama\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e in dictionary key")
	public void test238() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ana=1");
		assertEquals(JsonParser.parseString("[[\"ana\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f in dictionary key")
	public void test239() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aoa=1");
		assertEquals(JsonParser.parseString("[[\"aoa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 in dictionary key")
	public void test240() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("apa=1");
		assertEquals(JsonParser.parseString("[[\"apa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 in dictionary key")
	public void test241() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aqa=1");
		assertEquals(JsonParser.parseString("[[\"aqa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 in dictionary key")
	public void test242() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ara=1");
		assertEquals(JsonParser.parseString("[[\"ara\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 in dictionary key")
	public void test243() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("asa=1");
		assertEquals(JsonParser.parseString("[[\"asa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 in dictionary key")
	public void test244() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ata=1");
		assertEquals(JsonParser.parseString("[[\"ata\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 in dictionary key")
	public void test245() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aua=1");
		assertEquals(JsonParser.parseString("[[\"aua\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 in dictionary key")
	public void test246() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ava=1");
		assertEquals(JsonParser.parseString("[[\"ava\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 in dictionary key")
	public void test247() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("awa=1");
		assertEquals(JsonParser.parseString("[[\"awa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 in dictionary key")
	public void test248() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("axa=1");
		assertEquals(JsonParser.parseString("[[\"axa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 in dictionary key")
	public void test249() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aya=1");
		assertEquals(JsonParser.parseString("[[\"aya\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a in dictionary key")
	public void test250() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aza=1");
		assertEquals(JsonParser.parseString("[[\"aza\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b in dictionary key")
	public void test251() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a{a=1");
		} );
	}

	@Test
	@DisplayName("0x7c in dictionary key")
	public void test252() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a|a=1");
		} );
	}

	@Test
	@DisplayName("0x7d in dictionary key")
	public void test253() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a}a=1");
		} );
	}

	@Test
	@DisplayName("0x7e in dictionary key")
	public void test254() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a~a=1");
		} );
	}

	@Test
	@DisplayName("0x7f in dictionary key")
	public void test255() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("a\u007fa=1");
		} );
	}

	@Test
	@DisplayName("0x00 starting a dictionary key")
	public void test256() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0000a=1");
		} );
	}

	@Test
	@DisplayName("0x01 starting a dictionary key")
	public void test257() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0001a=1");
		} );
	}

	@Test
	@DisplayName("0x02 starting a dictionary key")
	public void test258() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0002a=1");
		} );
	}

	@Test
	@DisplayName("0x03 starting a dictionary key")
	public void test259() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0003a=1");
		} );
	}

	@Test
	@DisplayName("0x04 starting a dictionary key")
	public void test260() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0004a=1");
		} );
	}

	@Test
	@DisplayName("0x05 starting a dictionary key")
	public void test261() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0005a=1");
		} );
	}

	@Test
	@DisplayName("0x06 starting a dictionary key")
	public void test262() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0006a=1");
		} );
	}

	@Test
	@DisplayName("0x07 starting a dictionary key")
	public void test263() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0007a=1");
		} );
	}

	@Test
	@DisplayName("0x08 starting a dictionary key")
	public void test264() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\ba=1");
		} );
	}

	@Test
	@DisplayName("0x09 starting a dictionary key")
	public void test265() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\ta=1");
		} );
	}

	@Test
	@DisplayName("0x0a starting a dictionary key")
	public void test266() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\n"
							+ "a=1");
		} );
	}

	@Test
	@DisplayName("0x0b starting a dictionary key")
	public void test267() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000ba=1");
		} );
	}

	@Test
	@DisplayName("0x0c starting a dictionary key")
	public void test268() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\fa=1");
		} );
	}

	@Test
	@DisplayName("0x0d starting a dictionary key")
	public void test269() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\ra=1");
		} );
	}

	@Test
	@DisplayName("0x0e starting a dictionary key")
	public void test270() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000ea=1");
		} );
	}

	@Test
	@DisplayName("0x0f starting a dictionary key")
	public void test271() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u000fa=1");
		} );
	}

	@Test
	@DisplayName("0x10 starting a dictionary key")
	public void test272() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0010a=1");
		} );
	}

	@Test
	@DisplayName("0x11 starting a dictionary key")
	public void test273() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0011a=1");
		} );
	}

	@Test
	@DisplayName("0x12 starting a dictionary key")
	public void test274() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0012a=1");
		} );
	}

	@Test
	@DisplayName("0x13 starting a dictionary key")
	public void test275() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0013a=1");
		} );
	}

	@Test
	@DisplayName("0x14 starting a dictionary key")
	public void test276() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0014a=1");
		} );
	}

	@Test
	@DisplayName("0x15 starting a dictionary key")
	public void test277() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0015a=1");
		} );
	}

	@Test
	@DisplayName("0x16 starting a dictionary key")
	public void test278() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0016a=1");
		} );
	}

	@Test
	@DisplayName("0x17 starting a dictionary key")
	public void test279() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0017a=1");
		} );
	}

	@Test
	@DisplayName("0x18 starting a dictionary key")
	public void test280() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0018a=1");
		} );
	}

	@Test
	@DisplayName("0x19 starting a dictionary key")
	public void test281() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u0019a=1");
		} );
	}

	@Test
	@DisplayName("0x1a starting a dictionary key")
	public void test282() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001aa=1");
		} );
	}

	@Test
	@DisplayName("0x1b starting a dictionary key")
	public void test283() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001ba=1");
		} );
	}

	@Test
	@DisplayName("0x1c starting a dictionary key")
	public void test284() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001ca=1");
		} );
	}

	@Test
	@DisplayName("0x1d starting a dictionary key")
	public void test285() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001da=1");
		} );
	}

	@Test
	@DisplayName("0x1e starting a dictionary key")
	public void test286() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001ea=1");
		} );
	}

	@Test
	@DisplayName("0x1f starting a dictionary key")
	public void test287() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u001fa=1");
		} );
	}

	@Test
	@DisplayName("0x20 starting a dictionary key")
	public void test288() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(" a=1");
		assertEquals(JsonParser.parseString("[[\"a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x21 starting a dictionary key")
	public void test289() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("!a=1");
		} );
	}

	@Test
	@DisplayName("0x22 starting a dictionary key")
	public void test290() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\"a=1");
		} );
	}

	@Test
	@DisplayName("0x23 starting a dictionary key")
	public void test291() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("#a=1");
		} );
	}

	@Test
	@DisplayName("0x24 starting a dictionary key")
	public void test292() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("$a=1");
		} );
	}

	@Test
	@DisplayName("0x25 starting a dictionary key")
	public void test293() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("%a=1");
		} );
	}

	@Test
	@DisplayName("0x26 starting a dictionary key")
	public void test294() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("&a=1");
		} );
	}

	@Test
	@DisplayName("0x27 starting a dictionary key")
	public void test295() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("'a=1");
		} );
	}

	@Test
	@DisplayName("0x28 starting a dictionary key")
	public void test296() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("(a=1");
		} );
	}

	@Test
	@DisplayName("0x29 starting a dictionary key")
	public void test297() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(")a=1");
		} );
	}

	@Test
	@DisplayName("0x2a starting a dictionary key")
	public void test298() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("*a=1");
		assertEquals(JsonParser.parseString("[[\"*a\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b starting a dictionary key")
	public void test299() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("+a=1");
		} );
	}

	@Test
	@DisplayName("0x2c starting a dictionary key")
	public void test300() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(",a=1");
		} );
	}

	@Test
	@DisplayName("0x2d starting a dictionary key")
	public void test301() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("-a=1");
		} );
	}

	@Test
	@DisplayName("0x2e starting a dictionary key")
	public void test302() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(".a=1");
		} );
	}

	@Test
	@DisplayName("0x2f starting a dictionary key")
	public void test303() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("/a=1");
		} );
	}

	@Test
	@DisplayName("0x30 starting a dictionary key")
	public void test304() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("0a=1");
		} );
	}

	@Test
	@DisplayName("0x31 starting a dictionary key")
	public void test305() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("1a=1");
		} );
	}

	@Test
	@DisplayName("0x32 starting a dictionary key")
	public void test306() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("2a=1");
		} );
	}

	@Test
	@DisplayName("0x33 starting a dictionary key")
	public void test307() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("3a=1");
		} );
	}

	@Test
	@DisplayName("0x34 starting a dictionary key")
	public void test308() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("4a=1");
		} );
	}

	@Test
	@DisplayName("0x35 starting a dictionary key")
	public void test309() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("5a=1");
		} );
	}

	@Test
	@DisplayName("0x36 starting a dictionary key")
	public void test310() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("6a=1");
		} );
	}

	@Test
	@DisplayName("0x37 starting a dictionary key")
	public void test311() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("7a=1");
		} );
	}

	@Test
	@DisplayName("0x38 starting a dictionary key")
	public void test312() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("8a=1");
		} );
	}

	@Test
	@DisplayName("0x39 starting a dictionary key")
	public void test313() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("9a=1");
		} );
	}

	@Test
	@DisplayName("0x3a starting a dictionary key")
	public void test314() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(":a=1");
		} );
	}

	@Test
	@DisplayName("0x3b starting a dictionary key")
	public void test315() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(";a=1");
		} );
	}

	@Test
	@DisplayName("0x3c starting a dictionary key")
	public void test316() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("<a=1");
		} );
	}

	@Test
	@DisplayName("0x3d starting a dictionary key")
	public void test317() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("=a=1");
		} );
	}

	@Test
	@DisplayName("0x3e starting a dictionary key")
	public void test318() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary(">a=1");
		} );
	}

	@Test
	@DisplayName("0x3f starting a dictionary key")
	public void test319() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("?a=1");
		} );
	}

	@Test
	@DisplayName("0x40 starting a dictionary key")
	public void test320() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("@a=1");
		} );
	}

	@Test
	@DisplayName("0x41 starting a dictionary key")
	public void test321() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Aa=1");
		} );
	}

	@Test
	@DisplayName("0x42 starting a dictionary key")
	public void test322() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ba=1");
		} );
	}

	@Test
	@DisplayName("0x43 starting a dictionary key")
	public void test323() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ca=1");
		} );
	}

	@Test
	@DisplayName("0x44 starting a dictionary key")
	public void test324() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Da=1");
		} );
	}

	@Test
	@DisplayName("0x45 starting a dictionary key")
	public void test325() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ea=1");
		} );
	}

	@Test
	@DisplayName("0x46 starting a dictionary key")
	public void test326() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Fa=1");
		} );
	}

	@Test
	@DisplayName("0x47 starting a dictionary key")
	public void test327() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ga=1");
		} );
	}

	@Test
	@DisplayName("0x48 starting a dictionary key")
	public void test328() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ha=1");
		} );
	}

	@Test
	@DisplayName("0x49 starting a dictionary key")
	public void test329() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ia=1");
		} );
	}

	@Test
	@DisplayName("0x4a starting a dictionary key")
	public void test330() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ja=1");
		} );
	}

	@Test
	@DisplayName("0x4b starting a dictionary key")
	public void test331() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ka=1");
		} );
	}

	@Test
	@DisplayName("0x4c starting a dictionary key")
	public void test332() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("La=1");
		} );
	}

	@Test
	@DisplayName("0x4d starting a dictionary key")
	public void test333() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ma=1");
		} );
	}

	@Test
	@DisplayName("0x4e starting a dictionary key")
	public void test334() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Na=1");
		} );
	}

	@Test
	@DisplayName("0x4f starting a dictionary key")
	public void test335() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Oa=1");
		} );
	}

	@Test
	@DisplayName("0x50 starting a dictionary key")
	public void test336() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Pa=1");
		} );
	}

	@Test
	@DisplayName("0x51 starting a dictionary key")
	public void test337() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Qa=1");
		} );
	}

	@Test
	@DisplayName("0x52 starting a dictionary key")
	public void test338() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ra=1");
		} );
	}

	@Test
	@DisplayName("0x53 starting a dictionary key")
	public void test339() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Sa=1");
		} );
	}

	@Test
	@DisplayName("0x54 starting a dictionary key")
	public void test340() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ta=1");
		} );
	}

	@Test
	@DisplayName("0x55 starting a dictionary key")
	public void test341() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ua=1");
		} );
	}

	@Test
	@DisplayName("0x56 starting a dictionary key")
	public void test342() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Va=1");
		} );
	}

	@Test
	@DisplayName("0x57 starting a dictionary key")
	public void test343() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Wa=1");
		} );
	}

	@Test
	@DisplayName("0x58 starting a dictionary key")
	public void test344() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Xa=1");
		} );
	}

	@Test
	@DisplayName("0x59 starting a dictionary key")
	public void test345() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Ya=1");
		} );
	}

	@Test
	@DisplayName("0x5a starting a dictionary key")
	public void test346() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("Za=1");
		} );
	}

	@Test
	@DisplayName("0x5b starting a dictionary key")
	public void test347() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("[a=1");
		} );
	}

	@Test
	@DisplayName("0x5c starting a dictionary key")
	public void test348() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\\a=1");
		} );
	}

	@Test
	@DisplayName("0x5d starting a dictionary key")
	public void test349() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("]a=1");
		} );
	}

	@Test
	@DisplayName("0x5e starting a dictionary key")
	public void test350() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("^a=1");
		} );
	}

	@Test
	@DisplayName("0x5f starting a dictionary key")
	public void test351() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("_a=1");
		} );
	}

	@Test
	@DisplayName("0x60 starting a dictionary key")
	public void test352() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("`a=1");
		} );
	}

	@Test
	@DisplayName("0x61 starting a dictionary key")
	public void test353() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("aa=1");
		assertEquals(JsonParser.parseString("[[\"aa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 starting a dictionary key")
	public void test354() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ba=1");
		assertEquals(JsonParser.parseString("[[\"ba\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 starting a dictionary key")
	public void test355() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ca=1");
		assertEquals(JsonParser.parseString("[[\"ca\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 starting a dictionary key")
	public void test356() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("da=1");
		assertEquals(JsonParser.parseString("[[\"da\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 starting a dictionary key")
	public void test357() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ea=1");
		assertEquals(JsonParser.parseString("[[\"ea\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 starting a dictionary key")
	public void test358() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("fa=1");
		assertEquals(JsonParser.parseString("[[\"fa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 starting a dictionary key")
	public void test359() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ga=1");
		assertEquals(JsonParser.parseString("[[\"ga\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 starting a dictionary key")
	public void test360() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ha=1");
		assertEquals(JsonParser.parseString("[[\"ha\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 starting a dictionary key")
	public void test361() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ia=1");
		assertEquals(JsonParser.parseString("[[\"ia\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a starting a dictionary key")
	public void test362() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ja=1");
		assertEquals(JsonParser.parseString("[[\"ja\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b starting a dictionary key")
	public void test363() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ka=1");
		assertEquals(JsonParser.parseString("[[\"ka\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c starting a dictionary key")
	public void test364() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("la=1");
		assertEquals(JsonParser.parseString("[[\"la\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d starting a dictionary key")
	public void test365() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ma=1");
		assertEquals(JsonParser.parseString("[[\"ma\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e starting a dictionary key")
	public void test366() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("na=1");
		assertEquals(JsonParser.parseString("[[\"na\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f starting a dictionary key")
	public void test367() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("oa=1");
		assertEquals(JsonParser.parseString("[[\"oa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 starting a dictionary key")
	public void test368() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("pa=1");
		assertEquals(JsonParser.parseString("[[\"pa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 starting a dictionary key")
	public void test369() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("qa=1");
		assertEquals(JsonParser.parseString("[[\"qa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 starting a dictionary key")
	public void test370() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ra=1");
		assertEquals(JsonParser.parseString("[[\"ra\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 starting a dictionary key")
	public void test371() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("sa=1");
		assertEquals(JsonParser.parseString("[[\"sa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 starting a dictionary key")
	public void test372() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ta=1");
		assertEquals(JsonParser.parseString("[[\"ta\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 starting a dictionary key")
	public void test373() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ua=1");
		assertEquals(JsonParser.parseString("[[\"ua\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 starting a dictionary key")
	public void test374() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("va=1");
		assertEquals(JsonParser.parseString("[[\"va\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 starting a dictionary key")
	public void test375() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("wa=1");
		assertEquals(JsonParser.parseString("[[\"wa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 starting a dictionary key")
	public void test376() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("xa=1");
		assertEquals(JsonParser.parseString("[[\"xa\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 starting a dictionary key")
	public void test377() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("ya=1");
		assertEquals(JsonParser.parseString("[[\"ya\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a starting a dictionary key")
	public void test378() {
		Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("za=1");
		assertEquals(JsonParser.parseString("[[\"za\",[1,[]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b starting a dictionary key")
	public void test379() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("{a=1");
		} );
	}

	@Test
	@DisplayName("0x7c starting a dictionary key")
	public void test380() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("|a=1");
		} );
	}

	@Test
	@DisplayName("0x7d starting a dictionary key")
	public void test381() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("}a=1");
		} );
	}

	@Test
	@DisplayName("0x7e starting a dictionary key")
	public void test382() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("~a=1");
		} );
	}

	@Test
	@DisplayName("0x7f starting a dictionary key")
	public void test383() {
		assertThrows(IllegalArgumentException.class, ()-> {
			Map<String, StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseDictionary("\u007fa=1");
		} );
	}

	@Test
	@DisplayName("0x00 in parameterised list key")
	public void test384() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0000a=1");
		} );
	}

	@Test
	@DisplayName("0x01 in parameterised list key")
	public void test385() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0001a=1");
		} );
	}

	@Test
	@DisplayName("0x02 in parameterised list key")
	public void test386() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0002a=1");
		} );
	}

	@Test
	@DisplayName("0x03 in parameterised list key")
	public void test387() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0003a=1");
		} );
	}

	@Test
	@DisplayName("0x04 in parameterised list key")
	public void test388() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0004a=1");
		} );
	}

	@Test
	@DisplayName("0x05 in parameterised list key")
	public void test389() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0005a=1");
		} );
	}

	@Test
	@DisplayName("0x06 in parameterised list key")
	public void test390() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0006a=1");
		} );
	}

	@Test
	@DisplayName("0x07 in parameterised list key")
	public void test391() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0007a=1");
		} );
	}

	@Test
	@DisplayName("0x08 in parameterised list key")
	public void test392() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\ba=1");
		} );
	}

	@Test
	@DisplayName("0x09 in parameterised list key")
	public void test393() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\ta=1");
		} );
	}

	@Test
	@DisplayName("0x0a in parameterised list key")
	public void test394() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\n"
							+ "a=1");
		} );
	}

	@Test
	@DisplayName("0x0b in parameterised list key")
	public void test395() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u000ba=1");
		} );
	}

	@Test
	@DisplayName("0x0c in parameterised list key")
	public void test396() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\fa=1");
		} );
	}

	@Test
	@DisplayName("0x0d in parameterised list key")
	public void test397() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\ra=1");
		} );
	}

	@Test
	@DisplayName("0x0e in parameterised list key")
	public void test398() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u000ea=1");
		} );
	}

	@Test
	@DisplayName("0x0f in parameterised list key")
	public void test399() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u000fa=1");
		} );
	}

	@Test
	@DisplayName("0x10 in parameterised list key")
	public void test400() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0010a=1");
		} );
	}

	@Test
	@DisplayName("0x11 in parameterised list key")
	public void test401() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0011a=1");
		} );
	}

	@Test
	@DisplayName("0x12 in parameterised list key")
	public void test402() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0012a=1");
		} );
	}

	@Test
	@DisplayName("0x13 in parameterised list key")
	public void test403() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0013a=1");
		} );
	}

	@Test
	@DisplayName("0x14 in parameterised list key")
	public void test404() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0014a=1");
		} );
	}

	@Test
	@DisplayName("0x15 in parameterised list key")
	public void test405() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0015a=1");
		} );
	}

	@Test
	@DisplayName("0x16 in parameterised list key")
	public void test406() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0016a=1");
		} );
	}

	@Test
	@DisplayName("0x17 in parameterised list key")
	public void test407() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0017a=1");
		} );
	}

	@Test
	@DisplayName("0x18 in parameterised list key")
	public void test408() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0018a=1");
		} );
	}

	@Test
	@DisplayName("0x19 in parameterised list key")
	public void test409() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u0019a=1");
		} );
	}

	@Test
	@DisplayName("0x1a in parameterised list key")
	public void test410() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001aa=1");
		} );
	}

	@Test
	@DisplayName("0x1b in parameterised list key")
	public void test411() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001ba=1");
		} );
	}

	@Test
	@DisplayName("0x1c in parameterised list key")
	public void test412() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001ca=1");
		} );
	}

	@Test
	@DisplayName("0x1d in parameterised list key")
	public void test413() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001da=1");
		} );
	}

	@Test
	@DisplayName("0x1e in parameterised list key")
	public void test414() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001ea=1");
		} );
	}

	@Test
	@DisplayName("0x1f in parameterised list key")
	public void test415() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u001fa=1");
		} );
	}

	@Test
	@DisplayName("0x20 in parameterised list key")
	public void test416() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a a=1");
		} );
	}

	@Test
	@DisplayName("0x21 in parameterised list key")
	public void test417() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a!a=1");
		} );
	}

	@Test
	@DisplayName("0x22 in parameterised list key")
	public void test418() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\"a=1");
		} );
	}

	@Test
	@DisplayName("0x23 in parameterised list key")
	public void test419() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a#a=1");
		} );
	}

	@Test
	@DisplayName("0x24 in parameterised list key")
	public void test420() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a$a=1");
		} );
	}

	@Test
	@DisplayName("0x25 in parameterised list key")
	public void test421() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a%a=1");
		} );
	}

	@Test
	@DisplayName("0x26 in parameterised list key")
	public void test422() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a&a=1");
		} );
	}

	@Test
	@DisplayName("0x27 in parameterised list key")
	public void test423() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a'a=1");
		} );
	}

	@Test
	@DisplayName("0x28 in parameterised list key")
	public void test424() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a(a=1");
		} );
	}

	@Test
	@DisplayName("0x29 in parameterised list key")
	public void test425() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a)a=1");
		} );
	}

	@Test
	@DisplayName("0x2a in parameterised list key")
	public void test426() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a*a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a*a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b in parameterised list key")
	public void test427() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a+a=1");
		} );
	}

	@Test
	@DisplayName("0x2c in parameterised list key")
	public void test428() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a,a=1");
		} );
	}

	@Test
	@DisplayName("0x2d in parameterised list key")
	public void test429() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a-a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a-a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2e in parameterised list key")
	public void test430() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a.a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a.a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2f in parameterised list key")
	public void test431() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a/a=1");
		} );
	}

	@Test
	@DisplayName("0x30 in parameterised list key")
	public void test432() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a0a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a0a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x31 in parameterised list key")
	public void test433() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a1a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a1a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x32 in parameterised list key")
	public void test434() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a2a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a2a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x33 in parameterised list key")
	public void test435() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a3a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a3a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x34 in parameterised list key")
	public void test436() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a4a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a4a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x35 in parameterised list key")
	public void test437() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a5a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a5a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x36 in parameterised list key")
	public void test438() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a6a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a6a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x37 in parameterised list key")
	public void test439() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a7a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a7a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x38 in parameterised list key")
	public void test440() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a8a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a8a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x39 in parameterised list key")
	public void test441() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a9a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a9a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3a in parameterised list key")
	public void test442() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a:a=1");
		} );
	}

	@Test
	@DisplayName("0x3b in parameterised list key")
	public void test443() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a;a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x3c in parameterised list key")
	public void test444() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a<a=1");
		} );
	}

	@Test
	@DisplayName("0x3d in parameterised list key")
	public void test445() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a=a=1");
		} );
	}

	@Test
	@DisplayName("0x3e in parameterised list key")
	public void test446() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a>a=1");
		} );
	}

	@Test
	@DisplayName("0x3f in parameterised list key")
	public void test447() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a?a=1");
		} );
	}

	@Test
	@DisplayName("0x40 in parameterised list key")
	public void test448() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a@a=1");
		} );
	}

	@Test
	@DisplayName("0x41 in parameterised list key")
	public void test449() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aAa=1");
		} );
	}

	@Test
	@DisplayName("0x42 in parameterised list key")
	public void test450() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aBa=1");
		} );
	}

	@Test
	@DisplayName("0x43 in parameterised list key")
	public void test451() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aCa=1");
		} );
	}

	@Test
	@DisplayName("0x44 in parameterised list key")
	public void test452() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aDa=1");
		} );
	}

	@Test
	@DisplayName("0x45 in parameterised list key")
	public void test453() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aEa=1");
		} );
	}

	@Test
	@DisplayName("0x46 in parameterised list key")
	public void test454() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aFa=1");
		} );
	}

	@Test
	@DisplayName("0x47 in parameterised list key")
	public void test455() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aGa=1");
		} );
	}

	@Test
	@DisplayName("0x48 in parameterised list key")
	public void test456() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aHa=1");
		} );
	}

	@Test
	@DisplayName("0x49 in parameterised list key")
	public void test457() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aIa=1");
		} );
	}

	@Test
	@DisplayName("0x4a in parameterised list key")
	public void test458() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aJa=1");
		} );
	}

	@Test
	@DisplayName("0x4b in parameterised list key")
	public void test459() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aKa=1");
		} );
	}

	@Test
	@DisplayName("0x4c in parameterised list key")
	public void test460() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aLa=1");
		} );
	}

	@Test
	@DisplayName("0x4d in parameterised list key")
	public void test461() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aMa=1");
		} );
	}

	@Test
	@DisplayName("0x4e in parameterised list key")
	public void test462() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aNa=1");
		} );
	}

	@Test
	@DisplayName("0x4f in parameterised list key")
	public void test463() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aOa=1");
		} );
	}

	@Test
	@DisplayName("0x50 in parameterised list key")
	public void test464() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aPa=1");
		} );
	}

	@Test
	@DisplayName("0x51 in parameterised list key")
	public void test465() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aQa=1");
		} );
	}

	@Test
	@DisplayName("0x52 in parameterised list key")
	public void test466() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aRa=1");
		} );
	}

	@Test
	@DisplayName("0x53 in parameterised list key")
	public void test467() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aSa=1");
		} );
	}

	@Test
	@DisplayName("0x54 in parameterised list key")
	public void test468() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aTa=1");
		} );
	}

	@Test
	@DisplayName("0x55 in parameterised list key")
	public void test469() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aUa=1");
		} );
	}

	@Test
	@DisplayName("0x56 in parameterised list key")
	public void test470() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aVa=1");
		} );
	}

	@Test
	@DisplayName("0x57 in parameterised list key")
	public void test471() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aWa=1");
		} );
	}

	@Test
	@DisplayName("0x58 in parameterised list key")
	public void test472() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aXa=1");
		} );
	}

	@Test
	@DisplayName("0x59 in parameterised list key")
	public void test473() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aYa=1");
		} );
	}

	@Test
	@DisplayName("0x5a in parameterised list key")
	public void test474() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aZa=1");
		} );
	}

	@Test
	@DisplayName("0x5b in parameterised list key")
	public void test475() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a[a=1");
		} );
	}

	@Test
	@DisplayName("0x5c in parameterised list key")
	public void test476() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\\a=1");
		} );
	}

	@Test
	@DisplayName("0x5d in parameterised list key")
	public void test477() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a]a=1");
		} );
	}

	@Test
	@DisplayName("0x5e in parameterised list key")
	public void test478() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a^a=1");
		} );
	}

	@Test
	@DisplayName("0x5f in parameterised list key")
	public void test479() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a_a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a_a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x60 in parameterised list key")
	public void test480() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a`a=1");
		} );
	}

	@Test
	@DisplayName("0x61 in parameterised list key")
	public void test481() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aaa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aaa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 in parameterised list key")
	public void test482() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aba=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aba\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 in parameterised list key")
	public void test483() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aca=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aca\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 in parameterised list key")
	public void test484() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ada=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ada\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 in parameterised list key")
	public void test485() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aea=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aea\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 in parameterised list key")
	public void test486() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; afa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"afa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 in parameterised list key")
	public void test487() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aga=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aga\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 in parameterised list key")
	public void test488() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aha=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aha\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 in parameterised list key")
	public void test489() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aia=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aia\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a in parameterised list key")
	public void test490() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aja=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aja\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b in parameterised list key")
	public void test491() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aka=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aka\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c in parameterised list key")
	public void test492() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ala=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ala\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d in parameterised list key")
	public void test493() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ama=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ama\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e in parameterised list key")
	public void test494() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ana=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ana\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f in parameterised list key")
	public void test495() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aoa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aoa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 in parameterised list key")
	public void test496() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; apa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"apa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 in parameterised list key")
	public void test497() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aqa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aqa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 in parameterised list key")
	public void test498() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ara=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ara\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 in parameterised list key")
	public void test499() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; asa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"asa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 in parameterised list key")
	public void test500() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ata=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ata\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 in parameterised list key")
	public void test501() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aua=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aua\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 in parameterised list key")
	public void test502() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ava=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ava\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 in parameterised list key")
	public void test503() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; awa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"awa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 in parameterised list key")
	public void test504() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; axa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"axa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 in parameterised list key")
	public void test505() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aya=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aya\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a in parameterised list key")
	public void test506() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aza=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aza\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b in parameterised list key")
	public void test507() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a{a=1");
		} );
	}

	@Test
	@DisplayName("0x7c in parameterised list key")
	public void test508() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a|a=1");
		} );
	}

	@Test
	@DisplayName("0x7d in parameterised list key")
	public void test509() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a}a=1");
		} );
	}

	@Test
	@DisplayName("0x7e in parameterised list key")
	public void test510() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a~a=1");
		} );
	}

	@Test
	@DisplayName("0x7f in parameterised list key")
	public void test511() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; a\u007fa=1");
		} );
	}

	@Test
	@DisplayName("0x00 starting a parameterised list key")
	public void test512() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0000a=1");
		} );
	}

	@Test
	@DisplayName("0x01 starting a parameterised list key")
	public void test513() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0001a=1");
		} );
	}

	@Test
	@DisplayName("0x02 starting a parameterised list key")
	public void test514() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0002a=1");
		} );
	}

	@Test
	@DisplayName("0x03 starting a parameterised list key")
	public void test515() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0003a=1");
		} );
	}

	@Test
	@DisplayName("0x04 starting a parameterised list key")
	public void test516() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0004a=1");
		} );
	}

	@Test
	@DisplayName("0x05 starting a parameterised list key")
	public void test517() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0005a=1");
		} );
	}

	@Test
	@DisplayName("0x06 starting a parameterised list key")
	public void test518() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0006a=1");
		} );
	}

	@Test
	@DisplayName("0x07 starting a parameterised list key")
	public void test519() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0007a=1");
		} );
	}

	@Test
	@DisplayName("0x08 starting a parameterised list key")
	public void test520() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \ba=1");
		} );
	}

	@Test
	@DisplayName("0x09 starting a parameterised list key")
	public void test521() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \ta=1");
		} );
	}

	@Test
	@DisplayName("0x0a starting a parameterised list key")
	public void test522() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \n"
							+ "a=1");
		} );
	}

	@Test
	@DisplayName("0x0b starting a parameterised list key")
	public void test523() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u000ba=1");
		} );
	}

	@Test
	@DisplayName("0x0c starting a parameterised list key")
	public void test524() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \fa=1");
		} );
	}

	@Test
	@DisplayName("0x0d starting a parameterised list key")
	public void test525() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \ra=1");
		} );
	}

	@Test
	@DisplayName("0x0e starting a parameterised list key")
	public void test526() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u000ea=1");
		} );
	}

	@Test
	@DisplayName("0x0f starting a parameterised list key")
	public void test527() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u000fa=1");
		} );
	}

	@Test
	@DisplayName("0x10 starting a parameterised list key")
	public void test528() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0010a=1");
		} );
	}

	@Test
	@DisplayName("0x11 starting a parameterised list key")
	public void test529() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0011a=1");
		} );
	}

	@Test
	@DisplayName("0x12 starting a parameterised list key")
	public void test530() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0012a=1");
		} );
	}

	@Test
	@DisplayName("0x13 starting a parameterised list key")
	public void test531() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0013a=1");
		} );
	}

	@Test
	@DisplayName("0x14 starting a parameterised list key")
	public void test532() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0014a=1");
		} );
	}

	@Test
	@DisplayName("0x15 starting a parameterised list key")
	public void test533() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0015a=1");
		} );
	}

	@Test
	@DisplayName("0x16 starting a parameterised list key")
	public void test534() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0016a=1");
		} );
	}

	@Test
	@DisplayName("0x17 starting a parameterised list key")
	public void test535() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0017a=1");
		} );
	}

	@Test
	@DisplayName("0x18 starting a parameterised list key")
	public void test536() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0018a=1");
		} );
	}

	@Test
	@DisplayName("0x19 starting a parameterised list key")
	public void test537() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u0019a=1");
		} );
	}

	@Test
	@DisplayName("0x1a starting a parameterised list key")
	public void test538() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001aa=1");
		} );
	}

	@Test
	@DisplayName("0x1b starting a parameterised list key")
	public void test539() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001ba=1");
		} );
	}

	@Test
	@DisplayName("0x1c starting a parameterised list key")
	public void test540() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001ca=1");
		} );
	}

	@Test
	@DisplayName("0x1d starting a parameterised list key")
	public void test541() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001da=1");
		} );
	}

	@Test
	@DisplayName("0x1e starting a parameterised list key")
	public void test542() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001ea=1");
		} );
	}

	@Test
	@DisplayName("0x1f starting a parameterised list key")
	public void test543() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u001fa=1");
		} );
	}

	@Test
	@DisplayName("0x20 starting a parameterised list key")
	public void test544() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo;  a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x21 starting a parameterised list key")
	public void test545() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; !a=1");
		} );
	}

	@Test
	@DisplayName("0x22 starting a parameterised list key")
	public void test546() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \"a=1");
		} );
	}

	@Test
	@DisplayName("0x23 starting a parameterised list key")
	public void test547() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; #a=1");
		} );
	}

	@Test
	@DisplayName("0x24 starting a parameterised list key")
	public void test548() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; $a=1");
		} );
	}

	@Test
	@DisplayName("0x25 starting a parameterised list key")
	public void test549() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; %a=1");
		} );
	}

	@Test
	@DisplayName("0x26 starting a parameterised list key")
	public void test550() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; &a=1");
		} );
	}

	@Test
	@DisplayName("0x27 starting a parameterised list key")
	public void test551() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 'a=1");
		} );
	}

	@Test
	@DisplayName("0x28 starting a parameterised list key")
	public void test552() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; (a=1");
		} );
	}

	@Test
	@DisplayName("0x29 starting a parameterised list key")
	public void test553() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; )a=1");
		} );
	}

	@Test
	@DisplayName("0x2a starting a parameterised list key")
	public void test554() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; *a=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"*a\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x2b starting a parameterised list key")
	public void test555() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; +a=1");
		} );
	}

	@Test
	@DisplayName("0x2c starting a parameterised list key")
	public void test556() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ,a=1");
		} );
	}

	@Test
	@DisplayName("0x2d starting a parameterised list key")
	public void test557() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; -a=1");
		} );
	}

	@Test
	@DisplayName("0x2e starting a parameterised list key")
	public void test558() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; .a=1");
		} );
	}

	@Test
	@DisplayName("0x2f starting a parameterised list key")
	public void test559() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; /a=1");
		} );
	}

	@Test
	@DisplayName("0x30 starting a parameterised list key")
	public void test560() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 0a=1");
		} );
	}

	@Test
	@DisplayName("0x31 starting a parameterised list key")
	public void test561() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 1a=1");
		} );
	}

	@Test
	@DisplayName("0x32 starting a parameterised list key")
	public void test562() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 2a=1");
		} );
	}

	@Test
	@DisplayName("0x33 starting a parameterised list key")
	public void test563() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 3a=1");
		} );
	}

	@Test
	@DisplayName("0x34 starting a parameterised list key")
	public void test564() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 4a=1");
		} );
	}

	@Test
	@DisplayName("0x35 starting a parameterised list key")
	public void test565() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 5a=1");
		} );
	}

	@Test
	@DisplayName("0x36 starting a parameterised list key")
	public void test566() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 6a=1");
		} );
	}

	@Test
	@DisplayName("0x37 starting a parameterised list key")
	public void test567() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 7a=1");
		} );
	}

	@Test
	@DisplayName("0x38 starting a parameterised list key")
	public void test568() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 8a=1");
		} );
	}

	@Test
	@DisplayName("0x39 starting a parameterised list key")
	public void test569() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; 9a=1");
		} );
	}

	@Test
	@DisplayName("0x3a starting a parameterised list key")
	public void test570() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; :a=1");
		} );
	}

	@Test
	@DisplayName("0x3b starting a parameterised list key")
	public void test571() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ;a=1");
		} );
	}

	@Test
	@DisplayName("0x3c starting a parameterised list key")
	public void test572() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; <a=1");
		} );
	}

	@Test
	@DisplayName("0x3d starting a parameterised list key")
	public void test573() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; =a=1");
		} );
	}

	@Test
	@DisplayName("0x3e starting a parameterised list key")
	public void test574() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; >a=1");
		} );
	}

	@Test
	@DisplayName("0x3f starting a parameterised list key")
	public void test575() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ?a=1");
		} );
	}

	@Test
	@DisplayName("0x40 starting a parameterised list key")
	public void test576() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; @a=1");
		} );
	}

	@Test
	@DisplayName("0x41 starting a parameterised list key")
	public void test577() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Aa=1");
		} );
	}

	@Test
	@DisplayName("0x42 starting a parameterised list key")
	public void test578() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ba=1");
		} );
	}

	@Test
	@DisplayName("0x43 starting a parameterised list key")
	public void test579() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ca=1");
		} );
	}

	@Test
	@DisplayName("0x44 starting a parameterised list key")
	public void test580() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Da=1");
		} );
	}

	@Test
	@DisplayName("0x45 starting a parameterised list key")
	public void test581() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ea=1");
		} );
	}

	@Test
	@DisplayName("0x46 starting a parameterised list key")
	public void test582() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Fa=1");
		} );
	}

	@Test
	@DisplayName("0x47 starting a parameterised list key")
	public void test583() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ga=1");
		} );
	}

	@Test
	@DisplayName("0x48 starting a parameterised list key")
	public void test584() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ha=1");
		} );
	}

	@Test
	@DisplayName("0x49 starting a parameterised list key")
	public void test585() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ia=1");
		} );
	}

	@Test
	@DisplayName("0x4a starting a parameterised list key")
	public void test586() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ja=1");
		} );
	}

	@Test
	@DisplayName("0x4b starting a parameterised list key")
	public void test587() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ka=1");
		} );
	}

	@Test
	@DisplayName("0x4c starting a parameterised list key")
	public void test588() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; La=1");
		} );
	}

	@Test
	@DisplayName("0x4d starting a parameterised list key")
	public void test589() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ma=1");
		} );
	}

	@Test
	@DisplayName("0x4e starting a parameterised list key")
	public void test590() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Na=1");
		} );
	}

	@Test
	@DisplayName("0x4f starting a parameterised list key")
	public void test591() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Oa=1");
		} );
	}

	@Test
	@DisplayName("0x50 starting a parameterised list key")
	public void test592() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Pa=1");
		} );
	}

	@Test
	@DisplayName("0x51 starting a parameterised list key")
	public void test593() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Qa=1");
		} );
	}

	@Test
	@DisplayName("0x52 starting a parameterised list key")
	public void test594() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ra=1");
		} );
	}

	@Test
	@DisplayName("0x53 starting a parameterised list key")
	public void test595() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Sa=1");
		} );
	}

	@Test
	@DisplayName("0x54 starting a parameterised list key")
	public void test596() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ta=1");
		} );
	}

	@Test
	@DisplayName("0x55 starting a parameterised list key")
	public void test597() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ua=1");
		} );
	}

	@Test
	@DisplayName("0x56 starting a parameterised list key")
	public void test598() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Va=1");
		} );
	}

	@Test
	@DisplayName("0x57 starting a parameterised list key")
	public void test599() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Wa=1");
		} );
	}

	@Test
	@DisplayName("0x58 starting a parameterised list key")
	public void test600() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Xa=1");
		} );
	}

	@Test
	@DisplayName("0x59 starting a parameterised list key")
	public void test601() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Ya=1");
		} );
	}

	@Test
	@DisplayName("0x5a starting a parameterised list key")
	public void test602() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; Za=1");
		} );
	}

	@Test
	@DisplayName("0x5b starting a parameterised list key")
	public void test603() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; [a=1");
		} );
	}

	@Test
	@DisplayName("0x5c starting a parameterised list key")
	public void test604() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \\a=1");
		} );
	}

	@Test
	@DisplayName("0x5d starting a parameterised list key")
	public void test605() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ]a=1");
		} );
	}

	@Test
	@DisplayName("0x5e starting a parameterised list key")
	public void test606() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ^a=1");
		} );
	}

	@Test
	@DisplayName("0x5f starting a parameterised list key")
	public void test607() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; _a=1");
		} );
	}

	@Test
	@DisplayName("0x60 starting a parameterised list key")
	public void test608() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; `a=1");
		} );
	}

	@Test
	@DisplayName("0x61 starting a parameterised list key")
	public void test609() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; aa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"aa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x62 starting a parameterised list key")
	public void test610() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ba=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ba\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x63 starting a parameterised list key")
	public void test611() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ca=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ca\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x64 starting a parameterised list key")
	public void test612() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; da=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"da\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x65 starting a parameterised list key")
	public void test613() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ea=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ea\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x66 starting a parameterised list key")
	public void test614() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; fa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"fa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x67 starting a parameterised list key")
	public void test615() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ga=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ga\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x68 starting a parameterised list key")
	public void test616() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ha=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ha\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x69 starting a parameterised list key")
	public void test617() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ia=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ia\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6a starting a parameterised list key")
	public void test618() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ja=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ja\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6b starting a parameterised list key")
	public void test619() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ka=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ka\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6c starting a parameterised list key")
	public void test620() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; la=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"la\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6d starting a parameterised list key")
	public void test621() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ma=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ma\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6e starting a parameterised list key")
	public void test622() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; na=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"na\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x6f starting a parameterised list key")
	public void test623() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; oa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"oa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x70 starting a parameterised list key")
	public void test624() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; pa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"pa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x71 starting a parameterised list key")
	public void test625() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; qa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"qa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x72 starting a parameterised list key")
	public void test626() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ra=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ra\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x73 starting a parameterised list key")
	public void test627() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; sa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"sa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x74 starting a parameterised list key")
	public void test628() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ta=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ta\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x75 starting a parameterised list key")
	public void test629() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ua=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ua\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x76 starting a parameterised list key")
	public void test630() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; va=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"va\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x77 starting a parameterised list key")
	public void test631() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; wa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"wa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x78 starting a parameterised list key")
	public void test632() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; xa=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"xa\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x79 starting a parameterised list key")
	public void test633() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ya=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"ya\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7a starting a parameterised list key")
	public void test634() {
		List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; za=1");
		assertEquals(JsonParser.parseString("[[{\"__type\":\"token\",\"value\":\"foo\"},[[\"za\",1]]]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("0x7b starting a parameterised list key")
	public void test635() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; {a=1");
		} );
	}

	@Test
	@DisplayName("0x7c starting a parameterised list key")
	public void test636() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; |a=1");
		} );
	}

	@Test
	@DisplayName("0x7d starting a parameterised list key")
	public void test637() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; }a=1");
		} );
	}

	@Test
	@DisplayName("0x7e starting a parameterised list key")
	public void test638() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; ~a=1");
		} );
	}

	@Test
	@DisplayName("0x7f starting a parameterised list key")
	public void test639() {
		assertThrows(IllegalArgumentException.class, ()-> {
			List<StructuredHttpHeaders.ItemOrInnerList> result=StructuredHttpHeaders.parseList("foo; \u007fa=1");
		} );
	}
}
