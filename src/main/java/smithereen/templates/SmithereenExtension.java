package smithereen.templates;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;

import java.util.HashMap;
import java.util.Map;

public class SmithereenExtension extends AbstractExtension{
	@Override
	public Map<String, Function> getFunctions(){
		HashMap<String, Function> r=new HashMap<>();
		r.put("L", new LangFunction());
		r.put("LD", new LangDateFunction());
		r.put("LG", new LangGenderedFunction());
		r.put("LP", new LangPluralFunction());
		r.put("renderAttachments", new RenderAttachmentsFunction());
		r.put("json", new JsonFunction());
		return r;
	}

	@Override
	public Map<String, Filter> getFilters(){
		HashMap<String, Filter> r=new HashMap<>();
		r.put("pictureForAvatar", new PictureForAvatarFilter());
		r.put("postprocessHTML", new PostprocessHTMLFilter());
		return r;
	}
}
