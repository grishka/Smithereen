package smithereen;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import smithereen.http.StructuredHeadersTestUtils;
import smithereen.http.StructuredHttpHeaders;

public class GenerateStructuredHeadersTests{
	private static Gson gson=new GsonBuilder()
			.disableHtmlEscaping()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.create();

	public static void main(String[] args) throws IOException{
		File[] files=new File("codegen/structured-field-tests").listFiles();
		for(File f:files){
			if(!f.getName().endsWith(".json"))
				continue;
			String name=f.getName();
			String className=Arrays.stream(name.substring(0, name.length()-5).split("-")).map(s->s.substring(0, 1).toUpperCase()+s.substring(1)).collect(Collectors.joining())+"Tests";
			try(FileReader reader=new FileReader(f)){
				List<TestCase> testCases=gson.fromJson(reader, new TypeToken<>(){});
				TypeSpec.Builder parseClassBuilder=TypeSpec.classBuilder("Parse"+className)
						.addModifiers(Modifier.PUBLIC);
				TypeSpec.Builder serializeClassBuilder=TypeSpec.classBuilder("Serialize"+className)
						.addModifiers(Modifier.PUBLIC);

				int i=0;
				for(TestCase test:testCases){
					MethodSpec.Builder methodBuilder=MethodSpec.methodBuilder("test"+i)
							.addModifiers(Modifier.PUBLIC)
							.addAnnotation(Test.class)
							.addAnnotation(AnnotationSpec.builder(DisplayName.class).addMember("value", "$S", test.name).build());

					if(test.mustFail){
						methodBuilder.beginControlFlow("$T.assertThrows($T.class, ()->", Assertions.class, IllegalArgumentException.class);
					}

					switch(test.headerType){
						case "item" -> methodBuilder.addStatement("$T result=$T.parseItem($S)", StructuredHttpHeaders.Item.class, StructuredHttpHeaders.class, String.join(",", test.raw));
						case "list" -> methodBuilder.addStatement("$T result=$T.parseList($S)", ParameterizedTypeName.get(List.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHttpHeaders.class, String.join(",", test.raw));
						case "dictionary" -> methodBuilder.addStatement("$T result=$T.parseDictionary($S)", ParameterizedTypeName.get(Map.class, String.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHttpHeaders.class, String.join(",", test.raw));
					}

					if(test.mustFail){
						methodBuilder.endControlFlow(")");
					}else{
						methodBuilder.addStatement("$T.assertEquals($T.parseString($S), $T.toJson(result))", Assertions.class, JsonParser.class, test.expected.toString(), StructuredHeadersTestUtils.class);
					}

					parseClassBuilder.addMethod(methodBuilder.build());

					if(!test.mustFail){
						methodBuilder=MethodSpec.methodBuilder("test"+i)
								.addModifiers(Modifier.PUBLIC)
								.addAnnotation(Test.class)
								.addAnnotation(AnnotationSpec.builder(DisplayName.class).addMember("value", "$S", test.name).build());

						switch(test.headerType){
							case "item" -> methodBuilder.addStatement("$T source=$T.itemFromJson($T.parseString($S))", StructuredHttpHeaders.Item.class, StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
							case "list" -> methodBuilder.addStatement("$T source=$T.listFromJson($T.parseString($S))", ParameterizedTypeName.get(List.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
							case "dictionary" -> methodBuilder.addStatement("$T source=$T.dictionaryFromJson($T.parseString($S))", ParameterizedTypeName.get(Map.class, String.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
						}

						methodBuilder.addStatement("$T.assertEquals($S, $T.serialize(source))", Assertions.class, test.canonical!=null && !test.canonical.isEmpty() ? test.canonical.getFirst() : String.join(",", test.raw), StructuredHttpHeaders.class);
						serializeClassBuilder.addMethod(methodBuilder.build());
					}

					i++;
				}

				JavaFile.builder("smithereen.http.structuredHeaders", parseClassBuilder.build())
						.addStaticImport(Assertions.class, "*")
						.indent("\t")
						.build()
						.writeTo(new File("src/test/java"));
				JavaFile.builder("smithereen.http.structuredHeaders", serializeClassBuilder.build())
						.addStaticImport(Assertions.class, "*")
						.indent("\t")
						.build()
						.writeTo(new File("src/test/java"));
			}
		}

		files=new File("codegen/structured-field-tests/serialisation-tests").listFiles();
		for(File f:files){
			if(!f.getName().endsWith(".json"))
				continue;
			String name=f.getName();
			String className=Arrays.stream(name.substring(0, name.length()-5).split("-")).map(s->s.substring(0, 1).toUpperCase()+s.substring(1)).collect(Collectors.joining())+"Tests";
			try(FileReader reader=new FileReader(f)){
				List<TestCase> testCases=gson.fromJson(reader, new TypeToken<>(){});
				TypeSpec.Builder serializeClassBuilder=TypeSpec.classBuilder("SerializeOnly"+className)
						.addModifiers(Modifier.PUBLIC);

				int i=0;
				for(TestCase test:testCases){
					MethodSpec.Builder methodBuilder=MethodSpec.methodBuilder("test"+i)
							.addModifiers(Modifier.PUBLIC)
							.addAnnotation(Test.class)
							.addAnnotation(AnnotationSpec.builder(DisplayName.class).addMember("value", "$S", test.name).build());

					if(test.mustFail){
						methodBuilder.beginControlFlow("$T.assertThrows($T.class, ()->", Assertions.class, IllegalArgumentException.class);
					}

					switch(test.headerType){
						case "item" -> methodBuilder.addStatement("$T source=$T.itemFromJson($T.parseString($S))", StructuredHttpHeaders.Item.class, StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
						case "list" -> methodBuilder.addStatement("$T source=$T.listFromJson($T.parseString($S))", ParameterizedTypeName.get(List.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
						case "dictionary" -> methodBuilder.addStatement("$T source=$T.dictionaryFromJson($T.parseString($S))", ParameterizedTypeName.get(Map.class, String.class, StructuredHttpHeaders.ItemOrInnerList.class), StructuredHeadersTestUtils.class, JsonParser.class, test.expected.toString());
					}

					if(test.mustFail){
						methodBuilder.addStatement("$T.serialize(source)", StructuredHttpHeaders.class);
						methodBuilder.endControlFlow(")");
					}else{
						methodBuilder.addStatement("$T.assertEquals($S, $T.serialize(source))", Assertions.class, test.canonical!=null && !test.canonical.isEmpty() ? test.canonical.getFirst() : String.join(",", test.raw), StructuredHttpHeaders.class);
					}
					serializeClassBuilder.addMethod(methodBuilder.build());
					i++;
				}

				JavaFile.builder("smithereen.http.structuredHeaders", serializeClassBuilder.build())
						.addStaticImport(Assertions.class, "*")
						.indent("\t")
						.build()
						.writeTo(new File("src/test/java"));
			}
		}

	}

	private static class TestCase{
		public String name;
		public List<String> raw;
		public List<String> canonical;
		public String headerType;
		public JsonElement expected;
		public boolean mustFail;
	}
}
