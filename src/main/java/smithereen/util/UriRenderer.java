package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.net.IDN;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import spark.utils.StringUtils;

/**
 * Transforms a {@link URI} into a human-readable string by decoding
 * <a href="https://en.wikipedia.org/wiki/Internationalized_domain_name">internationalized domain names</a>
 * and percent-encoded parts.
 * <p>
 * Percent-encoded parts are decoded so that the resulting string is the same as if you'd pasted it into a browser's address bar,
 * i.e. whitespaces, ASCII control characters and other sneaky characters are kept percent-encoded in order to not fuck up the UI.
 */
public class UriRenderer{
	private final boolean hostOnly;

	private UriRenderer(boolean hostOnly){
		this.hostOnly=hostOnly;
	}

	public static final UriRenderer DEFAULT=new UriRenderer(false);

	public static final UriRenderer HOST_ONLY=new UriRenderer(true);

	private boolean shouldDecodeCodePoint(int cp){
		if(0<=cp && cp<0x80){
			// ASCII
			return cp==0x22      // "
					|| cp==0x2D  // -
					|| (0x30<=cp && cp<=0x39) // 0-9
					|| cp==0x3C  // <
					|| cp==0x3E  // >
					|| (0x41<=cp && cp<=0x5A) // A-Z
					|| cp==0x5F  // _
					|| (0x61<=cp && cp<=0x7A) // a-z
					|| cp==0x7E; // ~
		}

		// The list of code points below is copied from Chromium verbatim
		// https://github.com/chromium/chromium/blob/ba3c200c1564977873107f5656c015253ba129b1/base/strings/escape.cc#L276-L380
		return !(
				// Per http://tools.ietf.org/html/rfc3987#section-4.1, certain BiDi
				// control characters are not allowed to appear unescaped in URLs.
				cp==0x200E ||          // LEFT-TO-RIGHT MARK         (%E2%80%8E)
						cp==0x200F ||  // RIGHT-TO-LEFT MARK         (%E2%80%8F)
						cp==0x202A ||  // LEFT-TO-RIGHT EMBEDDING    (%E2%80%AA)
						cp==0x202B ||  // RIGHT-TO-LEFT EMBEDDING    (%E2%80%AB)
						cp==0x202C ||  // POP DIRECTIONAL FORMATTING (%E2%80%AC)
						cp==0x202D ||  // LEFT-TO-RIGHT OVERRIDE     (%E2%80%AD)
						cp==0x202E ||  // RIGHT-TO-LEFT OVERRIDE     (%E2%80%AE)

						// The Unicode Technical Report (TR9) as referenced by RFC 3987 above has
						// since added some new BiDi control characters that are not safe to
						// unescape. http://www.unicode.org/reports/tr9
						cp==0x061C ||  // ARABIC LETTER MARK         (%D8%9C)
						cp==0x2066 ||  // LEFT-TO-RIGHT ISOLATE      (%E2%81%A6)
						cp==0x2067 ||  // RIGHT-TO-LEFT ISOLATE      (%E2%81%A7)
						cp==0x2068 ||  // FIRST STRONG ISOLATE       (%E2%81%A8)
						cp==0x2069 ||  // POP DIRECTIONAL ISOLATE    (%E2%81%A9)

						// The following spoofable characters are also banned in unescaped URLs,
						// because they could be used to imitate parts of a web browser's UI.
						cp==0x1F50F ||  // LOCK WITH INK PEN    (%F0%9F%94%8F)
						cp==0x1F510 ||  // CLOSED LOCK WITH KEY (%F0%9F%94%90)
						cp==0x1F512 ||  // LOCK                 (%F0%9F%94%92)
						cp==0x1F513 ||  // OPEN LOCK            (%F0%9F%94%93)

						// Spaces are also banned, as they can be used to scroll text out of view.
						cp==0x0085 ||  // NEXT LINE                  (%C2%85)
						cp==0x00A0 ||  // NO-BREAK SPACE             (%C2%A0)
						cp==0x1680 ||  // OGHAM SPACE MARK           (%E1%9A%80)
						cp==0x2000 ||  // EN QUAD                    (%E2%80%80)
						cp==0x2001 ||  // EM QUAD                    (%E2%80%81)
						cp==0x2002 ||  // EN SPACE                   (%E2%80%82)
						cp==0x2003 ||  // EM SPACE                   (%E2%80%83)
						cp==0x2004 ||  // THREE-PER-EM SPACE         (%E2%80%84)
						cp==0x2005 ||  // FOUR-PER-EM SPACE          (%E2%80%85)
						cp==0x2006 ||  // SIX-PER-EM SPACE           (%E2%80%86)
						cp==0x2007 ||  // FIGURE SPACE               (%E2%80%87)
						cp==0x2008 ||  // PUNCTUATION SPACE          (%E2%80%88)
						cp==0x2009 ||  // THIN SPACE                 (%E2%80%89)
						cp==0x200A ||  // HAIR SPACE                 (%E2%80%8A)
						cp==0x2028 ||  // LINE SEPARATOR             (%E2%80%A8)
						cp==0x2029 ||  // PARAGRAPH SEPARATOR        (%E2%80%A9)
						cp==0x202F ||  // NARROW NO-BREAK SPACE      (%E2%80%AF)
						cp==0x205F ||  // MEDIUM MATHEMATICAL SPACE  (%E2%81%9F)
						cp==0x3000 ||  // IDEOGRAPHIC SPACE          (%E3%80%80)
						// U+2800 is rendered as a space, but is not considered whitespace (see
						// crbug.com/1068531).
						cp==0x2800 ||  // BRAILLE PATTERN BLANK      (%E2%A0%80)

						// Default Ignorable ([:Default_Ignorable_cp=Yes:]) and Format
						// characters ([:Cf:]) are also banned (see crbug.com/824715).
						cp==0x00AD ||  // SOFT HYPHEN               (%C2%AD)
						cp==0x034F ||  // COMBINING GRAPHEME JOINER (%CD%8F)
						// Arabic number formatting
						(cp>=0x0600 && cp<=0x0605) ||
						// U+061C is already banned as a BiDi control character.
						cp==0x06DD ||  // ARABIC END OF AYAH          (%DB%9D)
						cp==0x070F ||  // SYRIAC ABBREVIATION MARK    (%DC%8F)
						cp==0x08E2 ||  // ARABIC DISPUTED END OF AYAH (%E0%A3%A2)
						cp==0x115F ||  // HANGUL CHOSEONG FILLER      (%E1%85%9F)
						cp==0x1160 ||  // HANGUL JUNGSEONG FILLER     (%E1%85%A0)
						cp==0x17B4 ||  // KHMER VOWEL INHERENT AQ     (%E1%9E%B4)
						cp==0x17B5 ||  // KHMER VOWEL INHERENT AA     (%E1%9E%B5)
						cp==0x180B ||  // MONGOLIAN FREE VARIATION SELECTOR ONE
						// (%E1%A0%8B)
						cp==0x180C ||  // MONGOLIAN FREE VARIATION SELECTOR TWO
						// (%E1%A0%8C)
						cp==0x180D ||  // MONGOLIAN FREE VARIATION SELECTOR THREE
						// (%E1%A0%8D)
						cp==0x180E ||  // MONGOLIAN VOWEL SEPARATOR   (%E1%A0%8E)
						cp==0x200B ||  // ZERO WIDTH SPACE            (%E2%80%8B)
						cp==0x200C ||  // ZERO WIDTH SPACE NON-JOINER (%E2%80%8C)
						cp==0x200D ||  // ZERO WIDTH JOINER           (%E2%80%8D)
						// U+200E, U+200F, U+202A--202E, and U+2066--2069 are already banned as
						// BiDi control characters.
						cp==0x2060 ||  // WORD JOINER          (%E2%81%A0)
						cp==0x2061 ||  // FUNCTION APPLICATION (%E2%81%A1)
						cp==0x2062 ||  // INVISIBLE TIMES      (%E2%81%A2)
						cp==0x2063 ||  // INVISIBLE SEPARATOR  (%E2%81%A3)
						cp==0x2064 ||  // INVISIBLE PLUS       (%E2%81%A4)
						cp==0x2065 ||  // null (%E2%81%A5)
						// 0x2066--0x2069 are already banned as a BiDi control characters.
						// General Punctuation - Deprecated (U+206A--206F)
						(cp>=0x206A && cp<=0x206F) ||
						cp==0x3164 ||  // HANGUL FILLER (%E3%85%A4)
						(cp>=0xFFF0 && cp<=0xFFF8) ||  // null
						// Variation selectors (%EF%B8%80 -- %EF%B8%8F)
						(cp>=0xFE00 && cp<=0xFE0F) ||
						cp==0xFEFF ||   // ZERO WIDTH NO-BREAK SPACE (%EF%BB%BF)
						cp==0xFFA0 ||   // HALFWIDTH HANGUL FILLER (%EF%BE%A0)
						cp==0xFFF9 ||   // INTERLINEAR ANNOTATION ANCHOR     (%EF%BF%B9)
						cp==0xFFFA ||   // INTERLINEAR ANNOTATION SEPARATOR  (%EF%BF%BA)
						cp==0xFFFB ||   // INTERLINEAR ANNOTATION TERMINATOR (%EF%BF%BB)
						cp==0x110BD ||  // KAITHI NUMBER SIGN       (%F0%91%82%BD)
						cp==0x110CD ||  // KAITHI NUMBER SIGN ABOVE (%F0%91%83%8D)
						// Egyptian hieroglyph formatting (%F0%93%90%B0 -- %F0%93%90%B8)
						(cp>=0x13430 && cp<=0x13438) ||
						// Shorthand format controls (%F0%9B%B2%A0 -- %F0%9B%B2%A3)
						(cp>=0x1BCA0 && cp<=0x1BCA3) ||
						// Beams and slurs (%F0%9D%85%B3 -- %F0%9D%85%BA)
						(cp>=0x1D173 && cp<=0x1D17A) ||
						// Tags, Variation Selectors, nulls
						(cp>=0xE0000 && cp<=0xE0FFF));
	}

	public @NotNull String render(@NotNull URI url){
		if(url.isOpaque()) return url.toString();
		StringBuilder sb=new StringBuilder();
		if(!hostOnly){
			String scheme=url.getScheme();
			if(StringUtils.isNotEmpty(scheme)){
				sb.append(scheme);
				sb.append("://");
			}
			String user=url.getRawUserInfo();
			if(user!=null){
				sb.append(safePercentDecode(user));
				sb.append('@');
			}
		}
		String host=url.getHost();
		if(StringUtils.isNotEmpty(host)){
			sb.append(IDN.toUnicode(host));
			int port=url.getPort();
			if(port>=0){
				sb.append(':');
				sb.append(port);
			}
		}else{
			String authority=url.getAuthority();
			if(StringUtils.isNotEmpty(authority)){
				sb.append(IDN.toUnicode(authority));
			}
		}
		if(!hostOnly){
			String path=url.getRawPath();
			if(path!=null){
				String decodedPath=Arrays.stream(path.split("/")).map(this::safePercentDecode).collect(Collectors.joining("/"));
				sb.append(decodedPath);
				if(path.endsWith("/")){
					sb.append('/');
				}
			}
			String query=url.getRawQuery();
			if(query!=null){
				sb.append("?");
				sb.append(Arrays.stream(query.split("&")).map(kv->Arrays.stream(kv.split("=", 2)).map(this::safePercentDecode).collect(Collectors.joining("="))).collect(Collectors.joining("&")));
			}
			String fragment=url.getRawFragment();
			if(fragment!=null){
				sb.append('#');
				sb.append(safePercentDecode(fragment));
			}
		}
		return sb.toString();
	}

	private static CharsetDecoder createDecoder(){
		CharsetDecoder decoder=StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		return decoder;
	}

	private @NotNull String safePercentDecode(@NotNull String input){
		// java.net.URLDecoder replaces invalid UTF-8 sequences with � and doesn't allow us to process the input
		// one code point at a time, so we implement our own decoder.
		int inputLength=input.length();
		StringBuilder sb=new StringBuilder(inputLength);

		// These are lazily initialized
		ByteBuffer buf=null;
		CharsetDecoder utf8Decoder=null;
		CharBuffer charBuf=null;

		for(int inputPos=0;inputPos<inputLength;){
			char c=input.charAt(inputPos);
			switch(c){
				case '+':
					sb.append('+');
					++inputPos;
					continue;
				case '%':
					if(buf!=null){
						// Reset before the new iteration
						buf.limit(buf.capacity());
						buf.position(0);
						charBuf.limit(charBuf.capacity());
						charBuf.position(0);
					}
					do{
						if(inputPos+3>inputLength) return input;
						int b;
						try{
							b=Integer.parseInt(input, inputPos+1, inputPos+3, 16);
						}catch(NumberFormatException e){
							return input;
						}
						if(b<0 || b>0xFF) return input;
						if(buf==null){
							// At most this many characters is remaining in the input string,
							// since we each percent-encoded byte is exactly three characters: '%' and two hex digits.
							buf=ByteBuffer.allocate((inputLength-inputPos)/3);
							utf8Decoder=createDecoder();
							charBuf=CharBuffer.allocate(2);
						}
						buf.put((byte) b);
						inputPos+=3;
					}while(inputPos<inputLength && input.charAt(inputPos)=='%');
					int numBytes=buf.position();
					buf.limit(numBytes);
					buf.position(0);
					do{
						// Decode the next code point.
						int posBefore=buf.position();
						// First try decoding only one char.
						charBuf.limit(1);
						charBuf.position(0);
						CoderResult res=utf8Decoder.decode(buf, charBuf, false);
						if(res==CoderResult.OVERFLOW && charBuf.position()==0){
							// The code point doesn't fit into a single char, increase the limit and try again.
							// Two-char buffer is definitely enough for a single code point, as code points are at most 4 bytes.
							charBuf.limit(2);
							utf8Decoder.decode(buf, charBuf, false);
						}
						int posAfter=buf.position();
						if(posBefore==posAfter){
							// The input buffer wasn't advanced either because of an unfinished UTF-8 byte sequence,
							// or because of malformed input. Skip this byte, try with the next one.
							percentEncodeByte(buf, posBefore, posBefore+1, sb);
							buf.position(buf.position()+1);
							continue;
						}
						charBuf.limit(charBuf.position());
						charBuf.position(0);
						int codePoint=Character.codePointAt(charBuf, 0);
						if(shouldDecodeCodePoint(codePoint)){
							sb.append(charBuf);
						}else if(codePoint==0x20){
							sb.append('+');
						}else{
							percentEncodeByte(buf, posBefore, posAfter, sb);
						}
					}while(buf.position()<buf.limit());
					continue;
				default:
					sb.append(c);
					++inputPos;
			}
		}
		return sb.toString();
	}

	private static void percentEncodeByte(ByteBuffer buf, int from, int to, StringBuilder sb){
		for(int j=from;j<to;j++){
			sb.append(String.format("%%%02X", Byte.toUnsignedInt(buf.get(j))));
		}
	}
}
