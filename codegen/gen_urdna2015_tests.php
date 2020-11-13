<?php

$mf=json_decode(file_get_contents("https://json-ld.github.io/normalization/tests/manifest-urdna2015.jsonld"));
$j=[
"package smithereen.jsonld;",
"",
"import org.junit.jupiter.api.DisplayName;",
"import org.junit.jupiter.api.Test;",
"",
"import java.util.*;",
"",
"import org.json.*;",
"",
"import static org.junit.jupiter.api.Assertions.*;",
"import static smithereen.jsonld.TestUtils.*;",
"",
"class URDNA2015Tests{",
"",
];

foreach($mf->entries as $test){
	$j[]="\t@Test";
	$j[]="\t@DisplayName(\"".str_replace('"', '\"', $test->name)."\")";
	list(,$testID)=explode("#", $test->id);
	$j[]="\tvoid $testID(){";
	if(!file_exists("src/test/resources/urdna2015/".$test->action)){
		file_put_contents("src/test/resources/urdna2015/".$test->action, file_get_contents("https://json-ld.github.io/normalization/tests/".$test->action));
	}
	if(!file_exists("src/test/resources/urdna2015/".$test->result)){
		file_put_contents("src/test/resources/urdna2015/".$test->result, file_get_contents("https://json-ld.github.io/normalization/tests/".$test->result));
	}
	$j[]="\t\tList<String> input=readResourceAsLines(\"/urdna2015/{$test->action}\");";
	$j[]="\t\tList<RDFTriple> result=URDNA2015.normalize(parseRDF(input));";
	$j[]="\t\tList<String> expect=readResourceAsLines(\"/urdna2015/{$test->result}\");";
	$j[]="\t\tList<String> strResult=eachToString(result);";
	$j[]="\t\tCollections.sort(strResult);";
	$j[]="\t\tassertLinesMatch(expect, strResult);";
	$j[]="\t}";
	$j[]="";
}

$j[]="}";

file_put_contents("../src/test/java/smithereen/jsonld/URDNA2015Tests.java", implode("\n", $j));