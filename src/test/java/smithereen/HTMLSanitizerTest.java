package smithereen;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class HTMLSanitizerTest{

	@BeforeAll
	public static void setupConfig(){
		Config.domain="smithereen.local";
	}

	@Test
	public void testFormattingTagsAreKept(){
		String in="<p>Allowed tags include: <i>italics</i>, <b>bold</b>, <u>underline</u>, <s>strikethrough</s>, and <code>code</code>.</p><p><strong>&lt;strong&gt;</strong> and <em>&lt;em&gt;</em> are also allowed.</p>";
		String out=Utils.sanitizeHTML(in).replace("\n", "");
		assertEquals(in, out);
	}

	@Test
	public void testMastodonTootMicroformatIsKept(){
		String in="<p>Экстра-кадр: Чик-чик-чик-чирик!..</p><p><a href=\"https://mastodon.host/tags/photo\" class=\"mention hashtag\" rel=\"tag\">#<span>photo</span></a> <a href=\"https://mastodon.host/tags/birds\" class=\"mention hashtag\" rel=\"tag\">#<span>birds</span></a> <a href=\"https://mastodon.host/tags/sparrow\" class=\"mention hashtag\" rel=\"tag\">#<span>sparrow</span></a> <a href=\"https://mastodon.host/tags/%D1%84%D0%BE%D1%82%D0%BE\" class=\"mention hashtag\" rel=\"tag\">#<span>фото</span></a> <a href=\"https://mastodon.host/tags/%D0%BF%D1%82%D0%B8%D1%86%D1%8B\" class=\"mention hashtag\" rel=\"tag\">#<span>птицы</span></a> <a href=\"https://mastodon.host/tags/%D0%B2%D0%BE%D1%80%D0%BE%D0%B1%D0%B5%D0%B9\" class=\"mention hashtag\" rel=\"tag\">#<span>воробей</span></a></p><p><span class=\"h-card\"><a href=\"https://mastodonsocial.ru/@rf\" class=\"u-url mention\">@<span>rf</span></a></span> <span class=\"h-card\"><a href=\"https://mastodon.social/@russian_mastodon\" class=\"u-url mention\">@<span>russian_mastodon</span></a></span></p>";
		String expected="<p>Экстра-кадр: Чик-чик-чик-чирик!..</p>\n"+
				"<p><a href=\"https://mastodon.host/tags/photo\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>photo</span></a> <a href=\"https://mastodon.host/tags/birds\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>birds</span></a> <a href=\"https://mastodon.host/tags/sparrow\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>sparrow</span></a> <a href=\"https://mastodon.host/tags/%D1%84%D0%BE%D1%82%D0%BE\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>фото</span></a> <a href=\"https://mastodon.host/tags/%D0%BF%D1%82%D0%B8%D1%86%D1%8B\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>птицы</span></a> <a href=\"https://mastodon.host/tags/%D0%B2%D0%BE%D1%80%D0%BE%D0%B1%D0%B5%D0%B9\" class=\"mention hashtag\" rel=\"tag\" target=\"_blank\">#<span>воробей</span></a></p>\n"+
				"<p><span class=\"h-card\"><a href=\"https://mastodonsocial.ru/@rf\" class=\"u-url mention\" target=\"_blank\">@<span>rf</span></a></span> <span class=\"h-card\"><a href=\"https://mastodon.social/@russian_mastodon\" class=\"u-url mention\" target=\"_blank\">@<span>russian_mastodon</span></a></span></p>";

		String out=Utils.sanitizeHTML(in);
		assertEquals(expected, out);
	}

	@Test
	public void testTargetBlankIsAddedToExternalLinks(){
		String in="<a href=\"https://example.com\">External link</a>";
		String expected="<a href=\"https://example.com\" target=\"_blank\">External link</a>";
		String out=Utils.sanitizeHTML(in);
		assertEquals(expected, out);
	}

	@Test
	public void testTargetBlankIsNotAddedToLocalLinks(){
		String inout="<a href=\"https://smithereen.local/someuser\" class=\"mention\">Some user</a>";
		assertEquals(inout, Utils.sanitizeHTML(inout));
	}

	@Test
	public void testBadAttributesAreRemoved(){
		String in="<p onmouseover=\"alert('xss')\">Look I'm a hacker</p>";
		String expected="<p>Look I'm a hacker</p>";
		assertEquals(expected, Utils.sanitizeHTML(in));
	}

	@Test
	public void testBadTagsAreRemoved(){
		String in="<div>Test</div>";
		String expected="Test";
		assertEquals(expected, Utils.sanitizeHTML(in));
	}

	@Test
	public void testJavascriptUrlsAreRemoved(){
		String in="<a href=\"javascript:alert('xss')\">Click for XSS</a>";
		String expected="<a>Click for XSS</a>";
		assertEquals(expected, Utils.sanitizeHTML(in));
	}

	@Test
	public void testUnsupportedUrlSchemesAreRemoved(){
		String in="<a href=\"ftp://example.com/kitten.jpg\">Test</a>";
		String expected="<a>Test</a>";
		assertEquals(expected, Utils.sanitizeHTML(in));
	}

	@Test
	public void testRelativeUrlsAreExpended(){
		String in="<a href=\"/test.html\">Relative link</a>";
		String expected="<a href=\"https://example.com/test.html\" target=\"_blank\">Relative link</a>";
		assertEquals(expected, Utils.sanitizeHTML(in, URI.create("https://example.com/")));
	}
}
