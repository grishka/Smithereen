package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.AudioAttachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.attachments.VideoAttachment;
import smithereen.lang.Lang;

public class DescribeAttachmentsFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		Lang l=Lang.get(context.getLocale());
		List<Attachment> attachments=(List<Attachment>) args.get("attachments");
		int photoCount=0, videoCount=0, audioCount=0;
		for(Attachment a:attachments){
			if(a instanceof PhotoAttachment){
				photoCount++;
			}else if(a instanceof VideoAttachment){
				videoCount++;
			}else if(a instanceof AudioAttachment){
				audioCount++;
			}
		}
		ArrayList<String> parts=new ArrayList<>();
		if(photoCount>0){
			parts.add(photoCount>1 ? l.get("content_type_X_photos", Map.of("count", photoCount)) : l.get("content_type_photo"));
		}
		if(videoCount>0){
			parts.add(videoCount>1 ? l.get("content_type_X_videos", Map.of("count", videoCount)) : l.get("content_type_video"));
		}
		if(audioCount>0){
			parts.add(audioCount>1 ? l.get("content_type_X_audios", Map.of("count", audioCount)) : l.get("content_type_audio"));
		}
		return String.join(", ", parts);
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("attachments");
	}
}
