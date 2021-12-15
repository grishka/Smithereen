package smithereen.templates;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.tokenParser.TokenParser;

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
			"getDate", new InstantToDateFunction()
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
			"truncateText", new TruncateTextFilter()
		);
	}

	@Override
	public List<TokenParser> getTokenParsers(){
		return List.of(new EnqueueScriptTokenParser());
	}
}
