package smithereen.activitypub;

import smithereen.data.ForeignUser;

public class ActivityHandlerContext{
	private String origRequestBody;
	public final ForeignUser ldSignatureOwner;
	public final ForeignUser httpSignatureOwner;

	public ActivityHandlerContext(String origRequestBody, ForeignUser ldSignatureOwner, ForeignUser httpSignatureOwner){
		this.origRequestBody=origRequestBody;
		this.ldSignatureOwner=ldSignatureOwner;
		this.httpSignatureOwner=httpSignatureOwner;
	}
}
