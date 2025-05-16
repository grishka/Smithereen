package smithereen.activitypub.objects;

import smithereen.model.ActorStatus;

public class LocalActivityPubActorStatus extends ActivityPubActorStatus{
	private final ActorStatus status;

	public LocalActivityPubActorStatus(ActorStatus status){
		this.status=status;
	}

	@Override
	public ActorStatus asNativeStatus(){
		return status;
	}
}
