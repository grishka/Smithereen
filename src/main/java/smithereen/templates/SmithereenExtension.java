package smithereen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.tokenParser.TokenParser;
import smithereen.templates.filters.ForceEscapeFilter;
import smithereen.templates.filters.NameFilter;
import smithereen.templates.filters.Nl2brFilter;
import smithereen.templates.filters.PictureForAvatarFilter;
import smithereen.templates.filters.PictureForPhotoFilter;
import smithereen.templates.filters.PostprocessHTMLFilter;
import smithereen.templates.filters.StripHTMLFilter;
import smithereen.templates.filters.TruncateTextFilter;
import smithereen.templates.functions.AddQueryParamsFunction;
import smithereen.templates.functions.ArraysEqualFunction;
import smithereen.templates.functions.DescribeAttachmentsFunction;
import smithereen.templates.functions.FormatTimeFunction;
import smithereen.templates.functions.InlineTextResourceFunction;
import smithereen.templates.functions.InstantToDateFunction;
import smithereen.templates.functions.InstantToTimeFunction;
import smithereen.templates.functions.JsonFunction;
import smithereen.templates.functions.LangDateFunction;
import smithereen.templates.functions.LangFunction;
import smithereen.templates.functions.ProfileRelFunction;
import smithereen.templates.functions.ProfileUrlFunction;
import smithereen.templates.functions.RandomStringFunction;
import smithereen.templates.functions.RenderAttachmentsFunction;
import smithereen.templates.functions.RenderPhotoGridFunction;

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
		f.put("profileRel", new ProfileRelFunction());
		f.put("arraysEqual", new ArraysEqualFunction());
		f.put("renderPhotoGrid", new RenderPhotoGridFunction());
		f.put("inlineTextResource", new InlineTextResourceFunction());
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
