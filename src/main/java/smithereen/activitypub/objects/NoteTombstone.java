package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;

public non-sealed class NoteTombstone extends NoteOrQuestion{
	@Override
	public String getType(){
		return "Tombstone";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		obj.addProperty("formerType", "Note");

		return obj;
	}
}
