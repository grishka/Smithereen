package smithereen.jsonld;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import org.json.*;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.jsonld.TestUtils.*;

class URDNA2015Tests{

	@Test
	@DisplayName("simple id")
	void test001(){
		List<String> input=readResourceAsLines("/urdna2015/test001-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test001-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("duplicate property iri values")
	void test002(){
		List<String> input=readResourceAsLines("/urdna2015/test002-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test002-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("bnode")
	void test003(){
		List<String> input=readResourceAsLines("/urdna2015/test003-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test003-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("bnode plus embed w/subject")
	void test004(){
		List<String> input=readResourceAsLines("/urdna2015/test004-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test004-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("bnode embed")
	void test005(){
		List<String> input=readResourceAsLines("/urdna2015/test005-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test005-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("multiple rdf types")
	void test006(){
		List<String> input=readResourceAsLines("/urdna2015/test006-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test006-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("coerce CURIE value")
	void test007(){
		List<String> input=readResourceAsLines("/urdna2015/test007-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test007-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("single subject complex")
	void test008(){
		List<String> input=readResourceAsLines("/urdna2015/test008-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test008-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("multiple subjects - complex")
	void test009(){
		List<String> input=readResourceAsLines("/urdna2015/test009-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test009-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("type")
	void test010(){
		List<String> input=readResourceAsLines("/urdna2015/test010-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test010-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("type-coerced type")
	void test011(){
		List<String> input=readResourceAsLines("/urdna2015/test011-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test011-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("type-coerced type, remove duplicate reference")
	void test012(){
		List<String> input=readResourceAsLines("/urdna2015/test012-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test012-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("type-coerced type, cycle")
	void test013(){
		List<String> input=readResourceAsLines("/urdna2015/test013-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test013-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("check types")
	void test014(){
		List<String> input=readResourceAsLines("/urdna2015/test014-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test014-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("top level context")
	void test015(){
		List<String> input=readResourceAsLines("/urdna2015/test015-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test015-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - dual link - embed")
	void test016(){
		List<String> input=readResourceAsLines("/urdna2015/test016-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test016-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - dual link - non-embed")
	void test017(){
		List<String> input=readResourceAsLines("/urdna2015/test017-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test017-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - self link")
	void test018(){
		List<String> input=readResourceAsLines("/urdna2015/test018-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test018-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - disjoint self links")
	void test019(){
		List<String> input=readResourceAsLines("/urdna2015/test019-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test019-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - diamond")
	void test020(){
		List<String> input=readResourceAsLines("/urdna2015/test020-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test020-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - circle of 2")
	void test021(){
		List<String> input=readResourceAsLines("/urdna2015/test021-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test021-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 2")
	void test022(){
		List<String> input=readResourceAsLines("/urdna2015/test022-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test022-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - circle of 3")
	void test023(){
		List<String> input=readResourceAsLines("/urdna2015/test023-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test023-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (1-2-3)")
	void test024(){
		List<String> input=readResourceAsLines("/urdna2015/test024-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test024-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (1-3-2)")
	void test025(){
		List<String> input=readResourceAsLines("/urdna2015/test025-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test025-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (2-1-3)")
	void test026(){
		List<String> input=readResourceAsLines("/urdna2015/test026-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test026-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (2-3-1)")
	void test027(){
		List<String> input=readResourceAsLines("/urdna2015/test027-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test027-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (3-2-1)")
	void test028(){
		List<String> input=readResourceAsLines("/urdna2015/test028-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test028-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - double circle of 3 (3-1-2)")
	void test029(){
		List<String> input=readResourceAsLines("/urdna2015/test029-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test029-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("blank node - point at circle of 3")
	void test030(){
		List<String> input=readResourceAsLines("/urdna2015/test030-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test030-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("bnode (1)")
	void test031(){
		List<String> input=readResourceAsLines("/urdna2015/test031-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test031-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("bnode (2)")
	void test032(){
		List<String> input=readResourceAsLines("/urdna2015/test032-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test032-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("disjoint identical subgraphs (1)")
	void test033(){
		List<String> input=readResourceAsLines("/urdna2015/test033-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test033-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("disjoint identical subgraphs (2)")
	void test034(){
		List<String> input=readResourceAsLines("/urdna2015/test034-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test034-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered w/strings (1)")
	void test035(){
		List<String> input=readResourceAsLines("/urdna2015/test035-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test035-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered w/strings (2)")
	void test036(){
		List<String> input=readResourceAsLines("/urdna2015/test036-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test036-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered w/strings (3)")
	void test037(){
		List<String> input=readResourceAsLines("/urdna2015/test037-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test037-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered 4 bnodes, reordered 2 properties (1)")
	void test038(){
		List<String> input=readResourceAsLines("/urdna2015/test038-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test038-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered 4 bnodes, reordered 2 properties (2)")
	void test039(){
		List<String> input=readResourceAsLines("/urdna2015/test039-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test039-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered 6 bnodes (1)")
	void test040(){
		List<String> input=readResourceAsLines("/urdna2015/test040-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test040-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered 6 bnodes (2)")
	void test041(){
		List<String> input=readResourceAsLines("/urdna2015/test041-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test041-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("reordered 6 bnodes (3)")
	void test042(){
		List<String> input=readResourceAsLines("/urdna2015/test042-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test042-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("literal with language")
	void test043(){
		List<String> input=readResourceAsLines("/urdna2015/test043-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test043-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("evil (1)")
	void test044(){
		List<String> input=readResourceAsLines("/urdna2015/test044-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test044-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("evil (2)")
	void test045(){
		List<String> input=readResourceAsLines("/urdna2015/test045-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test045-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("evil (3)")
	void test046(){
		List<String> input=readResourceAsLines("/urdna2015/test046-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test046-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("deep diff (1)")
	void test047(){
		List<String> input=readResourceAsLines("/urdna2015/test047-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test047-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("deep diff (2)")
	void test048(){
		List<String> input=readResourceAsLines("/urdna2015/test048-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test048-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("remove null")
	void test049(){
		List<String> input=readResourceAsLines("/urdna2015/test049-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test049-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("nulls")
	void test050(){
		List<String> input=readResourceAsLines("/urdna2015/test050-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test050-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("merging subjects")
	void test051(){
		List<String> input=readResourceAsLines("/urdna2015/test051-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test051-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("alias keywords")
	void test052(){
		List<String> input=readResourceAsLines("/urdna2015/test052-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test052-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("@list")
	void test053(){
		List<String> input=readResourceAsLines("/urdna2015/test053-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test053-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("t-graph")
	void test054(){
		List<String> input=readResourceAsLines("/urdna2015/test054-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test054-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("simple reorder (1)")
	void test055(){
		List<String> input=readResourceAsLines("/urdna2015/test055-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test055-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("simple reorder (2)")
	void test056(){
		List<String> input=readResourceAsLines("/urdna2015/test056-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test056-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("unnamed graph")
	void test057(){
		List<String> input=readResourceAsLines("/urdna2015/test057-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test057-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("unnamed graph with blank node objects")
	void test058(){
		List<String> input=readResourceAsLines("/urdna2015/test058-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test058-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("n-quads parsing")
	void test059(){
		List<String> input=readResourceAsLines("/urdna2015/test059-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test059-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("n-quads escaping")
	void test060(){
		List<String> input=readResourceAsLines("/urdna2015/test060-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test060-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("same literal value with multiple languages")
	void test061(){
		List<String> input=readResourceAsLines("/urdna2015/test061-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test061-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

	@Test
	@DisplayName("same literal value with multiple datatypes")
	void test062(){
		List<String> input=readResourceAsLines("/urdna2015/test062-in.nq");
		List<RDFTriple> result=URDNA2015.normalize(parseRDF(input));
		List<String> expect=readResourceAsLines("/urdna2015/test062-urdna2015.nq");
		List<String> strResult=eachToString(result);
		Collections.sort(strResult);
		assertLinesMatch(expect, strResult);
	}

}