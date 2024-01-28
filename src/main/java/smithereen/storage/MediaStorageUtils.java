package smithereen.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.libvips.VipsImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class MediaStorageUtils{
	private static final Logger LOG=LoggerFactory.getLogger(MediaStorageUtils.class);
	public static final int QUALITY_LOSSLESS=-1;

	public static long writeResizedWebpImage(VipsImage img, int widthOrSize, int height, int quality, File file, int[] outSize) throws IOException{
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

	public static JsonObject serializeAttachment(ActivityPubObject att){
		if(att instanceof LocalImage li){
			return new JsonObjectBuilder()
					.add("type", "_LocalImage")
					.add("_fileID", li.fileID)
					.build();
		}
		JsonObject o=att.asActivityPubObject(null, new SerializerContext(null, (String)null));
		return o;
	}

	public static void fillAttachmentObjects(List<ActivityPubObject> attachObjects, List<String> attachmentIDs, int attachmentCount, int maxAttachments) throws SQLException{
		for(String id:attachmentIDs){
			String[] idParts=id.split(":");
			if(idParts.length!=2)
				continue;
			long fileID;
			byte[] fileRandomID;
			try{
				byte[] _fileID=Base64.getUrlDecoder().decode(idParts[0]);
				fileRandomID=Base64.getUrlDecoder().decode(idParts[1]);
				if(_fileID.length!=8 || fileRandomID.length!=18)
					continue;
				fileID=XTEA.deobfuscateObjectID(Utils.unpackLong(_fileID), ObfuscatedObjectIDType.MEDIA_FILE);
			}catch(IllegalArgumentException x){
				continue;
			}
			MediaFileRecord mfr=MediaStorage.getMediaFileRecord(fileID);
			if(mfr==null || !Arrays.equals(mfr.id().randomID(), fileRandomID))
				continue;
			LocalImage img=new LocalImage();
			img.fileID=fileID;
			img.fillIn(mfr);
			attachObjects.add(img);
			attachmentCount++;
			if(attachmentCount==maxAttachments)
				break;
		}
	}

	public static void deleteAbandonedFiles(){
		try{
			List<MediaFileRecord> fileRecords=MediaStorage.getUnreferencedMediaFileRecords();
			if(fileRecords.isEmpty()){
				LOG.trace("No files to delete");
				return;
			}
			LOG.trace("Deleting: {}", fileRecords);
			Set<MediaFileID> deleted=MediaFileStorageDriver.getInstance().deleteFiles(fileRecords.stream().map(MediaFileRecord::id).collect(Collectors.toSet()));
			if(!deleted.isEmpty())
				MediaStorage.deleteMediaFileRecords(deleted.stream().map(MediaFileID::id).collect(Collectors.toSet()));
		}catch(SQLException x){
			LOG.warn("Failed to delete unused media files", x);
		}
	}
}
