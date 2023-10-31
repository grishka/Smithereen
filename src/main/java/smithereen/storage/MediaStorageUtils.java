package smithereen.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.libvips.VipsImage;
import spark.utils.StringUtils;

public class MediaStorageUtils{
	private static final Logger LOG=LoggerFactory.getLogger(MediaStorageUtils.class);
	public static final int QUALITY_LOSSLESS=-1;

	public static long writeResizedWebpImage(VipsImage img, int widthOrSize, int height, int quality, String keyHex, File basePath, int[] outSize) throws IOException{
		File file=new File(basePath, keyHex+".webp");
		double factor;
		if(height==0){
			factor=(double) widthOrSize/(double) Math.max(img.getWidth(), img.getHeight());
		}else{
			factor=Math.min((double)widthOrSize/(double)img.getWidth(), (double)height/(double)img.getHeight());
		}

		ArrayList<String> args=new ArrayList<>();
		if(quality==QUALITY_LOSSLESS){
			args.add("lossless=true");
		}else{
			args.add("Q="+quality);
		}

		boolean strip=!img.hasColorProfile();
		if(!strip){
			for(String key:img.getFields()){
				if(!"icc-profile-data".equals(key))
					img.removeField(key);
			}
		}else{
			args.add("strip=true");
		}

		if(factor>1.0){
			img.writeToFile(file.getAbsolutePath()+"["+String.join(",", args)+"]");
			outSize[0]=img.getWidth();
			outSize[1]=img.getHeight();
		}else{
			VipsImage resized=img.resize(factor);
			try{
				resized.writeToFile(file.getAbsolutePath()+"["+String.join(",", args)+"]");
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
		if(doc instanceof LocalImage img){
			File file=new File(Config.uploadPath, img.path+"/"+img.localID+".webp");
			if(file.exists())
				file.delete();
			else
				LOG.warn("{} does not exist", file.getAbsolutePath());
		}
	}

	public static JsonObject serializeAttachment(ActivityPubObject att){
		JsonObject o=att.asActivityPubObject(null, new SerializerContext(null, (String)null));
		if(att instanceof Document){
			Document d=(Document) att;
			if(StringUtils.isNotEmpty(d.localID)){
				o.addProperty("_lid", d.localID);
				if(d instanceof LocalImage){
					LocalImage im=(LocalImage) d;
					JsonArray sizes=new JsonArray();
					sizes.add(im.width);
					sizes.add(im.height);
					o.add("_sz", sizes);
					if(im.path!=null)
						o.addProperty("_p", im.path);
					o.addProperty("type", "_LocalImage");
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
