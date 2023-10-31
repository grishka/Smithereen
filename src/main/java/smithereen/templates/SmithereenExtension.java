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
		Map<String, Function> f=new HashMap<>();
		f.put("L", new LangFunction());
		f.put("LD", new LangDateFunction());
		f.put("renderAttachments", new RenderAttachmentsFunction());
		f.put("json", new JsonFunction());
		f.put("formatTime", new FormatTimeFunction());
		f.put("getTime", new InstantToTimeFunction());
		f.put("getDate", new InstantToDateFunction());
		f.put("describeAttachments", new DescribeAttachmentsFunction());
		f.put("addQueryParams", new AddQueryParamsFunction());
		f.put("randomString", new RandomStringFunction());
		f.put("profileURL", new ProfileUrlFunction());
		return f;
	}

	@Override
	public Map<String, Filter> getFilters(){
		Map<String, Filter> f=new HashMap<>();
		f.put("pictureForAvatar", new PictureForAvatarFilter());
		f.put("pictureForPhoto", new PictureForPhotoFilter());
		f.put("postprocessHTML", new PostprocessHTMLFilter());
		f.put("forceEscape", new ForceEscapeFilter());
		f.put("nl2br", new Nl2brFilter());
		f.put("truncateText", new TruncateTextFilter());
		f.put("stripHTML", new StripHTMLFilter());
		f.put("name", new NameFilter());
		return f;
	}

	@Override
	public List<TokenParser> getTokenParsers(){
		return List.of(new EnqueueScriptTokenParser());
	}
}
