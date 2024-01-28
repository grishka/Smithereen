package smithereen.model.media;

import java.util.Base64;

import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.util.XTEA;

public record MediaFileID(long id, byte[] randomID, int originalOwnerID, MediaFileType type){
	public String getIDForClient(){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.MEDIA_FILE)))
				+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(randomID);
	}
}
