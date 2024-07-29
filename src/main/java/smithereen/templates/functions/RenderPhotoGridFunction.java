package smithereen.templates.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.Utils;
import smithereen.model.SizedImage;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.templates.MediaLayoutHelper;
import smithereen.text.TextProcessor;

public class RenderPhotoGridFunction implements Function{
	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber){
		List<Photo> photos=(List<Photo>) args.get("photos");
		String list=(String) args.get("list");
		MediaLayoutHelper.TiledLayoutResult layout;
		ArrayList<String> lines=new ArrayList<>();
		String gridStyle="";
		int totalW, totalH;
		if(photos.size()>1){
			layout=MediaLayoutHelper.makeLayout(photos);
			totalW=layout.width;
			totalH=layout.height;
			gridStyle="grid-template: "+Arrays.stream(layout.rowSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))
					+" / "+Arrays.stream(layout.columnSizes).mapToObj(sz->(sz+"fr")).collect(Collectors.joining(" "))+";";
		}else{
			layout=null;
			Photo photo=photos.getFirst();
			totalW=photo.getWidth();
			totalH=photo.getHeight();
		}
		lines.add("<div class=\"feedPhotoGrid\" style=\"aspect-ratio: "+totalW+"/"+totalH+";"+gridStyle+"\">");
		int i=0;
		for(Photo ph:photos){
			String style="";
			int size;
			if(layout!=null){
				MediaLayoutHelper.TiledLayoutResult.Tile tile=layout.tiles[i];
				if(tile.rowSpan>1){
					style+="grid-row: span "+tile.rowSpan+";";
				}
				if(tile.colSpan>1){
					style+="grid-column: span "+tile.colSpan+";";
				}
				size=Math.round(Math.max(tile.width*510, tile.height*510));
			}else{
				size=510;
			}
			SizedImage.Type type;
			if(size<=100){
				type=SizedImage.Type.PHOTO_THUMB_SMALL;
			}else if(size<=320){
				type=SizedImage.Type.PHOTO_THUMB_MEDIUM;
			}else{
				type=SizedImage.Type.PHOTO_SMALL;
			}

			PhotoViewerInlineData data=new PhotoViewerInlineData(i, list, ph.image.getURLsForPhotoViewer());
			String attrs="onclick=\"return openPhotoViewer(this)\" data-pv=\""+TextProcessor.escapeHTML(Utils.gson.toJson(data))+"\" data-pv-ctx=\""+list+"\"";

			lines.add("<a href=\""+ph.getURL()+"\" style=\""+style+"\" "+attrs+">");
			lines.add(ph.image.generateHTML(type, null, "", 0, 0, true));
			lines.add("</a>");
			i++;
		}
		lines.add("</div>");
		return new SafeString(String.join("\n", lines));
	}

	@Override
	public List<String> getArgumentNames(){
		return List.of("photos", "list");
	}
}
