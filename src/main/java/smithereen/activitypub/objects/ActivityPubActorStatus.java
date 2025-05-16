package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;

import smithereen.activitypub.SerializerContext;
import smithereen.model.ActorStatus;
import smithereen.util.UriBuilder;

public class ActivityPubActorStatus extends ActivityPubObject{
	@Override
	public String getType(){
		return "ActorStatus";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		serializerContext.addSmAlias("ActorStatus");
		return super.asActivityPubObject(obj, serializerContext);
	}

	public static ActivityPubActorStatus fromNativeStatus(ActorStatus status, Actor owner){
		ActivityPubActorStatus s=new LocalActivityPubActorStatus(status);
		s.activityPubID=status.apId()==null ? new UriBuilder(owner.activityPubID).appendPath("statuses").appendPath(status.updatedAt().getEpochSecond()+"").build() : status.apId();
		s.attributedTo=owner.activityPubID;
		s.published=status.updatedAt();
		s.endTime=status.expiry();
		s.content=status.text();
		return s;
	}

	public ActorStatus asNativeStatus(){
		return new ActorStatus(
				content,
				published==null ? Instant.now() : published,
				endTime,
				activityPubID
		);
	}
}
