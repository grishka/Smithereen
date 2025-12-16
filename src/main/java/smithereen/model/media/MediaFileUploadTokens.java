package smithereen.model.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import smithereen.Config;
import smithereen.util.CryptoUtils;

public class MediaFileUploadTokens{

	private MediaFileUploadTokens(){}

	public static byte[] hash(MediaFileUploadPurpose purpose, int ownerID, byte[] fileRandomID){
		String s=purpose+","+ownerID;
		try{
			ByteArrayOutputStream buf=new ByteArrayOutputStream();
			buf.write(Config.fileRandomIdKey);
			buf.write(fileRandomID);
			buf.write(s.getBytes(StandardCharsets.UTF_8));
			return CryptoUtils.sha256(buf.toByteArray());
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}

	public static String getToken(MediaFileID id, MediaFileUploadPurpose purpose, int ownerID){
		byte[] rawToken=new byte[MediaFileID.RANDOM_ID_LENGTH+CryptoUtils.SHA256_LENGTH];
		System.arraycopy(id.randomID(), 0, rawToken, 0, MediaFileID.RANDOM_ID_LENGTH);
		System.arraycopy(hash(purpose, ownerID, id.randomID()), 0, rawToken, MediaFileID.RANDOM_ID_LENGTH, CryptoUtils.SHA256_LENGTH);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);
	}

	public static boolean verifyToken(String token, MediaFileID id, MediaFileUploadPurpose expectedPurpose, int expectedOwnerID){
		return token.equals(getToken(id, expectedPurpose, expectedOwnerID));
	}
}
