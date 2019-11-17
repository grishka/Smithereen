package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.net.URI;

import smithereen.activitypub.ContextCollector;

public abstract class ActivityPubLink extends ActivityPubObject{
	public URI href;

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		obj.put("href", href);
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception{
		super.parseActivityPubObject(obj);
		href=tryParseURL(obj.getString("href"));
		return this;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder(getType()+"{");
		sb.append(super.toString());
		if(href!=null){
			sb.append("href=");
			sb.append(href);
		}
		sb.append('}');
		return sb.toString();
	}
}
