package smithereen.activitypub;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import smithereen.Config;
import smithereen.DisallowLocalhostInterceptor;
import smithereen.LoggingInterceptor;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.objects.WebfingerResponse;
import smithereen.data.UriBuilder;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.jsonld.JLD;
import smithereen.jsonld.JLDException;
import smithereen.jsonld.JLDProcessor;
import smithereen.jsonld.LinkedDataSignatures;
import spark.utils.StringUtils;

public class ActivityPub{

	public static final URI AS_PUBLIC=URI.create(JLD.ACTIVITY_STREAMS+"#Public");
	public static final String CONTENT_TYPE="application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"";

	private static OkHttpClient httpClient;
	private static LruCache<String, String> domainRedirects=new LruCache<>(100);

	static{
		httpClient=new OkHttpClient.Builder()
				.addNetworkInterceptor(new DisallowLocalhostInterceptor())
//				.addNetworkInterceptor(new LoggingInterceptor())
				.build();
	}

	public static ActivityPubObject fetchRemoteObject(URI _uri) throws IOException{
		URI uri;
		String token;
		if("bear".equals(_uri.getScheme())){
			Map<String, String> params=UriBuilder.parseQueryString(_uri.getRawQuery());
			if(!params.containsKey("u") || !params.containsKey("t"))
				throw new IllegalArgumentException("Malformed bearcap URI: "+_uri);
			uri=URI.create(params.get("u"));
			token=params.get("t");
		}else{
			uri=_uri;
			token=null;
		}
		if(Config.isLocal(uri))
			throw new IllegalStateException("Local URI in fetchRemoteObject: "+_uri);
		if(!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme()))
			throw new IllegalStateException("Invalid URI scheme in fetchRemoteObject: "+uri);
		Request.Builder builder=new Request.Builder()
				.url(uri.toString())
				.header("Accept", CONTENT_TYPE);
		if(token!=null)
			builder.header("Authorization", "Bearer "+token);
		signRequest(builder, uri, ServiceActor.getInstance(), null, "get");
		Request req=builder.build();
		Call call=httpClient.newCall(req);
		Response resp=call.execute();
		try(ResponseBody body=resp.body()){
			if(!resp.isSuccessful()){
				throw new ObjectNotFoundException("Response is not successful: remote server returned "+resp.code()+" "+resp.message());
			}

			try{
				JsonElement el=JsonParser.parseReader(body.charStream());
				JsonObject converted=JLDProcessor.convertToLocalContext(el.getAsJsonObject());
//				System.out.println(converted.toString(4));
				ActivityPubObject obj=ActivityPubObject.parse(converted);
				if(obj==null)
					throw new UnsupportedRemoteObjectTypeException("Unsupported object type "+converted.get("type"));
				if(obj.activityPubID!=null && !obj.activityPubID.getHost().equalsIgnoreCase(uri.getHost()))
					throw new BadRequestException("Domain in object ID ("+obj.activityPubID+") doesn't match domain in its URI ("+uri+")");
				return obj;
			}catch(JLDException|JsonParseException x){
				x.printStackTrace();
				throw new IOException(x);
			}
		}
	}

	private static Request.Builder signRequest(Request.Builder builder, URI url, Actor actor, byte[] body, String method){
		String path=url.getPath();
		String host=url.getHost();
		if(url.getPort()!=-1)
			host+=":"+url.getPort();
		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String date=dateFormat.format(new Date());
		String digestHeader;
		if(body!=null){
			digestHeader="SHA-256=";
			try{
				digestHeader+=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
			}catch(NoSuchAlgorithmException ignore){}
		}else{
			digestHeader=null;
		}
		String strToSign="(request-target): "+method.toLowerCase()+" "+path+"\nhost: "+host+"\ndate: "+date;
		if(digestHeader!=null)
			strToSign+="\ndigest: "+digestHeader;

		Signature sig;
		byte[] signature;
		try{
			sig=Signature.getInstance("SHA256withRSA");
			sig.initSign(actor.privateKey);
			sig.update(strToSign.getBytes(StandardCharsets.UTF_8));
			signature=sig.sign();
		}catch(Exception x){
			x.printStackTrace();
			throw new RuntimeException(x);
		}

		String keyID=actor.activityPubID+"#main-key";
		builder.header("Signature", Utils.serializeSignatureHeader(List.of(Map.of(
					"keyId", keyID,
					"headers", "(request-target) host date"+(digestHeader!=null ? " digest" : ""),
					"algorithm", "rsa-sha256",
					"signature", Base64.getEncoder().encodeToString(signature)
				))))
				.header("Date", date);
		if(digestHeader!=null)
			builder.header("Digest", digestHeader);
		return builder;
	}

	public static void postActivity(URI inboxUrl, Activity activity, Actor actor) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");
		JsonObject body=activity.asRootActivityPubObject();
		LinkedDataSignatures.sign(body, actor.privateKey, actor.activityPubID+"#main-key");
		System.out.println("Sending activity: "+body);
		postActivity(inboxUrl, body.toString(), actor);
	}

	public static void postActivity(URI inboxUrl, String activityJson, Actor actor) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		byte[] body=activityJson.getBytes(StandardCharsets.UTF_8);
		Request req=signRequest(
					new Request.Builder()
					.url(inboxUrl.toString())
					.post(RequestBody.create(MediaType.parse("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""), body)),
				inboxUrl, actor, body, "post")
				.build();
		Response resp=httpClient.newCall(req).execute();
		System.out.println(resp.toString());
		try(ResponseBody rb=resp.body()){
			if(!resp.isSuccessful())
				System.out.println(rb.string());
		}
	}

	public static boolean isPublic(URI uri){
		return uri.equals(AS_PUBLIC) || ("as".equals(uri.getScheme()) && "Public".equals(uri.getSchemeSpecificPart()));
	}

	private static URI doWebfingerRequest(String username, String domain, String uriTemplate) throws IOException{
		String resource="acct:"+username+"@"+domain;
		String url;
		if(StringUtils.isEmpty(uriTemplate)){
			url="https://"+domain+"/.well-known/webfinger?resource="+resource;
		}else{
			url=uriTemplate.replace("{uri}", resource);
		}
		Request req=new Request.Builder()
				.url(url)
				.build();
		Response resp=httpClient.newCall(req).execute();
		try(ResponseBody body=resp.body()){
			if(resp.isSuccessful()){
				WebfingerResponse wr=Utils.gson.fromJson(body.charStream(), WebfingerResponse.class);

				if(!resource.equalsIgnoreCase(wr.subject))
					throw new IOException("Invalid response");
				for(WebfingerResponse.Link link:wr.links){
					if("self".equals(link.rel) && "application/activity+json".equals(link.type) && link.href!=null){
						return link.href;
					}
				}
				throw new IOException("Link not found");
			}else if(resp.code()==404){
				throw new ObjectNotFoundException("User "+username+"@"+domain+" does not exist");
			}else{
				throw new IOException("Failed to resolve username "+username+"@"+domain);
			}
		}catch(JsonParseException x){
			throw new IOException("Response parse failed", x);
		}
	}

	public static URI resolveUsername(String username, String domain) throws IOException{
		if(domain!=null && domain.equalsIgnoreCase(Config.domain))
			throw new IllegalArgumentException("Local domain in resolveUsername");
		String redirect;
		synchronized(ActivityPub.class){
			redirect=domainRedirects.get(domain);
		}
		try{
			URI uri=doWebfingerRequest(username, domain, redirect);
			if(redirect==null){
				synchronized(ActivityPub.class){
					// Cache an empty string indicating that this domain doesn't have a redirect.
					// This is to avoid a useless host-meta request when a nonexistent username is looked up on that instance.
					domainRedirects.put(domain, "");
				}
			}
			return uri;
		}catch(ObjectNotFoundException x){
			if(redirect==null){
				Request req=new Request.Builder()
						.url("https://"+domain+"/.well-known/host-meta")
						.header("Accept", "application/xrd+xml")
						.build();
				Response resp=httpClient.newCall(req).execute();
				try(ResponseBody body=resp.body()){
					if(resp.isSuccessful()){
						DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
						DocumentBuilder builder=factory.newDocumentBuilder();
						Document doc=builder.parse(body.byteStream());
						NodeList nodes=doc.getElementsByTagName("Link");
						for(int i=0; i<nodes.getLength(); i++){
							Node node=nodes.item(i);
							NamedNodeMap attrs=node.getAttributes();
							if(attrs!=null){
								Node _rel=attrs.getNamedItem("rel");
								Node _type=attrs.getNamedItem("type");
								Node _template=attrs.getNamedItem("template");
								if(_rel!=null && _type!=null && _template!=null){
									String rel=_rel.getNodeValue();
									String type=_type.getNodeValue();
									String template=_template.getNodeValue();
									if("lrdd".equals(rel) && "application/xrd+xml".equals(type)){
										if((template.startsWith("https://") || (Config.useHTTP && template.startsWith("http://"))) && template.contains("{uri}")){
											synchronized(ActivityPub.class){
												if(("https://"+domain+"/.well-known/webfinger?resource={uri}").equals(template)){
													// this isn't a real redirect
													domainRedirects.put(domain, "");
													// don't repeat the request, we already know that username doesn't exist (but the webfinger endpoint does)
													throw new ObjectNotFoundException(x);
												}else{
													System.out.println("Found domain redirect: "+domain+" -> "+template);
													domainRedirects.put(domain, template);
												}
											}
											return doWebfingerRequest(username, domain, template);
										}else{
											throw new ObjectNotFoundException("Malformed URI template '"+template+"' in host-meta domain redirect", x);
										}
									}
								}
							}
						}
					}
				}catch(ParserConfigurationException|SAXException e){
					throw new ObjectNotFoundException("Webfinger returned 404 and host-meta can't be parsed", e);
				}
			}
			throw new ObjectNotFoundException(x);
		}
	}
}
