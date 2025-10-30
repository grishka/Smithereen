package smithereen.api.methods;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.util.CryptoUtils;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;

public class PhotosMethods{
	public static Object getAttachmentUploadServer(ApplicationContext ctx, ApiCallContext actx){
		String data=new JsonObjectBuilder()
				.add("id", actx.self.id)
				.add("ct", System.currentTimeMillis()/1000L)
				.build()
				.toString();
		String url=UriBuilder.local()
				.path("api", "uploadAttachmentPhoto")
				.queryParam("d", Base64.getUrlEncoder().withoutPadding().encodeToString(CryptoUtils.aesGcmEncrypt(data.getBytes(StandardCharsets.UTF_8), ApiUtils.UPLOAD_KEY)))
				.build()
				.toString();
		return Map.of("upload_url", url);
	}
}
