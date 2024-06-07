package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.Post;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public non-sealed class Note extends NoteOrQuestion{

	@Override
	public Post asNativePost(ApplicationContext context){
		Post post=super.asNativePost(context);
		if(StringUtils.isNotEmpty(name)){
			post.text="<p><b>"+TextProcessor.stripHTML(name, false)+"</b></p>\n"+post.text;
		}
		return post;
	}

	@Override
	public String getType(){
		return "Note";
	}
}
