package smithereen.activitypub;

import java.net.URI;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.data.User;

public class ActivityHandlerContext{
	private String origRequestBody;
	public final Actor ldSignatureOwner;
	public final Actor httpSignatureOwner;
	public final ApplicationContext appContext;

	public ActivityHandlerContext(ApplicationContext appContext, String origRequestBody, Actor ldSignatureOwner, Actor httpSignatureOwner){
		this.appContext=appContext;
		this.origRequestBody=origRequestBody;
		this.ldSignatureOwner=ldSignatureOwner;
		this.httpSignatureOwner=httpSignatureOwner;
	}

	public void forwardActivity(List<URI> inboxes, User signer){
		if(ldSignatureOwner==null)
			throw new IllegalStateException("Can't forward an activity without LD-signature");
		ActivityPubWorker.getInstance().forwardActivity(origRequestBody, signer, inboxes, ldSignatureOwner.activityPubID.getHost());
	}
}
