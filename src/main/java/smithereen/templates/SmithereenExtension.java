package smithereen.templates;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.tokenParser.TokenParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmithereenExtension extends AbstractExtension{
	@Override
	public Map<String, Function> getFunctions(){
		return Map.of(
			"L", new LangFunction(),
			"LD", new LangDateFunction(),
			"renderAttachments", new RenderAttachmentsFunction(),
			"json", new JsonFunction(),
			"formatTime", new FormatTimeFunction(),
			"getTime", new InstantToTimeFunction(),
			"getDate", new InstantToDateFunction(),
			"describeAttachments", new DescribeAttachmentsFunction(),
			"addQueryParams", new AddQueryParamsFunction(),
			"randomString", new RandomStringFunction()
		);
	}

	@Override
	public Map<String, Filter> getFilters(){
		return Map.of(
			"pictureForAvatar", new PictureForAvatarFilter(),
			"pictureForPhoto", new PictureForPhotoFilter(),
			"postprocessHTML", new PostprocessHTMLFilter(),
			"forceEscape", new ForceEscapeFilter(),
			"nl2br", new Nl2brFilter(),
			"truncateText", new TruncateTextFilter(),
			"stripHTML", new StripHTMLFilter()
		);
	}

	@Override
	public List<TokenParser> getTokenParsers(){
		return List.of(new EnqueueScriptTokenParser());
	}
}
