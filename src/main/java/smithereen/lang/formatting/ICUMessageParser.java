package smithereen.lang.formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smithereen.lang.Inflector;
import spark.utils.StringUtils;

// Inspired by https://github.com/format-message/format-message/blob/master/packages/format-message-parse/index.js
// ICU4J is too damn huge and inflexible
public class ICUMessageParser{
	private static final char ARG_OPEN='{';
	private static final char ARG_CLOSE='}';
	private static final char ARG_SEP=',';
	private static final char NUM_ARG='#';
	private static final char ESC='\'';
	private static final String OFFSET="offset:";

	private static final List<String> SIMPLE_TYPES=List.of("number", "date", "time", "inflect");
	private static final List<String> SUB_TYPES=List.of("plural", "select");
	private static final List<String> VALID_PLURAL_KEYS=List.of("zero", "one", "two", "few", "many", "other");

	public static Object parse(String text){
		if(!text.contains("{"))
			return text;

		return new StringTemplate(new ParserImpl(text).parse("", ""));
	}

	private static class ParserImpl{
		private final String patternStr;
		private final char[] pattern;
		private int index;

		public ParserImpl(String pattern){
			patternStr=pattern;
			this.pattern=pattern.toCharArray();
		}

		private List<FormatterNode> parse(String parentType, String parentID){
			ArrayList<FormatterNode> nodes=new ArrayList<>();

			String text=parseText(parentType);
			if(StringUtils.isNotEmpty(text))
				nodes.add(new LiteralFormatterNode(text));
			while(index<pattern.length){
				if(pattern[index]==ARG_CLOSE){
					if(StringUtils.isEmpty(parentType))
						syntaxError(null, null, null);
					break;
				}
				nodes.add(parsePlaceholder(parentID));
				text=parseText(parentType);
				if(!text.isEmpty())
					nodes.add(new LiteralFormatterNode(text));
			}

			return nodes;
		}

		private String parseText(String parentType){
			boolean isHashSpecial="plural".equals(parentType);
			boolean isArgStyle="{style}".equals(parentType);
			StringBuilder text=new StringBuilder();

			while(index<pattern.length){
				char ch=pattern[index];
				if(ch==ARG_OPEN || ch==ARG_CLOSE || (isHashSpecial && ch==NUM_ARG) || (isArgStyle && Character.isWhitespace(ch))){
					break;
				}else if(ch==ESC){
					ch=pattern[++index];
					if(ch==ESC){
						text.append(ch);
						index++;
					}else if(ch==ARG_OPEN || ch==ARG_CLOSE || (isHashSpecial && ch==NUM_ARG)){
						text.append(ch);
						while(++index<pattern.length){
							ch=pattern[index];
							if(ch==ESC && pattern[index+1]==ESC){
								text.append(ESC);
								index++;
							}else if(ch==ESC){
								index++;
								break;
							}else{
								text.append(ch);
							}
						}
					}else{
						text.append(ESC);
					}
				}else{
					text.append(ch);
					index++;
				}
			}

			return text.toString();
		}

		private FormatterNode parsePlaceholder(String parentID){
			if(pattern[index]==NUM_ARG){
				index++;
				return new NumberFormatterNode(parentID, null);
			}

			if(pattern[index]!=ARG_OPEN)
				syntaxError(ARG_OPEN+"", null, null);
			index++;
			skipWhitespace();

			String id=parseID();
			if(id.isEmpty())
				syntaxError("placeholder id", null, null);
			skipWhitespace();

			char ch=pattern[index];
			if(ch==ARG_CLOSE){
				index++;
				return new SubstitutionFormatterNode(id);
			}

			if(ch!=ARG_SEP)
				syntaxError(ARG_SEP+" or "+ARG_CLOSE, null, null);
			index++;
			skipWhitespace();

			String type=parseID();
			if(type.isEmpty())
				syntaxError("placeholder type", null, null);
			skipWhitespace();
			ch=pattern[index];
			if(ch==ARG_CLOSE){
				if(SUB_TYPES.contains(type))
					syntaxError(type+" sub-messages", null, null);
				index++;
				return switch(type){
					case "number" -> new NumberFormatterNode(id, null);
					case "inflect" -> throw new ICUMessageSyntaxException("'inflect' requires a case as an argument");
					default -> throw new ICUMessageSyntaxException("unknown type "+type);
				};
			}

			if(ch!=ARG_SEP)
				syntaxError(ARG_SEP+" or "+ARG_CLOSE, null, null);
			index++;
			skipWhitespace();

			FormatterNode res;
			if(type.equals("plural")){
				int offset=parsePluralOffset();
				skipWhitespace();
				res=new PluralFormatterNode(id, parseSubMessages(type, id), offset);
			}else if(type.equals("select")){
				res=new SelectFormatterNode(id, parseSubMessages(type, id));
			}else if(SIMPLE_TYPES.contains(type)){
				res=switch(type){
					case "number" -> new NumberFormatterNode(id, parseSimpleFormat());
					case "inflect" -> new InflectFormatterNode(id, Inflector.Case.valueOf(parseSimpleFormat().toUpperCase()));
					default -> throw new ICUMessageSyntaxException("unknown type "+type);
				};
			}else{
				throw new ICUMessageSyntaxException("unknown type "+type);
			}
			skipWhitespace();
			if(index>=pattern.length)
				syntaxError(ARG_CLOSE+"", "end of string", null);
			if(pattern[index]!=ARG_CLOSE)
				syntaxError(ARG_CLOSE+"", null, null);
			index++;
			return res;
		}

		public void skipWhitespace(){
			while(index<pattern.length && Character.isWhitespace(pattern[index])){
				index++;
			}
		}

		public String parseID(){
			StringBuilder id=new StringBuilder();
			while(index<pattern.length){
				char ch=pattern[index];
				if(ch==ARG_OPEN || ch==ARG_CLOSE || ch==ARG_SEP || ch==NUM_ARG || ch==ESC || Character.isWhitespace(ch))
					break;
				id.append(ch);
				index++;
			}
			return id.toString();
		}

		private int parsePluralOffset(){
			if(pattern.length>index+OFFSET.length() && patternStr.startsWith(OFFSET, index)){
				index+=OFFSET.length();
				skipWhitespace();
				int start=index;
				while(index<pattern.length && pattern[index]>='0' && pattern[index]<='9'){
					index++;
				}
				if(start==index)
					syntaxError("offset number", null, null);
				return Integer.parseInt(patternStr.substring(start, index));
			}
			return 0;
		}

		private Map<String, List<FormatterNode>> parseSubMessages(String parentType, String parentID){
			Map<String, List<FormatterNode>> subMessages=new HashMap<>();
			while(index<pattern.length && pattern[index]!=ARG_CLOSE){
				String selector=parseID();
				if(selector.isEmpty())
					syntaxError("sub-message selector", null, null);
				if("plural".equals(parentType)){
					if(!VALID_PLURAL_KEYS.contains(selector)){
						if(selector.matches("=\\d+"))
							selector=selector.substring(1);
						else
							syntaxError(null, null, selector+" is not a valid plural key");
					}
				}
				skipWhitespace();
				subMessages.put(selector, parseSubMessage(parentType, parentID));
				skipWhitespace();
			}
			if(!subMessages.containsKey("other") && /*SUB_TYPES.contains(parentType)*/ parentType.equals("plural"))
				syntaxError(null, null, "'other' sub-message must be specified in "+parentType);
			return subMessages;
		}

		private List<FormatterNode> parseSubMessage(String parentType, String parentID){
			if(pattern[index]!=ARG_OPEN)
				syntaxError(ARG_OPEN+" to start sub-message", null, null);
			index++;
			List<FormatterNode> message=parse(parentType, parentID);
			if(pattern[index]!=ARG_CLOSE)
				syntaxError(ARG_CLOSE+" to end sub-message", null, null);
			index++;
			return message;
		}

		private String parseSimpleFormat(){
			String style=parseText("{style}");
			if(style.isEmpty())
				syntaxError("placeholder style name", null, null);
			return style;
		}

		private void syntaxError(String expected, String found, String message){
			if(message==null){
				if(found==null){
					if(index>=pattern.length){
						found="end of message pattern";
					}else{
						String id=parseID();
						found=StringUtils.isEmpty(id) ? (pattern[index]+"") : id;
					}
				}
				if(expected==null)
					message="Unexpected "+found+" found";
				else
					message="Expected "+expected+" but found "+found;
			}
			throw new ICUMessageSyntaxException(message+" at offset "+index);
		}
	}
}
