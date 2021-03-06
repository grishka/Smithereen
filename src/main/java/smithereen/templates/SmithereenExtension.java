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
		HashMap<String, Function> r=new HashMap<>();
		r.put("L", new LangFunction());
		r.put("LD", new LangDateFunction());
		r.put("LG", new LangGenderedFunction());
		r.put("LP", new LangPluralFunction());
		r.put("LI", new LangInflectedFunction());
		r.put("renderAttachments", new RenderAttachmentsFunction());
		r.put("json", new JsonFunction());
		r.put("formatTime", new FormatTimeFunction());
		return r;
	}

	@Override
	public Map<String, Filter> getFilters(){
		HashMap<String, Filter> r=new HashMap<>();
		r.put("pictureForAvatar", new PictureForAvatarFilter());
		r.put("postprocessHTML", new PostprocessHTMLFilter());
		r.put("forceEscape", new ForceEscapeFilter());
		r.put("nl2br", new Nl2brFilter());
		return r;
	}

	@Override
	public List<TokenParser> getTokenParsers(){
		return List.of(new EnqueueScriptTokenParser());
	}
}
