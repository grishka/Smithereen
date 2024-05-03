package smithereen;

import org.junit.jupiter.api.Test;

import java.net.URI;

import smithereen.text.TextProcessor;

import static org.junit.jupiter.api.Assertions.*;

public class HTMLProcessingTest{

	public HTMLProcessingTest(){
		Config.domain="smithereen.local";
	}

	@Test
	public void testFormattingTagsAreKept(){
		String in="<p>Allowed tags include: <i>italics</i>, <b>bold</b>, <u>underline</u>, <s>strikethrough</s>, and <code>code</code>.</p><p><strong>&lt;strong&gt;</strong> and <em>&lt;em&gt;</em> are also allowed.</p>";
		String out=TextProcessor.sanitizeHTML(in).replace("\n", "");
		assertEquals(in, out);
	}

	@Test
	public void testMastodonTootMicroformatIsKept(){
		String in="<p>Экстра-кадр: Чик-чик-чик-чирик!..</p><p><a href=\"https://mastodon.host/tags/photo\" class=\"mention hashtag\" rel=\"tag\">#<span>photo</span></a> <a href=\"https://mastodon.host/tags/birds\" class=\"mention hashtag\" rel=\"tag\">#<span>birds</span></a> <a href=\"https://mastodon.host/tags/sparrow\" class=\"mention hashtag\" rel=\"tag\">#<span>sparrow</span></a> <a href=\"https://mastodon.host/tags/%D1%84%D0%BE%D1%82%D0%BE\" class=\"mention hashtag\" rel=\"tag\">#<span>фото</span></a> <a href=\"https://mastodon.host/tags/%D0%BF%D1%82%D0%B8%D1%86%D1%8B\" class=\"mention hashtag\" rel=\"tag\">#<span>птицы</span></a> <a href=\"https://mastodon.host/tags/%D0%B2%D0%BE%D1%80%D0%BE%D0%B1%D0%B5%D0%B9\" class=\"mention hashtag\" rel=\"tag\">#<span>воробей</span></a></p><p><span class=\"h-card\"><a href=\"https://mastodonsocial.ru/@rf\" class=\"u-url mention\">@<span>rf</span></a></span> <span class=\"h-card\"><a href=\"https://mastodon.social/@russian_mastodon\" class=\"u-url mention\">@<span>russian_mastodon</span></a></span></p>";
		String expected="<p>Экстра-кадр: Чик-чик-чик-чирик!..</p>\n"+
				"<p><a href=\"https://mastodon.host/tags/photo\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>photo</span></a> <a href=\"https://mastodon.host/tags/birds\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>birds</span></a> <a href=\"https://mastodon.host/tags/sparrow\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>sparrow</span></a> <a href=\"https://mastodon.host/tags/%D1%84%D0%BE%D1%82%D0%BE\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>фото</span></a> <a href=\"https://mastodon.host/tags/%D0%BF%D1%82%D0%B8%D1%86%D1%8B\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>птицы</span></a> <a href=\"https://mastodon.host/tags/%D0%B2%D0%BE%D1%80%D0%BE%D0%B1%D0%B5%D0%B9\" class=\"mention hashtag\" rel=\"noopener ugc\" target=\"_blank\">#<span>воробей</span></a></p>\n"+
				"<p><span class=\"h-card\"><a href=\"https://mastodonsocial.ru/@rf\" class=\"u-url mention\" target=\"_blank\" rel=\"noopener ugc\">@<span>rf</span></a></span> <span class=\"h-card\"><a href=\"https://mastodon.social/@russian_mastodon\" class=\"u-url mention\" target=\"_blank\" rel=\"noopener ugc\">@<span>russian_mastodon</span></a></span></p>";

		String out=TextProcessor.postprocessPostHTMLForDisplay(TextProcessor.sanitizeHTML(in), false);
		assertEquals(expected.replaceAll("\\s*\n", ""), out.replaceAll("\\s*\n", ""));
	}

	@Test
	public void testTargetBlankIsAddedToExternalLinks(){
		String in="<a href=\"https://example.com\">External link</a>";
		String expected="<a href=\"https://example.com\" target=\"_blank\" rel=\"noopener ugc\">External link</a>";
		String out=TextProcessor.postprocessPostHTMLForDisplay(TextProcessor.sanitizeHTML(in), false);
		assertEquals(expected, out);
	}

	@Test
	public void testTargetBlankIsNotAddedToLocalLinks(){
		String inout="<a href=\"https://smithereen.local/someuser\" class=\"mention\">Some user</a>";
		assertEquals(inout, TextProcessor.sanitizeHTML(inout));
	}

	@Test
	public void testBadAttributesAreRemoved(){
		String in="<p onmouseover=\"alert('xss')\">Look I'm a hacker</p>";
		String expected="<p>Look I'm a hacker</p>";
		assertEquals(expected, TextProcessor.sanitizeHTML(in));
	}

	@Test
	public void testBadTagsAreRemoved(){
		String in="<div>Test</div>";
		String expected="Test";
		assertEquals(expected, TextProcessor.sanitizeHTML(in));
	}

	@Test
	public void testJavascriptUrlsAreRemoved(){
		String in="<a href=\"javascript:alert('xss')\">Click for XSS</a>";
		String expected="<a>Click for XSS</a>";
		assertEquals(expected, TextProcessor.sanitizeHTML(in));
	}

	@Test
	public void testUnsupportedUrlSchemesAreRemoved(){
		String in="<a href=\"ftp://example.com/kitten.jpg\">Test</a>";
		String expected="<a>Test</a>";
		assertEquals(expected, TextProcessor.sanitizeHTML(in));
	}

	@Test
	public void testRelativeUrlsAreExpended(){
		String in="<a href=\"/test.html\">Relative link</a>";
		String expected="<a href=\"https://example.com/test.html\" target=\"_blank\" rel=\"noopener ugc\">Relative link</a>";
		assertEquals(expected, TextProcessor.postprocessPostHTMLForDisplay(TextProcessor.sanitizeHTML(in, URI.create("https://example.com/")), false));
	}

	@Test
	public void testPreprocessingSimple(){
		String in="This is a test";
		String expected="<p>This is a test</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingParagraphsSimple(){
		String in="This is paragraph 1.\n\nThis is paragraph 2.";
		String expected="<p>This is paragraph 1.</p>\n<p>This is paragraph 2.</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingParagraphsWithTags(){
		String in="This is <b>paragraph 1.\n\nThis </b>is paragraph 2.";
		String expected="<p>This is <b>paragraph 1.</b></p>\n<p><b>This </b>is paragraph 2.</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingParagraphsWithTagsWithAttrs(){
		String in="This is <a href=\"https://example.com\" onclick=\"alert('xss')\">paragraph <i>1.\n\nThis</i> </a>is paragraph 2.";
		String expected="<p>This is <a href=\"https://example.com\">paragraph <i>1.</i></a></p>\n<p><a href=\"https://example.com\"><i>This</i> </a>is paragraph 2.</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingPreIsUnmodified(){
		String in="""
			<pre>Line 1
				<i>Line 2</i>
			
			
					Line 3
						Line 4
			</pre>""";
		assertEquals(in, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingXssSimple(){
		String in="<script>alert('xss');</script>Test";
		String expected="<p>Test</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingXssWhateverTheFuckWasThat(){
		String in="'\"/test/></title/</script/<style/--><script>document.body.innerHTML = '<div style=\"font-size: 100px; color: red;\">ПРИВЕТ =)</div>'</script>`'<!--";
		assertFalse(TextProcessor.preprocessPostHTML(in, null).contains("script"));
	}

	@Test
	public void testPreprocessingLineBreaksAndParagraphs(){
		String in="""
				Test!
				Test!
				
				<b>Test!
				Test!</b>
				
				
				
				Test!
				Test!
				<p>Test!
				</p>""";
		String expected="<p>Test!<br>Test!</p>\n<p><b>Test!<br>Test!</b></p>\n<p>Test!<br>Test!</p>\n<p>Test!</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingLinks(){
		String in="https://example.com/";
		String expected="<p><a href=\"https://example.com/\">https://example.com/</a></p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}

	@Test
	public void testPreprocessingLinksWithPunctuationAtEnd(){
		String in="https://x.com/somepath.\nhttps://x.com/somepath?\nhttps://x.com/somepath?query=string?\nhttps://x.com/somepath!\nhttps://x.com/somepath:";
		String expected="<p><a href=\"https://x.com/somepath\">https://x.com/somepath</a>.<br><a href=\"https://x.com/somepath\">https://x.com/somepath</a>?<br><a href=\"https://x.com/somepath?query=string\">https://x.com/somepath?query=string</a>?<br><a href=\"https://x.com/somepath\">https://x.com/somepath</a>!<br><a href=\"https://x.com/somepath\">https://x.com/somepath</a>:</p>";
		assertEquals(expected, TextProcessor.preprocessPostHTML(in, null));
	}
}
