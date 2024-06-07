package smithereen;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import smithereen.util.PublicSuffixList;

public class PublicSuffixListTests{
	@BeforeAll
	public static void setup(){
		PublicSuffixList.update(List.of("com", "*.foo.com", "*.jp", "*.hokkaido.jp", "*.tokyo.jp", "!pref.hokkaido.jp", "!metro.tokyo.jp"));
	}

	@Test
	public void testRule1(){
		assertEquals("foo.com", PublicSuffixList.getRegisteredDomain("foo.com"));
	}

	@Test
	public void testRule2_1(){
		assertNull(PublicSuffixList.getRegisteredDomain("bar.foo.com"));
	}

	@Test
	public void testRule2_2(){
		assertEquals("example.bar.foo.com", PublicSuffixList.getRegisteredDomain("example.bar.foo.com"));
	}

	@Test
	public void testRule3_1(){
		assertEquals("foo.bar.jp", PublicSuffixList.getRegisteredDomain("foo.bar.jp"));
	}

	@Test
	public void testRule3_2(){
		assertNull(PublicSuffixList.getRegisteredDomain("bar.jp"));
	}

	@Test
	public void testRule4_1(){
		assertEquals("foo.bar.hokkaido.jp", PublicSuffixList.getRegisteredDomain("foo.bar.hokkaido.jp"));
	}

	@Test
	public void testRule4_2(){
		assertNull(PublicSuffixList.getRegisteredDomain("bar.hokkaido.jp"));
	}

	@Test
	public void testRule5_1(){
		assertEquals("foo.bar.tokyo.jp", PublicSuffixList.getRegisteredDomain("foo.bar.tokyo.jp"));
	}

	@Test
	public void testRule5_2(){
		assertNull(PublicSuffixList.getRegisteredDomain("bar.tokyo.jp"));
	}

	@Test
	public void testRule6(){
		assertEquals("pref.hokkaido.jp", PublicSuffixList.getRegisteredDomain("pref.hokkaido.jp"));
	}

	@Test
	public void testRule7(){
		assertEquals("metro.tokyo.jp", PublicSuffixList.getRegisteredDomain("metro.tokyo.jp"));
	}
}
