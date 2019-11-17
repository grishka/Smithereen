package smithereen.activitypub;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import smithereen.Config;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.User;
import smithereen.jsonld.JLDDocument;

public class ActivityPub{

	private static OkHttpClient httpClient;

	static{
		httpClient=new OkHttpClient.Builder()
				.build();
	}

	public static ActivityPubObject fetchRemoteObject(String url) throws IOException, JSONException{
		Request req=new Request.Builder()
				.url(url)
				.header("Accept", "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"")
				.build();
		Call call=httpClient.newCall(req);
		Response resp=call.execute();
		ResponseBody body=resp.body();
		if(!resp.isSuccessful()){
			body.close();
			return null;
		}
		String r=body.string();
		body.close();
		try{
			return ActivityPubObject.parse(JLDDocument.convertToLocalContext(new JSONObject(r)));
		}catch(Exception x){
			throw new JSONException(x);
		}
	}

	public static void postActivity(URI inboxUrl, Activity activity, User user) throws IOException{
		System.out.println("Sending activity: "+activity);
		String path=inboxUrl.getPath();
		String host=inboxUrl.getHost();
		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date=dateFormat.format(new Date());
		String strToSign="(request-target): post "+path+"\nhost: "+host+"\ndate: "+date;

		Signature sig;
		byte[] signature;
		try{
			sig=Signature.getInstance("SHA256withRSA");
			sig.initSign(user.privateKey);
			sig.update(strToSign.getBytes(StandardCharsets.UTF_8));
			signature=sig.sign();
		}catch(Exception x){
			x.printStackTrace();
			throw new RuntimeException(x);
		}

		String sigHeader="keyId=\""+Config.localURI(user.username+"#main-key")+"\",headers=\"(request-target) host date\",signature=\""+Base64.getEncoder().encodeToString(signature)+"\"";

		Request req=new Request.Builder()
				.url(inboxUrl.toString())
				.header("Signature", sigHeader)
				.header("Date", date)
				//.header("Content-Type", "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"")
				.post(RequestBody.create(MediaType.parse("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""), activity.asRootActivityPubObject().toString()))
				.build();
		Response resp=httpClient.newCall(req).execute();
		resp.body().close();
		System.out.println(resp.toString());
	}
}
