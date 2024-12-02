package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
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
import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.libvips.VipsImage;
import smithereen.model.Account;
import smithereen.model.CachedRemoteImage;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileMetadata;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileType;
import smithereen.model.photos.Photo;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.util.BlurHash;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.utils.StringUtils;

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
			JsonObjectBuilder jb=new JsonObjectBuilder()
					.add("type", "_LocalImage")
					.add("_fileID", li.fileID);
			if(li.photoID!=0)
				jb.add("_photoID", li.photoID);
			if(li.rotation!=null && li.rotation!=SizedImage.Rotation._0)
				jb.add("_rot", li.rotation.value());
			if(StringUtils.isNotEmpty(li.name))
				jb.add("name", li.name);
			return jb.build();
		}
		return att.asActivityPubObject(null, new SerializerContext(null, (String)null));
	}

	public static void fillAttachmentObjects(ApplicationContext context, User self, List<ActivityPubObject> attachObjects, List<String> attachmentIDs, Map<String, String> altTexts, int attachmentCount, int maxAttachments) throws SQLException{
		for(String id:attachmentIDs){
			String[] idParts=id.split(":");
			if(idParts.length!=2)
				continue;

			if("photo".equals(idParts[0])){
				long photoID=XTEA.decodeObjectID(idParts[1], ObfuscatedObjectIDType.PHOTO);
				Photo photo=context.getPhotosController().getPhotoIgnoringPrivacy(photoID);
				if(photo.localFileID==0){
					LOG.debug("Not attaching photo {} to {}'s post because it's not local (AP ID {})", photoID, self.username, photo.apID);
					continue;
				}
				try{
					context.getPrivacyController().enforceObjectPrivacy(self, photo);
				}catch(UserActionNotAllowedException x){
					LOG.debug("Not attaching photo {} to {}'s post because they can't access it", photoID, self.username);
					continue;
				}
				LocalImage img=new LocalImage();
				img.fileID=photo.localFileID;
				img.photoID=photoID;
				if(photo.metadata!=null)
					img.rotation=photo.metadata.rotation;
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(img.fileID);
				img.fillIn(mfr);
				attachObjects.add(img);
			}else{
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
				img.name=altTexts.get(id);
				attachObjects.add(img);
			}
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
				MediaFileStorageDriver.getInstance().storeFile(resizedFile, fileRecord.id(), false);

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

	public static LocalImage copyRemoteImageToLocalStorage(@NotNull User self, @NotNull SizedImage image) throws SQLException, IOException{
		if(image instanceof LocalImage)
			throw new IllegalArgumentException("The method name literally says it's for REMOTE images. Why would you pass a local image here?");
		String cacheKey=switch(image){
			case CachedRemoteImage cri -> cri.cacheKey;
			case NonCachedRemoteImage nri -> ((MediaCache.PhotoItem)MediaCache.getInstance().downloadAndPut(nri.getOriginalURI(), "image/jpeg", MediaCache.ItemType.PHOTO, false, 0, 0)).key;
			default -> throw new IllegalStateException("Unexpected value: " + image);
		};
		File file=new File(Config.mediaCachePath, cacheKey+".webp");
		if(!file.exists())
			throw new IllegalStateException("This file was supposed to exist, but somehow it doesn't");
		String blurhash;
		VipsImage img=null;
		try{
			img=new VipsImage(file.getAbsolutePath());
			blurhash=BlurHash.encode(img, 4, 4);
		}finally{
			if(img!=null)
				img.release();
		}
		MediaFileMetadata meta=new ImageMetadata(image.getOriginalDimensions().width, image.getOriginalDimensions().height, blurhash, null);
		MediaFileRecord fileRecord=MediaStorage.createMediaFileRecord(MediaFileType.IMAGE_PHOTO, file.length(), self.id, meta);
		LocalImage li=new LocalImage();
		li.fileID=fileRecord.id().id();
		li.fillIn(fileRecord);
		MediaFileStorageDriver.getInstance().storeFile(file, fileRecord.id(), true);
		return li;
	}
}
