package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Collections;

import org.json.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class CompactTests{

	/***
	* Unreferenced nodes not containing properties are dropped
	*/
	@Test
	@DisplayName("drop free-floating nodes")
	void t0001(){
		JSONArray input=readResourceAsArray("/compact/0001-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0001-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0001-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Basic term and value compaction
	*/
	@Test
	@DisplayName("basic")
	void t0002(){
		JSONArray input=readResourceAsArray("/compact/0002-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0002-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0002-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Properties mapped to null or which are never mapped are dropped
	*/
	@Test
	@DisplayName("drop null and unmapped properties")
	void t0003(){
		JSONArray input=readResourceAsArray("/compact/0003-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0003-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0003-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Containers mapped to @set keep empty arrays
	*/
	@Test
	@DisplayName("optimize @set, keep empty arrays")
	void t0004(){
		JSONArray input=readResourceAsArray("/compact/0004-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0004-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0004-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact uses prefixes in @type
	*/
	@Test
	@DisplayName("@type and prefix compaction")
	void t0005(){
		JSONArray input=readResourceAsArray("/compact/0005-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0005-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0005-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Values not matching a coerced @type remain in expanded form
	*/
	@Test
	@DisplayName("keep expanded object format if @type doesn't match")
	void t0006(){
		JSONArray input=readResourceAsArray("/compact/0006-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0006-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0006-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* External context is added to the compacted document
	*/
	@Test
	@DisplayName("add context")
	void t0007(){
		JSONArray input=readResourceAsArray("/compact/0007-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0007-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0007-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Aliases for keywords are used in compacted document
	*/
	@Test
	@DisplayName("alias keywords")
	void t0008(){
		JSONArray input=readResourceAsArray("/compact/0008-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0008-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0008-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Value with @id is compacted to string if property cast to @id
	*/
	@Test
	@DisplayName("compact @id")
	void t0009(){
		JSONArray input=readResourceAsArray("/compact/0009-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0009-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0009-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* An array of objects is serialized with @graph
	*/
	@Test
	@DisplayName("array to @graph")
	void t0010(){
		JSONArray input=readResourceAsArray("/compact/0010-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0010-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0010-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Expanded value with type xsd:dateTime is represented as string with type coercion
	*/
	@Test
	@DisplayName("compact date")
	void t0011(){
		JSONArray input=readResourceAsArray("/compact/0011-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0011-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0011-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Native values are unmodified during compaction
	*/
	@Test
	@DisplayName("native types")
	void t0012(){
		JSONArray input=readResourceAsArray("/compact/0012-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0012-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0012-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Values with @language remain in expanded form by default
	*/
	@Test
	@DisplayName("@value with @language")
	void t0013(){
		JSONArray input=readResourceAsArray("/compact/0013-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0013-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0013-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Aliasing @graph uses alias in compacted document
	*/
	@Test
	@DisplayName("array to aliased @graph")
	void t0014(){
		JSONArray input=readResourceAsArray("/compact/0014-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0014-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0014-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Property with values of different types use most appropriate term when compacting
	*/
	@Test
	@DisplayName("best match compaction")
	void t0015(){
		JSONArray input=readResourceAsArray("/compact/0015-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0015-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0015-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting a document with multiple embedded uses of @graph
	*/
	@Test
	@DisplayName("recursive named graphs")
	void t0016(){
		JSONArray input=readResourceAsArray("/compact/0016-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0016-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0016-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Mapping a term to null causes the property and its values to be removed from the compacted document
	*/
	@Test
	@DisplayName("A term mapping to null removes the mapping")
	void t0017(){
		JSONArray input=readResourceAsArray("/compact/0017-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0017-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0017-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Lists with values of different types use best term in compacted document
	*/
	@Test
	@DisplayName("best matching term for lists")
	void t0018(){
		JSONArray input=readResourceAsArray("/compact/0018-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0018-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0018-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Duplicate values in @list or @set are retained in compacted document
	*/
	@Test
	@DisplayName("Keep duplicate values in @list and @set")
	void t0019(){
		JSONArray input=readResourceAsArray("/compact/0019-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0019-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0019-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* A term with @container: @list is also used as the value of an @id, if appropriate
	*/
	@Test
	@DisplayName("Compact @id that is a property IRI when @container is @list")
	void t0020(){
		JSONArray input=readResourceAsArray("/compact/0020-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0020-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0020-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* @vocab is used to create relative properties and types if no other term matches
	*/
	@Test
	@DisplayName("Compact properties and types using @vocab")
	void t0021(){
		JSONArray input=readResourceAsArray("/compact/0021-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0021-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0021-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact nested properties using @list containers
	*/
	@Test
	@DisplayName("@list compaction of nested properties")
	void t0022(){
		JSONArray input=readResourceAsArray("/compact/0022-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0022-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0022-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* @vocab takes precedence over prefixes - even if the result is longer
	*/
	@Test
	@DisplayName("prefer @vocab over compacted IRIs")
	void t0023(){
		JSONArray input=readResourceAsArray("/compact/0023-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0023-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0023-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* The most specific term that matches all of the elements in the list, taking into account the default language, must be selected.
	*/
	@Test
	@DisplayName("most specific term matching in @list.")
	void t0024(){
		JSONArray input=readResourceAsArray("/compact/0024-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0024-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0024-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Multiple values with different languages use language maps if property has @container: @language
	*/
	@Test
	@DisplayName("Language maps")
	void t0025(){
		JSONArray input=readResourceAsArray("/compact/0025-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0025-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0025-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Test appropriate property use given language maps with @vocab, a default language, and a competing term
	*/
	@Test
	@DisplayName("Language map term selection with complications")
	void t0026(){
		JSONArray input=readResourceAsArray("/compact/0026-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0026-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0026-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Fall back to term with @set container if term with language map is defined
	*/
	@Test
	@DisplayName("@container: @set with multiple values")
	void t0027(){
		JSONArray input=readResourceAsArray("/compact/0027-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0027-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0027-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Combination of keyword aliases and @vocab
	*/
	@Test
	@DisplayName("Alias keywords and use @vocab")
	void t0028(){
		JSONArray input=readResourceAsArray("/compact/0028-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0028-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0028-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Output uses index mapping if term is defined with @container: @index
	*/
	@Test
	@DisplayName("Simple @index map")
	void t0029(){
		JSONArray input=readResourceAsArray("/compact/0029-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0029-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0029-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Preserve @index tags if not compacted to an index map
	*/
	@Test
	@DisplayName("non-matching @container: @index")
	void t0030(){
		JSONArray input=readResourceAsArray("/compact/0030-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0030-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0030-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact traverses through @reverse
	*/
	@Test
	@DisplayName("Compact @reverse")
	void t0031(){
		JSONArray input=readResourceAsArray("/compact/0031-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0031-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0031-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact traverses through @reverse
	*/
	@Test
	@DisplayName("Compact keys in reverse-maps")
	void t0032(){
		JSONArray input=readResourceAsArray("/compact/0032-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0032-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0032-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* A reverse map is replaced with a matching property defined with @reverse
	*/
	@Test
	@DisplayName("Compact reverse-map to reverse property")
	void t0033(){
		JSONArray input=readResourceAsArray("/compact/0033-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0033-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0033-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Do not use reverse property if no other property matches as normal property
	*/
	@Test
	@DisplayName("Skip property with @reverse if no match")
	void t0034(){
		JSONArray input=readResourceAsArray("/compact/0034-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0034-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0034-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact node references to strings for reverse properties using @type: @id
	*/
	@Test
	@DisplayName("Compact @reverse node references using strings")
	void t0035(){
		JSONArray input=readResourceAsArray("/compact/0035-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0035-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0035-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact using both reverse properties and index containers
	*/
	@Test
	@DisplayName("Compact reverse properties using index containers")
	void t0036(){
		JSONArray input=readResourceAsArray("/compact/0036-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0036-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0036-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact keys in @reverse using @vocab
	*/
	@Test
	@DisplayName("Compact keys in @reverse using @vocab")
	void t0037(){
		JSONArray input=readResourceAsArray("/compact/0037-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0037-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0037-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Complex round-tripping use case from Drupal
	*/
	@Test
	@DisplayName("Index map round-tripping")
	void t0038(){
		JSONArray input=readResourceAsArray("/compact/0038-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0038-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0038-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Value of @graph is always an array
	*/
	@Test
	@DisplayName("@graph is array")
	void t0039(){
		JSONArray input=readResourceAsArray("/compact/0039-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0039-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0039-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Ensure that value of @list is always an array
	*/
	@Test
	@DisplayName("@list is array")
	void t0040(){
		JSONArray input=readResourceAsArray("/compact/0040-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0040-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0040-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* If an index is present, a term having an @list container is not selected
	*/
	@Test
	@DisplayName("index rejects term having @list")
	void t0041(){
		JSONArray input=readResourceAsArray("/compact/0041-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0041-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0041-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Make sure keyword aliasing works if a list can't be compacted
	*/
	@Test
	@DisplayName("@list keyword aliasing")
	void t0042(){
		JSONArray input=readResourceAsArray("/compact/0042-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0042-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0042-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Ensure that @vocab compaction isn't used if the result collides with a term
	*/
	@Test
	@DisplayName("select term over @vocab")
	void t0043(){
		JSONArray input=readResourceAsArray("/compact/0043-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0043-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0043-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Prefer properties with @type: @vocab in reverse-maps if the value can be compacted to a term
	*/
	@Test
	@DisplayName("@type: @vocab in reverse-map")
	void t0044(){
		JSONArray input=readResourceAsArray("/compact/0044-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0044-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0044-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Values of @id are transformed to relative IRIs, terms are ignored
	*/
	@Test
	@DisplayName("@id value uses relative IRI, not term")
	void t0045(){
		JSONArray input=readResourceAsArray("/compact/0045-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0045-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0045-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Wrap top-level array into @graph even if no context is passed
	*/
	@Test
	@DisplayName("multiple objects without @context use @graph")
	void t0046(){
		JSONArray input=readResourceAsArray("/compact/0046-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0046-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0046-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Relative URLs remain relative after compaction
	*/
	@Test
	@DisplayName("Round-trip relative URLs")
	void t0047(){
		JSONArray input=readResourceAsArray("/compact/0047-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0047-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0047-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Prefer terms with a language mapping set to null over terms without language-mapping for non-strings
	*/
	@Test
	@DisplayName("term with @language: null")
	void t0048(){
		JSONArray input=readResourceAsArray("/compact/0048-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0048-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0048-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* List compaction without @container: @list still uses strings if @type: @id
	*/
	@Test
	@DisplayName("Round tripping of lists that contain just IRIs")
	void t0049(){
		JSONArray input=readResourceAsArray("/compact/0049-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0049-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0049-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Node references in reverse properties are not compacted to strings without explicit type-coercion
	*/
	@Test
	@DisplayName("Reverse properties require @type: @id to use string values")
	void t0050(){
		JSONArray input=readResourceAsArray("/compact/0050-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0050-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0050-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Native values survive round-tripping with @list
	*/
	@Test
	@DisplayName("Round tripping @list with scalar")
	void t0051(){
		JSONArray input=readResourceAsArray("/compact/0051-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0051-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0051-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Native values survive round-tripping with @list and @graph alias
	*/
	@Test
	@DisplayName("Round tripping @list with scalar and @graph alias")
	void t0052(){
		JSONArray input=readResourceAsArray("/compact/0052-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0052-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0052-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact to @type: @vocab when no @type: @id term available
	*/
	@Test
	@DisplayName("Use @type: @vocab if no @type: @id")
	void t0053(){
		JSONArray input=readResourceAsArray("/compact/0053-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0053-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0053-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact to @type: @vocab and compact @id to term
	*/
	@Test
	@DisplayName("Compact to @type: @vocab and compact @id to term")
	void t0054(){
		JSONArray input=readResourceAsArray("/compact/0054-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0054-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0054-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term
	*/
	@Test
	@DisplayName("Round tripping @type: @vocab")
	void t0055(){
		JSONArray input=readResourceAsArray("/compact/0055-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0055-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0055-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term
	*/
	@Test
	@DisplayName("Prefer @type: @vocab over @type: @id for terms")
	void t0056(){
		JSONArray input=readResourceAsArray("/compact/0056-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0056-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0056-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting IRI value of property with @type: @vocab can use term; more complex
	*/
	@Test
	@DisplayName("Complex round tripping @type: @vocab and @type: @id")
	void t0057(){
		JSONArray input=readResourceAsArray("/compact/0057-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0057-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0057-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Choose a term having @type: @id over @type: @value if value is not a term
	*/
	@Test
	@DisplayName("Prefer @type: @id over @type: @vocab for non-terms")
	void t0058(){
		JSONArray input=readResourceAsArray("/compact/0058-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0058-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0058-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* If there's no term with @type: @id, use terms with @type: @vocab for IRIs not mapped to terms
	*/
	@Test
	@DisplayName("Term with @type: @vocab if no @type: @id")
	void t0059(){
		JSONArray input=readResourceAsArray("/compact/0059-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0059-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0059-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* If there's no term with @type: @vocab, use terms with @type: @id for IRIs mapped to terms
	*/
	@Test
	@DisplayName("Term with @type: @id if no @type: @vocab and term value")
	void t0060(){
		JSONArray input=readResourceAsArray("/compact/0060-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0060-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0060-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Separate IRIs for the same property to use term with more specific @type (@id vs. @vocab)
	*/
	@Test
	@DisplayName("@type: @vocab/@id with values matching either")
	void t0061(){
		JSONArray input=readResourceAsArray("/compact/0061-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0061-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0061-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Relative IRIs don't round-trip with @type: @vocab
	*/
	@Test
	@DisplayName("@type: @vocab and relative IRIs")
	void t0062(){
		JSONArray input=readResourceAsArray("/compact/0062-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0062-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0062-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Term with @type: @vocab will use compact IRIs
	*/
	@Test
	@DisplayName("Compact IRI round-tripping with @type: @vocab")
	void t0063(){
		JSONArray input=readResourceAsArray("/compact/0063-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0063-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0063-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Given values with both @index and @language and term index-map term, use index map
	*/
	@Test
	@DisplayName("Compact language-tagged and indexed strings to index-map")
	void t0064(){
		JSONArray input=readResourceAsArray("/compact/0064-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0064-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0064-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Language-tagged and indexed strings don't compact to language-map
	*/
	@Test
	@DisplayName("Language-tagged and indexed strings with language-map")
	void t0065(){
		JSONArray input=readResourceAsArray("/compact/0065-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0065-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0065-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Complex use cases for relative IRI compaction
	*/
	@Test
	@DisplayName("Relative IRIs")
	void t0066(){
		JSONArray input=readResourceAsArray("/compact/0066-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0066-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0066-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact reverse property whose values are unlabeled blank nodes
	*/
	@Test
	@DisplayName("Reverse properties with blank nodes")
	void t0067(){
		JSONArray input=readResourceAsArray("/compact/0067-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0067-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0067-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Single values of reverse properties are compacted as values of ordinary properties
	*/
	@Test
	@DisplayName("Single value reverse properties")
	void t0068(){
		JSONArray input=readResourceAsArray("/compact/0068-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0068-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0068-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Single values are kept in array form for reverse properties if the container is to @set
	*/
	@Test
	@DisplayName("Single value reverse properties with @set")
	void t0069(){
		JSONArray input=readResourceAsArray("/compact/0069-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0069-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0069-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Setting compactArrays to false causes single element arrays to be retained
	*/
	@Test
	@DisplayName("compactArrays option")
	void t0070(){
		JSONArray input=readResourceAsArray("/compact/0070-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0070-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0070-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"), false);
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Expanding input with multiple @contexts and compacting with just one doesn't output undefined properties
	*/
	@Test
	@DisplayName("Input has multiple @contexts, output has one")
	void t0071(){
		JSONArray input=readResourceAsArray("/compact/0071-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0071-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0071-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Ensure that the default language is handled correctly for unmapped properties
	*/
	@Test
	@DisplayName("Default language and unmapped properties")
	void t0072(){
		JSONArray input=readResourceAsArray("/compact/0072-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0072-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0072-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Ensure that compaction works with mapped @id and @type
	*/
	@Test
	@DisplayName("Mapped @id and @type")
	void t0073(){
		JSONArray input=readResourceAsArray("/compact/0073-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0073-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0073-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Ensure that compaction works for empty list when property has container declared as @list and type as @id
	*/
	@Test
	@DisplayName("Container as a list with type of @id")
	void t0074(){
		JSONArray input=readResourceAsArray("/compact/0074-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0074-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0074-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting a relative round-trips
	*/
	@Test
	@DisplayName("Compact using relative fragment identifier")
	void t0075(){
		JSONArray input=readResourceAsArray("/compact/0075-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0075-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0075-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compacting IRI equivalent to base, uses last path segment of base ending in '/'
	*/
	@Test
	@DisplayName("Compacting IRI equivalent to base")
	void t0076(){
		JSONArray input=readResourceAsArray("/compact/0076-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0076-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0076-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Test appropriate property use given language maps with @vocab, a default language, no language, and competing terms
	*/
	@Test
	@DisplayName("Language map term selection with complications")
	void t0089(){
		JSONArray input=readResourceAsArray("/compact/0089-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0089-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0089-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Complex use cases for relative IRI compaction or properties
	*/
	@Test
	@DisplayName("Relative propererty IRIs with @vocab: ''")
	void t0095(){
		JSONArray input=readResourceAsArray("/compact/0095-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0095-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0095-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Complex use cases for relative IRI compaction or properties
	*/
	@Test
	@DisplayName("Relative propererty IRIs with @vocab: ''")
	void t0107(){
		JSONArray input=readResourceAsArray("/compact/0107-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0107-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0107-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Compact with context including JavaScript Object property names
	*/
	@Test
	@DisplayName("context with JavaScript Object property names")
	void t0108(){
		JSONArray input=readResourceAsArray("/compact/0108-in.jsonld");
		Object expect=readResourceAsJSON("/compact/0108-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/0108-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

	/***
	* Verifies that an exception is raised in Compaction when attempting to compact a list of lists
	*/
	@Test
	@DisplayName("Compaction to list of lists")
	void te001(){
		assertThrows(JLDException.class, ()->{
			JSONArray input=readResourceAsArray("/compact/e001-in.jsonld");
			JSONObject context=(JSONObject)readResourceAsJSON("/compact/e001-context.jsonld");
			JLDDocument.compact(input, context.get("@context"));
		}, "compaction to list of lists");
	}

	/***
	* The most specific term that matches all of the elements in the list, taking into account the default language, must be selected, without considering case of language.
	*/
	@Test
	@DisplayName("most specific term matching in @list.")
	void tla01(){
		JSONArray input=readResourceAsArray("/compact/la01-in.jsonld");
		Object expect=readResourceAsJSON("/compact/la01-out.jsonld");
		JSONObject context=(JSONObject)readResourceAsJSON("/compact/la01-context.jsonld");
		JSONObject compacted=JLDDocument.compact(input, context.get("@context"));
		compacted.put("@context", context.get("@context"));
		assertEqualJLD(expect, compacted);
	}

}