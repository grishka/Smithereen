package smithereen.activitypub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionQueryResult;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.objects.WebfingerResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.jsonld.JLD;
import smithereen.jsonld.JLDException;
import smithereen.jsonld.JLDProcessor;
import smithereen.jsonld.LinkedDataSignatures;
import smithereen.model.FederationRestriction;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.Server;
import smithereen.model.StatsType;
import smithereen.storage.GroupStorage;
import smithereen.util.DisallowLocalhostInterceptor;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.UserAgentInterceptor;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class ActivityPub{

	public static final URI AS_PUBLIC=URI.create(JLD.ACTIVITY_STREAMS+"#Public");
	public static final String CONTENT_TYPE="application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"";
	private static final Logger LOG=LoggerFactory.getLogger(ActivityPub.class);

	public static final OkHttpClient httpClient;
	private static LruCache<String, String> domainRedirects=new LruCache<>(100);

	static{
		httpClient=new OkHttpClient.Builder()
				.addNetworkInterceptor(new DisallowLocalhostInterceptor())
				.addNetworkInterceptor(new UserAgentInterceptor())
//				.addNetworkInterceptor(new LoggingInterceptor())
				.build();
	}

	public static ActivityPubObject fetchRemoteObject(URI _uri, Actor signer, JsonObject actorToken, ApplicationContext ctx) throws IOException{
		return fetchRemoteObjectInternal(_uri, signer, actorToken, ctx, true);
	}

	private static ActivityPubObject fetchRemoteObjectInternal(URI _uri, Actor signer, JsonObject actorToken, ApplicationContext ctx, boolean tryHTML) throws IOException{
		LOG.trace("Fetching remote object from {}", _uri);
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

		Server server=ctx.getModerationController().getOrAddServer(uri.getAuthority());
		FederationRestriction restriction=server.restriction();
		if(restriction!=null){
			if(restriction.type==FederationRestriction.RestrictionType.SUSPENSION){
				throw new ObjectNotFoundException("Federation with "+server.host()+" is blocked by this server's policies");
			}
		}

		Request.Builder builder=new Request.Builder()
				.url(uri.toString())
				.header("Accept", CONTENT_TYPE);
		if(token!=null)
			builder.header("Authorization", "Bearer "+token);
		else if(actorToken!=null)
			builder.header("Authorization", "ActivityPubActorToken "+actorToken);
		signRequest(builder, uri, signer==null ? ServiceActor.getInstance() : signer, null, "get");
		Request req=builder.build();
		Call call=httpClient.newCall(req);
		Response resp=call.execute();
		try(ResponseBody body=resp.body()){
			Objects.requireNonNull(body);
			if(!resp.isSuccessful()){
				throw new ObjectNotFoundException("Response is not successful: remote server returned "+resp.code()+" "+resp.message());
			}
			MediaType contentType=body.contentType();
			if(tryHTML && contentType!=null && "text".equals(contentType.type()) && "html".equals(contentType.subtype())){
				LOG.trace("Received HTML, trying to extract <link>");
				org.jsoup.nodes.Document doc=Jsoup.parse(body.string(), uri.toString());
				for(Element el:doc.select("link[rel=alternate]")){
					LOG.trace("Candidate element: {}", el);
					String type=el.attr("type");
					if("application/activity+json".equals(type) || CONTENT_TYPE.equals(type)){
						String url=el.absUrl("href");
						LOG.trace("Will follow redirect: {}", url);
						if(StringUtils.isNotEmpty(url)){
							try{
								return fetchRemoteObjectInternal(UriBuilder.parseAndEncode(url), signer, actorToken, ctx, false);
							}catch(URISyntaxException x){
								throw new ObjectNotFoundException("Failed to parse URL from <link rel=\"alternate\">", x);
							}
						}
					}
				}
			}
			// Allow "application/activity+json" or "application/ld+json"
			if(contentType==null || !"application".equals(contentType.type()) || !("activity+json".equals(contentType.subtype()) || "ld+json".equals(contentType.subtype()))){
				throw new ObjectNotFoundException("Invalid Content-Type: "+contentType);
			}

			try{
				JsonElement el=JsonParser.parseReader(body.charStream());
				JsonObject converted=JLDProcessor.convertToLocalContext(el.getAsJsonObject());
				ActivityPubObject obj=ActivityPubObject.parse(converted);
				if(obj==null)
					throw new UnsupportedRemoteObjectTypeException("Unsupported object type "+converted.get("type"));
				if(obj.activityPubID!=null && !obj.activityPubID.getHost().equalsIgnoreCase(uri.getHost()))
					throw new BadRequestException("Domain in object ID ("+obj.activityPubID+") doesn't match domain in its URI ("+uri+")");
				return obj;
			}catch(JLDException|JsonParseException x){
				LOG.error("Exception while parsing or converting remote object", x);
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

		byte[] signature;
		try{
			Signature sig=Signature.getInstance("SHA256withRSA");
			sig.initSign(actor.privateKey);
			sig.update(strToSign.getBytes(StandardCharsets.UTF_8));
			signature=sig.sign();
		}catch(Exception x){
			LOG.error("Exception while signing request", x);
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

	public static void postActivity(URI inboxUrl, Activity activity, Actor actor, ApplicationContext ctx, boolean isRetry) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		Server server=ctx.getModerationController().getServerByDomain(inboxUrl.getAuthority());
		if(server.getAvailability()==Server.Availability.DOWN){
			LOG.debug("Not sending {} activity to server {} because it's down", activity.getType(), server.host());
			return;
		}
		if(server.restriction()!=null){
			if(server.restriction().type==FederationRestriction.RestrictionType.SUSPENSION){
				LOG.debug("Not sending {} activity to server {} because federation with it is blocked", activity.getType(), server.host());
				return;
			}
		}

		JsonObject body=activity.asRootActivityPubObject(ctx, inboxUrl.getAuthority());
		LinkedDataSignatures.sign(body, actor.privateKey, actor.activityPubID+"#main-key");
		LOG.debug("Sending activity: {}", body);
		postActivityInternal(inboxUrl, body.toString(), actor, server, ctx, isRetry);
	}

	public static void postActivity(URI inboxUrl, String activityJson, Actor actor, ApplicationContext ctx) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		Server server=ctx.getModerationController().getServerByDomain(inboxUrl.getAuthority());
		if(server.getAvailability()==Server.Availability.DOWN){
			LOG.debug("Not forwarding activity to server {} because it's down", server.host());
			return;
		}
		if(server.restriction()!=null){
			if(server.restriction().type==FederationRestriction.RestrictionType.SUSPENSION){
				LOG.debug("Not forwarding activity to server {} because federation with it is blocked", server.host());
				return;
			}
		}

		postActivityInternal(inboxUrl, activityJson, actor, server, ctx, false);
	}

	private static void postActivityInternal(URI inboxUrl, String activityJson, Actor actor, Server server, ApplicationContext ctx, boolean isRetry) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		byte[] body=activityJson.getBytes(StandardCharsets.UTF_8);
		Request req=signRequest(
					new Request.Builder()
					.url(inboxUrl.toString())
					.post(RequestBody.create(MediaType.parse("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""), body)),
				inboxUrl, actor, body, "post")
				.build();
		try{
			Response resp=httpClient.newCall(req).execute();
			LOG.debug("Post activity response: {}", resp);
			try(ResponseBody rb=resp.body()){
				if(!resp.isSuccessful()){
					LOG.debug("Response body: {}", rb.string());
					if(resp.code()!=403){
						if(resp.code()/100==5){ // IOException does trigger retrying, FederationException does not. We want retries for 5xx (server) errors.
							throw new IOException("Response is not successful: "+resp.code());
						}else{
							throw new FederationException("Response is not successful: "+resp.code());
						}
					}
				}
			}
			ctx.getStatsController().incrementDaily(StatsType.SERVER_ACTIVITIES_SENT, server.id());
			if(server.getAvailability()!=Server.Availability.UP){
				ctx.getModerationController().resetServerAvailability(server);
			}
		}catch(IOException x){
			if(!isRetry){
				ctx.getModerationController().recordFederationFailure(server);
				ctx.getStatsController().incrementDaily(StatsType.SERVER_ACTIVITIES_FAILED_ATTEMPTS, server.id());
			}
			throw x;
		}
	}

	public static boolean isPublic(URI uri){
		return uri.equals(AS_PUBLIC) || ("as".equals(uri.getScheme()) && "Public".equals(uri.getSchemeSpecificPart()));
	}

	private static URI doWebfingerRequest(String username, String domain, String uriTemplate) throws IOException{
		String resource="acct:"+username+"@"+domain;
		String url;
		if(StringUtils.isEmpty(uriTemplate)){
			url=(Config.useHTTP ? "http" : "https")+"://"+domain+"/.well-known/webfinger?resource="+resource;
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
					if("self".equals(link.rel) && ("application/activity+json".equals(link.type) || CONTENT_TYPE.equals(link.type)) && link.href!=null){
						LOG.trace("Successfully resolved {}@{} to {}", username, domain, link.href);
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
		try{
			domain=Utils.convertIdnToAsciiIfNeeded(domain);
		}catch(IllegalArgumentException x){
			throw new ObjectNotFoundException("Malformed domain '"+domain+"'", x);
		}
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
						.url((Config.useHTTP ? "http" : "https")+"://"+domain+"/.well-known/host-meta")
						.header("Accept", "application/xrd+xml")
						.build();
				Response resp=httpClient.newCall(req).execute();
				try(ResponseBody body=resp.body()){
					if(resp.isSuccessful()){
						DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
						factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
						factory.setXIncludeAware(false);
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
												if(template.endsWith("://"+domain+"/.well-known/webfinger?resource={uri}")){
													// this isn't a real redirect
													domainRedirects.put(domain, "");
													// don't repeat the request, we already know that username doesn't exist (but the webfinger endpoint does)
													throw new ObjectNotFoundException(x);
												}else{
													LOG.debug("Found domain redirect: {} -> {}", domain, template);
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

	public static Actor verifyHttpSignature(spark.Request req, Actor userHint) throws ParseException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, SQLException{
		String sigHeader=req.headers("Signature");
		if(sigHeader==null)
			throw new BadRequestException("Request is missing Signature header");
		List<Map<String, String>> values=parseSignatureHeader(sigHeader);
		if(values.isEmpty())
			throw new BadRequestException("Signature header has invalid format");
		Map<String, String> supportedSig=null;
		for(Map<String, String> sig:values){
			if("rsa-sha256".equalsIgnoreCase(sig.get("algorithm")) || "hs2019".equalsIgnoreCase(sig.get("algorithm"))){
				supportedSig=sig;
				break;
			}
		}
		if(supportedSig==null)
			throw new BadRequestException("Unsupported signature algorithm \""+values.get(0).get("algorithm")+"\", expected \"rsa-sha256\" or \"hs2019\"");

		if(!supportedSig.containsKey("keyId"))
			throw new BadRequestException("Signature header is missing keyId field");
		if(!supportedSig.containsKey("signature"))
			throw new BadRequestException("Signature header is missing signature field");
		if(!supportedSig.containsKey("headers"))
			throw new BadRequestException("Signature header is missing headers field");

		String keyId=supportedSig.get("keyId");
		byte[] signature=Base64.getDecoder().decode(supportedSig.get("signature"));
		List<String> headers=Arrays.asList(supportedSig.get("headers").split(" "));

		if(!headers.contains("(request-target)"))
			throw new BadRequestException("(request-target) is not in signed headers");
		if(!headers.contains("date"))
			throw new BadRequestException("date is not in signed headers");
		if(!headers.contains("host"))
			throw new BadRequestException("host is not in signed headers");

		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		long unixtime=dateFormat.parse(req.headers("date")).getTime();
		long now=System.currentTimeMillis();
		long diff=now-unixtime;
		if(diff>30000L)
			throw new BadRequestException("Date is too far in the future (difference: "+diff+"ms)");
		if(diff<-30000L)
			throw new BadRequestException("Date is too far in the past (difference: "+diff+"ms)");

		URI userID=Utils.userIdFromKeyId(URI.create(keyId));
		Actor user;
		if(userHint!=null && userHint.activityPubID.equals(userID))
			user=userHint;
		else
			user=context(req).getObjectLinkResolver().resolve(userID, Actor.class, true, false, false);

		ArrayList<String> sigParts=new ArrayList<>();
		for(String header:headers){
			String value;
			if(header.equals("(request-target)")){
				value=req.requestMethod().toLowerCase()+" "+req.pathInfo();
			}else{
				value=req.headers(header);
			}
			sigParts.add(header+": "+value);
		}
		String sigStr=String.join("\n", sigParts);
		Signature sig=Signature.getInstance("SHA256withRSA");
		sig.initVerify(user.publicKey);
		sig.update(sigStr.getBytes(StandardCharsets.UTF_8));
		if(!sig.verify(signature)){
			LOG.debug("Failed signature header: {}", sigHeader);
			LOG.debug("Failed signature string: '{}'", sigStr);
			throw new BadRequestException("Signature failed to verify");
		}
		return user;
	}

	private static String generateActorTokenStringToBeSigned(JsonObject obj){
		return obj.keySet()
				.stream()
				.filter(key->!key.equals("signature"))
				.map(key->key+": "+obj.getAsJsonPrimitive(key).toString())
				.sorted()
				.collect(Collectors.joining("\n"));
	}

	public static JsonObject generateActorToken(@NotNull ApplicationContext context, @NotNull Actor actor, @NotNull Group group){
		group.ensureLocal();
		try{
			if(!GroupStorage.areThereGroupMembersWithDomain(group.id, actor.domain))
				throw new UserActionNotAllowedException("There are no "+actor.domain+" members in this group");
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		Instant now=Instant.now();
		JsonObject token=new JsonObjectBuilder()
				.add("issuer", group.activityPubID.toString())
				.add("actor", actor.activityPubID.toString())
				.add("issuedAt", formatDateAsISO(now))
				.add("validUntil", formatDateAsISO(now.plus(30, ChronoUnit.MINUTES)))
				.build();
		String strToSign=generateActorTokenStringToBeSigned(token);
		byte[] signature;
		try{
			Signature sig=Signature.getInstance("SHA256withRSA");
			sig.initSign(group.privateKey);
			sig.update(strToSign.getBytes(StandardCharsets.UTF_8));
			signature=sig.sign();
		}catch(Exception x){
			LOG.error("Exception while signing request", x);
			throw new RuntimeException(x);
		}

		String keyID=group.activityPubID+"#main-key";
		token.add("signatures", new JsonArrayBuilder().add(
				new JsonObjectBuilder()
						.add("algorithm", "rsa-sha256")
						.add("keyId", keyID)
						.add("signature", Base64.getEncoder().encodeToString(signature))
						.build()
		).build());

		return token;
	}

	/**
	 * Verify an actor token for user membership in a closed or private group.
	 * @param token The actor token as received from the group
	 * @param user The user for which the token was issued
	 * @param group The group that issued the token
	 * @throws UserActionNotAllowedException if the token is not valid
	 */
	public static void verifyActorToken(@NotNull JsonObject token, @NotNull Actor user, @NotNull ForeignGroup group){
		try{
			URI issuerID=new URI(token.getAsJsonPrimitive("issuer").getAsString());
			if(!issuerID.equals(group.activityPubID))
				throw new IllegalArgumentException("Issuer ID '"+issuerID+"' doesn't match the expected '"+group.activityPubID+"'");
			URI actorID=new URI(token.getAsJsonPrimitive("actor").getAsString());
			if(!actorID.equals(user.activityPubID))
				throw new IllegalArgumentException("Actor ID '"+actorID+"' doesn't match the expected '"+user.activityPubID+"'");
			Instant now=Instant.now();
			Instant issuedAt=DateTimeFormatter.ISO_INSTANT.parse(token.getAsJsonPrimitive("issuedAt").getAsString(), Instant::from);
			if(issuedAt.isAfter(now.plus(5, ChronoUnit.MINUTES)))
				throw new IllegalArgumentException("issuedAt is in the future");
			Instant validUntil=DateTimeFormatter.ISO_INSTANT.parse(token.getAsJsonPrimitive("validUntil").getAsString(), Instant::from);
			if(validUntil.isBefore(now.minus(5, ChronoUnit.MINUTES)))
				throw new IllegalArgumentException("This token has expired");
			if(issuedAt.isAfter(validUntil))
				throw new IllegalArgumentException("issuedAt is after validUntil");
			if(ChronoUnit.MINUTES.between(issuedAt, validUntil)>120)
				throw new IllegalArgumentException("The validity period is longer than 2 hours");
			JsonArray signatures=token.getAsJsonArray("signatures");
			JsonObject rsaSignature=null;
			for(JsonElement _sig:signatures){
				JsonObject sig=_sig.getAsJsonObject();
				if("rsa-sha256".equals(sig.getAsJsonPrimitive("algorithm").getAsString())){
					rsaSignature=sig;
					break;
				}
			}
			if(rsaSignature==null)
				throw new IllegalArgumentException("Signature with 'rsa-sha256' algorithm not found");
			byte[] signature=Base64.getDecoder().decode(rsaSignature.getAsJsonPrimitive("signature").getAsString());
			String sigStr=generateActorTokenStringToBeSigned(token);
			Signature sig=Signature.getInstance("SHA256withRSA");
			sig.initVerify(group.publicKey);
			sig.update(sigStr.getBytes(StandardCharsets.UTF_8));
			if(!sig.verify(signature)){
				throw new IllegalArgumentException("Actor token signature failed to verify");
			}
			LOG.debug("Successfully verified actor token {}", token);
		}catch(Exception x){
			throw new UserActionNotAllowedException("Actor token is not valid: "+x.getMessage(), x);
		}
	}

	public static JsonObject fetchActorToken(@NotNull ApplicationContext context, @NotNull Actor actor, @NotNull ForeignGroup group){
		String url=Objects.requireNonNull(group.actorTokenEndpoint).toString();
		Request.Builder builder=new Request.Builder()
				.url(url);
		signRequest(builder, group.actorTokenEndpoint, actor, null, "get");
		Call call=httpClient.newCall(builder.build());
		try(Response resp=call.execute()){
			if(resp.isSuccessful()){
				JsonObject obj=JsonParser.parseReader(resp.body().charStream()).getAsJsonObject();
				verifyActorToken(obj, actor, group);
				return obj;
			}else{
				LOG.warn("Response for actor token for user {} in group {} was not successful: {}", actor.activityPubID, group.activityPubID, resp);
			}
		}catch(Exception x){
			LOG.warn("Error fetching actor token for user {} in group {}", actor.activityPubID, group.activityPubID, x);
		}
		return null;
	}

	public static CollectionQueryResult performCollectionQuery(@NotNull Actor actor, @NotNull URI collectionID, @NotNull Collection<URI> query){
		actor.ensureRemote();
		if(actor.collectionQueryEndpoint==null)
			throw new IllegalArgumentException("This actor does not have a collection query endpoint");
		if(!collectionID.getHost().equals(actor.activityPubID.getHost()))
			throw new IllegalArgumentException("Collection ID and actor ID hostnames don't match");
		if(query.isEmpty())
			throw new IllegalArgumentException("Query is empty");
		Request.Builder builder=new Request.Builder()
				.url(HttpUrl.get(actor.collectionQueryEndpoint.toString()));
		FormBody.Builder body=new FormBody.Builder().add("collection", collectionID.toString());
		for(URI uri:query)
			body.add("item", uri.toString());
		builder.post(body.build());
		signRequest(builder, actor.collectionQueryEndpoint, actor, null, "post");
		try(Response resp=httpClient.newCall(builder.build()).execute()){
			if(resp.isSuccessful()){
				JsonElement el=JsonParser.parseReader(resp.body().charStream());
				JsonObject converted=JLDProcessor.convertToLocalContext(el.getAsJsonObject());
				ActivityPubObject aobj=ActivityPubObject.parse(converted);
				if(aobj==null)
					throw new UnsupportedRemoteObjectTypeException("Unsupported object type "+converted.get("type"));
				if(aobj instanceof CollectionQueryResult cqr){
					if(!collectionID.equals(cqr.partOf))
						throw new FederationException("part_of in the collection query result '"+cqr.partOf+"' does not match expected '"+collectionID+"'");
					return cqr;
				}else{
					throw new UnsupportedRemoteObjectTypeException("Expected object of type sm:CollectionQueryResult, got "+aobj.getType());
				}
			}else{
				LOG.warn("Response for collection query {} was not successful: {}", collectionID, resp);
				return CollectionQueryResult.empty(collectionID);
			}
		}catch(Exception x){
			LOG.warn("Error querying collection {}", collectionID, x);
			return CollectionQueryResult.empty(collectionID);
		}
	}
}
