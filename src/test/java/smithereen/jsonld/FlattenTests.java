package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import com.google.gson.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class FlattenTests{

	/***
	* Flattening drops unreferenced nodes having only @id
	*/
	@Test
	@DisplayName("drop free-floating nodes")
	void t0001(){
		JsonElement input=readResourceAsJSON("/flatten/0001-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0001-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0001-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening terms with different types of values
	*/
	@Test
	@DisplayName("basic")
	void t0002(){
		JsonElement input=readResourceAsJSON("/flatten/0002-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0002-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0002-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Verifies that null values and unmapped properties are removed from expanded output
	*/
	@Test
	@DisplayName("drop null and unmapped properties")
	void t0003(){
		JsonElement input=readResourceAsJSON("/flatten/0003-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0003-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0003-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Uses of @set are removed in expansion; values of @set, or just plain values which are empty arrays are retained
	*/
	@Test
	@DisplayName("optimize @set, keep empty arrays")
	void t0004(){
		JsonElement input=readResourceAsJSON("/flatten/0004-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0004-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0004-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* If a keyword is aliased, it is not used when flattening
	*/
	@Test
	@DisplayName("do not expand aliased @id/@type")
	void t0005(){
		JsonElement input=readResourceAsJSON("/flatten/0005-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0005-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0005-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Aliased keywords expand in resulting document
	*/
	@Test
	@DisplayName("alias keywords")
	void t0006(){
		JsonElement input=readResourceAsJSON("/flatten/0006-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0006-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0006-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Expand strings to expanded value with @type: xsd:dateTime
	*/
	@Test
	@DisplayName("date type-coercion")
	void t0007(){
		JsonElement input=readResourceAsJSON("/flatten/0007-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0007-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0007-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Keep expanded values with @language, drop non-conforming value objects containing just @language
	*/
	@Test
	@DisplayName("@value with @language")
	void t0008(){
		JsonElement input=readResourceAsJSON("/flatten/0008-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0008-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0008-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Use of @graph to contain multiple nodes within array
	*/
	@Test
	@DisplayName("@graph with terms")
	void t0009(){
		JsonElement input=readResourceAsJSON("/flatten/0009-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0009-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0009-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening native scalar retains native scalar within expanded value
	*/
	@Test
	@DisplayName("native types")
	void t0010(){
		JsonElement input=readResourceAsJSON("/flatten/0010-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0010-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0010-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* A value of a property with @type: @id coercion expands to a node reference
	*/
	@Test
	@DisplayName("coerced @id")
	void t0011(){
		JsonElement input=readResourceAsJSON("/flatten/0011-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0011-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0011-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening objects containing chained objects flattens all objects
	*/
	@Test
	@DisplayName("@graph with embed")
	void t0012(){
		JsonElement input=readResourceAsJSON("/flatten/0012-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0012-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0012-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening an expanded/flattened document maintains input document
	*/
	@Test
	@DisplayName("flatten already expanded")
	void t0013(){
		JsonElement input=readResourceAsJSON("/flatten/0013-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0013-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0013-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening aliased @set and @value
	*/
	@Test
	@DisplayName("@set of @value objects with keyword aliases")
	void t0014(){
		JsonElement input=readResourceAsJSON("/flatten/0014-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0014-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0014-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* An array of multiple @set nodes are collapsed into a single array
	*/
	@Test
	@DisplayName("collapse set of sets, keep empty lists")
	void t0015(){
		JsonElement input=readResourceAsJSON("/flatten/0015-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0015-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0015-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Setting @context to null within an embedded object resets back to initial context state
	*/
	@Test
	@DisplayName("context reset")
	void t0016(){
		JsonElement input=readResourceAsJSON("/flatten/0016-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0016-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0016-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening with @graph and @id aliases
	*/
	@Test
	@DisplayName("@graph and @id aliased")
	void t0017(){
		JsonElement input=readResourceAsJSON("/flatten/0017-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0017-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0017-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* override default @language in terms; only language-tag strings
	*/
	@Test
	@DisplayName("override default @language")
	void t0018(){
		JsonElement input=readResourceAsJSON("/flatten/0018-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0018-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0018-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening a value of null removes the value
	*/
	@Test
	@DisplayName("remove @value = null")
	void t0019(){
		JsonElement input=readResourceAsJSON("/flatten/0019-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0019-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0019-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* @graph used under a node is retained
	*/
	@Test
	@DisplayName("do not remove @graph if not at top-level")
	void t0020(){
		JsonElement input=readResourceAsJSON("/flatten/0020-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0020-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0020-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* @graph used at the top level is retained if there are other properties
	*/
	@Test
	@DisplayName("do not remove @graph at top-level if not only property")
	void t0021(){
		JsonElement input=readResourceAsJSON("/flatten/0021-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0021-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0021-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening with a default language applies that language to string values
	*/
	@Test
	@DisplayName("flatten value with default language")
	void t0022(){
		JsonElement input=readResourceAsJSON("/flatten/0022-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0022-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0022-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening lists and sets with properties having coercion coerces list/set values
	*/
	@Test
	@DisplayName("Flattening list/set with coercion")
	void t0023(){
		JsonElement input=readResourceAsJSON("/flatten/0023-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0023-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0023-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Tests that contexts in an array are merged
	*/
	@Test
	@DisplayName("Multiple contexts")
	void t0024(){
		JsonElement input=readResourceAsJSON("/flatten/0024-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0024-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0024-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening different kinds of terms and Compact IRIs
	*/
	@Test
	@DisplayName("Problematic IRI flattening tests")
	void t0025(){
		JsonElement input=readResourceAsJSON("/flatten/0025-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0025-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0025-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening term mapping to @type uses @type syntax
	*/
	@Test
	@DisplayName("Term definition with @id: @type")
	void t0026(){
		JsonElement input=readResourceAsJSON("/flatten/0026-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0026-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0026-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Duplicate values in @list and @set are not merged
	*/
	@Test
	@DisplayName("Duplicate values in @list and @set")
	void t0027(){
		JsonElement input=readResourceAsJSON("/flatten/0027-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0027-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0027-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* @vocab is used to compact properties and @type, but is not used for @id
	*/
	@Test
	@DisplayName("Use @vocab in properties and @type but not in @id")
	void t0028(){
		JsonElement input=readResourceAsJSON("/flatten/0028-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0028-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0028-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Language Maps expand values to include @language
	*/
	@Test
	@DisplayName("Language maps")
	void t0030(){
		JsonElement input=readResourceAsJSON("/flatten/0030-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0030-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0030-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening native types with type coercion adds the coerced type to an expanded value representation and retains the native value representation
	*/
	@Test
	@DisplayName("type-coercion of native types")
	void t0031(){
		JsonElement input=readResourceAsJSON("/flatten/0031-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0031-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0031-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Mapping a term to null decouples it from @vocab
	*/
	@Test
	@DisplayName("Null term and @vocab")
	void t0032(){
		JsonElement input=readResourceAsJSON("/flatten/0032-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0032-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0032-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Verifies that terms can be defined using @vocab
	*/
	@Test
	@DisplayName("Using @vocab with with type-coercion")
	void t0033(){
		JsonElement input=readResourceAsJSON("/flatten/0033-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0033-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0033-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Verifies multiple values from separate terms are deterministically made multiple values of the IRI associated with the terms
	*/
	@Test
	@DisplayName("Multiple properties expanding to the same IRI")
	void t0034(){
		JsonElement input=readResourceAsJSON("/flatten/0034-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0034-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0034-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Pathological tests of language maps
	*/
	@Test
	@DisplayName("Language maps with @vocab, default language, and colliding property")
	void t0035(){
		JsonElement input=readResourceAsJSON("/flatten/0035-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0035-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0035-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening index maps for terms defined with @container: @index
	*/
	@Test
	@DisplayName("Flattening @index")
	void t0036(){
		JsonElement input=readResourceAsJSON("/flatten/0036-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0036-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0036-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flattening @reverse keeps @reverse
	*/
	@Test
	@DisplayName("Flattening reverse properties")
	void t0037(){
		JsonElement input=readResourceAsJSON("/flatten/0037-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0037-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0037-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Blank nodes are not relabeled during expansion
	*/
	@Test
	@DisplayName("Flattening blank node labels")
	void t0038(){
		JsonElement input=readResourceAsJSON("/flatten/0038-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0038-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0038-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Terms within @reverse are expanded
	*/
	@Test
	@DisplayName("Using terms in a reverse-maps")
	void t0039(){
		JsonElement input=readResourceAsJSON("/flatten/0039-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0039-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0039-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Only invoke language and index map expansion if the value is a JSON object
	*/
	@Test
	@DisplayName("language and index expansion on non-objects")
	void t0040(){
		JsonElement input=readResourceAsJSON("/flatten/0040-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0040-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0040-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Free-floating values in sets are removed, free-floating lists are removed completely
	*/
	@Test
	@DisplayName("Free-floating sets and lists")
	void t0041(){
		JsonElement input=readResourceAsJSON("/flatten/0041-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0041-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0041-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Lists objects are implicit unlabeled blank nodes and thus never equivalent
	*/
	@Test
	@DisplayName("List objects not equivalent")
	void t0042(){
		JsonElement input=readResourceAsJSON("/flatten/0042-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0042-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0042-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flatten a test manifest
	*/
	@Test
	@DisplayName("Sample test manifest extract")
	void t0043(){
		JsonElement input=readResourceAsJSON("/flatten/0043-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0043-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0043-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Proper (re-)labeling of blank nodes if used with reverse properties.
	*/
	@Test
	@DisplayName("Blank nodes with reverse properties")
	void t0045(){
		JsonElement input=readResourceAsJSON("/flatten/0045-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0045-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0045-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Usage of empty strings in identifiers needs special care when constructing the node map.
	*/
	@Test
	@DisplayName("Empty string as identifier")
	void t0046(){
		JsonElement input=readResourceAsJSON("/flatten/0046-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0046-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0046-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Compacting a relative round-trips
	*/
	@Test
	@DisplayName("Flatten using relative fragment identifier properly joins to base")
	void t0047(){
		JsonElement input=readResourceAsJSON("/flatten/0047-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0047-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("http://example.org/"));
		assertEquals(expect, flattened);
	}

	/***
	* Node definitions contained within lists are flattend to top level.
	*/
	@Test
	@DisplayName("@list with embedded object")
	void t0048(){
		JsonElement input=readResourceAsJSON("/flatten/0048-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0048-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0048-in.jsonld"));
		assertEquals(expect, flattened);
	}

	/***
	* Flatten with context including JavaScript Object property names
	*/
	@Test
	@DisplayName("context with JavaScript Object property names")
	void t0049(){
		JsonElement input=readResourceAsJSON("/flatten/0049-in.jsonld");
		JsonElement expect=readResourceAsJSON("/flatten/0049-out.jsonld");
		JsonArray flattened=JLDProcessor.flatten(input, URI.create("https://w3c.github.io/json-ld-api/tests/flatten/0049-in.jsonld"));
		assertEquals(expect, flattened);
	}

}