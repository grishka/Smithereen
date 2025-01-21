package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.Post;

public class LocalPostNote extends Note{
	public final Post nativePost;

	public LocalPostNote(Post nativePost){
		this.nativePost=nativePost;
	}

	@Override
	public Post asNativePost(ApplicationContext context){
		return nativePost;
	}
}
