package smithereen.text;

import org.jsoup.safety.Safelist;

public class Whitelist extends Safelist{
	public Whitelist(){
		super();
	}

	public Whitelist(Safelist copy){
		super(copy);
	}

	@Override
	public Whitelist addTags(String... tags){
		super.addTags(tags);
		return this;
	}

	@Override
	public Whitelist removeTags(String... tags){
		super.removeTags(tags);
		return this;
	}

	@Override
	public Whitelist addAttributes(String tag, String... attributes){
		super.addAttributes(tag, attributes);
		return this;
	}

	@Override
	public Whitelist removeAttributes(String tag, String... attributes){
		super.removeAttributes(tag, attributes);
		return this;
	}

	@Override
	public Whitelist addEnforcedAttribute(String tag, String attribute, String value){
		super.addEnforcedAttribute(tag, attribute, value);
		return this;
	}

	@Override
	public Whitelist removeEnforcedAttribute(String tag, String attribute){
		super.removeEnforcedAttribute(tag, attribute);
		return this;
	}

	@Override
	public Whitelist preserveRelativeLinks(boolean preserve){
		super.preserveRelativeLinks(preserve);
		return this;
	}

	@Override
	public Whitelist addProtocols(String tag, String attribute, String... protocols){
		super.addProtocols(tag, attribute, protocols);
		return this;
	}

	@Override
	public Whitelist removeProtocols(String tag, String attribute, String... removeProtocols){
		super.removeProtocols(tag, attribute, removeProtocols);
		return this;
	}
}
