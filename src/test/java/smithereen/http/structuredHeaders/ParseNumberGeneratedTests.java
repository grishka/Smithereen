package smithereen.http.structuredHeaders;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.IllegalArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class ParseNumberGeneratedTests {
	@Test
	@DisplayName("1 digits of zero")
	public void test0() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("1 digit small integer")
	public void test1() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1");
		assertEquals(JsonParser.parseString("[1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("1 digit large integer")
	public void test2() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9");
		assertEquals(JsonParser.parseString("[9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digits of zero")
	public void test3() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit small integer")
	public void test4() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11");
		assertEquals(JsonParser.parseString("[11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit large integer")
	public void test5() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99");
		assertEquals(JsonParser.parseString("[99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digits of zero")
	public void test6() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit small integer")
	public void test7() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111");
		assertEquals(JsonParser.parseString("[111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit large integer")
	public void test8() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999");
		assertEquals(JsonParser.parseString("[999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digits of zero")
	public void test9() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit small integer")
	public void test10() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111");
		assertEquals(JsonParser.parseString("[1111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit large integer")
	public void test11() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999");
		assertEquals(JsonParser.parseString("[9999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digits of zero")
	public void test12() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit small integer")
	public void test13() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111");
		assertEquals(JsonParser.parseString("[11111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit large integer")
	public void test14() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999");
		assertEquals(JsonParser.parseString("[99999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digits of zero")
	public void test15() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit small integer")
	public void test16() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111");
		assertEquals(JsonParser.parseString("[111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit large integer")
	public void test17() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999");
		assertEquals(JsonParser.parseString("[999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digits of zero")
	public void test18() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit small integer")
	public void test19() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111");
		assertEquals(JsonParser.parseString("[1111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit large integer")
	public void test20() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999");
		assertEquals(JsonParser.parseString("[9999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digits of zero")
	public void test21() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit small integer")
	public void test22() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111");
		assertEquals(JsonParser.parseString("[11111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit large integer")
	public void test23() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999");
		assertEquals(JsonParser.parseString("[99999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digits of zero")
	public void test24() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit small integer")
	public void test25() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111");
		assertEquals(JsonParser.parseString("[111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit large integer")
	public void test26() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999");
		assertEquals(JsonParser.parseString("[999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digits of zero")
	public void test27() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit small integer")
	public void test28() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111");
		assertEquals(JsonParser.parseString("[1111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit large integer")
	public void test29() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999999");
		assertEquals(JsonParser.parseString("[9999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digits of zero")
	public void test30() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit small integer")
	public void test31() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111");
		assertEquals(JsonParser.parseString("[11111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit large integer")
	public void test32() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999999");
		assertEquals(JsonParser.parseString("[99999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digits of zero")
	public void test33() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit small integer")
	public void test34() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111");
		assertEquals(JsonParser.parseString("[111111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit large integer")
	public void test35() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999");
		assertEquals(JsonParser.parseString("[999999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digits of zero")
	public void test36() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit small integer")
	public void test37() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111111");
		assertEquals(JsonParser.parseString("[1111111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit large integer")
	public void test38() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999999999");
		assertEquals(JsonParser.parseString("[9999999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digits of zero")
	public void test39() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit small integer")
	public void test40() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111111");
		assertEquals(JsonParser.parseString("[11111111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit large integer")
	public void test41() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999999999");
		assertEquals(JsonParser.parseString("[99999999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digits of zero")
	public void test42() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000000");
		assertEquals(JsonParser.parseString("[0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit small integer")
	public void test43() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111111");
		assertEquals(JsonParser.parseString("[111111111111111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit large integer")
	public void test44() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999999");
		assertEquals(JsonParser.parseString("[999999999999999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit 0, 1 fractional small decimal")
	public void test45() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit, 1 fractional 0 decimal")
	public void test46() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.0");
		assertEquals(JsonParser.parseString("[1.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit, 1 fractional small decimal")
	public void test47() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.1");
		assertEquals(JsonParser.parseString("[1.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("2 digit, 1 fractional large decimal")
	public void test48() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9.9");
		assertEquals(JsonParser.parseString("[9.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit 0, 2 fractional small decimal")
	public void test49() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 2 fractional 0 decimal")
	public void test50() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.00");
		assertEquals(JsonParser.parseString("[1.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 2 fractional small decimal")
	public void test51() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.11");
		assertEquals(JsonParser.parseString("[1.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 2 fractional large decimal")
	public void test52() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9.99");
		assertEquals(JsonParser.parseString("[9.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit 0, 3 fractional small decimal")
	public void test53() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 3 fractional 0 decimal")
	public void test54() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.000");
		assertEquals(JsonParser.parseString("[1.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 3 fractional small decimal")
	public void test55() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1.111");
		assertEquals(JsonParser.parseString("[1.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 3 fractional large decimal")
	public void test56() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9.999");
		assertEquals(JsonParser.parseString("[9.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit 0, 1 fractional small decimal")
	public void test57() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 1 fractional 0 decimal")
	public void test58() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.0");
		assertEquals(JsonParser.parseString("[11.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 1 fractional small decimal")
	public void test59() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.1");
		assertEquals(JsonParser.parseString("[11.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("3 digit, 1 fractional large decimal")
	public void test60() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99.9");
		assertEquals(JsonParser.parseString("[99.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit 0, 2 fractional small decimal")
	public void test61() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 2 fractional 0 decimal")
	public void test62() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.00");
		assertEquals(JsonParser.parseString("[11.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 2 fractional small decimal")
	public void test63() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.11");
		assertEquals(JsonParser.parseString("[11.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 2 fractional large decimal")
	public void test64() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99.99");
		assertEquals(JsonParser.parseString("[99.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit 0, 3 fractional small decimal")
	public void test65() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 3 fractional 0 decimal")
	public void test66() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.000");
		assertEquals(JsonParser.parseString("[11.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 3 fractional small decimal")
	public void test67() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11.111");
		assertEquals(JsonParser.parseString("[11.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 3 fractional large decimal")
	public void test68() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99.999");
		assertEquals(JsonParser.parseString("[99.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit 0, 1 fractional small decimal")
	public void test69() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 1 fractional 0 decimal")
	public void test70() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.0");
		assertEquals(JsonParser.parseString("[111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 1 fractional small decimal")
	public void test71() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.1");
		assertEquals(JsonParser.parseString("[111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("4 digit, 1 fractional large decimal")
	public void test72() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999.9");
		assertEquals(JsonParser.parseString("[999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit 0, 2 fractional small decimal")
	public void test73() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 2 fractional 0 decimal")
	public void test74() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.00");
		assertEquals(JsonParser.parseString("[111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 2 fractional small decimal")
	public void test75() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.11");
		assertEquals(JsonParser.parseString("[111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 2 fractional large decimal")
	public void test76() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999.99");
		assertEquals(JsonParser.parseString("[999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit 0, 3 fractional small decimal")
	public void test77() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 3 fractional 0 decimal")
	public void test78() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.000");
		assertEquals(JsonParser.parseString("[111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 3 fractional small decimal")
	public void test79() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111.111");
		assertEquals(JsonParser.parseString("[111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 3 fractional large decimal")
	public void test80() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999.999");
		assertEquals(JsonParser.parseString("[999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit 0, 1 fractional small decimal")
	public void test81() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 1 fractional 0 decimal")
	public void test82() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.0");
		assertEquals(JsonParser.parseString("[1111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 1 fractional small decimal")
	public void test83() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.1");
		assertEquals(JsonParser.parseString("[1111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("5 digit, 1 fractional large decimal")
	public void test84() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999.9");
		assertEquals(JsonParser.parseString("[9999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit 0, 2 fractional small decimal")
	public void test85() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 2 fractional 0 decimal")
	public void test86() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.00");
		assertEquals(JsonParser.parseString("[1111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 2 fractional small decimal")
	public void test87() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.11");
		assertEquals(JsonParser.parseString("[1111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 2 fractional large decimal")
	public void test88() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999.99");
		assertEquals(JsonParser.parseString("[9999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit 0, 3 fractional small decimal")
	public void test89() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 3 fractional 0 decimal")
	public void test90() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.000");
		assertEquals(JsonParser.parseString("[1111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 3 fractional small decimal")
	public void test91() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111.111");
		assertEquals(JsonParser.parseString("[1111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 3 fractional large decimal")
	public void test92() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999.999");
		assertEquals(JsonParser.parseString("[9999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit 0, 1 fractional small decimal")
	public void test93() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 1 fractional 0 decimal")
	public void test94() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.0");
		assertEquals(JsonParser.parseString("[11111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 1 fractional small decimal")
	public void test95() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.1");
		assertEquals(JsonParser.parseString("[11111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("6 digit, 1 fractional large decimal")
	public void test96() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999.9");
		assertEquals(JsonParser.parseString("[99999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit 0, 2 fractional small decimal")
	public void test97() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 2 fractional 0 decimal")
	public void test98() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.00");
		assertEquals(JsonParser.parseString("[11111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 2 fractional small decimal")
	public void test99() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.11");
		assertEquals(JsonParser.parseString("[11111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 2 fractional large decimal")
	public void test100() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999.99");
		assertEquals(JsonParser.parseString("[99999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit 0, 3 fractional small decimal")
	public void test101() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 3 fractional 0 decimal")
	public void test102() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.000");
		assertEquals(JsonParser.parseString("[11111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 3 fractional small decimal")
	public void test103() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111.111");
		assertEquals(JsonParser.parseString("[11111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 3 fractional large decimal")
	public void test104() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999.999");
		assertEquals(JsonParser.parseString("[99999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit 0, 1 fractional small decimal")
	public void test105() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 1 fractional 0 decimal")
	public void test106() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.0");
		assertEquals(JsonParser.parseString("[111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 1 fractional small decimal")
	public void test107() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.1");
		assertEquals(JsonParser.parseString("[111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("7 digit, 1 fractional large decimal")
	public void test108() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999.9");
		assertEquals(JsonParser.parseString("[999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit 0, 2 fractional small decimal")
	public void test109() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 2 fractional 0 decimal")
	public void test110() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.00");
		assertEquals(JsonParser.parseString("[111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 2 fractional small decimal")
	public void test111() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.11");
		assertEquals(JsonParser.parseString("[111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 2 fractional large decimal")
	public void test112() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999.99");
		assertEquals(JsonParser.parseString("[999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit 0, 3 fractional small decimal")
	public void test113() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 3 fractional 0 decimal")
	public void test114() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.000");
		assertEquals(JsonParser.parseString("[111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 3 fractional small decimal")
	public void test115() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111.111");
		assertEquals(JsonParser.parseString("[111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 3 fractional large decimal")
	public void test116() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999.999");
		assertEquals(JsonParser.parseString("[999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit 0, 1 fractional small decimal")
	public void test117() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 1 fractional 0 decimal")
	public void test118() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.0");
		assertEquals(JsonParser.parseString("[1111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 1 fractional small decimal")
	public void test119() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.1");
		assertEquals(JsonParser.parseString("[1111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("8 digit, 1 fractional large decimal")
	public void test120() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999.9");
		assertEquals(JsonParser.parseString("[9999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit 0, 2 fractional small decimal")
	public void test121() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 2 fractional 0 decimal")
	public void test122() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.00");
		assertEquals(JsonParser.parseString("[1111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 2 fractional small decimal")
	public void test123() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.11");
		assertEquals(JsonParser.parseString("[1111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 2 fractional large decimal")
	public void test124() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999.99");
		assertEquals(JsonParser.parseString("[9999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit 0, 3 fractional small decimal")
	public void test125() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 3 fractional 0 decimal")
	public void test126() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.000");
		assertEquals(JsonParser.parseString("[1111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 3 fractional small decimal")
	public void test127() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111.111");
		assertEquals(JsonParser.parseString("[1111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 3 fractional large decimal")
	public void test128() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999.999");
		assertEquals(JsonParser.parseString("[9999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit 0, 1 fractional small decimal")
	public void test129() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 1 fractional 0 decimal")
	public void test130() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.0");
		assertEquals(JsonParser.parseString("[11111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 1 fractional small decimal")
	public void test131() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.1");
		assertEquals(JsonParser.parseString("[11111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("9 digit, 1 fractional large decimal")
	public void test132() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999.9");
		assertEquals(JsonParser.parseString("[99999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit 0, 2 fractional small decimal")
	public void test133() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 2 fractional 0 decimal")
	public void test134() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.00");
		assertEquals(JsonParser.parseString("[11111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 2 fractional small decimal")
	public void test135() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.11");
		assertEquals(JsonParser.parseString("[11111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 2 fractional large decimal")
	public void test136() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999.99");
		assertEquals(JsonParser.parseString("[99999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit 0, 3 fractional small decimal")
	public void test137() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 3 fractional 0 decimal")
	public void test138() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.000");
		assertEquals(JsonParser.parseString("[11111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 3 fractional small decimal")
	public void test139() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111.111");
		assertEquals(JsonParser.parseString("[11111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 3 fractional large decimal")
	public void test140() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999.999");
		assertEquals(JsonParser.parseString("[99999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit 0, 1 fractional small decimal")
	public void test141() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 1 fractional 0 decimal")
	public void test142() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.0");
		assertEquals(JsonParser.parseString("[111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 1 fractional small decimal")
	public void test143() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.1");
		assertEquals(JsonParser.parseString("[111111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("10 digit, 1 fractional large decimal")
	public void test144() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999.9");
		assertEquals(JsonParser.parseString("[999999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit 0, 2 fractional small decimal")
	public void test145() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 2 fractional 0 decimal")
	public void test146() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.00");
		assertEquals(JsonParser.parseString("[111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 2 fractional small decimal")
	public void test147() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.11");
		assertEquals(JsonParser.parseString("[111111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 2 fractional large decimal")
	public void test148() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999.99");
		assertEquals(JsonParser.parseString("[999999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit 0, 3 fractional small decimal")
	public void test149() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 3 fractional 0 decimal")
	public void test150() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.000");
		assertEquals(JsonParser.parseString("[111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 3 fractional small decimal")
	public void test151() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111.111");
		assertEquals(JsonParser.parseString("[111111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 3 fractional large decimal")
	public void test152() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999.999");
		assertEquals(JsonParser.parseString("[999999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit 0, 1 fractional small decimal")
	public void test153() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 1 fractional 0 decimal")
	public void test154() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.0");
		assertEquals(JsonParser.parseString("[1111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 1 fractional small decimal")
	public void test155() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.1");
		assertEquals(JsonParser.parseString("[1111111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("11 digit, 1 fractional large decimal")
	public void test156() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999999.9");
		assertEquals(JsonParser.parseString("[9999999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit 0, 2 fractional small decimal")
	public void test157() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 2 fractional 0 decimal")
	public void test158() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.00");
		assertEquals(JsonParser.parseString("[1111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 2 fractional small decimal")
	public void test159() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.11");
		assertEquals(JsonParser.parseString("[1111111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 2 fractional large decimal")
	public void test160() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999999.99");
		assertEquals(JsonParser.parseString("[9999999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit 0, 3 fractional small decimal")
	public void test161() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("0000000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 3 fractional 0 decimal")
	public void test162() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.000");
		assertEquals(JsonParser.parseString("[1111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 3 fractional small decimal")
	public void test163() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("1111111111.111");
		assertEquals(JsonParser.parseString("[1111111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 3 fractional large decimal")
	public void test164() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("9999999999.999");
		assertEquals(JsonParser.parseString("[9999999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit 0, 1 fractional small decimal")
	public void test165() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 1 fractional 0 decimal")
	public void test166() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.0");
		assertEquals(JsonParser.parseString("[11111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 1 fractional small decimal")
	public void test167() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.1");
		assertEquals(JsonParser.parseString("[11111111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("12 digit, 1 fractional large decimal")
	public void test168() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999999.9");
		assertEquals(JsonParser.parseString("[99999999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit 0, 2 fractional small decimal")
	public void test169() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 2 fractional 0 decimal")
	public void test170() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.00");
		assertEquals(JsonParser.parseString("[11111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 2 fractional small decimal")
	public void test171() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.11");
		assertEquals(JsonParser.parseString("[11111111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 2 fractional large decimal")
	public void test172() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999999.99");
		assertEquals(JsonParser.parseString("[99999999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit 0, 3 fractional small decimal")
	public void test173() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("00000000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 3 fractional 0 decimal")
	public void test174() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.000");
		assertEquals(JsonParser.parseString("[11111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 3 fractional small decimal")
	public void test175() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("11111111111.111");
		assertEquals(JsonParser.parseString("[11111111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 3 fractional large decimal")
	public void test176() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("99999999999.999");
		assertEquals(JsonParser.parseString("[99999999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit 0, 1 fractional small decimal")
	public void test177() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000.1");
		assertEquals(JsonParser.parseString("[0.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 1 fractional 0 decimal")
	public void test178() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.0");
		assertEquals(JsonParser.parseString("[111111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 1 fractional small decimal")
	public void test179() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.1");
		assertEquals(JsonParser.parseString("[111111111111.1,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("13 digit, 1 fractional large decimal")
	public void test180() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999.9");
		assertEquals(JsonParser.parseString("[999999999999.9,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit 0, 2 fractional small decimal")
	public void test181() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000.11");
		assertEquals(JsonParser.parseString("[0.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 2 fractional 0 decimal")
	public void test182() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.00");
		assertEquals(JsonParser.parseString("[111111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 2 fractional small decimal")
	public void test183() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.11");
		assertEquals(JsonParser.parseString("[111111111111.11,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("14 digit, 2 fractional large decimal")
	public void test184() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999.99");
		assertEquals(JsonParser.parseString("[999999999999.99,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit 0, 3 fractional small decimal")
	public void test185() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000.111");
		assertEquals(JsonParser.parseString("[0.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit, 3 fractional 0 decimal")
	public void test186() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.000");
		assertEquals(JsonParser.parseString("[111111111111.0,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit, 3 fractional small decimal")
	public void test187() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("111111111111.111");
		assertEquals(JsonParser.parseString("[111111111111.111,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("15 digit, 3 fractional large decimal")
	public void test188() {
		StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999.999");
		assertEquals(JsonParser.parseString("[999999999999.999,[]]"), StructuredHeadersTestUtils.toJson(result));
	}

	@Test
	@DisplayName("too many digit 0 decimal")
	public void test189() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000000.0");
		} );
	}

	@Test
	@DisplayName("too many fractional digits 0 decimal")
	public void test190() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("000000000000.0000");
		} );
	}

	@Test
	@DisplayName("too many digit 9 decimal")
	public void test191() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999999.9");
		} );
	}

	@Test
	@DisplayName("too many fractional digits 9 decimal")
	public void test192() {
		assertThrows(IllegalArgumentException.class, ()-> {
			StructuredHttpHeaders.Item result=StructuredHttpHeaders.parseItem("999999999999.9999");
		} );
	}
}
