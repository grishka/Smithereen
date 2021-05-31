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
"import com.google.gson.*;",
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
	$id=$test->{"@id"};
	if($id=="#t0113" || $id=="#t0112"){ // no one uses @index in real life anyway
		continue;
	}
	if($id=="#t0095" || $id=="#t0107"){ // these are probably meant for 1.1
		continue;
	}
	$j[]="\t/***\n\t* {$test->purpose}\n\t*/";
	$j[]="\t@Test";
	$j[]="\t@DisplayName(\"".str_replace('"', '\"', $test->name)."\")";
	$j[]="\tvoid ".substr($test->{"@id"}, 1)."(){";
	$type=$test->{"@type"}[0];
	$baseURL="https://w3c.github.io/json-ld-api/tests/";
	if(!file_exists("src/test/resources/".$test->input)){
		file_put_contents("src/test/resources/".$test->input, file_get_contents($baseURL.$test->input));
	}
	if(isset($test->expect) && !file_exists("src/test/resources/".$test->expect)){
		file_put_contents("src/test/resources/".$test->expect, file_get_contents($baseURL.$test->expect));
	}
	if(isset($test->expect) && !file_exists("src/test/resources/".$test->context)){
		file_put_contents("src/test/resources/".$test->context, file_get_contents($baseURL.$test->context));
	}
	if($type=="jld:PositiveEvaluationTest"){
		$j[]="\t\tJsonArray input=JLDProcessor.expandToArray(readResourceAsJSON(\"/{$test->input}\"), URI.create(\"{$baseURL}{$test->input}\"));";
		$j[]="\t\tJsonElement expect=readResourceAsJSON(\"/{$test->expect}\");";
		$j[]="\t\tJsonObject context=readResourceAsJSON(\"/{$test->context}\").getAsJsonObject();";
		if(isset($test->option->compactArrays) && !$test->option->compactArrays)
			$j[]="\t\tJsonObject compacted=JLDProcessor.compact(input, context.get(\"@context\"), false, URI.create(\"{$baseURL}{$test->input}\"));";
		else
			$j[]="\t\tJsonObject compacted=JLDProcessor.compact(input, context.get(\"@context\"), true, URI.create(\"{$baseURL}{$test->input}\"));";
		$j[]="\t\tassertEquals(expect, compacted);";
	}else if($type=="jld:NegativeEvaluationTest"){
		$j[]="\t\tassertThrows(JLDException.class, ()->{";
		$j[]="\t\t\tJsonArray input=JLDProcessor.expandToArray(readResourceAsJSON(\"/{$test->input}\"), URI.create(\"{$baseURL}{$test->input}\"));";
		$j[]="\t\t\tJsonObject context=readResourceAsJSON(\"/{$test->context}\").getAsJsonObject();";
		$j[]="\t\t\tJLDProcessor.compact(input, context.get(\"@context\"));";
		$j[]="\t\t}, \"{$test->expectErrorCode}\");";
	}else{
		die("Unknown test type $type\n");
	}
	$j[]="\t}";
	$j[]="";
}

$j[]="}";

file_put_contents("src/test/java/smithereen/jsonld/CompactTests.java", implode("\n", $j));