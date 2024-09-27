package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.Post;

public class LocalPostNoteTombstone extends NoteTombstone{
	public final Post nativePost;

	public LocalPostNoteTombstone(Post nativePost){
		this.nativePost=nativePost;
	}

	@Override
	public Post asNativePost(ApplicationContext context){
		return nativePost;
	}
}
