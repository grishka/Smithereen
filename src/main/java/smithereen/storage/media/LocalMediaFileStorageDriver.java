package smithereen.storage.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

import smithereen.Config;
import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.media.MediaFileID;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.ImgProxy;
import smithereen.util.XTEA;

public class LocalMediaFileStorageDriver extends MediaFileStorageDriver{
	@Override
	public void storeFile(File localFile, MediaFileID id) throws IOException{
		int oid=Math.abs(id.originalOwnerID());
		File dir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/10000%100));
		if(!dir.exists() && !dir.mkdirs())
			throw new IOException("Failed to create directories");
		File targetFile=new File(Config.uploadPath, getFilePath(id));
		Files.move(localFile.toPath(), targetFile.toPath());
	}

	@Override
	public InputStream openStream(MediaFileID id) throws IOException{
		return new FileInputStream(getFilePath(id));
	}

	@Override
	public void deleteFile(MediaFileID id) throws IOException{
		Files.deleteIfExists(Path.of(getFilePath(id)));
	}

	@Override
	public ImgProxy.UrlBuilder getImgProxyURL(MediaFileID id){
		String url="local://"+Config.imgproxyLocalUploads+"/"+getFilePath(id);
		return new ImgProxy.UrlBuilder(url);
	}

	private String getFilePath(MediaFileID id){
		int oid=Math.abs(id.originalOwnerID());
		return String.format(Locale.US, "%02d/%02d/%02d/", oid%100, oid/100%100, oid/10000%100)+
				Base64.getUrlEncoder().withoutPadding().encodeToString(id.randomID())+"_"+
				Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(id.id(), ObfuscatedObjectIDType.MEDIA_FILE)))+
				switch(id.type()){
					case IMAGE_AVATAR, IMAGE_PHOTO, IMAGE_GRAFFITI -> ".webp";
				};
	}
}
