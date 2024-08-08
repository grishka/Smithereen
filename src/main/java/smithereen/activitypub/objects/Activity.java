package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.model.ActivityPubRepresentable;
import smithereen.util.UriBuilder;

public abstract class Activity extends ActivityPubObject{

	public LinkOrObject actor;
	public LinkOrObject object;
	public LinkOrObject target;
	public List<LinkOrObject> result;
	public LinkOrObject origin;
	public LinkOrObject instrument;

	public abstract String getType();

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.add("actor", actor.serialize(serializerContext));
		if(object!=null)
			obj.add("object", object.serialize(serializerContext));
		if(target!=null)
			obj.add("target", target.serialize(serializerContext));
		if(result!=null && !result.isEmpty())
			obj.add("result", serializeLinkOrObjectArray(result, serializerContext));
		if(origin!=null)
			obj.add("origin", origin.serialize(serializerContext));
		if(instrument!=null)
			obj.add("instrument", instrument.serialize(serializerContext));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		actor=tryParseLinkOrObject(Objects.requireNonNull(obj.get("actor"), "actor must not be null"), parserContext);
		object=tryParseLinkOrObject(Objects.requireNonNull(obj.get("object"), "object must not be null"), parserContext);
		target=tryParseLinkOrObject(obj.get("target"), parserContext);
		result=tryParseArrayOfLinksOrObjects(obj.get("result"), parserContext);
		origin=tryParseLinkOrObject(obj.get("origin"), parserContext);
		instrument=tryParseLinkOrObject(obj.get("instrument"), parserContext);
		return this;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder(getType());
		sb.append('{');
		sb.append(super.toString());
		if(actor!=null){
			sb.append("actor=");
			sb.append(actor);
		}
		if(object!=null){
			sb.append(", object=");
			sb.append(object);
		}
		if(target!=null){
			sb.append(", target=");
			sb.append(target);
		}
		if(result!=null){
			sb.append(", result=");
			sb.append(result);
		}
		if(origin!=null){
			sb.append(", origin=");
			sb.append(origin);
		}
		if(instrument!=null){
			sb.append(", instrument=");
			sb.append(instrument);
		}
		sb.append('}');
		return sb.toString();
	}

	public <T extends Activity> T withActorAndObjectLinks(Actor actor, ActivityPubObject object){
		this.actor=new LinkOrObject(actor.activityPubID);
		this.object=new LinkOrObject(object.activityPubID);
		//noinspection unchecked
		return (T) this;
	}

	public <T extends Activity> T withActorAndObjectLinks(Actor actor, ActivityPubRepresentable object){
		this.actor=new LinkOrObject(actor.activityPubID);
		this.object=new LinkOrObject(object.getActivityPubID());
		//noinspection unchecked
		return (T) this;
	}

	public <T extends Activity> T withActorLinkAndObject(Actor actor, ActivityPubObject object){
		this.actor=new LinkOrObject(actor.activityPubID);
		this.object=new LinkOrObject(object);
		//noinspection unchecked
		return (T) this;
	}

	public <T extends Activity> T withActorFragmentID(String fragment){
		activityPubID=new UriBuilder(Objects.requireNonNull(actor.link)).fragment(fragment).build();
		//noinspection unchecked
		return (T) this;
	}

	public <T extends Activity> T withTarget(URI target){
		this.target=new LinkOrObject(target);
		//noinspection unchecked
		return (T) this;
	}
}
