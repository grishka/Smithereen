package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;

import com.google.gson.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class ExpandTests{

	/***
	* Expand drops unreferenced nodes having only @id
	*/
	@Test
	@DisplayName("drop free-floating nodes")
	void t0001(){
		JsonElement input=readResourceAsJSON("/expand/0001-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0001-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0001-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding terms with different types of values
	*/
	@Test
	@DisplayName("basic")
	void t0002(){
		JsonElement input=readResourceAsJSON("/expand/0002-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0002-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0002-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Verifies that null values and unmapped properties are removed from expanded output
	*/
	@Test
	@DisplayName("drop null and unmapped properties")
	void t0003(){
		JsonElement input=readResourceAsJSON("/expand/0003-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0003-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0003-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Uses of @set are removed in expansion; values of @set, or just plain values which are empty arrays are retained
	*/
	@Test
	@DisplayName("optimize @set, keep empty arrays")
	void t0004(){
		JsonElement input=readResourceAsJSON("/expand/0004-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0004-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0004-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* If a keyword is aliased, it is not used when expanding
	*/
	@Test
	@DisplayName("do not expand aliased @id/@type")
	void t0005(){
		JsonElement input=readResourceAsJSON("/expand/0005-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0005-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0005-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Aliased keywords expand in resulting document
	*/
	@Test
	@DisplayName("alias keywords")
	void t0006(){
		JsonElement input=readResourceAsJSON("/expand/0006-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0006-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0006-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expand strings to expanded value with @type: xsd:dateTime
	*/
	@Test
	@DisplayName("date type-coercion")
	void t0007(){
		JsonElement input=readResourceAsJSON("/expand/0007-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0007-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0007-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Keep expanded values with @language, drop non-conforming value objects containing just @language
	*/
	@Test
	@DisplayName("@value with @language")
	void t0008(){
		JsonElement input=readResourceAsJSON("/expand/0008-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0008-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0008-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of @graph to contain multiple nodes within array
	*/
	@Test
	@DisplayName("@graph with terms")
	void t0009(){
		JsonElement input=readResourceAsJSON("/expand/0009-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0009-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0009-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding native scalar retains native scalar within expanded value
	*/
	@Test
	@DisplayName("native types")
	void t0010(){
		JsonElement input=readResourceAsJSON("/expand/0010-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0010-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0010-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* A value of a property with @type: @id coercion expands to a node reference
	*/
	@Test
	@DisplayName("coerced @id")
	void t0011(){
		JsonElement input=readResourceAsJSON("/expand/0011-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0011-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0011-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of @graph to contain multiple nodes within array
	*/
	@Test
	@DisplayName("@graph with embed")
	void t0012(){
		JsonElement input=readResourceAsJSON("/expand/0012-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0012-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0012-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expand does not mess up already expanded document
	*/
	@Test
	@DisplayName("expand already expanded")
	void t0013(){
		JsonElement input=readResourceAsJSON("/expand/0013-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0013-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0013-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding aliased @set and @value
	*/
	@Test
	@DisplayName("@set of @value objects with keyword aliases")
	void t0014(){
		JsonElement input=readResourceAsJSON("/expand/0014-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0014-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0014-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* An array of multiple @set nodes are collapsed into a single array
	*/
	@Test
	@DisplayName("collapse set of sets, keep empty lists")
	void t0015(){
		JsonElement input=readResourceAsJSON("/expand/0015-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0015-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0015-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Setting @context to null within an embedded object resets back to initial context state
	*/
	@Test
	@DisplayName("context reset")
	void t0016(){
		JsonElement input=readResourceAsJSON("/expand/0016-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0016-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0016-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding with @graph and @id aliases
	*/
	@Test
	@DisplayName("@graph and @id aliased")
	void t0017(){
		JsonElement input=readResourceAsJSON("/expand/0017-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0017-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0017-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* override default @language in terms; only language-tag strings
	*/
	@Test
	@DisplayName("override default @language")
	void t0018(){
		JsonElement input=readResourceAsJSON("/expand/0018-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0018-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0018-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding a value of null removes the value
	*/
	@Test
	@DisplayName("remove @value = null")
	void t0019(){
		JsonElement input=readResourceAsJSON("/expand/0019-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0019-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0019-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* @graph used under a node is retained
	*/
	@Test
	@DisplayName("do not remove @graph if not at top-level")
	void t0020(){
		JsonElement input=readResourceAsJSON("/expand/0020-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0020-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0020-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* @graph used at the top level is retained if there are other properties
	*/
	@Test
	@DisplayName("do not remove @graph at top-level if not only property")
	void t0021(){
		JsonElement input=readResourceAsJSON("/expand/0021-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0021-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0021-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding with a default language applies that language to string values
	*/
	@Test
	@DisplayName("expand value with default language")
	void t0022(){
		JsonElement input=readResourceAsJSON("/expand/0022-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0022-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0022-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding lists and sets with properties having coercion coerces list/set values
	*/
	@Test
	@DisplayName("Expanding list/set with coercion")
	void t0023(){
		JsonElement input=readResourceAsJSON("/expand/0023-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0023-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0023-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Tests that contexts in an array are merged
	*/
	@Test
	@DisplayName("Multiple contexts")
	void t0024(){
		JsonElement input=readResourceAsJSON("/expand/0024-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0024-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0024-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding different kinds of terms and Compact IRIs
	*/
	@Test
	@DisplayName("Problematic IRI expansion tests")
	void t0025(){
		JsonElement input=readResourceAsJSON("/expand/0025-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0025-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0025-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding term mapping to @type uses @type syntax
	*/
	@Test
	@DisplayName("Term definition with @id: @type")
	void t0026(){
		JsonElement input=readResourceAsJSON("/expand/0026-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0026-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0026-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Duplicate values in @list and @set are not merged
	*/
	@Test
	@DisplayName("Duplicate values in @list and @set")
	void t0027(){
		JsonElement input=readResourceAsJSON("/expand/0027-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0027-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0027-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* @vocab is used to compact properties and @type, but is not used for @id
	*/
	@Test
	@DisplayName("Use @vocab in properties and @type but not in @id")
	void t0028(){
		JsonElement input=readResourceAsJSON("/expand/0028-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0028-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0028-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* @base is used to compact @id; test with different relative IRIs
	*/
	@Test
	@DisplayName("Relative IRIs")
	void t0029(){
		JsonElement input=readResourceAsJSON("/expand/0029-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0029-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0029-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Language Maps expand values to include @language
	*/
	@Test
	@DisplayName("Language maps")
	void t0030(){
		JsonElement input=readResourceAsJSON("/expand/0030-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0030-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0030-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding native types with type coercion adds the coerced type to an expanded value representation and retains the native value representation
	*/
	@Test
	@DisplayName("type-coercion of native types")
	void t0031(){
		JsonElement input=readResourceAsJSON("/expand/0031-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0031-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0031-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Mapping a term to null decouples it from @vocab
	*/
	@Test
	@DisplayName("Null term and @vocab")
	void t0032(){
		JsonElement input=readResourceAsJSON("/expand/0032-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0032-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0032-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Verifies that terms can be defined using @vocab
	*/
	@Test
	@DisplayName("Using @vocab with with type-coercion")
	void t0033(){
		JsonElement input=readResourceAsJSON("/expand/0033-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0033-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0033-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Verifies multiple values from separate terms are deterministically made multiple values of the IRI associated with the terms
	*/
	@Test
	@DisplayName("Multiple properties expanding to the same IRI")
	void t0034(){
		JsonElement input=readResourceAsJSON("/expand/0034-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0034-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0034-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Pathological tests of language maps
	*/
	@Test
	@DisplayName("Language maps with @vocab, default language, and colliding property")
	void t0035(){
		JsonElement input=readResourceAsJSON("/expand/0035-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0035-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0035-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding index maps for terms defined with @container: @index
	*/
	@Test
	@DisplayName("Expanding @index")
	void t0036(){
		JsonElement input=readResourceAsJSON("/expand/0036-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0036-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0036-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding @reverse keeps @reverse
	*/
	@Test
	@DisplayName("Expanding @reverse")
	void t0037(){
		JsonElement input=readResourceAsJSON("/expand/0037-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0037-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0037-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Blank nodes are not relabeled during expansion
	*/
	@Test
	@DisplayName("Expanding blank node labels")
	void t0038(){
		JsonElement input=readResourceAsJSON("/expand/0038-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0038-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0038-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Terms within @reverse are expanded
	*/
	@Test
	@DisplayName("Using terms in a reverse-maps")
	void t0039(){
		JsonElement input=readResourceAsJSON("/expand/0039-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0039-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0039-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Only invoke language and index map expansion if the value is a JSON object
	*/
	@Test
	@DisplayName("language and index expansion on non-objects")
	void t0040(){
		JsonElement input=readResourceAsJSON("/expand/0040-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0040-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0040-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* 
	*/
	@Test
	@DisplayName("@language: null resets the default language")
	void t0041(){
		JsonElement input=readResourceAsJSON("/expand/0041-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0041-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0041-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding terms defined as reverse properties uses @reverse in expanded document
	*/
	@Test
	@DisplayName("Reverse properties")
	void t0042(){
		JsonElement input=readResourceAsJSON("/expand/0042-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0042-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0042-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding a reverse property within a @reverse undoes both reversals
	*/
	@Test
	@DisplayName("Using reverse properties inside a @reverse-container")
	void t0043(){
		JsonElement input=readResourceAsJSON("/expand/0043-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0043-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0043-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Ensure index maps use language mapping
	*/
	@Test
	@DisplayName("Index maps with language mappings")
	void t0044(){
		JsonElement input=readResourceAsJSON("/expand/0044-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0044-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0044-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding top-level value objects causes them to be removed
	*/
	@Test
	@DisplayName("Top-level value objects")
	void t0045(){
		JsonElement input=readResourceAsJSON("/expand/0045-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0045-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0045-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding free-floating nodes causes them to be removed
	*/
	@Test
	@DisplayName("Free-floating nodes")
	void t0046(){
		JsonElement input=readResourceAsJSON("/expand/0046-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0046-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0046-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Free-floating values in sets are removed, free-floating lists are removed completely
	*/
	@Test
	@DisplayName("Free-floating values in sets and free-floating lists")
	void t0047(){
		JsonElement input=readResourceAsJSON("/expand/0047-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0047-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0047-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Values of @id are not expanded as terms
	*/
	@Test
	@DisplayName("Terms are ignored in @id")
	void t0048(){
		JsonElement input=readResourceAsJSON("/expand/0048-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0048-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0048-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* String values of a reverse property with @type: @id are treated as IRIs
	*/
	@Test
	@DisplayName("String values of reverse properties")
	void t0049(){
		JsonElement input=readResourceAsJSON("/expand/0049-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0049-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0049-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Term definitions using compact IRIs don't inherit the definitions of the prefix
	*/
	@Test
	@DisplayName("Term definitions with prefix separate from prefix definitions")
	void t0050(){
		JsonElement input=readResourceAsJSON("/expand/0050-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0050-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0050-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding terms which are keyword aliases
	*/
	@Test
	@DisplayName("Expansion of keyword aliases in term definitions")
	void t0051(){
		JsonElement input=readResourceAsJSON("/expand/0051-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0051-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0051-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* If @vocab is defined, term definitions are expanded relative to @vocab
	*/
	@Test
	@DisplayName("@vocab-relative IRIs in term definitions")
	void t0052(){
		JsonElement input=readResourceAsJSON("/expand/0052-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0052-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0052-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding values of properties of @type: @vocab does not further expand absolute IRIs
	*/
	@Test
	@DisplayName("Expand absolute IRI with @type: @vocab")
	void t0053(){
		JsonElement input=readResourceAsJSON("/expand/0053-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0053-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0053-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding values of properties of @type: @vocab does not expand term values
	*/
	@Test
	@DisplayName("Expand term with @type: @vocab")
	void t0054(){
		JsonElement input=readResourceAsJSON("/expand/0054-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0054-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0054-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding values of properties of @type: @vocab expands relative IRIs using @vocab
	*/
	@Test
	@DisplayName("Expand @vocab-relative term with @type: @vocab")
	void t0055(){
		JsonElement input=readResourceAsJSON("/expand/0055-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0055-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0055-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Checks that expansion uses appropriate base depending on term definition having @type @id or @vocab
	*/
	@Test
	@DisplayName("Use terms with @type: @vocab but not with @type: @id")
	void t0056(){
		JsonElement input=readResourceAsJSON("/expand/0056-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0056-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0056-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Relative values of terms with @type: @vocab expand relative to @vocab
	*/
	@Test
	@DisplayName("Expand relative IRI with @type: @vocab")
	void t0057(){
		JsonElement input=readResourceAsJSON("/expand/0057-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0057-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0057-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Compact IRIs are expanded normally even if term has @type: @vocab
	*/
	@Test
	@DisplayName("Expand compact IRI with @type: @vocab")
	void t0058(){
		JsonElement input=readResourceAsJSON("/expand/0058-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0058-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0058-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Setting @vocab to null removes a previous definition
	*/
	@Test
	@DisplayName("Reset @vocab by setting it to null")
	void t0059(){
		JsonElement input=readResourceAsJSON("/expand/0059-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0059-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0059-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Setting @base to an IRI and then resetting it to nil
	*/
	@Test
	@DisplayName("Overwrite document base with @base and reset it again")
	void t0060(){
		JsonElement input=readResourceAsJSON("/expand/0060-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0060-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0060-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expanding native types when coercing to arbitrary datatypes
	*/
	@Test
	@DisplayName("Coercing native types to arbitrary datatypes")
	void t0061(){
		JsonElement input=readResourceAsJSON("/expand/0061-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0061-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0061-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Pathological relative IRIs
	*/
	@Test
	@DisplayName("Various relative IRIs with with @base")
	void t0062(){
		JsonElement input=readResourceAsJSON("/expand/0062-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0062-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0062-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expaning reverse properties with an index-container
	*/
	@Test
	@DisplayName("Reverse property and index container")
	void t0063(){
		JsonElement input=readResourceAsJSON("/expand/0063-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0063-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0063-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expand reverse property whose values are unlabeled blank nodes
	*/
	@Test
	@DisplayName("bnode values of reverse properties")
	void t0064(){
		JsonElement input=readResourceAsJSON("/expand/0064-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0064-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0064-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Keys that are not mapped to an IRI in a reverse-map are dropped
	*/
	@Test
	@DisplayName("Drop unmapped keys in reverse map")
	void t0065(){
		JsonElement input=readResourceAsJSON("/expand/0065-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0065-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0065-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expand uses @vocab to expand keys in reverse-maps
	*/
	@Test
	@DisplayName("Reverse-map keys with @vocab")
	void t0066(){
		JsonElement input=readResourceAsJSON("/expand/0066-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0066-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0066-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* prefix:suffix values are not interpreted as compact IRIs if suffix begins with two slashes
	*/
	@Test
	@DisplayName("prefix://suffix not a compact IRI")
	void t0067(){
		JsonElement input=readResourceAsJSON("/expand/0067-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0067-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0067-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* prefix:suffix values are not interpreted as compact IRIs if prefix is an underscore
	*/
	@Test
	@DisplayName("_:suffix values are not a compact IRI")
	void t0068(){
		JsonElement input=readResourceAsJSON("/expand/0068-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0068-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0068-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Redefine compact IRI to define type mapping using the compact IRI itself as value of @id
	*/
	@Test
	@DisplayName("Compact IRI as term with type mapping")
	void t0069(){
		JsonElement input=readResourceAsJSON("/expand/0069-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0069-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0069-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Redefine compact IRI to define type mapping using the compact IRI itself as string value
	*/
	@Test
	@DisplayName("Compact IRI as term defined using equivalent compact IRI")
	void t0070(){
		JsonElement input=readResourceAsJSON("/expand/0070-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0070-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0070-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Term definitions may look like compact IRIs
	*/
	@Test
	@DisplayName("Redefine terms looking like compact IRIs")
	void t0071(){
		JsonElement input=readResourceAsJSON("/expand/0071-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0071-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0071-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Redefining a term as itself when @vocab is defined uses @vocab, not previous term definition
	*/
	@Test
	@DisplayName("Redefine term using @vocab, not itself")
	void t0072(){
		JsonElement input=readResourceAsJSON("/expand/0072-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0072-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0072-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Objects are unordered, so serialized node definition containing @context may have @context at the end of the node definition
	*/
	@Test
	@DisplayName("@context not first property")
	void t0073(){
		JsonElement input=readResourceAsJSON("/expand/0073-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0073-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0073-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Objects are unordered, so serialized node definition containing @id may have @id at the end of the node definition
	*/
	@Test
	@DisplayName("@id not first property")
	void t0074(){
		JsonElement input=readResourceAsJSON("/expand/0074-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0074-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0074-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Use @vocab to map all properties to blank node identifiers
	*/
	@Test
	@DisplayName("@vocab as blank node identifier")
	void t0075(){
		JsonElement input=readResourceAsJSON("/expand/0075-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0075-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0075-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of the base option overrides the document location
	*/
	@Test
	@DisplayName("base option overrides document location")
	void t0076(){
		JsonElement input=readResourceAsJSON("/expand/0076-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0076-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("http://example/base/"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of multiple reverse properties
	*/
	@Test
	@DisplayName("multiple reverse properties")
	void t0078(){
		JsonElement input=readResourceAsJSON("/expand/0078-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0078-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0078-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Value Expansion does not expand native values, such as booleans, to a node object
	*/
	@Test
	@DisplayName("Do not expand native values to IRIs")
	void t0088(){
		JsonElement input=readResourceAsJSON("/expand/0088-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0088-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0088-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of an empty @base is applied to the base option
	*/
	@Test
	@DisplayName("empty @base applied to the base option")
	void t0089(){
		JsonElement input=readResourceAsJSON("/expand/0089-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0089-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("http://example/base/"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of a relative @base overrides base option and document location
	*/
	@Test
	@DisplayName("relative @base overrides base option and document location")
	void t0090(){
		JsonElement input=readResourceAsJSON("/expand/0090-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0090-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("http://example/base/"));
		assertEquals(expect, expanded);
	}

	/***
	* Use of a relative and absolute @base overrides base option and document location
	*/
	@Test
	@DisplayName("relative and absolute @base overrides base option and document location")
	void t0091(){
		JsonElement input=readResourceAsJSON("/expand/0091-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0091-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("http://example/base/"));
		assertEquals(expect, expanded);
	}

	/***
	* Do not treat as absolute IRIs values that look like compact IRIs if they're not absolute
	*/
	@Test
	@DisplayName("IRI expansion of fragments including ':'")
	void t0109(){
		JsonElement input=readResourceAsJSON("/expand/0109-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0109-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0109-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Expand with context including JavaScript Object property names
	*/
	@Test
	@DisplayName("context with JavaScript Object property names")
	void t0113(){
		JsonElement input=readResourceAsJSON("/expand/0113-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0113-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0113-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Relative property IRIs with relative @vocab in 1.0
	*/
	@Test
	@DisplayName("Verifies that relative IRIs as properties with @vocab: '' in 1.0 generate an error")
	void t0115(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/0115-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0115-in.jsonld"));
		}, "invalid vocab mapping");
	}

	/***
	* Relative property IRIs with relative @vocab in 1.0
	*/
	@Test
	@DisplayName("Verifies that relative IRIs as properties with relative @vocab in 1.0 generate an error")
	void t0116(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/0116-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0116-in.jsonld"));
		}, "invalid vocab mapping");
	}

	/***
	* Verify URI resolution relative to base (without trailing slash, without path) according to RFC 3986
	*/
	@Test
	@DisplayName("Base without trailing slash, without path")
	void t0129(){
		JsonElement input=readResourceAsJSON("/expand/0129-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0129-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0129-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Verify URI resolution relative to base (without trailing slash, with path) according to RFC 3986
	*/
	@Test
	@DisplayName("Base without trailing slash, with path")
	void t0130(){
		JsonElement input=readResourceAsJSON("/expand/0130-in.jsonld");
		JsonElement expect=readResourceAsJSON("/expand/0130-out.jsonld");
		JsonArray expanded=JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/0130-in.jsonld"));
		assertEquals(expect, expanded);
	}

	/***
	* Verifies that an exception is raised on expansion when processing an invalid context aliasing a keyword to another keyword
	*/
	@Test
	@DisplayName("Keywords cannot be aliased to other keywords")
	void ter01(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er01-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er01-in.jsonld"));
		}, "keyword redefinition");
	}

	/***
	* Verifies that an exception is raised on expansion when processing a context referencing itself
	*/
	@Test
	@DisplayName("A context may not include itself recursively (direct)")
	void ter02(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er02-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er02-in.jsonld"));
		}, "recursive context inclusion");
	}

	/***
	* Verifies that an exception is raised on expansion when processing a context referencing itself indirectly
	*/
	@Test
	@DisplayName("A context may not include itself recursively (indirect)")
	void ter03(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er03-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er03-in.jsonld"));
		}, "recursive context inclusion");
	}

	/***
	* Verifies that an exception is raised on expansion when a context dereference results in an error
	*/
	@Test
	@DisplayName("Error dereferencing a remote context")
	void ter04(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er04-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er04-in.jsonld"));
		}, "loading remote context failed");
	}

	/***
	* Verifies that an exception is raised on expansion when a context is not a string or object
	*/
	@Test
	@DisplayName("Invalid local context")
	void ter06(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er06-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er06-in.jsonld"));
		}, "invalid local context");
	}

	/***
	* Verifies that an exception is raised on expansion when a context contains an invalid @base
	*/
	@Test
	@DisplayName("Invalid base IRI")
	void ter07(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er07-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er07-in.jsonld"));
		}, "invalid base IRI");
	}

	/***
	* Verifies that an exception is raised on expansion when a context contains an invalid @vocab mapping
	*/
	@Test
	@DisplayName("Invalid vocab mapping")
	void ter08(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er08-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er08-in.jsonld"));
		}, "invalid vocab mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a context contains an invalid @language
	*/
	@Test
	@DisplayName("Invalid default language")
	void ter09(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er09-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er09-in.jsonld"));
		}, "invalid default language");
	}

	/***
	* Verifies that an exception is raised on expansion when a cyclic IRI mapping is found
	*/
	@Test
	@DisplayName("Cyclic IRI mapping")
	void ter10(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er10-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er10-in.jsonld"));
		}, "cyclic IRI mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid term definition is found
	*/
	@Test
	@DisplayName("Invalid term definition")
	void ter11(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er11-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er11-in.jsonld"));
		}, "invalid term definition");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid type mapping is found
	*/
	@Test
	@DisplayName("Invalid type mapping (not a string)")
	void ter12(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er12-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er12-in.jsonld"));
		}, "invalid type mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid type mapping is found
	*/
	@Test
	@DisplayName("Invalid type mapping (not absolute IRI)")
	void ter13(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er13-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er13-in.jsonld"));
		}, "invalid type mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid reverse property is found
	*/
	@Test
	@DisplayName("Invalid reverse property (contains @id)")
	void ter14(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er14-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er14-in.jsonld"));
		}, "invalid reverse property");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid IRI mapping is found
	*/
	@Test
	@DisplayName("Invalid IRI mapping (@reverse not a string)")
	void ter15(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er15-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er15-in.jsonld"));
		}, "invalid IRI mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid reverse property is found
	*/
	@Test
	@DisplayName("Invalid reverse property (invalid @container)")
	void ter17(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er17-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er17-in.jsonld"));
		}, "invalid reverse property");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid IRI mapping is found
	*/
	@Test
	@DisplayName("Invalid IRI mapping (@id not a string)")
	void ter18(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er18-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er18-in.jsonld"));
		}, "invalid IRI mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid keyword alias is found
	*/
	@Test
	@DisplayName("Invalid keyword alias (@context)")
	void ter19(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er19-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er19-in.jsonld"));
		}, "invalid keyword alias");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid IRI mapping is found
	*/
	@Test
	@DisplayName("Invalid IRI mapping (no vocab mapping)")
	void ter20(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er20-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er20-in.jsonld"));
		}, "invalid IRI mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid language mapping is found
	*/
	@Test
	@DisplayName("Invalid language mapping")
	void ter22(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er22-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er22-in.jsonld"));
		}, "invalid language mapping");
	}

	/***
	* Verifies that an exception is raised on expansion when a invalid type mapping is found
	*/
	@Test
	@DisplayName("Invalid IRI mapping (relative IRI in @type)")
	void ter23(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er23-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er23-in.jsonld"));
		}, "invalid type mapping");
	}

	/***
	* Verifies that an exception is raised in Expansion when a list of lists is found
	*/
	@Test
	@DisplayName("List of lists (from array)")
	void ter24(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er24-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er24-in.jsonld"));
		}, "list of lists");
	}

	/***
	* Verifies that an exception is raised in Expansion when a invalid reverse property map is found
	*/
	@Test
	@DisplayName("Invalid reverse property map")
	void ter25(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er25-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er25-in.jsonld"));
		}, "invalid reverse property map");
	}

	/***
	* Verifies that an exception is raised in Expansion when colliding keywords are found
	*/
	@Test
	@DisplayName("Colliding keywords")
	void ter26(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er26-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er26-in.jsonld"));
		}, "colliding keywords");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid @id value is found
	*/
	@Test
	@DisplayName("Invalid @id value")
	void ter27(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er27-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er27-in.jsonld"));
		}, "invalid @id value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid type value is found
	*/
	@Test
	@DisplayName("Invalid type value")
	void ter28(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er28-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er28-in.jsonld"));
		}, "invalid type value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid value object value is found
	*/
	@Test
	@DisplayName("Invalid value object value")
	void ter29(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er29-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er29-in.jsonld"));
		}, "invalid value object value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid language-tagged string value is found
	*/
	@Test
	@DisplayName("Invalid language-tagged string")
	void ter30(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er30-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er30-in.jsonld"));
		}, "invalid language-tagged string");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid @index value value is found
	*/
	@Test
	@DisplayName("Invalid @index value")
	void ter31(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er31-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er31-in.jsonld"));
		}, "invalid @index value");
	}

	/***
	* Verifies that an exception is raised in Expansion when a list of lists is found
	*/
	@Test
	@DisplayName("List of lists (from array)")
	void ter32(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er32-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er32-in.jsonld"));
		}, "list of lists");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid @reverse value is found
	*/
	@Test
	@DisplayName("Invalid @reverse value")
	void ter33(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er33-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er33-in.jsonld"));
		}, "invalid @reverse value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid reverse property value is found
	*/
	@Test
	@DisplayName("Invalid reverse property value (in @reverse)")
	void ter34(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er34-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er34-in.jsonld"));
		}, "invalid reverse property value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid language map value is found
	*/
	@Test
	@DisplayName("Invalid language map value")
	void ter35(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er35-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er35-in.jsonld"));
		}, "invalid language map value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid reverse property value is found
	*/
	@Test
	@DisplayName("Invalid reverse property value (through coercion)")
	void ter36(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er36-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er36-in.jsonld"));
		}, "invalid reverse property value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid value object is found
	*/
	@Test
	@DisplayName("Invalid value object (unexpected keyword)")
	void ter37(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er37-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er37-in.jsonld"));
		}, "invalid value object");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid value object is found
	*/
	@Test
	@DisplayName("Invalid value object (@type and @language)")
	void ter38(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er38-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er38-in.jsonld"));
		}, "invalid value object");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid language-tagged value is found
	*/
	@Test
	@DisplayName("Invalid language-tagged value")
	void ter39(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er39-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er39-in.jsonld"));
		}, "invalid language-tagged value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid typed value is found
	*/
	@Test
	@DisplayName("Invalid typed value")
	void ter40(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er40-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er40-in.jsonld"));
		}, "invalid typed value");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid set or list object is found
	*/
	@Test
	@DisplayName("Invalid set or list object")
	void ter41(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er41-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er41-in.jsonld"));
		}, "invalid set or list object");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid IRI is used for @reverse.
	*/
	@Test
	@DisplayName("Invalid reverse id")
	void ter50(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er50-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er50-in.jsonld"));
		}, "invalid IRI mapping");
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid value object value is found using a value alias
	*/
	@Test
	@DisplayName("Invalid value object value using a value alias")
	void ter51(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er51-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er51-in.jsonld"));
		}, "invalid value object value");
	}

	/***
	* Verifies that an exception is raised on expansion when a context contains a definition for the empty term
	*/
	@Test
	@DisplayName("Definition for the empty term")
	void ter52(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er52-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er52-in.jsonld"));
		}, "invalid term definition");
	}

	/***
	* The value of @type in a value object MUST be a string or null.
	*/
	@Test
	@DisplayName("Invalid value object, multiple values for @type.")
	void ter54(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er54-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er54-in.jsonld"));
		}, "invalid typed value");
	}

	/***
	* The value of @type in an expanded term definition object MUST be a string or null.
	*/
	@Test
	@DisplayName("Invalid term definition, multiple values for @type.")
	void ter55(){
		assertThrows(JLDException.class, ()->{
			JsonElement input=readResourceAsJSON("/expand/er55-in.jsonld");
			JLDProcessor.expandToArray(input, URI.create("https://w3c.github.io/json-ld-api/tests/expand/er55-in.jsonld"));
		}, "invalid type mapping");
	}

}