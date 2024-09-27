package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.Post;

public class LocalPostQuestion extends Question{
	public final Post nativePost;

	public LocalPostQuestion(Post nativePost){
		this.nativePost=nativePost;
	}

	@Override
	public Post asNativePost(ApplicationContext context){
		return nativePost;
	}
}
