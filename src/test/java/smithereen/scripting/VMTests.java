package smithereen.scripting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VMTests{
	@Test
	public void testAddition(){
		Script s=Script.compile("return 1+5;");
		assertEquals(ScriptValue.of(6), ScriptVM.execute(s));
	}

	@Test
	public void testSubtraction(){
		Script s=Script.compile("return 1-5;");
		assertEquals(ScriptValue.of(-4), ScriptVM.execute(s));
	}

	@Test
	public void testMultiplication(){
		Script s=Script.compile("return 5*6;");
		assertEquals(ScriptValue.of(30), ScriptVM.execute(s));
	}

	@Test
	public void testDivision(){
		Script s=Script.compile("return 2/4;");
		assertEquals(ScriptValue.of(0.5), ScriptVM.execute(s));
	}

	@Test
	public void testRemainder(){
		Script s=Script.compile("return 8%5;");
		assertEquals(ScriptValue.of(3), ScriptVM.execute(s));
	}

	@Test
	public void testArithmeticPrecedence(){
		Script s=Script.compile("return 2*3+10/2-5%3;");
		assertEquals(ScriptValue.of(9), ScriptVM.execute(s));
	}

	@Test
	public void testArithmeticPrecedenceWithParenthesis(){
		Script s=Script.compile("return 3*(3+10)/2-5%3;");
		assertEquals(ScriptValue.of(17.5), ScriptVM.execute(s));
	}

	@Test
	public void testStringConcatenation(){
		Script s=Script.compile("return \"str1\"+\" \"+\"str2\";");
		assertEquals(ScriptValue.of("str1 str2"), ScriptVM.execute(s));
	}

	@Test
	public void testStringConcatenationWithNumber(){
		Script s=Script.compile("""
				var a="test"+2;
				var b=3+"test";
				return b+a;""");
		assertEquals(ScriptValue.of("3testtest2"), ScriptVM.execute(s));
	}

	@Test
	public void testVariables(){
		Script s=Script.compile("""
				var a="test";
				return a;""");
		assertEquals(ScriptValue.of("test"), ScriptVM.execute(s));
	}

	@Test
	public void testVariableScopes(){
		assertThrows(ScriptCompilationException.class, ()->Script.compile("""
			var a="outer";
			{
				var b="inner";
			}
			return b;"""));
	}

	@Test
	public void testVariableScopeShadowing1(){
		Script s=Script.compile("""
				var a="fail";
				{
					var a="pass";
					return a;
				}""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testVariableScopeShadowing2(){
		Script s=Script.compile("""
				var a="pass";
				{
					var a="fail";
				}
				return a;""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testConditionSimple(){
		Script s=Script.compile("""
				var a="test";
				if(a=="lol"){
					return "fail";
				}else{
					return "pass";
				}""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testAndShortCircuiting(){
		Script s=Script.compile("""
				var a=5;
				var b=0;
				return a!=5 && a/b==6;""");
		assertEquals(ScriptValue.FALSE, ScriptVM.execute(s));
	}

	@Test
	public void testOrShortCircuiting(){
		Script s=Script.compile("""
				var a=5;
				var b=0;
				return a==5 || a/b==6;""");
		assertEquals(ScriptValue.TRUE, ScriptVM.execute(s));
	}

	@Test
	public void testConditionAndOr(){
		Script s=Script.compile("""
				var a="test";
				var b=5;
				if(a=="lol" || b==5 || false){
					if(a=="test" && b!=4 && b>2 && true)
						return "pass";
					return "fail 1";
				}else{
					return "fail 2";
				}""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testWhileLoop(){
		Script s=Script.compile("""
				var result="";
				var i=0;
				while(i<15){
					result=result+i;
					if(i<14)
						result=result+" ";
					i=i+1;
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3 4 5 6 7 8 9 10 11 12 13 14"), ScriptVM.execute(s));
	}

	@Test
	public void testNestedWhileLoops(){
		Script s=Script.compile("""
				var result="";
				var i=0;
				while(i<5){
					result=result+i;
					var j=0;
					while(j<3){
						result=result+","+j;
						j=j+1;
					}
					if(i<4)
						result=result+" ";
					i=i+1;
				}
				return result;""");
		assertEquals(ScriptValue.of("0,0,1,2 1,0,1,2 2,0,1,2 3,0,1,2 4,0,1,2"), ScriptVM.execute(s));
	}

	@Test
	public void testInfiniteWhileLoop(){
		Script s=Script.compile("while(true){}");
		assertThrows(ScriptRuntimeException.class, ()->ScriptVM.execute(s));
	}

	@Test
	public void testEmptyScript(){
		Script s=Script.compile("");
		assertNull(ScriptVM.execute(s));
	}

	@Test
	public void testForLoop(){
		Script s=Script.compile("""
				var result="";
				for(var i=0;i<15;i=i+1){
					result=result+i;
					if(i<14)
						result=result+" ";
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3 4 5 6 7 8 9 10 11 12 13 14"), ScriptVM.execute(s));
	}

	@Test
	public void testForLoopWithoutIncrement(){
		Script s=Script.compile("""
				var result="";
				for(var i=0;i<15;){
					result=result+i;
					if(i<14)
						result=result+" ";
					i=i+1;
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3 4 5 6 7 8 9 10 11 12 13 14"), ScriptVM.execute(s));
	}

	@Test
	public void testForLoopWithoutInit(){
		Script s=Script.compile("""
				var result="";
				var i=0;
				for(;i<15;i=i+1){
					result=result+i;
					if(i<14)
						result=result+" ";
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3 4 5 6 7 8 9 10 11 12 13 14"), ScriptVM.execute(s));
	}

	@Test
	public void testInfiniteForLoop(){
		Script s=Script.compile("for(;;){}");
		assertThrows(ScriptRuntimeException.class, ()->ScriptVM.execute(s));
	}

	@Test
	public void testLoopContinue(){
		Script s=Script.compile("""
				var result="";
				for(var i=0;i<15;i=i+1){
					if(i>5 && i<10)
						continue;
					result=result+i;
					if(i<14)
						result=result+" ";
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3 4 5 10 11 12 13 14"), ScriptVM.execute(s));
	}

	@Test
	public void testLoopBreak(){
		Script s=Script.compile("""
				var result="";
				for(var i=0;i<15;i=i+1){
					result=result+i;
					if(i==3)
						break;
					if(i<14)
						result=result+" ";
				}
				return result;""");
		assertEquals(ScriptValue.of("0 1 2 3"), ScriptVM.execute(s));
	}

	@Test
	public void testTernaryOperator1(){
		Script s=Script.compile("""
				var a=1;
				var b=2;
				return a==b ? "fail" : "pass";""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testTernaryOperator2(){
		Script s=Script.compile("""
				var a=1;
				var b=2;
				return a!=b ? "pass" : "fail";""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testObjectLiteral(){
		Script s=Script.compile("return {bareIdentifier: 12, 'singleQuotes': 34, \"doubleQuotes\": '56'};");
		assertEquals(ScriptValue.of(Map.of(
				"bareIdentifier", ScriptValue.of(12),
				"singleQuotes", ScriptValue.of(34),
				"doubleQuotes", ScriptValue.of("56")
		)), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldAccess(){
		Script s=Script.compile("""
				var obj={field1: 'fail', field2: 'pass', field3: null, field4: false, field5: {}};
				return obj.field2;""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldAssignment1(){
		Script s=Script.compile("""
				var obj={field1: 'fail', field2: 'pass', field3: null, field4: false, field5: {}};
				obj.field1="pass";
				return obj.field1;""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldAssignment2(){
		Script s=Script.compile("""
				var obj={};
				obj.field1={};
				obj.field1.test="pass";
				return obj.field1.test;""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldDeletion(){
		Script s=Script.compile("""
				var obj={field1: {inner: 1}, field2: 2};
				delete obj.field1.inner;
				return obj;""");
		assertEquals(ScriptValue.of(Map.of("field1", ScriptValue.of(Map.of()), "field2", ScriptValue.of(2))), ScriptVM.execute(s));
	}

	@Test
	public void testArrayLiteral(){
		Script s=Script.compile("return [1, 2, 3, 'heh', false];");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(1),
				ScriptValue.of(2),
				ScriptValue.of(3),
				ScriptValue.of("heh"),
				ScriptValue.FALSE
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArraySubscript(){
		Script s=Script.compile("""
				var arr=[1, 2, 3, 'heh', false];
				var i=2;
				return arr[i];""");
		assertEquals(ScriptValue.of(3), ScriptVM.execute(s));
	}

	@Test
	public void testArraySubscriptAssignment(){
		Script s=Script.compile("""
				var arr=[1, 2, 3, 'heh', false];
				arr[2]="reassigned";
				return arr;""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(1),
				ScriptValue.of(2),
				ScriptValue.of("reassigned"),
				ScriptValue.of("heh"),
				ScriptValue.FALSE
		)), ScriptVM.execute(s));
	}

	@Test
	public void testObjectArrayFieldSelection(){
		Script s=Script.compile("""
				var arr=[1, {name: "test", otherField: "sdfdsf"}, {name: "test2", yetAnotherField: "kjfjkdsahfjdkaf"}];
				return arr@.name;""");
		ArrayList<ScriptValue> expected=new ArrayList<>(); // List.of() hates nulls for some stupid reason
		expected.add(null);
		expected.add(ScriptValue.of("test"));
		expected.add(ScriptValue.of("test2"));
		assertEquals(ScriptValue.of(expected), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldAccessSubscript(){
		Script s=Script.compile("""
				var obj={field1: 'fail', field2: 'pass', field3: null, field4: false, field5: {}};
				return obj['field2'];""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testObjectFieldAssignmentSubscript(){
		Script s=Script.compile("""
				var obj={field1: 'fail', field2: 'pass', field3: null, field4: false, field5: {}};
				obj['field1']="pass";
				return obj.field1;""");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s));
	}

	@Test
	public void testArrayLengthProperty(){
		Script s=Script.compile("return [1, 2, 3, 4].length;");
		assertEquals(ScriptValue.of(4), ScriptVM.execute(s));
	}

	@Test
	public void testStringLengthProperty(){
		Script s=Script.compile("return 'qweqwe'.length;");
		assertEquals(ScriptValue.of(6), ScriptVM.execute(s));
	}

	@Test
	public void testParseInt(){
		Script s=Script.compile("return [parseInt('123'), parseInt('-123'), parseInt('123dfsfdf'), parseInt('qw12'), parseInt('-'), parseInt({}), parseInt({qwe: 1}), parseInt([]), parseInt([1]), parseInt(true), parseInt(false)];");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(123),
				ScriptValue.of(-123),
				ScriptValue.of(123),
				ScriptValue.of(0),
				ScriptValue.of(0),
				ScriptValue.of(0),
				ScriptValue.of(1),
				ScriptValue.of(0),
				ScriptValue.of(1),
				ScriptValue.of(1),
				ScriptValue.of(0)
		)), ScriptVM.execute(s));
	}

	@Test
	public void testParseDouble(){
		Script s=Script.compile("return [parseDouble('12.34'), parseDouble('-12.34'), parseDouble('12.34qwe'), parseDouble('12.34e3'), parseDouble('qw12'), parseDouble('-'), parseDouble({}), parseDouble({qwe: 1}), parseDouble([]), parseDouble([1]), parseDouble(true), parseDouble(false)];");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(12.34),
				ScriptValue.of(-12.34),
				ScriptValue.of(12.34),
				ScriptValue.of(12340),
				ScriptValue.of(0),
				ScriptValue.of(0),
				ScriptValue.of(0),
				ScriptValue.of(1),
				ScriptValue.of(0),
				ScriptValue.of(1),
				ScriptValue.of(1),
				ScriptValue.of(0)
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArrayPush(){
		Script s=Script.compile("""
				var arr=[];
				arr.push(1);
				arr.push(2, 3, 4);
				return [arr.push("3"), arr];""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(5),
				ScriptValue.of(List.of(ScriptValue.of(1), ScriptValue.of(2), ScriptValue.of(3), ScriptValue.of(4), ScriptValue.of("3")))
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArraySlice(){
		Script s=Script.compile("""
				var animals = ["ant", "bison", "camel", "duck", "elephant"];
				return [
					animals.slice(2),
					animals.slice(2, 4),
					animals.slice(1, 5),
					animals.slice(-2),
					animals.slice(2, -1),
					animals.slice(1, 0),
					animals.slice(20, 1)
				];
				""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(List.of(ScriptValue.of("camel"), ScriptValue.of("duck"), ScriptValue.of("elephant"))),
				ScriptValue.of(List.of(ScriptValue.of("camel"), ScriptValue.of("duck"))),
				ScriptValue.of(List.of(ScriptValue.of("bison"), ScriptValue.of("camel"), ScriptValue.of("duck"), ScriptValue.of("elephant"))),
				ScriptValue.of(List.of(ScriptValue.of("duck"), ScriptValue.of("elephant"))),
				ScriptValue.of(List.of(ScriptValue.of("camel"), ScriptValue.of("duck"))),
				ScriptValue.of(List.of()),
				ScriptValue.of(List.of())
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArrayPop(){
		Script s=Script.compile("""
			var a=[1, 2, 3, 4];
			return [a.pop(), a.length, [].pop()];""");
		ArrayList<ScriptValue> expected=new ArrayList<>();
		expected.add(ScriptValue.of(4));
		expected.add(ScriptValue.of(3));
		expected.add(null);
		assertEquals(ScriptValue.of(expected), ScriptVM.execute(s));
	}

	@Test
	public void testArrayShift(){
		Script s=Script.compile("""
			var a=[1, 2, 3, 4];
			return [a.shift(), a.length, [].shift()];""");
		ArrayList<ScriptValue> expected=new ArrayList<>();
		expected.add(ScriptValue.of(1));
		expected.add(ScriptValue.of(3));
		expected.add(null);
		assertEquals(ScriptValue.of(expected), ScriptVM.execute(s));
	}

	@Test
	public void testArrayUnshift(){
		Script s=Script.compile("""
				var arr=[];
				arr.unshift(1);
				arr.unshift(2, 3, 4);
				return [arr.unshift("3"), arr];""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(5),
				ScriptValue.of(List.of(ScriptValue.of("3"), ScriptValue.of(2), ScriptValue.of(3), ScriptValue.of(4), ScriptValue.of(1)))
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArraySplice(){
		Script s=Script.compile("""
				var fruits = ["Banana", "Orange", "Apple", "Mango"];
				fruits.splice(2, 0, "Lemon", "Kiwi");
				var numbers = [1, 2, 5];
				numbers.splice(-1, 0, 3, 4);
				var empty=[];
				empty.splice(2, 2, 5);
				var justDelete=[1, 2, 3];
				justDelete.splice(1, 1);
				var deleteToEnd=[1, 2, 3];
				deleteToEnd.splice(1);
				return [fruits, numbers, empty, justDelete, deleteToEnd];""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(List.of(ScriptValue.of("Banana"), ScriptValue.of("Orange"), ScriptValue.of("Lemon"), ScriptValue.of("Kiwi"), ScriptValue.of("Apple"), ScriptValue.of("Mango"))),
				ScriptValue.of(List.of(ScriptValue.of(1), ScriptValue.of(2), ScriptValue.of(3), ScriptValue.of(4), ScriptValue.of(5))),
				ScriptValue.of(List.of(ScriptValue.of(5))),
				ScriptValue.of(List.of(ScriptValue.of(1), ScriptValue.of(3))),
				ScriptValue.of(List.of(ScriptValue.of(1)))
		)), ScriptVM.execute(s));
	}

	@Test
	public void testArrayIndexOf(){
		Script s=Script.compile("""
				var arr=[1, 2, 0, 1, "1", null, {test: 1}];
				return [
					arr.indexOf("qwe"),
					arr.indexOf(1),
					arr.indexOf(null),
					arr.indexOf("1"),
					arr.indexOf(0),
					arr.indexOf({test: 1})
				];""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(-1),
				ScriptValue.of(0),
				ScriptValue.of(5),
				ScriptValue.of(4),
				ScriptValue.of(2),
				ScriptValue.of(6)
		)), ScriptVM.execute(s));
	}

	@Test
	public void testStringSplit(){
		Script s=Script.compile("return ['1.2.3'.split('.'), 'foo|bar|baz'.split('|', 2)];");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of(List.of(ScriptValue.of("1"), ScriptValue.of("2"), ScriptValue.of("3"))),
				ScriptValue.of(List.of(ScriptValue.of("foo"), ScriptValue.of("bar|baz")))
		)), ScriptVM.execute(s));
	}

	@Test
	public void testStringSubstr(){
		Script s=Script.compile("""
				var string = "Mozilla";
				return [
					string.substr(0, 1), // 'M'
					string.substr(1, 0), // ''
					string.substr(-1, 1), // 'a'
					string.substr(1, -1), // ''
					string.substr(-3), // 'lla'
					string.substr(1), // 'ozilla'
					string.substr(-20, 2), // 'Mo'
					string.substr(20, 2) // ''
				];""");
		assertEquals(ScriptValue.of(List.of(
				ScriptValue.of("M"),
				ScriptValue.of(""),
				ScriptValue.of("a"),
				ScriptValue.of(""),
				ScriptValue.of("lla"),
				ScriptValue.of("ozilla"),
				ScriptValue.of("Mo"),
				ScriptValue.of("")
		)), ScriptVM.execute(s));
	}

	@Test
	public void testStringIndexOf(){
		Script s=Script.compile("return ['this is a test string'.indexOf('is a'), 'qwe'.indexOf('rty')];");
		assertEquals(ScriptValue.of(List.of(ScriptValue.of(5), ScriptValue.of(-1))), ScriptVM.execute(s));
	}

	@Test
	public void testGettingArguments(){
		Script s=Script.compile("return Args.test_arg;");
		assertEquals(ScriptValue.of("pass"), ScriptVM.execute(s, new ScriptEnvironment(name->name.equals("test_arg") ? "pass" : null, null)));
	}

	@Test
	public void testApiCall(){
		Script s=Script.compile("return API.users.get({user_id: 1})[0];");
		ScriptValue resObj=ScriptValue.of(Map.of(
				"id", ScriptValue.of(1),
				"first_name", ScriptValue.of("Pavel"),
				"last_name", ScriptValue.of("Durov")
		));
		assertEquals(resObj, ScriptVM.execute(s, new ScriptEnvironment(name->null, (method, args)->{
			if(method.equals("users.get") && args.get("user_id") instanceof ScriptValue.Num(double id) && id==1){
				return ScriptValue.of(List.of(resObj));
			}
			return null;
		})));
	}
}
