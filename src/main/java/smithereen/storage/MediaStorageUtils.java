package smithereen.storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.libvips.VImage;
import spark.utils.StringUtils;

public class MediaStorageUtils{

	public static long writeResizedWebpImage(VImage img, int widthOrSize, int height, int quality, String keyHex, File basePath, int[] outSize) throws IOException{
		File file=new File(basePath, keyHex+".webp");
		double factor;
		if(height==0){
			factor=(double) widthOrSize/(double) Math.max(img.getWidth(), img.getHeight());
		}else{
			factor=Math.min((double)widthOrSize/(double)img.getWidth(), (double)height/(double)img.getHeight());
		}
		if(factor>1.0){
			img.writeToFile(new String[]{file.getAbsolutePath()+"[Q="+quality+",strip=true]"});
			outSize[0]=img.getWidth();
			outSize[1]=img.getHeight();
		}else{
			VImage resized=img.resize(factor);
			try{
				resized.writeToFile(new String[]{file.getAbsolutePath()+"[Q="+quality+",strip=true]"});
				outSize[0]=resized.getWidth();
				outSize[1]=resized.getHeight();
			}finally{
				resized.release();
			}
		}
		return file.length();
	}

	public static void deleteAttachmentFiles(List<ActivityPubObject> attachments){
		for(ActivityPubObject o:attachments){
			if(o instanceof Document)
				deleteAttachmentFiles((Document)o);
		}
	}

	public static void deleteAttachmentFiles(Document doc){
		if(doc instanceof LocalImage){
			LocalImage img=(LocalImage) doc;
			File file=new File(Config.uploadPath, img.path+"/"+img.localID+".webp");
			if(file.exists())
				file.delete();
			else
				System.out.println(file.getAbsolutePath()+" does not exist");
		}
	}

	public static JSONObject serializeAttachment(ActivityPubObject att){
		JSONObject o=att.asActivityPubObject(null, new ContextCollector());
		if(att instanceof Document){
			Document d=(Document) att;
			if(StringUtils.isNotEmpty(d.localID)){
				o.put("_lid", d.localID);
				if(d instanceof LocalImage){
					LocalImage im=(LocalImage) d;
					JSONArray sizes=new JSONArray();
					sizes.put(im.width);
					sizes.put(im.height);
					o.put("_sz", sizes);
					if(im.path!=null)
						o.put("_p", im.path);
					o.put("type", "_LocalImage");
				}
				o.remove("url");
				o.remove("id");
				o.remove("width");
				o.remove("height");
				o.remove("mediaType");
			}
		}
		return o;
	}
}
