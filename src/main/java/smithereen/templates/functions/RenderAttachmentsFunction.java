package smithereen.templates.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unbescape.html.HtmlEscape;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.lang.Lang;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.PostLikeObject;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.AudioAttachment;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.attachments.SizedAttachment;
import smithereen.model.attachments.VideoAttachment;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.viewmodel.AudioAttachmentViewModel;
import smithereen.templates.MediaLayoutHelper;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.util.BlurHash;
import spark.utils.StringUtils;

public class RenderAttachmentsFunction implements Function{

	private static final Logger log=LoggerFactory.getLogger(RenderAttachmentsFunction.class);

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext evaluationContext, int lineNumber){
		AttachmentHostContentObject obj=(AttachmentHostContentObject) args.get("object");
		List<Attachment> attachments=obj.getProcessedAttachments();
		String photoList=obj.getPhotoListID();
		String overrideLinks=(String) args.get("overrideLinks");
		String listURL=(String) args.get("listURL");
		for(Attachment a:attachments){
			if(a instanceof GraffitiAttachment ga){
				Actor owner=(Actor) args.get("owner");
				Lang lang=Lang.get(evaluationContext.getLocale());
				if(owner instanceof User u){
					ga.boxTitle=lang.get("graffiti_on_user_X_wall", Map.of("name", u.getFirstLastAndGender()));
				}else if(owner instanceof Group g){
					ga.boxTitle=lang.get("graffiti_on_group_X_wall", Map.of("name", g.name));
				}
			}
		}
		ArrayList<String> lines=new ArrayList<>();
		List<SizedAttachment> sized=attachments.stream().filter(a->a instanceof SizedAttachment).map(a->(SizedAttachment)a).limit(10).collect(Collectors.toList());
		if(!sized.isEmpty()){
			MediaLayoutHelper.TiledLayoutResult tiledLayout;
			int totalW, totalH;
			if(sized.size()==1){
				SizedAttachment sa=sized.getFirst();
				if(sa.isSizeKnown()){
					totalW=sa.getWidth();
					totalH=sa.getHeight();
				}else{
					totalW=totalH=1;
				}
				tiledLayout=null;
			}else{
				tiledLayout=MediaLayoutHelper.makeLayout(sized);
				totalW=tiledLayout.width;
				totalH=tiledLayout.height;
			}

			String gridStyle="";
			if(tiledLayout!=null){
				gridStyle="grid-template: "+Arrays.stream(tiledLayout.rowSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))
						+" / "+Arrays.stream(tiledLayout.columnSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))+";";
			}

			lines.add("<div class=\"aspectBoxW\"><div class=\"aspectBox\" style=\"aspect-ratio: "+totalW+"/"+totalH+";"+gridStyle+"\">");

			if(sized.size()==1){
				SizedAttachment sa=sized.getFirst();
				if(sa instanceof PhotoAttachment photo){
					renderPhotoAttachment(photo, lines, 510, overrideLinks, 0, photoList, listURL);
				}
			}else{
				int i=0;
				for(SizedAttachment att : sized){
					MediaLayoutHelper.TiledLayoutResult.Tile tile=tiledLayout.tiles[i];
					String cellStyle="";
					if(tile!=null){
						if(tile.rowSpan>1){
							cellStyle+="grid-row: span "+tile.rowSpan+";";
						}
						if(tile.colSpan>1){
							cellStyle+="grid-column: span "+tile.colSpan+";";
						}
					}
					lines.add("<div style=\""+cellStyle+"\">");
					if(att instanceof PhotoAttachment photo){
						renderPhotoAttachment(photo, lines, Math.round(Math.max(tile.width*510, tile.height*510)), overrideLinks, i, photoList, listURL);
					}
					lines.add("</div>");
					i++;
				}
			}
			lines.add("</div></div>");
		}

		// Now do non-sized attachments
		for(Attachment att:attachments){
			if(att instanceof VideoAttachment va)
				lines.add("<video src=\""+HtmlEscape.escapeHtml4Xml(va.url.toString())+"\" controls playsinline></video>");
		}
		renderAudioAttachments(attachments, obj, lines, evaluationContext);

		if(!lines.isEmpty()){
			lines.add("</div>");
			lines.addFirst("<div class=\"postAttachments\">");
		}
		return new SafeString(String.join("\n", lines));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("object", "owner", "overrideLinks", "listURL");
	}

	private void renderPhotoAttachment(PhotoAttachment photo, List<String> lines, int size, String overrideLinks, int index, String photoList, String listGetURL){
		SizedImage.Type type;
		if(size<=100){
			type=SizedImage.Type.PHOTO_THUMB_SMALL;
		}else if(size<=320){
			type=SizedImage.Type.PHOTO_THUMB_MEDIUM;
		}else{
			type=SizedImage.Type.PHOTO_SMALL;
		}

		String styleAttr=null;
		if(StringUtils.isNotEmpty(photo.blurHash)){
			styleAttr=String.format(Locale.US, "background-color: #%06X", BlurHash.decodeToSingleColor(photo.blurHash));
		}

		if(photo instanceof GraffitiAttachment ga){
			URI full=photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_ORIGINAL, SizedImage.Format.PNG);
			String href=overrideLinks!=null ? overrideLinks : full.toString();
			String attrs;
			if(overrideLinks!=null){
				attrs="target=\"_blank\"";
			}else{
				attrs="data-box-title=\""+TextProcessor.escapeHTML(ga.boxTitle)+"\" onclick=\"return showGraffitiBox(this)\"";
			}
			lines.add("<a class=\"graffiti\" href=\""+href+"\" "+attrs+"><img src=\""+full+"\" width=\""+GraffitiAttachment.WIDTH+"\" height=\""+GraffitiAttachment.HEIGHT+"\"/></a>");
		}else{
			String href=overrideLinks!=null ? overrideLinks : Objects.toString(photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_ORIGINAL, SizedImage.Format.JPEG));
			String attrs;
			if(overrideLinks!=null){
				attrs="target=\"_blank\"";
			}else{
				PhotoViewerInlineData data=new PhotoViewerInlineData(index, photoList, photo.image.getURLsForPhotoViewer());
				attrs="onclick=\"return openPhotoViewer(this)\" data-pv=\""+TextProcessor.escapeHTML(Utils.gson.toJson(data))+"\" data-pv-ctx=\""+photoList+"\"";
				if(listGetURL!=null){
					attrs+=" data-pv-url=\""+listGetURL+"\"";
				}
			}
			lines.add("<a class=\"photo\" href=\""+href+"\" "+attrs+">"+photo.image.generateHTML(type, null, styleAttr, 0, 0, true, photo.description)+"</a>");
		}
	}

	private void renderAudioAttachments(List<Attachment> attachments, AttachmentHostContentObject obj, List<String> lines, EvaluationContext evaluationContext){
		Lang l=Lang.get(evaluationContext.getLocale());

		int audioIndex=0; // TODO: It would be better use absolute IDs for audio objects.
		//  If we implement storing information about audios in the database,
		//  we can use the entry's primary key for that.
		List<AudioAttachmentViewModel> viewModels=new ArrayList<>();
		for(Attachment att: attachments){
			if(att instanceof AudioAttachment audio){
				long hostID;
				switch(obj){
					case PostLikeObject p -> hostID=p.getObjectID();
					case MailMessage m -> hostID=m.id;
				}
				Duration duration=null; // TODO: Try to parse it from the activity object or from ID3 tags
				String artist=l.get("audio_unknown_artist"); // TODO: Try to parse it from the activity object or from ID3 tags
				String title=l.get("audio_unknown_title"); // TODO: Try to parse it from the activity object or from ID3 tags
				AudioAttachmentViewModel viewModel=new AudioAttachmentViewModel(
						hostID+"_"+audioIndex++,
						l.formatDuration(duration),
						duration==null ? -1 : duration.getSeconds(),
						artist,
						title,
						audio.url
				);
				viewModels.add(viewModel);
			}
		}
		if(viewModels.isEmpty()) return;
		PebbleTemplate template=Templates.getTemplate("post_audio_attachment_list");
		StringWriter writer=new StringWriter();
		boolean isLayer=Boolean.TRUE.equals(evaluationContext.getVariable("isPostInLayer"));
		try{
			template.evaluate(writer, Map.of("audios", viewModels, "randomID", evaluationContext.getVariable("randomID"), "isPostInLayer", isLayer), evaluationContext.getLocale());
		}catch(IOException e){
			log.error("Failure while evaluating template", e);
		}
		lines.add(writer.toString());
	}
}
