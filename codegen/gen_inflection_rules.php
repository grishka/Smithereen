<?php

$rules=json_decode(file_get_contents("inflect_ru.json"));

$j=[
	"package smithereen.lang;",
	"",
	"import smithereen.data.User;",
	"",
	"// Auto-generated, don't edit",
	"// Сгенерировано из https://github.com/petrovich/petrovich-rules, не редактируйте",
	"// Скрипт: /codegen/gen_inflection_rules.php",
	"class RussianInflectionRulesGenerated{",
	""
];

function generateCases($cases){
	global $j;
	$caseNames=["GENITIVE", "DATIVE", "ACCUSATIVE", "INSTRUMENTAL", "PREPOSITIONAL"];
	if(implode("", $cases)=="....."){
		$j[]="\t\t\treturn input;";
		return;
	}
	$j[]="\t\t\tswitch(_case){";
	for($i=0;$i<count($cases);$i++){
		$j[]="\t\t\t\tcase {$caseNames[$i]}:";
		$c=$cases[$i];
		if($c=="."){
			$j[]="\t\t\t\t\treturn input;";
			continue;
		}else if($c{0}=="-"){
			$toRemove=0;
			for(;$c{$toRemove}=="-";$toRemove++){}
			$j[]="\t\t\t\t\treturn input.substring(0, input.length()-$toRemove)+\"".str_replace("-", "", $c)."\";";
		}else{
			$j[]="\t\t\t\t\treturn input+\"$c\";";
		}
	}
	$j[]="\t\t\t}";
}

function generate($key, $method){
	global $j, $rules;
	$all=[];
	if(isset($rules->{$key}->exceptions)){
		$all['e']=$rules->{$key}->exceptions;
	}
	$all['r']=$rules->{$key}->suffixes;
	$j[]="\tpublic static String $method(String input, User.Gender gender, Inflector.Case _case, boolean firstWord){";
	$j[]="\t\tString inputLower=input.toLowerCase();";
	foreach($all as $gk=>$group){
		foreach ($group as $ex) {
			$and=[];
			$or=[];
			if(isset($ex->tags) && array_search("first_word", $ex->tags)!==FALSE){
				$and[]="firstWord";
			}
			if($ex->gender!="androgynous"){
				$and[]="gender==User.Gender.".strtoupper($ex->gender);
			}
			foreach($ex->test as $tst){
				$or[]="inputLower.".($gk=="e" ? "equals" : "endsWith")."(\"$tst\")";
			}
			$ifstr="\t\tif(";
			if(count($and)){
				$ifstr.=implode(" && ", $and)." && (";
			}
			$ifstr.=implode(" || ", $or);
			if(count($and)){
				$ifstr.=")";
			}
			$ifstr.="){";
			$j[]=$ifstr;
			generateCases($ex->mods);
			$j[]="\t\t}";
		}
	}
	$j[]="\t\treturn input;";
	$j[]="\t}";
	$j[]="";
}

function generateGenderRules($key, $method){
	global $j, $rules;
	$j[]="\tpublic static User.Gender $method(String input){";
	$j[]="\t\tinput=input.toLowerCase();";
	$exceptions=$rules->{$key}->exceptions;
	$suffixes=$rules->{$key}->suffixes;
	if(isset($exceptions)){
		foreach($exceptions as $gender=>$list){
			$if=[];
			foreach($list as $rule){
				$if[]="input.equals(\"$rule\")";
			}
			$j[]="\t\tif(".implode(" || ", $if).")";
			$j[]="\t\t\treturn User.Gender.".strtoupper(str_replace("androgynous", "unknown", $gender)).";";
			$j[]="";
		}
	}
	foreach($suffixes as $gender=>$list){
		$if=[];
		foreach($list as $rule){
			$if[]="input.endsWith(\"$rule\")";
		}
		$j[]="\t\tif(".implode(" || ", $if).")";
		$j[]="\t\t\treturn User.Gender.".strtoupper(str_replace("androgynous", "unknown", $gender)).";";
		$j[]="";
	}
	$j[]="\t\treturn User.Gender.UNKNOWN;";
	$j[]="\t}";
	$j[]="";
}

generate("lastname", "inflectLastName");
generate("firstname", "inflectFirstName");
generate("middlename", "inflectMiddleName");

$rules=json_decode(file_get_contents("gender_ru.json"))->gender;
generateGenderRules("lastname", "genderForLastName");
generateGenderRules("firstname", "genderForFirstName");
generateGenderRules("middlename", "genderForMiddleName");

$j[]="}";

file_put_contents("../src/main/java/smithereen/lang/RussianInflectionRulesGenerated.java", implode("\n", $j));