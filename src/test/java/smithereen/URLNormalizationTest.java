package smithereen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import smithereen.util.UriRenderer;
import spark.utils.StringUtils;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.readResourceAsJSON;

@SuppressWarnings("HttpUrlsUsage")
public class URLNormalizationTest{

	public static class TestCase implements Named<TestCase>{
		String input;

		/**
		 * true means that the test currently fails, but we intend to fix this.
		 */
		private boolean muted;

		/**
		 * true means that parsing is expected to fail due to malformed input.
		 */
		boolean failure;

		// In case of successful parsing
		String href;
		private String protocol;
		private String username;
		private String password;
		String hostname;
		private String port;
		String pathname;
		private String search;
		private String hash;

		@NotNull
		String getQuotedInput(){
			return new JsonPrimitive(input).toString();
		}

		@Nullable
		String getExpectedNormalization(){
			return href;
		}

		@Nullable
		String getScheme(){
			if(StringUtils.isEmpty(protocol)){
				return null;
			}
			return protocol.substring(0, protocol.length()-1); // Remove trailing ':'
		}

		@Nullable
		String getUsername(){
			if(StringUtils.isEmpty(username))
				return null;
			return username;
		}

		@Nullable
		String getPassword(){
			if(StringUtils.isEmpty(password))
				return null;
			return password;
		}

		int getPort(){
			if(StringUtils.isEmpty(port))
				return -1;
			return Integer.parseInt(port);
		}

		@Nullable
		String getQuery(){
			if(StringUtils.isEmpty(search))
				return null;
			return search.substring(1); // Remove leading '?'
		}

		@Nullable
		String getFragment(){
			if(StringUtils.isEmpty(hash))
				return null;
			return hash.substring(1); // Remove leading '#'
		}

		@Override
		public String getName(){
			return new JsonPrimitive(input).toString();
		}

		@Override
		public TestCase getPayload(){
			return this;
		}
	}

	static Stream<TestCase> testCaseProvider(){
		JsonArray arr=readResourceAsJSON("/url-parsing/urltestdata.json").getAsJsonArray();
		return StreamSupport.stream(arr.spliterator(), false)
				.filter(JsonElement::isJsonObject)
				.map(JsonElement::getAsJsonObject)
				.map(e->Utils.gson.fromJson(e, TestCase.class));
	}

	private static class DisableIrrelevantTests implements ExecutionCondition{
		public static ConditionEvaluationResult evaluateExecutionCondition(@NotNull TestCase testCase){
			// The test data contains many correct non-HTTP URLs like "lolscheme:x x#x x"
			// that are not relevant in Smithereen.
			if(!testCase.input.startsWith("http://") && !testCase.input.startsWith("https://"))
				return ConditionEvaluationResult.disabled("Non-HTTP schemes are not relevant in Smithereen");

			// We don't support passwords either
			if(testCase.getPassword()!=null)
				return ConditionEvaluationResult.disabled("Passwords in URLs are not supported by Smithereen");

			return ConditionEvaluationResult.enabled(null);
		}

		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context){
			ParameterInfo pi=ParameterInfo.get(context);
			if(pi==null) return ConditionEvaluationResult.enabled(null);
			TestCase testCase=pi.getArguments().get(0, TestCase.class);
			return evaluateExecutionCondition(testCase);
		}
	}

	@ExtendWith(URLNormalizationTest.DisableIrrelevantTests.class)
	@ParameterizedTest
	@MethodSource("testCaseProvider")
	public void testNormalizeUrl(@NotNull TestCase testCase){
		try{
			String expectedNormalization=testCase.getExpectedNormalization();

			URI uri=Utils.normalizeURI(testCase.input);
			if(uri==null){
				if(testCase.failure){
					return;
				}else{
					fail("Should be able to normalize "+testCase.getQuotedInput()+" into "+new JsonPrimitive(Objects.requireNonNull(expectedNormalization)));
				}
			}

			assertEquals(testCase.hostname, uri.getHost(), "Hostname mismatch");
			assertEquals(testCase.getScheme(), uri.getScheme(), "Scheme mismatch");
			assertEquals(testCase.username, uri.getUserInfo(), "User mismatch");
			assertEquals(testCase.getPort(), uri.getPort(), "Port mismatch");
			assertEquals(testCase.pathname, uri.getPath(), "Path mismatch");
			assertEquals(testCase.getQuery(), uri.getQuery(), "Query mismatch");
			assertEquals(testCase.getFragment(), uri.getFragment(), "Fragment mismatch");

			assertEquals(expectedNormalization, uri.toASCIIString(), "Unexpected normalized form");

			if(testCase.muted){
				fail("The test "+testCase.getQuotedInput()+" can be unmuted. Remove the `\"mute\": true` line in the corresponding test case in /url-parsing/urltestdata.json.");
			}
		}catch(Exception | AssertionError e){
			if(!testCase.muted){
				throw e;
			}
		}
	}

	@Test
	@Disabled // Comment out to update mutes
	public void updateMutes(){
		JsonArray arr=readResourceAsJSON("/url-parsing/urltestdata.json").getAsJsonArray();
		for(JsonElement element: arr){
			if(!element.isJsonObject()) continue;
			JsonObject obj=element.getAsJsonObject();
			TestCase testCase=Utils.gson.fromJson(obj, TestCase.class);
			if(DisableIrrelevantTests.evaluateExecutionCondition(testCase).isDisabled()) continue;
			try{
				testNormalizeUrl(testCase);
			}catch(Exception | AssertionError ignored){
				if(testCase.muted){
					obj.remove("muted");
				}else{
					obj.addProperty("muted", true);
				}
			}
		}
		try(FileWriter fw=new FileWriter("src/test/resources/url-parsing/urltestdata.json")){
			JsonWriter jw=new JsonWriter(fw);
			jw.setIndent("  ");
			new GsonBuilder()
					.disableHtmlEscaping()
					.serializeNulls()
					.setPrettyPrinting()
					.create()
					.toJson(arr, jw);
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}

	private void testUrlRendering(String expected, String input){
		assertEquals(expected, UriRenderer.DEFAULT.render(URI.create(input)));
	}

	@Test
	public void testOpaqueUrl(){
		testUrlRendering("mailto:admin@xn--e1afmkfd.xn--j1aef", "mailto:admin@xn--e1afmkfd.xn--j1aef");
		testUrlRendering("ssh://git@github.com:grishka/Smithereen.git", "ssh://git@github.com:grishka/Smithereen.git");
	}

	@Test
	public void renderHierarchicalUrl(){
		testUrlRendering("https://администратор@пример.ком:443/путь/💩/с+пробелом/?ключ=значение#фрагмент+с+пробелом", "https://%D0%B0%D0%B4%D0%BC%D0%B8%D0%BD%D0%B8%D1%81%D1%82%D1%80%D0%B0%D1%82%D0%BE%D1%80@xn--e1afmkfd.xn--j1aef:443/%D0%BF%D1%83%D1%82%D1%8C/%F0%9F%92%A9/%D1%81+%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB%D0%BE%D0%BC/?%D0%BA%D0%BB%D1%8E%D1%87=%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5#%D1%84%D1%80%D0%B0%D0%B3%D0%BC%D0%B5%D0%BD%D1%82%20%D1%81%20%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB%D0%BE%D0%BC");
		testUrlRendering("https://пример.ком/путь-без-слэша-в-конце?ключ=зачение+с+пробелом", "https://пример.ком/%D0%BF%D1%83%D1%82%D1%8C-%D0%B1%D0%B5%D0%B7-%D1%81%D0%BB%D1%8D%D1%88%D0%B0-%D0%B2-%D0%BA%D0%BE%D0%BD%D1%86%D0%B5?%D0%BA%D0%BB%D1%8E%D1%87=%D0%B7%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5+%D1%81+%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB%D0%BE%D0%BC");

		// The test cases below are copied from Chromium and adapted
		// https://github.com/chromium/chromium/blob/b9ecf52649856b6d95e4efb02291c2b5cd81229f/base/strings/escape_unittest.cc#L212-L362
		testUrlRendering("http://example.com/", "http://example.com/");
		testUrlRendering("http://example.com/%25-OK", "http://example.com/%25%2dOK");
		testUrlRendering("http://example.com/%25%E1%A6", "http://example.com/%25%E1%A6");
		testUrlRendering("http://example.com/д%E1%A6д", "http://example.com/%D0%B4%E1%A6%D0%B4");
		testUrlRendering("http://example.com/%25%E1%A6OK", "http://example.com/%25%E1%A6OK");
		testUrlRendering("http://example.com/%25ᦙOK", "http://example.com/%25%E1%A6%99OK");

		// BiDi Control characters should not be unescaped.
		testUrlRendering("http://example.com/%25%D8%9COK", "http://example.com/%25%D8%9COK");
		testUrlRendering("http://example.com/%25%E2%80%8EOK", "http://example.com/%25%E2%80%8EOK");
		testUrlRendering("http://example.com/%25%E2%80%8FOK", "http://example.com/%25%E2%80%8FOK");
		testUrlRendering("http://example.com/%25%E2%80%AAOK", "http://example.com/%25%E2%80%AAOK");
		testUrlRendering("http://example.com/%25%E2%80%ABOK", "http://example.com/%25%E2%80%ABOK");
		testUrlRendering("http://example.com/%25%E2%80%AEOK", "http://example.com/%25%E2%80%AEOK");
		testUrlRendering("http://example.com/%25%E2%81%A6OK", "http://example.com/%25%E2%81%A6OK");
		testUrlRendering("http://example.com/%25%E2%81%A9OK", "http://example.com/%25%E2%81%A9OK");

		// Certain banned characters should not be unescaped.
		// U+1F50F LOCK WITH INK PEN
		testUrlRendering("http://example.com/%25%F0%9F%94%8FOK", "http://example.com/%25%F0%9F%94%8FOK");
		// U+1F510 CLOSED LOCK WITH KEY
		testUrlRendering("http://example.com/%25%F0%9F%94%90OK", "http://example.com/%25%F0%9F%94%90OK");
		// U+1F512 LOCK
		testUrlRendering("http://example.com/%25%F0%9F%94%92OK", "http://example.com/%25%F0%9F%94%92OK");
		// U+1F513 OPEN LOCK
		testUrlRendering("http://example.com/%25%F0%9F%94%93OK", "http://example.com/%25%F0%9F%94%93OK");

		// Spaces
		testUrlRendering("http://example.com/%C2%85+%C2%A0+%E1%9A%80+%E2%80%80", "http://example.com/%C2%85+%C2%A0+%E1%9A%80+%E2%80%80");
		testUrlRendering("http://example.com/%E2%80%81+%E2%80%82+%E2%80%83+%E2%80%84", "http://example.com/%E2%80%81+%E2%80%82+%E2%80%83+%E2%80%84");
		testUrlRendering("http://example.com/%E2%80%85+%E2%80%86+%E2%80%87+%E2%80%88", "http://example.com/%E2%80%85+%E2%80%86+%E2%80%87+%E2%80%88");
		testUrlRendering("http://example.com/%E2%80%89+%E2%80%8A+%E2%80%A8+%E2%80%A9", "http://example.com/%E2%80%89+%E2%80%8A+%E2%80%A8+%E2%80%A9");
		testUrlRendering("http://example.com/%E2%80%AF+%E2%81%9F+%E3%80%80", "http://example.com/%E2%80%AF+%E2%81%9F+%E3%80%80");
		testUrlRendering("http://example.com/%E2%A0%80", "http://example.com/%E2%A0%80");

		// Default Ignorable and Formatting characters should not be unescaped.
		testUrlRendering("http://example.com/%E2%81%A5+%EF%BF%B0+%EF%BF%B8", "http://example.com/%E2%81%A5+%EF%BF%B0+%EF%BF%B8");
		testUrlRendering("http://example.com/%F3%A0%82%80+%F3%A0%83%BF+%F3%A0%87%B0", "http://example.com/%F3%A0%82%80+%F3%A0%83%BF+%F3%A0%87%B0");
		testUrlRendering("http://example.com/%F3%A0%BF%BF+%C2%AD+%CD%8F", "http://example.com/%F3%A0%BF%BF+%C2%AD+%CD%8F");
		testUrlRendering("http://example.com/%D8%80++%D8%85+%DB%9D+%DC%8F+%E0%A3%A2", "http://example.com/%D8%80%20+%D8%85+%DB%9D+%DC%8F+%E0%A3%A2");
		testUrlRendering("http://example.com/%E1%85%9F+%E1%85%A0+%E1%9E%B4+%E1%9E%B5", "http://example.com/%E1%85%9F+%E1%85%A0+%E1%9E%B4+%E1%9E%B5");
		testUrlRendering("http://example.com/%E1%A0%8B+%E1%A0%8C+%E1%A0%8D+%E1%A0%8E", "http://example.com/%E1%A0%8B+%E1%A0%8C+%E1%A0%8D+%E1%A0%8E");
		testUrlRendering("http://example.com/%E2%80%8B+%E2%80%8C+%E2%80%8D+%E2%81%A0", "http://example.com/%E2%80%8B+%E2%80%8C+%E2%80%8D+%E2%81%A0");
		testUrlRendering("http://example.com/%E2%81%A1+%E2%81%A2+%E2%81%A3+%E2%81%A4", "http://example.com/%E2%81%A1+%E2%81%A2+%E2%81%A3+%E2%81%A4");
		testUrlRendering("http://example.com/%E3%85%A4+%EF%BB%BF+%EF%BE%A0+%EF%BF%B9", "http://example.com/%E3%85%A4+%EF%BB%BF+%EF%BE%A0+%EF%BF%B9");
		testUrlRendering("http://example.com/%EF%BF%BB+%F0%91%82%BD+%F0%91%83%8D", "http://example.com/%EF%BF%BB+%F0%91%82%BD+%F0%91%83%8D");
		testUrlRendering("http://example.com/%F0%93%90%B0+%F0%93%90%B8", "http://example.com/%F0%93%90%B0+%F0%93%90%B8");
		// General Punctuation - Deprecated (U+206A--206F)
		testUrlRendering("http://example.com/%E2%81%AA+%E2%81%AD+%E2%81%AF", "http://example.com/%E2%81%AA+%E2%81%AD+%E2%81%AF");
		// Variation selectors (U+FE00--FE0F)
		testUrlRendering("http://example.com/%EF%B8%80+%EF%B8%8C+%EF%B8%8D", "http://example.com/%EF%B8%80+%EF%B8%8C+%EF%B8%8D");
		// Shorthand format controls (U+1BCA0--1BCA3)
		testUrlRendering("http://example.com/%F0%9B%B2%A0+%F0%9B%B2%A1+%F0%9B%B2%A3", "http://example.com/%F0%9B%B2%A0+%F0%9B%B2%A1+%F0%9B%B2%A3");
		// Musical symbols beams and slurs (U+1D173--1D17A)
		testUrlRendering("http://example.com/%F0%9D%85%B3+%F0%9D%85%B9+%F0%9D%85%BA", "http://example.com/%F0%9D%85%B3+%F0%9D%85%B9+%F0%9D%85%BA");
		// Tags block (U+E0000--E007F), includes unassigned points
		testUrlRendering("http://example.com/%F3%A0%80%80+%F3%A0%80%81+%F3%A0%81%8F", "http://example.com/%F3%A0%80%80+%F3%A0%80%81+%F3%A0%81%8F");
		// Ideographic-specific variation selectors (U+E0100--E01EF)
		testUrlRendering("http://example.com/%F3%A0%84%80+%F3%A0%84%90+%F3%A0%87%AF", "http://example.com/%F3%A0%84%80+%F3%A0%84%90+%F3%A0%87%AF");

		// Two spoofing characters in a row should not be unescaped.
		testUrlRendering("http://example.com/%D8%9C%D8%9C", "http://example.com/%D8%9C%D8%9C");
		// Non-spoofing characters surrounded by spoofing characters should be
		// unescaped.
		testUrlRendering("http://example.com/%D8%9C¡%D8%9C¡", "http://example.com/%D8%9C%C2%A1%D8%9C%C2%A1");
		// Invalid UTF-8 characters surrounded by spoofing characters should be
		// unescaped.
		testUrlRendering("http://example.com/%D8%9C%85%D8%9C%85", "http://example.com/%D8%9C%85%D8%9C%85");
		// Test with enough trail bytes to overflow the CBU8_MAX_LENGTH-byte
		// buffer. The first two bytes are a spoofing character as well.
		testUrlRendering("http://example.com/%D8%9C%9C%9C%9C%9C%9C%9C%9C%9C%9C", "http://example.com/%D8%9C%9C%9C%9C%9C%9C%9C%9C%9C%9C");

		testUrlRendering("http://example.com/%A0%B1%C2%D3%E4%F5", "http://example.com/%A0%B1%C2%D3%E4%F5");
		testUrlRendering("http://example.com/%AA%BB%CC%DD%EE%FF", "http://example.com/%Aa%Bb%Cc%Dd%Ee%Ff");

		testUrlRendering("http://example.com/%01%02%03%04%05%06%07%08%09+%25", "http://example.com/%01%02%03%04%05%06%07%08%09+%25");
		testUrlRendering("http://example.com/Hello+%13%10%02", "http://example.com/Hello%20%13%10%02");
		testUrlRendering("http://example.com/%2F%5C", "http://example.com/%2F%5C");
	}
}

