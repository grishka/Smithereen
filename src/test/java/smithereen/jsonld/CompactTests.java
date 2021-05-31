package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Collections;

import com.google.gson.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class CompactTests{

	/***
	* Unreferenced nodes not containing properties are dropped
	*/
	@Test
	@DisplayName("drop free-floating nodes")
	void t0001(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0001-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0001-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0001-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0001-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0001-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Basic term and value compaction
	*/
	@Test
	@DisplayName("basic")
	void t0002(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0002-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0002-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0002-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0002-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0002-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Properties mapped to null or which are never mapped are dropped
	*/
	@Test
	@DisplayName("drop null and unmapped properties")
	void t0003(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0003-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0003-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0003-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0003-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0003-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Containers mapped to @set keep empty arrays
	*/
	@Test
	@DisplayName("optimize @set, keep empty arrays")
	void t0004(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0004-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0004-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0004-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0004-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0004-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact uses prefixes in @type
	*/
	@Test
	@DisplayName("@type and prefix compaction")
	void t0005(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0005-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0005-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0005-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0005-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0005-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Values not matching a coerced @type remain in expanded form
	*/
	@Test
	@DisplayName("keep expanded object format if @type doesn't match")
	void t0006(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0006-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0006-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0006-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0006-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0006-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* External context is added to the compacted document
	*/
	@Test
	@DisplayName("add context")
	void t0007(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0007-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0007-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0007-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0007-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0007-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Aliases for keywords are used in compacted document
	*/
	@Test
	@DisplayName("alias keywords")
	void t0008(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0008-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0008-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0008-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0008-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0008-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Value with @id is compacted to string if property cast to @id
	*/
	@Test
	@DisplayName("compact @id")
	void t0009(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0009-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0009-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0009-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0009-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0009-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* An array of objects is serialized with @graph
	*/
	@Test
	@DisplayName("array to @graph")
	void t0010(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0010-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0010-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0010-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0010-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0010-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Expanded value with type xsd:dateTime is represented as string with type coercion
	*/
	@Test
	@DisplayName("compact date")
	void t0011(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0011-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0011-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0011-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0011-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0011-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Native values are unmodified during compaction
	*/
	@Test
	@DisplayName("native types")
	void t0012(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0012-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0012-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0012-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0012-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0012-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Values with @language remain in expanded form by default
	*/
	@Test
	@DisplayName("@value with @language")
	void t0013(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0013-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0013-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0013-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0013-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0013-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Aliasing @graph uses alias in compacted document
	*/
	@Test
	@DisplayName("array to aliased @graph")
	void t0014(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0014-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0014-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0014-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0014-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0014-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Property with values of different types use most appropriate term when compacting
	*/
	@Test
	@DisplayName("best match compaction")
	void t0015(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0015-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0015-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0015-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0015-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0015-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting a document with multiple embedded uses of @graph
	*/
	@Test
	@DisplayName("recursive named graphs")
	void t0016(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0016-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0016-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0016-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0016-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0016-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Mapping a term to null causes the property and its values to be removed from the compacted document
	*/
	@Test
	@DisplayName("A term mapping to null removes the mapping")
	void t0017(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0017-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0017-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0017-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0017-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0017-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Lists with values of different types use best term in compacted document
	*/
	@Test
	@DisplayName("best matching term for lists")
	void t0018(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0018-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0018-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0018-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0018-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0018-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Duplicate values in @list or @set are retained in compacted document
	*/
	@Test
	@DisplayName("Keep duplicate values in @list and @set")
	void t0019(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0019-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0019-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0019-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0019-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0019-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* A term with @container: @list is also used as the value of an @id, if appropriate
	*/
	@Test
	@DisplayName("Compact @id that is a property IRI when @container is @list")
	void t0020(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0020-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0020-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0020-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0020-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0020-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* @vocab is used to create relative properties and types if no other term matches
	*/
	@Test
	@DisplayName("Compact properties and types using @vocab")
	void t0021(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0021-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0021-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0021-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0021-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0021-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact nested properties using @list containers
	*/
	@Test
	@DisplayName("@list compaction of nested properties")
	void t0022(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0022-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0022-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0022-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0022-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0022-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* @vocab takes precedence over prefixes - even if the result is longer
	*/
	@Test
	@DisplayName("prefer @vocab over compacted IRIs")
	void t0023(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0023-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0023-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0023-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0023-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0023-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* The most specific term that matches all of the elements in the list, taking into account the default language, must be selected.
	*/
	@Test
	@DisplayName("most specific term matching in @list.")
	void t0024(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0024-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0024-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0024-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0024-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0024-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Multiple values with different languages use language maps if property has @container: @language
	*/
	@Test
	@DisplayName("Language maps")
	void t0025(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0025-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0025-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0025-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0025-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0025-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Test appropriate property use given language maps with @vocab, a default language, and a competing term
	*/
	@Test
	@DisplayName("Language map term selection with complications")
	void t0026(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0026-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0026-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0026-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0026-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0026-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Fall back to term with @set container if term with language map is defined
	*/
	@Test
	@DisplayName("@container: @set with multiple values")
	void t0027(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0027-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0027-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0027-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0027-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0027-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Combination of keyword aliases and @vocab
	*/
	@Test
	@DisplayName("Alias keywords and use @vocab")
	void t0028(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0028-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0028-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0028-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0028-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0028-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Output uses index mapping if term is defined with @container: @index
	*/
	@Test
	@DisplayName("Simple @index map")
	void t0029(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0029-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0029-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0029-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0029-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0029-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Preserve @index tags if not compacted to an index map
	*/
	@Test
	@DisplayName("non-matching @container: @index")
	void t0030(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0030-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0030-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0030-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0030-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0030-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact traverses through @reverse
	*/
	@Test
	@DisplayName("Compact @reverse")
	void t0031(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0031-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0031-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0031-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0031-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0031-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact traverses through @reverse
	*/
	@Test
	@DisplayName("Compact keys in reverse-maps")
	void t0032(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0032-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0032-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0032-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0032-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0032-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* A reverse map is replaced with a matching property defined with @reverse
	*/
	@Test
	@DisplayName("Compact reverse-map to reverse property")
	void t0033(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0033-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0033-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0033-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0033-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0033-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Do not use reverse property if no other property matches as normal property
	*/
	@Test
	@DisplayName("Skip property with @reverse if no match")
	void t0034(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0034-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0034-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0034-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0034-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0034-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact node references to strings for reverse properties using @type: @id
	*/
	@Test
	@DisplayName("Compact @reverse node references using strings")
	void t0035(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0035-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0035-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0035-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0035-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0035-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact using both reverse properties and index containers
	*/
	@Test
	@DisplayName("Compact reverse properties using index containers")
	void t0036(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0036-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0036-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0036-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0036-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0036-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact keys in @reverse using @vocab
	*/
	@Test
	@DisplayName("Compact keys in @reverse using @vocab")
	void t0037(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0037-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0037-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0037-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0037-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0037-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Complex round-tripping use case from Drupal
	*/
	@Test
	@DisplayName("Index map round-tripping")
	void t0038(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0038-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0038-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0038-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0038-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0038-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Value of @graph is always an array
	*/
	@Test
	@DisplayName("@graph is array")
	void t0039(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0039-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0039-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0039-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0039-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0039-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Ensure that value of @list is always an array
	*/
	@Test
	@DisplayName("@list is array")
	void t0040(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0040-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0040-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0040-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0040-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0040-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* If an index is present, a term having an @list container is not selected
	*/
	@Test
	@DisplayName("index rejects term having @list")
	void t0041(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0041-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0041-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0041-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0041-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0041-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Make sure keyword aliasing works if a list can't be compacted
	*/
	@Test
	@DisplayName("@list keyword aliasing")
	void t0042(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0042-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0042-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0042-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0042-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0042-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Ensure that @vocab compaction isn't used if the result collides with a term
	*/
	@Test
	@DisplayName("select term over @vocab")
	void t0043(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0043-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0043-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0043-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0043-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0043-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Prefer properties with @type: @vocab in reverse-maps if the value can be compacted to a term
	*/
	@Test
	@DisplayName("@type: @vocab in reverse-map")
	void t0044(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0044-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0044-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0044-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0044-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0044-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Values of @id are transformed to relative IRIs, terms are ignored
	*/
	@Test
	@DisplayName("@id value uses relative IRI, not term")
	void t0045(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0045-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0045-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0045-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0045-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0045-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Wrap top-level array into @graph even if no context is passed
	*/
	@Test
	@DisplayName("multiple objects without @context use @graph")
	void t0046(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0046-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0046-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0046-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0046-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0046-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Relative URLs remain relative after compaction
	*/
	@Test
	@DisplayName("Round-trip relative URLs")
	void t0047(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0047-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0047-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0047-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0047-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0047-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Prefer terms with a language mapping set to null over terms without language-mapping for non-strings
	*/
	@Test
	@DisplayName("term with @language: null")
	void t0048(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0048-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0048-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0048-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0048-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0048-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* List compaction without @container: @list still uses strings if @type: @id
	*/
	@Test
	@DisplayName("Round tripping of lists that contain just IRIs")
	void t0049(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0049-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0049-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0049-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0049-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0049-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Node references in reverse properties are not compacted to strings without explicit type-coercion
	*/
	@Test
	@DisplayName("Reverse properties require @type: @id to use string values")
	void t0050(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0050-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0050-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0050-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0050-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0050-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Native values survive round-tripping with @list
	*/
	@Test
	@DisplayName("Round tripping @list with scalar")
	void t0051(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0051-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0051-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0051-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0051-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0051-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Native values survive round-tripping with @list and @graph alias
	*/
	@Test
	@DisplayName("Round tripping @list with scalar and @graph alias")
	void t0052(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0052-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0052-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0052-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0052-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0052-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact to @type: @vocab when no @type: @id term available
	*/
	@Test
	@DisplayName("Use @type: @vocab if no @type: @id")
	void t0053(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0053-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0053-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0053-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0053-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0053-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact to @type: @vocab and compact @id to term
	*/
	@Test
	@DisplayName("Compact to @type: @vocab and compact @id to term")
	void t0054(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0054-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0054-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0054-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0054-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0054-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term
	*/
	@Test
	@DisplayName("Round tripping @type: @vocab")
	void t0055(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0055-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0055-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0055-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0055-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0055-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term
	*/
	@Test
	@DisplayName("Prefer @type: @vocab over @type: @id for terms")
	void t0056(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0056-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0056-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0056-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0056-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0056-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term; more complex
	*/
	@Test
	@DisplayName("Complex round tripping @type: @vocab and @type: @id")
	void t0057(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0057-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0057-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0057-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0057-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0057-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Choose a term having @type: @id over @type: @value if value is not a term
	*/
	@Test
	@DisplayName("Prefer @type: @id over @type: @vocab for non-terms")
	void t0058(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0058-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0058-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0058-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0058-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0058-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* If there's no term with @type: @id, use terms with @type: @vocab for IRIs not mapped to terms
	*/
	@Test
	@DisplayName("Term with @type: @vocab if no @type: @id")
	void t0059(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0059-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0059-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0059-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0059-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0059-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* If there's no term with @type: @vocab, use terms with @type: @id for IRIs mapped to terms
	*/
	@Test
	@DisplayName("Term with @type: @id if no @type: @vocab and term value")
	void t0060(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0060-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0060-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0060-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0060-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0060-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Separate IRIs for the same property to use term with more specific @type (@id vs. @vocab)
	*/
	@Test
	@DisplayName("@type: @vocab/@id with values matching either")
	void t0061(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0061-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0061-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0061-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0061-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0061-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Relative IRIs don't round-trip with @type: @vocab
	*/
	@Test
	@DisplayName("@type: @vocab and relative IRIs")
	void t0062(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0062-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0062-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0062-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0062-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0062-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Term with @type: @vocab will use compact IRIs
	*/
	@Test
	@DisplayName("Compact IRI round-tripping with @type: @vocab")
	void t0063(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0063-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0063-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0063-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0063-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0063-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Given values with both @index and @language and term index-map term, use index map
	*/
	@Test
	@DisplayName("Compact language-tagged and indexed strings to index-map")
	void t0064(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0064-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0064-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0064-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0064-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0064-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Language-tagged and indexed strings don't compact to language-map
	*/
	@Test
	@DisplayName("Language-tagged and indexed strings with language-map")
	void t0065(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0065-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0065-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0065-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0065-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0065-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Complex use cases for relative IRI compaction
	*/
	@Test
	@DisplayName("Relative IRIs")
	void t0066(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0066-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0066-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0066-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0066-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0066-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact reverse property whose values are unlabeled blank nodes
	*/
	@Test
	@DisplayName("Reverse properties with blank nodes")
	void t0067(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0067-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0067-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0067-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0067-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0067-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Single values of reverse properties are compacted as values of ordinary properties
	*/
	@Test
	@DisplayName("Single value reverse properties")
	void t0068(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0068-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0068-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0068-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0068-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0068-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Single values are kept in array form for reverse properties if the container is to @set
	*/
	@Test
	@DisplayName("Single value reverse properties with @set")
	void t0069(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0069-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0069-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0069-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0069-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0069-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Setting compactArrays to false causes single element arrays to be retained
	*/
	@Test
	@DisplayName("compactArrays option")
	void t0070(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0070-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0070-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0070-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0070-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), false, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0070-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Expanding input with multiple @contexts and compacting with just one doesn't output undefined properties
	*/
	@Test
	@DisplayName("Input has multiple @contexts, output has one")
	void t0071(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0071-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0071-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0071-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0071-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0071-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Ensure that the default language is handled correctly for unmapped properties
	*/
	@Test
	@DisplayName("Default language and unmapped properties")
	void t0072(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0072-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0072-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0072-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0072-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0072-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Ensure that compaction works with mapped @id and @type
	*/
	@Test
	@DisplayName("Mapped @id and @type")
	void t0073(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0073-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0073-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0073-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0073-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0073-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Ensure that compaction works for empty list when property has container declared as @list and type as @id
	*/
	@Test
	@DisplayName("Container as a list with type of @id")
	void t0074(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0074-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0074-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0074-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0074-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0074-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting a relative round-trips
	*/
	@Test
	@DisplayName("Compact using relative fragment identifier")
	void t0075(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0075-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0075-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0075-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0075-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0075-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compacting IRI equivalent to base, uses last path segment of base ending in '/'
	*/
	@Test
	@DisplayName("Compacting IRI equivalent to base")
	void t0076(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0076-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0076-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0076-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0076-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0076-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Test appropriate property use given language maps with @vocab, a default language, no language, and competing terms
	*/
	@Test
	@DisplayName("Language map term selection with complications")
	void t0089(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0089-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0089-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0089-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0089-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0089-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Compact with context including JavaScript Object property names
	*/
	@Test
	@DisplayName("context with JavaScript Object property names")
	void t0108(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/0108-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/0108-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/0108-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/0108-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/0108-in.jsonld"));
		assertEquals(expect, compacted);
	}

	/***
	* Verifies that an exception is raised in Compaction when attempting to compact a list of lists
	*/
	@Test
	@DisplayName("Compaction to list of lists")
	void te001(){
		assertThrows(JLDException.class, ()->{
			JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/e001-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/e001-in.jsonld"));
			JsonObject context=readResourceAsJSON("/compact/e001-context.jsonld").getAsJsonObject();
			JLDProcessor.compact(input, context.get("@context"));
		}, "compaction to list of lists");
	}

	/***
	* The most specific term that matches all of the elements in the list, taking into account the default language, must be selected, without considering case of language.
	*/
	@Test
	@DisplayName("most specific term matching in @list.")
	void tla01(){
		JsonArray input=JLDProcessor.expandToArray(readResourceAsJSON("/compact/la01-in.jsonld"), URI.create("https://w3c.github.io/json-ld-api/tests/compact/la01-in.jsonld"));
		JsonElement expect=readResourceAsJSON("/compact/la01-out.jsonld");
		JsonObject context=readResourceAsJSON("/compact/la01-context.jsonld").getAsJsonObject();
		JsonObject compacted=JLDProcessor.compact(input, context.get("@context"), true, URI.create("https://w3c.github.io/json-ld-api/tests/compact/la01-in.jsonld"));
		assertEquals(expect, compacted);
	}

}