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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;

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
import smithereen.exceptions.ObjectNotFoundExceptionWithFallback;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.http.ExtendedHttpClient;
import smithereen.http.FormBodyPublisherBuilder;
import smithereen.http.HttpContentType;
import smithereen.http.ReaderBodyHandler;
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
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XmlParser;
import spark.Request;
import spark.utils.StringUtils;

import static java.time.temporal.ChronoField.*;
import static smithereen.Utils.*;

public class ActivityPub{

	public static final URI AS_PUBLIC=URI.create(JLD.ACTIVITY_STREAMS+"#Public");
	public static final String CONTENT_TYPE="application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"";
	public static final HttpContentType EXPECTED_CONTENT_TYPE=new HttpContentType("application/ld+json", Map.of("profile", "https://www.w3.org/ns/activitystreams"));
	private static final Logger LOG=LoggerFactory.getLogger(ActivityPub.class);
	// Serializing and signing activities is compute-bound.
	// Make the whole thing more responsive while activities are sent out to many servers at once by running that on a separate platform thread pool.
	private static final ExecutorService serializerSignerExecutor=Executors.newCachedThreadPool(Thread.ofPlatform().name("ActivityPubSerializerSigner", 0).factory());

	public static final HttpClient httpClient;
	private static LruCache<String, String> domainRedirects=new LruCache<>(100);
	private static final ZoneId GMT_TIMEZONE=ZoneId.of("GMT");
	private static final DateTimeFormatter HTTP_DATE_FORMATTER;

	static{
		httpClient=ExtendedHttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(15))
				.build();

		Map<Long, String> dow = new HashMap<>();
		dow.put(1L, "Mon");
		dow.put(2L, "Tue");
		dow.put(3L, "Wed");
		dow.put(4L, "Thu");
		dow.put(5L, "Fri");
		dow.put(6L, "Sat");
		dow.put(7L, "Sun");
		Map<Long, String> moy = new HashMap<>();
		moy.put(1L, "Jan");
		moy.put(2L, "Feb");
		moy.put(3L, "Mar");
		moy.put(4L, "Apr");
		moy.put(5L, "May");
		moy.put(6L, "Jun");
		moy.put(7L, "Jul");
		moy.put(8L, "Aug");
		moy.put(9L, "Sep");
		moy.put(10L, "Oct");
		moy.put(11L, "Nov");
		moy.put(12L, "Dec");
		HTTP_DATE_FORMATTER = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseLenient()
				.optionalStart()
				.appendText(DAY_OF_WEEK, dow)
				.appendLiteral(", ")
				.optionalEnd()
				.appendValue(DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE)
				.appendLiteral(' ')
				.appendText(MONTH_OF_YEAR, moy)
				.appendLiteral(' ')
				.appendValue(YEAR, 4)  // 2 digit year not handled
				.appendLiteral(' ')
				.appendValue(HOUR_OF_DAY, 2)
				.appendLiteral(':')
				.appendValue(MINUTE_OF_HOUR, 2)
				.optionalStart()
				.appendLiteral(':')
				.appendValue(SECOND_OF_MINUTE, 2)
				.optionalEnd()
				.appendLiteral(' ')
				.appendOffset("+HHMM", "GMT")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
				.toFormatter();
	}

	public static ActivityPubObject fetchRemoteObject(URI _uri, Actor signer, JsonObject actorToken, ApplicationContext ctx, boolean acceptHTML) throws IOException{
		return fetchRemoteObjectInternal(_uri, signer, actorToken, ctx, true, acceptHTML);
	}

	private static ActivityPubObject fetchRemoteObjectInternal(URI _uri, Actor signer, JsonObject actorToken, ApplicationContext ctx, boolean tryHTML, boolean acceptHTML) throws IOException{
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

		String acceptHeader=CONTENT_TYPE;
		if(acceptHTML)
			acceptHeader+=",text/html;q=0.6,*/*;q=0.5";
		HttpRequest.Builder builder=HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(10))
				.header("Accept", acceptHeader);
		if(token!=null)
			builder.header("Authorization", "Bearer "+token);
		else if(actorToken!=null)
			builder.header("Authorization", "ActivityPubActorToken "+actorToken);
		signRequest(builder, uri, signer==null ? ServiceActor.getInstance() : signer, null, "get");
		HttpResponse<InputStream> resp;
		try{
			resp=httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
		}catch(InterruptedException x){
			throw new RuntimeException(x);
		}
		if(resp.statusCode()/100!=2){
			if(Config.DEBUG){
				StringBuilder sb=new StringBuilder();
				try(BufferedReader reader=new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))){
					char[] buf=new char[1024];
					int read;
					while((read=reader.read(buf))>0){
						sb.append(buf, 0, read);
					}
				}
				LOG.warn("Failed response body: {}", sb);
			}else{
				try(InputStream in=resp.body()){
					while(in.skip(8192)>0L);
				}
			}
			throw new ObjectNotFoundException("Response is not successful: remote server returned "+resp.statusCode()+" for GET "+uri);
		}
		HttpContentType contentType=HttpContentType.from(resp.headers());
		try(InputStream in=resp.body()){
			if(tryHTML && contentType.matches("text/html")){
				LOG.trace("Received HTML, trying to extract <link>");
				org.jsoup.nodes.Document htmlDocument=Jsoup.parse(in, contentType.getCharset().name(), uri.toString());
				for(Element el:htmlDocument.select("link[rel=alternate]")){
					LOG.trace("Candidate element: {}", el);
					String type=el.attr("type");
					if("application/activity+json".equals(type) || CONTENT_TYPE.equals(type)){
						String url=el.absUrl("href");
						LOG.trace("Will follow redirect: {}", url);
						if(StringUtils.isNotEmpty(url)){
							try{
								return fetchRemoteObjectInternal(UriBuilder.parseAndEncode(url), signer, actorToken, ctx, false, false);
							}catch(URISyntaxException x){
								throw new ObjectNotFoundExceptionWithFallback("Failed to parse URL from <link rel=\"alternate\"> on HTML page at "+uri, x, htmlDocument);
							}
						}
					}
				}
				throw new ObjectNotFoundExceptionWithFallback("Received HTML that doesn't contain a <link>", htmlDocument);
			}
			// Allow "application/activity+json" or "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\""
			if(!contentType.matches("application/activity+json") && !contentType.matches(EXPECTED_CONTENT_TYPE)){
				throw new ObjectNotFoundException("Invalid Content-Type for "+uri+": "+contentType);
			}

			try{
				JsonElement el=JsonParser.parseReader(new InputStreamReader(in, contentType.getCharset()));
				JsonObject converted=JLDProcessor.convertToLocalContext(el.getAsJsonObject());
				ActivityPubObject obj=ActivityPubObject.parse(converted);
				if(obj==null)
					throw new UnsupportedRemoteObjectTypeException("Unsupported object type "+converted.get("type"));
				if(obj.activityPubID!=null && !Utils.uriHostMatches(obj.activityPubID, uri))
					throw new BadRequestException("Domain in object ID ("+obj.activityPubID+") doesn't match domain in its URI ("+uri+")");
				return obj;
			}catch(JLDException|JsonParseException x){
				LOG.error("Exception while parsing or converting remote object", x);
				throw new IOException(x);
			}
		}
	}

	private static HttpRequest.Builder signRequest(HttpRequest.Builder builder, URI url, Actor actor, byte[] body, String method){
		Future<HttpRequest.Builder> f=serializerSignerExecutor.submit(()->{
			String path=url.getPath();
			String query=url.getRawQuery();
			if(StringUtils.isNotEmpty(query))
				path+="?"+query;
			String host=url.getHost();
			if(url.getPort()!=-1)
				host+=":"+url.getPort();
			String date=HTTP_DATE_FORMATTER.format(ZonedDateTime.now(GMT_TIMEZONE));
			String digestHeader;
			if(body!=null){
				digestHeader="SHA-256=";
				try{
					digestHeader+=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
				}catch(NoSuchAlgorithmException x){
					throw new RuntimeException(x);
				}
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
		});
		try{
			return f.get();
		}catch(InterruptedException|ExecutionException e){
			throw new RuntimeException(e);
		}
	}

	public static void postActivity(URI inboxUrl, Activity activity, Actor actor, ApplicationContext ctx, boolean isRetry, EnumSet<Server.Feature> requiredServerFeatures, boolean throwFor403) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		Server server=ctx.getModerationController().getServerByDomain(inboxUrl.getAuthority());
		if(server.getAvailability()==Server.Availability.DOWN){
			LOG.debug("Not sending {} activity to server {} because it's down", activity.getType(), server.host());
			return;
		}
		if(requiredServerFeatures!=null && !requiredServerFeatures.isEmpty() && !server.features().containsAll(requiredServerFeatures)){
			LOG.debug("Not sending {} activity to server {} because its feature set {} does not include required features {}", activity.getType(), server.host(), server.features(), requiredServerFeatures);
			return;
		}
		if(server.restriction()!=null){
			if(server.restriction().type==FederationRestriction.RestrictionType.SUSPENSION){
				LOG.debug("Not sending {} activity to server {} because federation with it is blocked", activity.getType(), server.host());
				return;
			}
		}

		Future<JsonObject> f=serializerSignerExecutor.submit(()->{
			JsonObject body=activity.asRootActivityPubObject(ctx, inboxUrl.getAuthority());
			LinkedDataSignatures.sign(body, actor.privateKey, actor.activityPubID+"#main-key");
			return body;
		});
		JsonObject body;
		try{
			body=f.get();
		}catch(InterruptedException|ExecutionException x){
			LOG.warn("Failed to serialize and sign activity to send to {}", inboxUrl, x);
			return;
		}
		LOG.debug("Sending activity: {}", body);
		postActivityInternal(inboxUrl, body.toString(), actor, server, ctx, isRetry, throwFor403);
	}

	public static void forwardActivity(URI inboxUrl, String activityJson, Actor actor, ApplicationContext ctx, EnumSet<Server.Feature> requiredServerFeatures) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		Server server=ctx.getModerationController().getServerByDomain(inboxUrl.getAuthority());
		if(server.getAvailability()==Server.Availability.DOWN){
			LOG.debug("Not forwarding activity to server {} because it's down", server.host());
			return;
		}
		if(requiredServerFeatures!=null && !requiredServerFeatures.isEmpty() && !server.features().containsAll(requiredServerFeatures)){
			LOG.debug("Not forwarding activity to server {} because its feature set {} does not include required features {}", server.host(), server.features(), requiredServerFeatures);
			return;
		}
		if(server.restriction()!=null){
			if(server.restriction().type==FederationRestriction.RestrictionType.SUSPENSION){
				LOG.debug("Not forwarding activity to server {} because federation with it is blocked", server.host());
				return;
			}
		}

		postActivityInternal(inboxUrl, activityJson, actor, server, ctx, false, false);
	}

	private static void postActivityInternal(URI inboxUrl, String activityJson, Actor actor, Server server, ApplicationContext ctx, boolean isRetry, boolean throwFor403) throws IOException{
		if(actor.privateKey==null)
			throw new IllegalArgumentException("Sending an activity requires an actor that has a private key on this server.");

		byte[] body=activityJson.getBytes(StandardCharsets.UTF_8);
		HttpRequest req=signRequest(
				HttpRequest.newBuilder(inboxUrl)
						.header("Content-Type", CONTENT_TYPE)
						.POST(HttpRequest.BodyPublishers.ofByteArray(body)),
				inboxUrl, actor, body, "post")
				.timeout(Duration.ofSeconds(30))
				.build();
		try{
			HttpResponse<String> resp=httpClient.send(req, HttpResponse.BodyHandlers.ofString());
			LOG.debug("Post activity response: {}", resp);
			if(resp.statusCode()/100!=2){
				LOG.debug("Response body: {}", resp.body());
				if(resp.statusCode()!=403){
					if(resp.statusCode()/100==5){ // IOException does trigger retrying, FederationException does not. We want retries for 5xx (server) errors.
						throw new IOException("Response from "+inboxUrl+" is not successful: "+resp.statusCode());
					}else{
						throw new FederationException("Response from "+inboxUrl+" is not successful: "+resp.statusCode());
					}
				}else if(throwFor403){
					throw new UserActionNotAllowedException("Action not allowed: "+resp.body());
				}
				return;
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
		}catch(InterruptedException ignored){}
	}

	public static boolean isPublic(URI uri){
		return uri.equals(AS_PUBLIC) || ("as".equals(uri.getScheme()) && "Public".equals(uri.getSchemeSpecificPart()));
	}

	private static WebfingerResponse doWebfingerRequest(String resource, String domain, String uriTemplate) throws IOException{
		URI url;
		if(StringUtils.isEmpty(uriTemplate)){
			url=new UriBuilder()
					.scheme(Config.useHTTP ? "http" : "https")
					.authority(domain)
					.path(".well-known", "webfinger")
					.queryParam("resource", resource)
					.build();
		}else{
			url=URI.create(uriTemplate.replace("{uri}", URLEncoder.encode(resource, StandardCharsets.UTF_8)));
		}
		HttpRequest req=HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(10)).build();
		HttpResponse<Reader> resp;
		try{
			resp=httpClient.send(req, new ReaderBodyHandler());
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		try(Reader reader=resp.body()){
			if(resp.statusCode()/100==2){
				WebfingerResponse wr=Utils.gson.fromJson(reader, WebfingerResponse.class);
				return wr;
			}else if(resp.statusCode()==404){
				throw new ObjectNotFoundException("User "+resource+" does not exist");
			}else{
				throw new IOException("Failed to resolve username "+resource+", response code "+resp.statusCode());
			}
		}catch(JsonParseException x){
			throw new IOException("Response parse failed", x);
		}
	}

	private static URI tryGetActorIDFromWebfinger(String username, String domain, String uriTemplate) throws IOException{
		String resource="acct:"+username+"@"+domain;
		URI link=doWebfingerRequest(resource, domain, uriTemplate).getActivityPubActorID();
		if(link!=null){
			LOG.trace("Successfully resolved {}@{} to {}", username, domain, link);
			return link;
		}
		throw new IOException("Link not found");

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
			URI uri=tryGetActorIDFromWebfinger(username, domain, redirect);
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
				HttpRequest req=HttpRequest.newBuilder(new UriBuilder().scheme(Config.useHTTP ? "http" : "https").authority(domain).path(".well-known", "host-meta").build())
						.header("Accept", "application/xrd+xml")
						.timeout(Duration.ofSeconds(10))
						.build();
				HttpResponse<InputStream> resp;
				try{
					resp=httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
				}catch(InterruptedException e){
					throw new RuntimeException(e);
				}
				try(InputStream in=resp.body()){
					if(resp.statusCode()/100==2){
						DocumentBuilder builder=XmlParser.newDocumentBuilder();
						Document doc=builder.parse(in);
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
											return tryGetActorIDFromWebfinger(username, domain, template);
										}else{
											throw new ObjectNotFoundException("Malformed URI template '"+template+"' in host-meta domain redirect", x);
										}
									}
								}
							}
						}
					}
				}catch(SAXException e){
					throw new ObjectNotFoundException("Webfinger returned 404 and host-meta can't be parsed", e);
				}
			}
			throw new ObjectNotFoundException(x);
		}
	}

	public static Actor verifyHttpSignature(spark.Request req, Actor userHint) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
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

		Instant date=Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(req.headers("date")));
		Instant now=Instant.now();
		Instant minValidDate=now.minus(5, ChronoUnit.MINUTES);
		Instant maxValidDate=now.plus(5, ChronoUnit.MINUTES);
		if(date.isAfter(maxValidDate))
			throw new BadRequestException("Date is too far in the future (difference: "+now.until(date, ChronoUnit.SECONDS)+"s)");
		if(date.isBefore(minValidDate))
			throw new BadRequestException("Date is too far in the past (difference: "+now.until(date, ChronoUnit.SECONDS)+"s)");

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
				String query=req.queryString();
				if(StringUtils.isNotEmpty(query))
					value+=query;
			}else{
				value=req.headers(header);
			}
			sigParts.add(header+": "+value);
		}
		String sigStr=java.lang.String.join("\n", sigParts);
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

	public static String getRequesterDomain(Request req){
		if(req.headers("signature")==null) // Avoids needlessly polluting logs
			return null;
		try{
			return verifyHttpSignature(req, null).domain;
		}catch(Exception x){
			LOG.trace("Failed to verify signature header: {}", req.headers("signature"), x);
			return null;
		}
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
		HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
		signRequest(builder, group.actorTokenEndpoint, actor, null, "get");
		try{
			HttpResponse<Reader> resp=httpClient.send(builder.build(), new ReaderBodyHandler());
			try(Reader reader=resp.body()){
				if(resp.statusCode()/100==2){
					JsonObject obj=JsonParser.parseReader(reader).getAsJsonObject();
					verifyActorToken(obj, actor, group);
					return obj;
				}else{
					LOG.warn("Response for actor token for user {} in group {} was not successful: {}", actor.activityPubID, group.activityPubID, resp);
				}
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
		HttpRequest.Builder builder=HttpRequest.newBuilder(actor.collectionQueryEndpoint).timeout(Duration.ofSeconds(10));
		FormBodyPublisherBuilder body=new FormBodyPublisherBuilder().add("collection", collectionID.toString());
		for(URI uri:query)
			body.add("item", uri.toString());
		builder.POST(body.build()).header("Content-Type", FormBodyPublisherBuilder.CONTENT_TYPE);
		signRequest(builder, actor.collectionQueryEndpoint, actor, null, "post");
		try{
			HttpResponse<Reader> resp=httpClient.send(builder.build(), new ReaderBodyHandler());
			try(Reader reader=resp.body()){
				if(resp.statusCode()/100==2){
					JsonElement el=JsonParser.parseReader(reader);
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
			}
		}catch(Exception x){
			LOG.warn("Error querying collection {}", collectionID, x);
			return CollectionQueryResult.empty(collectionID);
		}
	}

	public static String resolveRemoteInteractionUriTemplate(String username, String domain){
		String resource;
		if(username==null){
			resource="https://"+domain;
		}else{
			resource="acct:"+username+"@"+domain;
		}
		try{
			String redirect;
			synchronized(ActivityPub.class){
				redirect=domainRedirects.get(domain);
			}
			WebfingerResponse resp=doWebfingerRequest(resource, domain, redirect);
			for(WebfingerResponse.Link link:resp.links){
				if("http://ostatus.org/schema/1.0/subscribe".equals(link.rel)){
					String template=link.template;
					if(template!=null && template.startsWith("https://") && template.contains("{uri}"))
						return template;
				}
			}
		}catch(ObjectNotFoundException x){
			if(username!=null)
				throw new ObjectNotFoundException("Failed to resolve remote interaction URI", x);
		}catch(IOException x){
			throw new ObjectNotFoundException("Failed to resolve remote interaction URI", x);
		}
		if(username==null){
			return "https://"+domain+"/authorize_interaction?uri={uri}";
		}
		throw new ObjectNotFoundException("Failed to resolve remote interaction URI");
	}
}
