package smithereen.templates;

import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.extension.escaper.SafeString;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import smithereen.data.PhotoSize;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.attachments.VideoAttachment;
import smithereen.storage.MediaStorageUtils;

public class RenderAttachmentsFunction implements Function{

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		List<Attachment> attachment=(List<Attachment>) args.get("attachments");

		ArrayList<String> lines=new ArrayList<>();
		for(Attachment obj:attachment){
			// TODO content warnings

			if(obj instanceof PhotoAttachment){
				PhotoAttachment photo=(PhotoAttachment)obj;
				PhotoSize jpeg1x, jpeg2x, webp1x, webp2x;
				jpeg1x=MediaStorageUtils.findBestPhotoSize(photo.sizes, PhotoSize.Format.JPEG, PhotoSize.Type.SMALL);
				jpeg2x=MediaStorageUtils.findBestPhotoSize(photo.sizes, PhotoSize.Format.JPEG, PhotoSize.Type.MEDIUM);
				webp1x=MediaStorageUtils.findBestPhotoSize(photo.sizes, PhotoSize.Format.WEBP, PhotoSize.Type.SMALL);
				webp2x=MediaStorageUtils.findBestPhotoSize(photo.sizes, PhotoSize.Format.WEBP, PhotoSize.Type.MEDIUM);
				lines.add("<picture>" +
						"<source srcset=\""+webp1x.src+", "+webp2x.src+" 2x\" type=\"image/webp\"/>" +
						"<source srcset=\""+jpeg1x.src+", "+jpeg2x.src+" 2x\" type=\"image/jpeg\"/>" +
						"<img src=\""+jpeg1x.src+"\"/>" +
						"</picture>");
			}else if(obj instanceof VideoAttachment){
				lines.add("<video src=\""+((VideoAttachment) obj).url+"\" controls></video>");
			}
		}
		if(!lines.isEmpty()){
			lines.add("</div>");
			lines.add(0, "<div class=\"postAttachments\">");
		}
		return new SafeString(String.join("\n", lines));
	}

	@Override
	public List<String> getArgumentNames(){
		return Collections.singletonList("attachments");
	}
}
