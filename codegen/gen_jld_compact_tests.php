<?php

$mf=json_decode(file_get_contents("https://w3c.github.io/json-ld-api/tests/compact-manifest.jsonld"));
$j=[
"package smithereen.jsonld;",
"",
"import org.junit.jupiter.api.DisplayName;",
"import org.junit.jupiter.api.Test;",
"",
"import java.io.*;",
"import java.nio.charset.StandardCharsets;",
"import java.net.URI;",
"import java.util.Collections;",
"",
"import org.json.*;",
"",
"import static org.junit.jupiter.api.Assertions.*;",
"import static smithereen.jsonld.TestUtils.*;",
"",
"class CompactTests{",
"",
];

foreach($mf->sequence as $test){
	if(isset($test->option) && isset($test->option->specVersion) && $test->option->specVersion!="json-ld-1.0"){
		echo "Skipping test {$test->name} because of incompatible spec version\n";
		continue;
	}
	$j[]="\t/***\n\t* {$test->purpose}\n\t*/";
	$j[]="\t@Test";
	$j[]="\t@DisplayName(\"".str_replace('"', '\"', $test->name)."\")";
	$j[]="\tvoid ".substr($test->{"@id"}, 1)."(){";
	$type=$test->{"@type"}[0];
	if(!file_exists("src/test/resources/".$test->input)){
		file_put_contents("src/test/resources/".$test->input, file_get_contents("https://w3c.github.io/json-ld-api/tests/".$test->input));
	}
	if(isset($test->expect) && !file_exists("src/test/resources/".$test->expect)){
		file_put_contents("src/test/resources/".$test->expect, file_get_contents("https://w3c.github.io/json-ld-api/tests/".$test->expect));
	}
	if(isset($test->expect) && !file_exists("src/test/resources/".$test->context)){
		file_put_contents("src/test/resources/".$test->context, file_get_contents("https://w3c.github.io/json-ld-api/tests/".$test->context));
	}
	if($type=="jld:PositiveEvaluationTest"){
		$j[]="\t\tJSONArray input=JLDProcessor.expandToArray(readResourceAsJSON(\"/{$test->input}\"));";
		$j[]="\t\tObject expect=readResourceAsJSON(\"/{$test->expect}\");";
		$j[]="\t\tJSONObject context=(JSONObject)readResourceAsJSON(\"/{$test->context}\");";
		if(isset($test->option->compactArrays) && !$test->option->compactArrays)
			$j[]="\t\tJSONObject compacted=JLDProcessor.compact(input, context.get(\"@context\"), false);";
		else
			$j[]="\t\tJSONObject compacted=JLDProcessor.compact(input, context.get(\"@context\"));";
		$j[]="\t\tassertEqualJLD(expect, compacted);";
	}else if($type=="jld:NegativeEvaluationTest"){
		$j[]="\t\tassertThrows(JLDException.class, ()->{";
		$j[]="\t\t\tJSONArray input=JLDProcessor.expandToArray(readResourceAsJSON(\"/{$test->input}\"));";
		$j[]="\t\t\tJSONObject context=(JSONObject)readResourceAsJSON(\"/{$test->context}\");";
		$j[]="\t\t\tJLDProcessor.compact(input, context.get(\"@context\"));";
		$j[]="\t\t}, \"{$test->expectErrorCode}\");";
	}else{
		die("Unknown test type $type\n");
	}
	$j[]="\t}";
	$j[]="";
}

$j[]="}";

file_put_contents("../src/test/java/smithereen/jsonld/CompactTests.java", implode("\n", $j));