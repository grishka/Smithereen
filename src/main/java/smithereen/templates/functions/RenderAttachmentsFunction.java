package smithereen.templates.functions;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import org.unbescape.html.HtmlEscape;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.Group;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.AudioAttachment;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.attachments.SizedAttachment;
import smithereen.model.attachments.VideoAttachment;
import smithereen.lang.Lang;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.templates.MediaLayoutHelper;
import smithereen.text.TextProcessor;
import smithereen.util.BlurHash;
import spark.utils.StringUtils;

public class RenderAttachmentsFunction implements Function{

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
			float aspect;
			MediaLayoutHelper.TiledLayoutResult tiledLayout;
			if(sized.size()==1){
				SizedAttachment sa=sized.getFirst();
				aspect=sa.isSizeKnown() ? Math.max(0.5f, Math.min(3f, (sa.getWidth()/(float)sa.getHeight()))) : 1f;
				tiledLayout=null;
			}else{
				tiledLayout=MediaLayoutHelper.makeLayout(sized);
				aspect=tiledLayout.width/(float)tiledLayout.height;
			}
			int pseudoWidth, pseudoHeight;
			if(aspect>1f){
				pseudoWidth=1000;
				pseudoHeight=Math.round(1000f/aspect);
			}else{
				pseudoWidth=Math.round(1000f*aspect);
				pseudoHeight=1000;
			}

			String gridStyle="";
			if(tiledLayout!=null){
				gridStyle="grid-template: "+Arrays.stream(tiledLayout.rowSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))
						+" / "+Arrays.stream(tiledLayout.columnSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))+";";
			}

			lines.add(String.format(Locale.US, "<span class=\"aspectWrapper\">" +
					"<svg class=\"pseudoImage\" width=\"%d\" height=\"%d\"></svg>" +
					"<div class=\"positioner\">" +
					"<div style=\"padding-top: %.2f%%\">" +
					"<div class=\"safariSucks\">" +
					"<div class=\"aspectBox\" style=\"%s\">", pseudoWidth, pseudoHeight, (1f/aspect)*100f, gridStyle));

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
			lines.add("</div></div></div></div></span>");
		}

		// Now do non-sized attachments
		for(Attachment att:attachments){
			if(att instanceof SizedAttachment)
				continue;
			if(att instanceof VideoAttachment va){
				lines.add("<video src=\""+HtmlEscape.escapeHtml4Xml(va.url.toString())+"\" controls></video>");
			}else if(att instanceof AudioAttachment aa){
				lines.add("<audio src=\""+HtmlEscape.escapeHtml4Xml(aa.url.toString())+"\" preload=\"none\" controls></audio>");
			}
		}

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
			lines.add("<a class=\"photo\" href=\""+href+"\" "+attrs+">"+photo.image.generateHTML(type, null, styleAttr, 0, 0, true)+"</a>");
		}
	}

}
