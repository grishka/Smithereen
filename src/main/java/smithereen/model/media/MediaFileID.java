package smithereen.model.media;

import java.util.Base64;

import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.util.XTEA;

public record MediaFileID(long id, byte[] randomID, int originalOwnerID, MediaFileType type){
	public static final int RANDOM_ID_LENGTH=18;

	public String getEncodedID(){
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.MEDIA_FILE);
	}

	public String getEncodedRandomID(){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomID);
	}
}
