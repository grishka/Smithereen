<?php

$mf=json_decode(file_get_contents("https://w3c.github.io/json-ld-api/tests/toRdf-manifest.jsonld"));
$j=[
"package smithereen.jsonld;",
"",
"import org.junit.jupiter.api.DisplayName;",
"import org.junit.jupiter.api.Test;",
"",
"import java.net.URI;",
"import java.util.*;",
"import java.util.stream.Collectors;",
"",
"import com.google.gson.*;",
"",
"import static org.junit.jupiter.api.Assertions.*;",
"import static smithereen.jsonld.TestUtils.*;",
"",
"class ToRDFTests{",
"",
];

foreach($mf->sequence as $test){
	if(isset($test->option) && isset($test->option->specVersion) && $test->option->specVersion!="json-ld-1.0"){
		echo "Skipping test {$test->name} because of incompatible spec version\n";
		continue;
	}
	$type=$test->{"@type"}[0];
	if($type=="jld:PositiveSyntaxTest"){
		// skip
		continue;
	}
	if($test->{"@id"}=="#te077") continue; // expandContext isn't a thing in real life
	if(substr($test->{"@id"}, 0, 4)=="#ter") continue;
	$j[]="\t/***\n\t* {$test->purpose}\n\t*/";
	$j[]="\t@Test";
	$j[]="\t@DisplayName(\"".str_replace('"', '\"', $test->name)."\")";
	$j[]="\tvoid ".substr($test->{"@id"}, 1)."(){";
	$inputURL="https://w3c.github.io/json-ld-api/tests/".$test->input;
	if(!file_exists("src/test/resources/".$test->input)){
		file_put_contents("src/test/resources/".$test->input, file_get_contents($inputURL));
	}
	if(isset($test->expect) && !file_exists("src/test/resources/".$test->expect)){
		file_put_contents("src/test/resources/".$test->expect, file_get_contents("https://w3c.github.io/json-ld-api/tests/".$test->expect));
	}
	if(isset($test->option) && isset($test->option->base))
		$inputURL=$test->option->base;
	if($type=="jld:PositiveEvaluationTest"){
		$j[]="\t\tJsonElement input=readResourceAsJSON(\"/{$test->input}\");";
		$j[]="\t\tArrayList<RDFTriple> result=JLDProcessor.toRDF(input, URI.create(\"$inputURL\"));";
		if(filesize("src/test/resources/".$test->expect)>0){
			$j[]="\t\tList<String> expect=readResourceAsLines(\"/{$test->expect}\");";
			$j[]="\t\tCollections.sort(expect);";
			$j[]="\t\tassertLinesMatch(expect, result.stream().map(Object::toString).sorted().collect(Collectors.toList()));";
		}else{
			$j[]="\t\tassertLinesMatch(Collections.emptyList(), result.stream().map(Object::toString).sorted().collect(Collectors.toList()));";
		}
	}else if($type=="jld:NegativeEvaluationTest"){
		$j[]="\t\tassertThrows(JLDException.class, ()->{";
		$j[]="\t\t\tJsonElement input=readResourceAsJSON(\"/{$test->input}\");";
		$j[]="\t\t\tJLDProcessor.toRDF(input, URI.create(\"$inputURL\"));";
		$j[]="\t\t}, \"{$test->expectErrorCode}\");";
	}else{
		die("Unknown test type $type\n");
	}
	$j[]="\t}";
	$j[]="";
}

$j[]="}";

file_put_contents("src/test/java/smithereen/jsonld/ToRDFTests.java", implode("\n", $j));