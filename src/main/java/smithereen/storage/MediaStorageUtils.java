package smithereen.storage;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.lang.Lang;
import smithereen.libvips.VipsImage;
import smithereen.model.Account;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileMetadata;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileType;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.util.BlurHash;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.Spark;

import static smithereen.Utils.lang;

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

	public static LocalImage saveUploadedImage(Request req, Response resp, Account self, boolean isGraffiti){
		Lang l=lang(req);
		try{
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 10*1024*1024, -1L, 0));
			Part part=req.raw().getPart("file");
			if(part.getSize()>10*1024*1024){
				// Payload Too Large
				Spark.halt(413, l.get("err_file_upload_too_large", Map.of("maxSize", l.formatFileSize(10*1024*1024))));
			}

			String mime=part.getContentType();
			if(!mime.startsWith("image/")){
				// Unsupported Media Type
				Spark.halt(415, l.get("err_file_upload_image_format"));
			}

			File temp=File.createTempFile("SmithereenUpload", null);
			VipsImage img;
			try{
				try(FileOutputStream out=new FileOutputStream(temp)){
					Utils.copyBytes(part.getInputStream(), out);
				}
				img=new VipsImage(temp.getAbsolutePath());
			}catch(IOException x){
				LOG.warn("VipsImage error", x);
				Spark.halt(400, l.get("err_file_upload_image_format"));
				throw new IllegalStateException();
			}
			if(img.hasAlpha()){
				VipsImage flat=img.flatten(255, 255, 255);
				img.release();
				img=flat;
			}

			if(isGraffiti && (img.getWidth()!=GraffitiAttachment.WIDTH || img.getHeight()!=GraffitiAttachment.HEIGHT)){
				LOG.warn("Unexpected graffiti size {}x{}", img.getWidth(), img.getHeight());
				throw new BadRequestException();
			}

			LocalImage photo=new LocalImage();
			MediaFileRecord fileRecord;
			try{
				File resizedFile=File.createTempFile("SmithereenUploadResized", ".webp");
				int[] outSize={0,0};
				MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, isGraffiti ? MediaStorageUtils.QUALITY_LOSSLESS : 93, resizedFile, outSize);
				MediaFileMetadata meta=new ImageMetadata(outSize[0], outSize[1], BlurHash.encode(img, 4, 4), null);
				fileRecord=MediaStorage.createMediaFileRecord(isGraffiti ? MediaFileType.IMAGE_GRAFFITI : MediaFileType.IMAGE_PHOTO, resizedFile.length(), self.user.id, meta);
				photo.fileID=fileRecord.id().id();
				photo.fillIn(fileRecord);
				MediaFileStorageDriver.getInstance().storeFile(resizedFile, fileRecord.id());

				temp.delete();
			}finally{
				img.release();
			}
			return photo;
		}catch(IOException | ServletException | SQLException x){
			LOG.error("File upload failed", x);
			Spark.halt(500, l.get("err_file_upload"));
			throw new IllegalStateException();
		}
	}
}
