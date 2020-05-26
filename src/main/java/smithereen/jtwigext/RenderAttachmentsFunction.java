package smithereen.jtwigext;

import org.jtwig.escape.EscapeEngine;
import org.jtwig.escape.NoneEscapeEngine;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.render.context.RenderContextHolder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.PhotoSize;
import smithereen.data.attachments.Attachment;
import smithereen.data.attachments.PhotoAttachment;
import smithereen.data.attachments.VideoAttachment;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;

public class RenderAttachmentsFunction extends SimpleJtwigFunction{

	@Override
	public String name(){
		return "renderAttachments";
	}

	@Override
	public Object execute(FunctionRequest functionRequest){
		functionRequest.minimumNumberOfArguments(1);
		RenderContextHolder.get().set(EscapeEngine.class, NoneEscapeEngine.instance());

		Object arg=functionRequest.get(0);
		if(!(arg instanceof List))
			return "";
		List<Attachment> attachment=(List<Attachment>) functionRequest.get(0);

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
		return String.join("\n", lines);
	}
}
