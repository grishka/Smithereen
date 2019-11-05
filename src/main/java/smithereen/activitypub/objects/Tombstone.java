package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.util.Date;

import smithereen.Utils;
import smithereen.activitypub.ContextCollector;

public class Tombstone extends ActivityPubObject{

	public String formerType;
	public Date deleted;

	@Override
	public String getType(){
		return "Tombstone";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(formerType!=null)
			obj.put("formerType", formerType);
		if(deleted!=null)
			obj.put("deleted", Utils.formatDateAsISO(deleted));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception{
		super.parseActivityPubObject(obj);
		formerType=obj.optString("formerType", null);
		if(obj.has("deleted"))
			deleted=tryParseDate(obj.getString("deleted"));
		return this;
	}
}
