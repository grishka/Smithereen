package smithereen.storage.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

import smithereen.Config;
import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.media.MediaFileID;
import smithereen.storage.ImgProxy;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

public class LocalMediaFileStorageDriver extends MediaFileStorageDriver{
	@Override
	public void storeFile(File localFile, MediaFileID id, boolean keepLocalFile, String downloadFileName) throws IOException{
		int oid=Math.abs(id.originalOwnerID());
		File dir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/10000%100));
		if(!dir.exists() && !dir.mkdirs())
			throw new IOException("Failed to create directories");
		File targetFile=new File(Config.uploadPath, getFilePath(id));
		if(keepLocalFile)
			Files.copy(localFile.toPath(), targetFile.toPath());
		else
			Files.move(localFile.toPath(), targetFile.toPath());
	}

	@Override
	public InputStream openStream(MediaFileID id) throws IOException{
		File targetFile=new File(Config.uploadPath, getFilePath(id));
		return new FileInputStream(targetFile);
	}

	@Override
	public void deleteFile(MediaFileID id) throws IOException{
		File targetFile=new File(Config.uploadPath, getFilePath(id));
		Files.deleteIfExists(targetFile.toPath());
	}

	@Override
	public ImgProxy.UrlBuilder getImgProxyURL(MediaFileID id){
		String url="local://"+Config.imgproxyLocalUploads+"/"+getFilePath(id);
		return new ImgProxy.UrlBuilder(url);
	}

	@Override
	public URI getFilePublicURL(MediaFileID id, String downloadFileName){
		URI url=Config.localURI(Config.uploadUrlPath+"/"+getFilePath(id));
		if(downloadFileName!=null)
			url=new UriBuilder(url).queryParam("fn", downloadFileName).build();
		return url;
	}

	private String getFilePath(MediaFileID id){
		int oid=Math.abs(id.originalOwnerID());
		return String.format(Locale.US, "%02d/%02d/%02d/", oid%100, oid/100%100, oid/10000%100)+
				Base64.getUrlEncoder().withoutPadding().encodeToString(id.randomID())+"_"+
				Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(id.id(), ObfuscatedObjectIDType.MEDIA_FILE)))+
				"."+id.type().getFileExtension();
	}
}
