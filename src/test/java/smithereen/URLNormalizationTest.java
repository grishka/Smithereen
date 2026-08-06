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
}

