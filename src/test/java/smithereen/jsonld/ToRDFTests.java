package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class ToRDFTests{

	/***
	* Tests generation of a triple using full URIs and a plain literal.
	*/
	@Test
	@DisplayName("Plain literal with URIs")
	void t0001(){
		Object input=readResourceAsJSON("/toRdf/0001-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0001-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0001-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests generation of a triple using a CURIE defined in the default context.
	*/
	@Test
	@DisplayName("Plain literal with CURIE from default context")
	void t0002(){
		Object input=readResourceAsJSON("/toRdf/0002-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0002-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0002-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a BNode is created if no explicit subject is set.
	*/
	@Test
	@DisplayName("Default subject is BNode")
	void t0003(){
		Object input=readResourceAsJSON("/toRdf/0003-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0003-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0003-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a plain literal is created with a language tag.
	*/
	@Test
	@DisplayName("Literal with language tag")
	void t0004(){
		Object input=readResourceAsJSON("/toRdf/0004-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0004-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0004-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a literal may be created using extended characters.
	*/
	@Test
	@DisplayName("Extended character set literal")
	void t0005(){
		Object input=readResourceAsJSON("/toRdf/0005-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0005-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0005-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests creation of a literal with a datatype.
	*/
	@Test
	@DisplayName("Typed literal")
	void t0006(){
		Object input=readResourceAsJSON("/toRdf/0006-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0006-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0006-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Verify that 'a' is an alias for rdf:type, and the object is created as an IRI.
	*/
	@Test
	@DisplayName("Tests 'a' generates rdf:type and object is implicit IRI")
	void t0007(){
		Object input=readResourceAsJSON("/toRdf/0007-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0007-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0007-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Generate an IRI using a prefix defined within an @context.
	*/
	@Test
	@DisplayName("Test prefix defined in @context")
	void t0008(){
		Object input=readResourceAsJSON("/toRdf/0008-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0008-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0008-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* An empty suffix may be used.
	*/
	@Test
	@DisplayName("Test using an empty suffix")
	void t0009(){
		Object input=readResourceAsJSON("/toRdf/0009-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0009-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0009-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* A property referencing an associative array gets object from subject of array.
	*/
	@Test
	@DisplayName("Test object processing defines object")
	void t0010(){
		Object input=readResourceAsJSON("/toRdf/0010-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0010-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0010-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* If no @ is specified, a BNode is created, and will be used as the object of an enclosing property.
	*/
	@Test
	@DisplayName("Test object processing defines object with implicit BNode")
	void t0011(){
		Object input=readResourceAsJSON("/toRdf/0011-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0011-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0011-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that Multiple Objects are for a Single Property using array syntax.
	*/
	@Test
	@DisplayName("Multiple Objects for a Single Property")
	void t0012(){
		Object input=readResourceAsJSON("/toRdf/0012-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0012-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0012-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that @list: [] generates an empty list.
	*/
	@Test
	@DisplayName("Creation of an empty list")
	void t0013(){
		Object input=readResourceAsJSON("/toRdf/0013-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0013-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0013-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that @list generates a list.
	*/
	@Test
	@DisplayName("Creation of a list with single element")
	void t0014(){
		Object input=readResourceAsJSON("/toRdf/0014-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0014-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0014-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that list with multiple elements.
	*/
	@Test
	@DisplayName("Creation of a list with multiple elements")
	void t0015(){
		Object input=readResourceAsJSON("/toRdf/0015-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0015-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0015-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Expanding an empty IRI uses the test file location.
	*/
	@Test
	@DisplayName("Empty IRI expands to resource location")
	void t0016(){
		Object input=readResourceAsJSON("/toRdf/0016-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0016-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0016-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Expanding a relative IRI uses the test file location.
	*/
	@Test
	@DisplayName("Relative IRI expands relative resource location")
	void t0017(){
		Object input=readResourceAsJSON("/toRdf/0017-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0017-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0017-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Expanding a fragment uses the test file location.
	*/
	@Test
	@DisplayName("Frag ID expands relative resource location")
	void t0018(){
		Object input=readResourceAsJSON("/toRdf/0018-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0018-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0018-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests coercion of object to anyURI when specified.
	*/
	@Test
	@DisplayName("Test type coercion to anyURI")
	void t0019(){
		Object input=readResourceAsJSON("/toRdf/0019-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0019-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0019-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests coercion of object to a typed literal when specified.
	*/
	@Test
	@DisplayName("Test type coercion to typed literal")
	void t0020(){
		Object input=readResourceAsJSON("/toRdf/0020-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0020-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0020-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a decimal value generates a xsd:double typed literal;.
	*/
	@Test
	@DisplayName("Test coercion of double value")
	void t0022(){
		Object input=readResourceAsJSON("/toRdf/0022-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0022-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0022-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a decimal value generates a xsd:integer typed literal.
	*/
	@Test
	@DisplayName("Test coercion of integer value")
	void t0023(){
		Object input=readResourceAsJSON("/toRdf/0023-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0023-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0023-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that a decimal value generates a xsd:boolean typed literal.
	*/
	@Test
	@DisplayName("Test coercion of boolean value")
	void t0024(){
		Object input=readResourceAsJSON("/toRdf/0024-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0024-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0024-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that an array with a single element on a property with @list coercion creates an RDF Collection.
	*/
	@Test
	@DisplayName("Test list coercion with single element")
	void t0025(){
		Object input=readResourceAsJSON("/toRdf/0025-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0025-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0025-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that @type with an array of types creates multiple types.
	*/
	@Test
	@DisplayName("Test creation of multiple types")
	void t0026(){
		Object input=readResourceAsJSON("/toRdf/0026-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0026-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0026-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Using @graph with other keys places triples in a named graph.
	*/
	@Test
	@DisplayName("Simple named graph (Wikidata)")
	void t0027(){
		Object input=readResourceAsJSON("/toRdf/0027-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0027-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0027-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Signing a graph.
	*/
	@Test
	@DisplayName("Simple named graph")
	void t0028(){
		Object input=readResourceAsJSON("/toRdf/0028-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0028-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0028-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that named graphs containing named graphs flatten to single level of graph naming.
	*/
	@Test
	@DisplayName("named graph with embedded named graph")
	void t0029(){
		Object input=readResourceAsJSON("/toRdf/0029-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0029-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0029-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests graphs containing subject references as strings.
	*/
	@Test
	@DisplayName("top-level graph with string subject reference")
	void t0030(){
		Object input=readResourceAsJSON("/toRdf/0030-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0030-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0030-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests conversion of reverse properties.
	*/
	@Test
	@DisplayName("Reverse property")
	void t0031(){
		Object input=readResourceAsJSON("/toRdf/0031-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0031-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0031-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that generated triples do not depend on order of @context.
	*/
	@Test
	@DisplayName("@context reordering")
	void t0032(){
		Object input=readResourceAsJSON("/toRdf/0032-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0032-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0032-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that generated triples do not depend on order of @id.
	*/
	@Test
	@DisplayName("@id reordering")
	void t0033(){
		Object input=readResourceAsJSON("/toRdf/0033-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0033-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0033-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Tests that generated triples do not depend on order of properties inside @context.
	*/
	@Test
	@DisplayName("context properties reordering")
	void t0034(){
		Object input=readResourceAsJSON("/toRdf/0034-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0034-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0034-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* xsd:double's canonical lexical is used when converting numbers without fraction that are coerced to xsd:double
	*/
	@Test
	@DisplayName("non-fractional numbers converted to xsd:double")
	void t0035(){
		Object input=readResourceAsJSON("/toRdf/0035-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0035-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0035-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* The toRDF algorithm does not relabel blank nodes; it reuses the counter from the nodeMapGeneration to generate new ones
	*/
	@Test
	@DisplayName("Use nodeMapGeneration bnode labels")
	void t0036(){
		Object input=readResourceAsJSON("/toRdf/0036-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0036-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0036-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Basic use of creating a named graph using an IRI name
	*/
	@Test
	@DisplayName("Dataset with a IRI named graph")
	void t0113(){
		Object input=readResourceAsJSON("/toRdf/0113-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0113-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0113-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Basic use of creating a named graph using a BNode name
	*/
	@Test
	@DisplayName("Dataset with a IRI named graph")
	void t0114(){
		Object input=readResourceAsJSON("/toRdf/0114-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0114-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0114-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Dataset with a default and two named graphs (IRI and BNode)
	*/
	@Test
	@DisplayName("Dataset with a default and two named graphs")
	void t0115(){
		Object input=readResourceAsJSON("/toRdf/0115-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0115-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0115-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Embedding @graph in a node creates a named graph
	*/
	@Test
	@DisplayName("Dataset from node with embedded named graph")
	void t0116(){
		Object input=readResourceAsJSON("/toRdf/0116-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0116-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0116-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Embedding @graph in a node creates a named graph. Graph name is created if there is no subject
	*/
	@Test
	@DisplayName("Dataset from node with embedded named graph (bnode)")
	void t0117(){
		Object input=readResourceAsJSON("/toRdf/0117-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0117-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0117-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Triples with blank node predicates are not dropped if the produce generalized RDF flag is true.
	*/
	@Test
	@DisplayName("produce generalized RDF flag")
	void t0118(){
		Object input=readResourceAsJSON("/toRdf/0118-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0118-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0118-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Proper (re-)labeling of blank nodes if used with reverse properties.
	*/
	@Test
	@DisplayName("Blank nodes with reverse properties")
	void t0119(){
		Object input=readResourceAsJSON("/toRdf/0119-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0119-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0119-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (0)")
	void t0120(){
		Object input=readResourceAsJSON("/toRdf/0120-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0120-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0120-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (1)")
	void t0121(){
		Object input=readResourceAsJSON("/toRdf/0121-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0121-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0121-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (2)")
	void t0122(){
		Object input=readResourceAsJSON("/toRdf/0122-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0122-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0122-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (3)")
	void t0123(){
		Object input=readResourceAsJSON("/toRdf/0123-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0123-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0123-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (4)")
	void t0124(){
		Object input=readResourceAsJSON("/toRdf/0124-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0124-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0124-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (5)")
	void t0125(){
		Object input=readResourceAsJSON("/toRdf/0125-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0125-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0125-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (6)")
	void t0126(){
		Object input=readResourceAsJSON("/toRdf/0126-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0126-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0126-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (7)")
	void t0127(){
		Object input=readResourceAsJSON("/toRdf/0127-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0127-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0127-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (8)")
	void t0128(){
		Object input=readResourceAsJSON("/toRdf/0128-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0128-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0128-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (9)")
	void t0129(){
		Object input=readResourceAsJSON("/toRdf/0129-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0129-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0129-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (10)")
	void t0130(){
		Object input=readResourceAsJSON("/toRdf/0130-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0130-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0130-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (11)")
	void t0131(){
		Object input=readResourceAsJSON("/toRdf/0131-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0131-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0131-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* IRI resolution according to RFC3986.
	*/
	@Test
	@DisplayName("IRI Resolution (12)")
	void t0132(){
		Object input=readResourceAsJSON("/toRdf/0132-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/0132-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/0132-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Free-floating nodes do not generate RDF triples (from expand-0001)
	*/
	@Test
	@DisplayName("drop free-floating nodes")
	void te001(){
		Object input=readResourceAsJSON("/toRdf/e001-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e001-in.jsonld"));
		assertEquals(0, result.size());
	}

	/***
	* Basic RDF conversion (from expand-0002)
	*/
	@Test
	@DisplayName("basic")
	void te002(){
		Object input=readResourceAsJSON("/toRdf/e002-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e002-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e002-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Properties mapped to null or which are never mapped are dropped (from expand-0003)
	*/
	@Test
	@DisplayName("drop null and unmapped properties")
	void te003(){
		Object input=readResourceAsJSON("/toRdf/e003-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e003-in.jsonld"));
		assertEquals(0, result.size());
	}

	/***
	* RDF version of expand-0004
	*/
	@Test
	@DisplayName("optimize @set, keep empty arrays")
	void te004(){
		Object input=readResourceAsJSON("/toRdf/e004-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e004-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e004-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0005
	*/
	@Test
	@DisplayName("do not expand aliased @id/@type")
	void te005(){
		Object input=readResourceAsJSON("/toRdf/e005-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e005-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e005-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0006
	*/
	@Test
	@DisplayName("alias keywords")
	void te006(){
		Object input=readResourceAsJSON("/toRdf/e006-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e006-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e006-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Type-coerced dates generate typed literals (from expand-0007)
	*/
	@Test
	@DisplayName("date type-coercion")
	void te007(){
		Object input=readResourceAsJSON("/toRdf/e007-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e007-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e007-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0008
	*/
	@Test
	@DisplayName("@value with @language")
	void te008(){
		Object input=readResourceAsJSON("/toRdf/e008-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e008-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e008-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0009
	*/
	@Test
	@DisplayName("@graph with terms")
	void te009(){
		Object input=readResourceAsJSON("/toRdf/e009-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e009-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e009-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Native types generate typed literals (from expand-0010)
	*/
	@Test
	@DisplayName("native types")
	void te010(){
		Object input=readResourceAsJSON("/toRdf/e010-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e010-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e010-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0011
	*/
	@Test
	@DisplayName("coerced @id")
	void te011(){
		Object input=readResourceAsJSON("/toRdf/e011-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e011-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e011-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0012
	*/
	@Test
	@DisplayName("@graph with embed")
	void te012(){
		Object input=readResourceAsJSON("/toRdf/e012-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e012-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e012-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0013
	*/
	@Test
	@DisplayName("expand already expanded")
	void te013(){
		Object input=readResourceAsJSON("/toRdf/e013-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e013-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e013-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0014
	*/
	@Test
	@DisplayName("@set of @value objects with keyword aliases")
	void te014(){
		Object input=readResourceAsJSON("/toRdf/e014-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e014-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e014-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0015
	*/
	@Test
	@DisplayName("collapse set of sets, keep empty lists")
	void te015(){
		Object input=readResourceAsJSON("/toRdf/e015-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e015-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e015-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0016
	*/
	@Test
	@DisplayName("context reset")
	void te016(){
		Object input=readResourceAsJSON("/toRdf/e016-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e016-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e016-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0017
	*/
	@Test
	@DisplayName("@graph and @id aliased")
	void te017(){
		Object input=readResourceAsJSON("/toRdf/e017-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e017-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e017-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0018
	*/
	@Test
	@DisplayName("override default @language")
	void te018(){
		Object input=readResourceAsJSON("/toRdf/e018-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e018-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e018-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0019
	*/
	@Test
	@DisplayName("remove @value = null")
	void te019(){
		Object input=readResourceAsJSON("/toRdf/e019-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e019-in.jsonld"));
		assertEquals(0, result.size());
	}

	/***
	* Embedded @graph without @id creates BNode-labeled named graph (from expand-0020)
	*/
	@Test
	@DisplayName("do not remove @graph if not at top-level")
	void te020(){
		Object input=readResourceAsJSON("/toRdf/e020-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e020-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e020-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0021
	*/
	@Test
	@DisplayName("do not remove @graph at top-level if not only property")
	void te021(){
		Object input=readResourceAsJSON("/toRdf/e021-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e021-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e021-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0022
	*/
	@Test
	@DisplayName("expand value with default language")
	void te022(){
		Object input=readResourceAsJSON("/toRdf/e022-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e022-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e022-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0023
	*/
	@Test
	@DisplayName("Lists and sets of properties with list/set coercion")
	void te023(){
		Object input=readResourceAsJSON("/toRdf/e023-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e023-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e023-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0024
	*/
	@Test
	@DisplayName("Multiple contexts")
	void te024(){
		Object input=readResourceAsJSON("/toRdf/e024-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e024-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e024-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0025
	*/
	@Test
	@DisplayName("Problematic IRI expansion tests")
	void te025(){
		Object input=readResourceAsJSON("/toRdf/e025-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e025-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e025-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0026
	*/
	@Test
	@DisplayName("Expanding term mapping to @type uses @type syntax")
	void te026(){
		Object input=readResourceAsJSON("/toRdf/e026-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e026-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e026-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0027
	*/
	@Test
	@DisplayName("Keep duplicate values in @list and @set")
	void te027(){
		Object input=readResourceAsJSON("/toRdf/e027-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e027-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e027-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0028
	*/
	@Test
	@DisplayName("Use @vocab in properties and @type but not in @id")
	void te028(){
		Object input=readResourceAsJSON("/toRdf/e028-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e028-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e028-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0029
	*/
	@Test
	@DisplayName("Relative IRIs")
	void te029(){
		Object input=readResourceAsJSON("/toRdf/e029-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e029-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e029-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0030
	*/
	@Test
	@DisplayName("Language maps")
	void te030(){
		Object input=readResourceAsJSON("/toRdf/e030-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e030-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e030-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0031
	*/
	@Test
	@DisplayName("type-coercion of native types")
	void te031(){
		Object input=readResourceAsJSON("/toRdf/e031-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e031-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e031-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0032
	*/
	@Test
	@DisplayName("Mapping a term to null decouples it from @vocab")
	void te032(){
		Object input=readResourceAsJSON("/toRdf/e032-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e032-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e032-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0033
	*/
	@Test
	@DisplayName("Using @vocab with with type-coercion")
	void te033(){
		Object input=readResourceAsJSON("/toRdf/e033-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e033-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e033-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0034
	*/
	@Test
	@DisplayName("Multiple properties expanding to the same IRI")
	void te034(){
		Object input=readResourceAsJSON("/toRdf/e034-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e034-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e034-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0035
	*/
	@Test
	@DisplayName("Language maps with @vocab, default language, and colliding property")
	void te035(){
		Object input=readResourceAsJSON("/toRdf/e035-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e035-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e035-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0036
	*/
	@Test
	@DisplayName("Expanding @index")
	void te036(){
		Object input=readResourceAsJSON("/toRdf/e036-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e036-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e036-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0037
	*/
	@Test
	@DisplayName("Expanding @reverse")
	void te037(){
		Object input=readResourceAsJSON("/toRdf/e037-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e037-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e037-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Triples with blank node predicates are dropped by default (from expand-0038).
	*/
	@Test
	@DisplayName("Drop blank node predicates by default")
	void te038(){
		Object input=readResourceAsJSON("/toRdf/e038-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e038-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e038-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0039
	*/
	@Test
	@DisplayName("Using terms in a reverse-maps")
	void te039(){
		Object input=readResourceAsJSON("/toRdf/e039-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e039-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e039-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0040
	*/
	@Test
	@DisplayName("language and index expansion on non-objects")
	void te040(){
		Object input=readResourceAsJSON("/toRdf/e040-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e040-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e040-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0041
	*/
	@Test
	@DisplayName("Reset the default language")
	void te041(){
		Object input=readResourceAsJSON("/toRdf/e041-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e041-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e041-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0042
	*/
	@Test
	@DisplayName("Expanding reverse properties")
	void te042(){
		Object input=readResourceAsJSON("/toRdf/e042-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e042-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e042-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0043
	*/
	@Test
	@DisplayName("Using reverse properties inside a @reverse-container")
	void te043(){
		Object input=readResourceAsJSON("/toRdf/e043-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e043-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e043-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0044
	*/
	@Test
	@DisplayName("Ensure index maps use language mapping")
	void te044(){
		Object input=readResourceAsJSON("/toRdf/e044-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e044-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e044-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0045
	*/
	@Test
	@DisplayName("Top-level value objects are removed")
	void te045(){
		Object input=readResourceAsJSON("/toRdf/e045-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e045-in.jsonld"));
		assertEquals(0, result.size());
	}

	/***
	* RDF version of expand-0046
	*/
	@Test
	@DisplayName("Free-floating nodes are removed")
	void te046(){
		Object input=readResourceAsJSON("/toRdf/e046-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e046-in.jsonld"));
		assertEquals(0, result.size());
	}

	/***
	* RDF version of expand-0047
	*/
	@Test
	@DisplayName("Remove free-floating set values and lists")
	void te047(){
		Object input=readResourceAsJSON("/toRdf/e047-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e047-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e047-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0048
	*/
	@Test
	@DisplayName("Terms are ignored in @id")
	void te048(){
		Object input=readResourceAsJSON("/toRdf/e048-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e048-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e048-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0049
	*/
	@Test
	@DisplayName("Using strings as value of a reverse property")
	void te049(){
		Object input=readResourceAsJSON("/toRdf/e049-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e049-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e049-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0050
	*/
	@Test
	@DisplayName("Term definitions with prefix separate from prefix definitions")
	void te050(){
		Object input=readResourceAsJSON("/toRdf/e050-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e050-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e050-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Verifies that an exception is raised in Expansion when an invalid IRI is used for @reverse.
	*/
	@Test
	@DisplayName("Invalid reverse id")
	void tee50(){
		assertThrows(JLDException.class, ()->{
			Object input=readResourceAsJSON("/toRdf/ee50-in.jsonld");
			JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/ee50-in.jsonld"));
		}, "invalid IRI mapping");
	}

	/***
	* RDF version of expand-0051
	*/
	@Test
	@DisplayName("Expansion of keyword aliases in term definitions")
	void te051(){
		Object input=readResourceAsJSON("/toRdf/e051-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e051-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e051-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0052
	*/
	@Test
	@DisplayName("@vocab-relative IRIs in term definitions")
	void te052(){
		Object input=readResourceAsJSON("/toRdf/e052-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e052-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e052-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0053
	*/
	@Test
	@DisplayName("Expand absolute IRI with @type: @vocab")
	void te053(){
		Object input=readResourceAsJSON("/toRdf/e053-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e053-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e053-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0054
	*/
	@Test
	@DisplayName("Expand term with @type: @vocab")
	void te054(){
		Object input=readResourceAsJSON("/toRdf/e054-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e054-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e054-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0055
	*/
	@Test
	@DisplayName("Expand @vocab-relative term with @type: @vocab")
	void te055(){
		Object input=readResourceAsJSON("/toRdf/e055-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e055-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e055-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0056
	*/
	@Test
	@DisplayName("Use terms with @type: @vocab but not with @type: @id")
	void te056(){
		Object input=readResourceAsJSON("/toRdf/e056-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e056-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e056-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0057
	*/
	@Test
	@DisplayName("Expand relative IRI with @type: @vocab")
	void te057(){
		Object input=readResourceAsJSON("/toRdf/e057-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e057-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e057-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0058
	*/
	@Test
	@DisplayName("Expand compact IRI with @type: @vocab")
	void te058(){
		Object input=readResourceAsJSON("/toRdf/e058-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e058-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e058-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0059
	*/
	@Test
	@DisplayName("Reset @vocab by setting it to null")
	void te059(){
		Object input=readResourceAsJSON("/toRdf/e059-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e059-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e059-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0060
	*/
	@Test
	@DisplayName("Overwrite document base with @base and reset it again")
	void te060(){
		Object input=readResourceAsJSON("/toRdf/e060-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e060-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e060-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0061
	*/
	@Test
	@DisplayName("Coercing native types to arbitrary datatypes")
	void te061(){
		Object input=readResourceAsJSON("/toRdf/e061-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e061-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e061-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0062
	*/
	@Test
	@DisplayName("Various relative IRIs with with @base")
	void te062(){
		Object input=readResourceAsJSON("/toRdf/e062-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e062-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e062-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0063
	*/
	@Test
	@DisplayName("Expand a reverse property with an index-container")
	void te063(){
		Object input=readResourceAsJSON("/toRdf/e063-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e063-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e063-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0064
	*/
	@Test
	@DisplayName("Expand reverse property whose values are unlabeled blank nodes")
	void te064(){
		Object input=readResourceAsJSON("/toRdf/e064-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e064-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e064-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0065
	*/
	@Test
	@DisplayName("Keys that are not mapped to an IRI in a reverse-map are dropped")
	void te065(){
		Object input=readResourceAsJSON("/toRdf/e065-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e065-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e065-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0066
	*/
	@Test
	@DisplayName("Use @vocab to expand keys in reverse-maps")
	void te066(){
		Object input=readResourceAsJSON("/toRdf/e066-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e066-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e066-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0067
	*/
	@Test
	@DisplayName("prefix:://sufffix not a compact IRI")
	void te067(){
		Object input=readResourceAsJSON("/toRdf/e067-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e067-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e067-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0068
	*/
	@Test
	@DisplayName("_::sufffix not a compact IRI")
	void te068(){
		Object input=readResourceAsJSON("/toRdf/e068-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e068-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e068-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0069
	*/
	@Test
	@DisplayName("Compact IRI as term with type mapping")
	void te069(){
		Object input=readResourceAsJSON("/toRdf/e069-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e069-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e069-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0070
	*/
	@Test
	@DisplayName("Redefine compact IRI with itself")
	void te070(){
		Object input=readResourceAsJSON("/toRdf/e070-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e070-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e070-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0071
	*/
	@Test
	@DisplayName("Redefine terms looking like compact IRIs")
	void te071(){
		Object input=readResourceAsJSON("/toRdf/e071-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e071-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e071-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* RDF version of expand-0072
	*/
	@Test
	@DisplayName("Redefine term using @vocab, not itself")
	void te072(){
		Object input=readResourceAsJSON("/toRdf/e072-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e072-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e072-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Objects are unordered, so serialized node definition containing @context may have @context at the end of the node definition
	*/
	@Test
	@DisplayName("@context not first property")
	void te073(){
		Object input=readResourceAsJSON("/toRdf/e073-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e073-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e073-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Objects are unordered, so serialized node definition containing @id may have @id at the end of the node definition
	*/
	@Test
	@DisplayName("@id not first property")
	void te074(){
		Object input=readResourceAsJSON("/toRdf/e074-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e074-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e074-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use @vocab to map all properties to blank node identifiers
	*/
	@Test
	@DisplayName("@vocab as blank node identifier")
	void te075(){
		Object input=readResourceAsJSON("/toRdf/e075-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e075-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e075-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use of the base option overrides the document location
	*/
	@Test
	@DisplayName("base option overrides document location")
	void te076(){
		Object input=readResourceAsJSON("/toRdf/e076-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("http://example/base/"));
		List<String> expect=readResourceAsLines("/toRdf/e076-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use of multiple reverse properties
	*/
	@Test
	@DisplayName("multiple reverse properties")
	void te078(){
		Object input=readResourceAsJSON("/toRdf/e078-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e078-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e078-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Value Expansion does not expand native values, such as booleans, to a node object
	*/
	@Test
	@DisplayName("Do not expand native values to IRIs")
	void te088(){
		Object input=readResourceAsJSON("/toRdf/e088-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e088-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e088-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use of an empty @base is applied to the base option
	*/
	@Test
	@DisplayName("empty @base applied to the base option")
	void te089(){
		Object input=readResourceAsJSON("/toRdf/e089-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("http://example/base/"));
		List<String> expect=readResourceAsLines("/toRdf/e089-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use of a relative @base overrides base option and document location
	*/
	@Test
	@DisplayName("relative @base overrides base option and document location")
	void te090(){
		Object input=readResourceAsJSON("/toRdf/e090-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("http://example/base/"));
		List<String> expect=readResourceAsLines("/toRdf/e090-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Use of a relative and absolute @base overrides base option and document location
	*/
	@Test
	@DisplayName("relative and absolute @base overrides base option and document location")
	void te091(){
		Object input=readResourceAsJSON("/toRdf/e091-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("http://example/base/"));
		List<String> expect=readResourceAsLines("/toRdf/e091-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Do not treat as absolute IRIs values that look like compact IRIs if they're not absolute
	*/
	@Test
	@DisplayName("IRI expansion of fragments including ':'")
	void te109(){
		Object input=readResourceAsJSON("/toRdf/e109-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e109-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e109-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Expand with context including JavaScript Object property names
	*/
	@Test
	@DisplayName("context with JavaScript Object property names")
	void te113(){
		Object input=readResourceAsJSON("/toRdf/e113-in.jsonld");
		ArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e113-in.jsonld"));
		List<String> expect=readResourceAsLines("/toRdf/e113-out.nq");
		assertEquals(expect.size(), result.size());
		for(RDFTriple t:result) assertTrue(expect.contains(t.toString()));
	}

	/***
	* Pathological relative property IRIs in 1.0
	*/
	@Test
	@DisplayName("Verifies that relative IRIs as properties with @vocab: '' in 1.0 generate an error")
	void te115(){
		assertThrows(JLDException.class, ()->{
			Object input=readResourceAsJSON("/toRdf/e115-in.jsonld");
			JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e115-in.jsonld"));
		}, "invalid vocab mapping");
	}

	/***
	* Pathological relative property IRIs in 1.0
	*/
	@Test
	@DisplayName("Verifies that relative IRIs as properties with relative @vocab in 1.0 generate an error")
	void te116(){
		assertThrows(JLDException.class, ()->{
			Object input=readResourceAsJSON("/toRdf/e116-in.jsonld");
			JLDProcessor.toRDF(input, URI.create("https://w3c.github.io/json-ld-api/tests/toRdf/e116-in.jsonld"));
		}, "invalid vocab mapping");
	}

}