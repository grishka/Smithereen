package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;

public non-sealed class NoteTombstone extends NoteOrQuestion{
	@Override
	public String getType(){
		return "Tombstone";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		super.asActivityPubObject(obj, contextCollector);

		obj.addProperty("formerType", "Note");

		return obj;
	}
}
