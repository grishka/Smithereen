package smithereen.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnauthorizedRequestException;
import smithereen.exceptions.UserErrorException;
import smithereen.http.StructuredHttpHeaders;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.fasp.FASPCapability;
import smithereen.model.fasp.FASPDebugCallback;
import smithereen.model.fasp.FASPProvider;
import smithereen.model.fasp.responses.FASPProviderInfoResponse;
import smithereen.storage.FASPStorage;
import smithereen.util.CryptoUtils;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import smithereen.util.validation.ObjectValidationException;
import smithereen.util.validation.ObjectValidator;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class FASPController{
	private static final Logger LOG=LoggerFactory.getLogger(FASPController.class);
	private final ApplicationContext context;
	private final ExecutorService executor=Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("FASPRequestThread-", 0).factory());
	private int unconfirmedCount=-1;

	public FASPController(ApplicationContext context){
		this.context=context;
	}

	public long createRegistration(String name, URI baseUrl, String remoteID, PublicKey publicKey, PrivateKey privateKey){
		try{
			unconfirmedCount=-1;
			long id=FASPStorage.createOrUpdateFaspProvider(name, baseUrl, remoteID, publicKey.getEncoded(), privateKey.getEncoded());
			executor.submit(()->{
				try{
					reloadProviderInfo(getProvider(id));
				}catch(UserErrorException x){
					LOG.warn("Can't load provider into for {}", baseUrl, x.getCause());
				}
			});
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public FASPProvider getProvider(long id){
		try{
			FASPProvider provider=FASPStorage.getFaspProvider(id);
			if(provider==null)
				throw new ObjectNotFoundException();
			return provider;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void reloadProviderInfo(FASPProvider provider){
		try{
			FASPProviderInfoResponse info=doRequest(provider, "/provider_info", "GET", FASPProviderInfoResponse.class);

			String privacyPolicyJson;
			if(info.privacyPolicy()==null){
				privacyPolicyJson=null;
			}else{
				Map<String, String> privacyURLs=info.privacyPolicy()
						.stream()
						.filter(l->l.language()!=null && l.url()!=null)
						.collect(Collectors.toMap(FASPProviderInfoResponse.PrivacyPolicyLink::language, l->l.url().toString()));
				privacyPolicyJson=privacyURLs.isEmpty() ? null : Utils.gson.toJson(privacyURLs);
			}

			int actorID=0;
			if(StringUtils.isNotEmpty(info.fediverseAccount())){
				try{
					ObjectLinkResolver.UsernameResolutionResult usernameRes=context.getObjectLinkResolver().resolveUsername(info.fediverseAccount(), true, EnumSet.of(ObjectLinkResolver.UsernameOwnerType.USER, ObjectLinkResolver.UsernameOwnerType.GROUP));
					actorID=switch(usernameRes.type()){
						case USER -> usernameRes.localID();
						case GROUP -> -usernameRes.localID();
						default -> throw new IllegalStateException("Unexpected value "+usernameRes.type());
					};
				}catch(ObjectNotFoundException ignored){}
			}

			FASPStorage.updateFaspProvider(provider.id, info.name(), info.signInUrl(),
					Utils.gson.toJson(info.capabilities().stream().collect(Collectors.toMap(FASPProviderInfoResponse.VersionedCapability::id, FASPProviderInfoResponse.VersionedCapability::version))),
					privacyPolicyJson, info.contactEmail(), actorID);
		}catch(IOException | UnauthorizedRequestException | ObjectValidationException x){
			LOG.debug("Failed to reload info for {}", provider.baseUrl, x);
			throw new UserErrorException("admin_fasp_get_info_error", x).withCauseMessage();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getUnconfirmedProviderCount(){
		if(unconfirmedCount!=-1)
			return unconfirmedCount;
		try{
			return unconfirmedCount=FASPStorage.getUnconfirmedFaspProviderCount();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<FASPProvider> getProviders(boolean confirmed){
		try{
			return FASPStorage.getAllFaspProviders(confirmed);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFaspRegistrationRequest(FASPProvider provider){
		if(provider.confirmed)
			throw new IllegalArgumentException("FASP already confirmed");
		try{
			FASPStorage.deleteFaspProvider(provider.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		unconfirmedCount--;
		// TODO audit log
	}

	public Map<FASPCapability, Throwable> confirmFaspRegistrationRequest(FASPProvider provider, EnumSet<FASPCapability> capabilities){
		if(provider.confirmed)
			throw new IllegalArgumentException("FASP already confirmed");
		try{
			FASPStorage.setFaspProviderConfirmed(provider.id);
			List<FASPCapability> capabilitiesAsList=capabilities.stream().toList();
			List<Callable<Void>> tasks=capabilities.stream().map(cap->(Callable<Void>)()->{
				setCapabilityState(provider, cap, cap.getSupportedVersion(), true);
				return null;
			}).toList();
			List<Future<Void>> results=executor.invokeAll(tasks);
			synchronized(provider){
				FASPStorage.updateFaspProviderEnabledCapabilities(provider.id, provider.enabledCapabilities.keySet().stream().collect(Collectors.toMap(cap->cap.id, provider.enabledCapabilities::get)));
			}
			HashMap<FASPCapability, Throwable> errors=new HashMap<>();
			int i=0;
			for(Future<Void> result:results){
				try{
					result.get();
				}catch(ExecutionException x){
					Throwable actualException=x.getCause();
					if(actualException instanceof UserErrorException uex){
						actualException=uex.getCause();
					}
					errors.put(capabilitiesAsList.get(i), actualException);
				}
				i++;
			}
			unconfirmedCount--;
			return errors;
		}catch(SQLException | InterruptedException x){
			throw new InternalServerErrorException(x);
		}
		// TODO audit log
	}

	public Map<FASPCapability, Throwable> setProviderCapabilities(FASPProvider provider, EnumSet<FASPCapability> capabilities){
		if(!provider.confirmed)
			throw new IllegalArgumentException("FASP not confirmed");
		try{
			List<FASPCapability> toEnable=capabilities.stream().filter(c->!provider.enabledCapabilities.containsKey(c)).toList();
			List<FASPCapability> toDisable=provider.enabledCapabilities.keySet().stream().filter(c->!capabilities.contains(c)).toList();
			LOG.debug("New capabilities for {}: enabling {}, disabling {}", provider.baseUrl, toEnable, toDisable);
			if(toEnable.isEmpty() && toDisable.isEmpty())
				return Map.of();
			List<FASPCapability> capabilitiesAsList=capabilities.stream().toList();
			ArrayList<Callable<Void>> tasks=new ArrayList<>();
			for(FASPCapability cap:toEnable){
				tasks.add(()->{
					setCapabilityState(provider, cap, cap.getSupportedVersion(), true);
					return null;
				});
			}
			for(FASPCapability cap:toDisable){
				tasks.add(()->{
					setCapabilityState(provider, cap, provider.enabledCapabilities.get(cap), false);
					return null;
				});
			}
			List<Future<Void>> results=executor.invokeAll(tasks);
			synchronized(provider){
				FASPStorage.updateFaspProviderEnabledCapabilities(provider.id, provider.enabledCapabilities.keySet().stream().collect(Collectors.toMap(cap->cap.id, provider.enabledCapabilities::get)));
			}
			HashMap<FASPCapability, Throwable> errors=new HashMap<>();
			int i=0;
			for(Future<Void> result:results){
				try{
					result.get();
				}catch(ExecutionException x){
					Throwable actualException=x.getCause();
					if(actualException instanceof UserErrorException uex){
						actualException=uex.getCause();
					}
					errors.put(capabilitiesAsList.get(i), actualException);
				}
				i++;
			}
			return errors;
		}catch(SQLException | InterruptedException x){
			throw new InternalServerErrorException(x);
		}
		// TODO audit log
	}

	private void setCapabilityState(FASPProvider provider, FASPCapability capability, String version, boolean active){
		if(!provider.capabilities.containsKey(capability))
			throw new IllegalArgumentException("This provider does not support this capability");
		if((provider.enabledCapabilities.containsKey(capability) && active) || (!provider.enabledCapabilities.containsKey(capability) && !active))
			return;
		try{
			doRequestWithoutResult(provider, "/capabilities/"+capability.id+"/"+version.split("\\.")[0]+"/activation", active ? "POST" : "DELETE", null);
			synchronized(provider){
				if(active)
					provider.enabledCapabilities.put(capability, version);
				else
					provider.enabledCapabilities.remove(capability);
			}
		}catch(IOException | UnauthorizedRequestException | ObjectValidationException x){
			LOG.debug("Failed to activate capability {} on {}", capability, provider.baseUrl, x);
			throw new UserErrorException(x);
		}
	}

	public void deleteProvider(FASPProvider provider){
		if(!provider.confirmed)
			throw new IllegalArgumentException();

		try{
			List<Callable<Void>> tasks=provider.enabledCapabilities.entrySet().stream().map(cap->(Callable<Void>) ()->{
				setCapabilityState(provider, cap.getKey(), cap.getValue(), true);
				return null;
			}).toList();
			executor.invokeAll(tasks);
		}catch(InterruptedException interruptedExceptionIsSoFuckingStupidIHateIt){
			throw new RuntimeException(interruptedExceptionIsSoFuckingStupidIHateIt);
		}
		try{
			FASPStorage.deleteFaspProvider(provider.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<FASPDebugCallback> getProviderDebugCallbacks(FASPProvider provider, int offset, int count){
		try{
			return FASPStorage.getFaspDebugCallbacks(provider.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void putDebugCallback(FASPProvider provider, InetAddress ip, String body){
		try{
			FASPStorage.putDebugCallback(provider.id, ip, body);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void makeDebugCallbackRequest(FASPProvider provider){
		if(!provider.confirmed || !provider.enabledCapabilities.containsKey(FASPCapability.DEBUG))
			throw new IllegalArgumentException();
		try{
			doRequestWithoutResult(provider, "/debug/v0/callback/logs", "POST", Map.of("hello", List.of("world", "fediverse")));
		}catch(IOException x){
			throw new UserErrorException(x);
		}
	}

	private <R, B> R doRequest(FASPProvider provider, String pathAndQuery, String method, Class<R> responseType, B body) throws IOException{
		URI baseUrl=provider.baseUrl.getPath().endsWith("/") ? provider.baseUrl : URI.create(provider.baseUrl.toString()+"/");
		URI requestUrl=baseUrl.resolve(pathAndQuery.startsWith("/") ? pathAndQuery.substring(1) : pathAndQuery);
		HttpRequest.Builder reqBuilder=HttpRequest.newBuilder(requestUrl)
				.header("Accept", "application/json");
		byte[] bodyBytes;
		if(body!=null){
			bodyBytes=Utils.gson.toJson(body).getBytes(StandardCharsets.UTF_8);
			reqBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
					.header("Content-Type", "application/json");
		}else{
			reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
			bodyBytes=new byte[0];
		}

		try{
			LOG.trace("Sending request: {} {}, {} bytes", method, requestUrl, bodyBytes.length);
			HttpRequest req=signRequestOrResponse(provider, reqBuilder, bodyBytes);
			HttpResponse<byte[]> resp=ActivityPub.httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
			FileOutputStream out=new FileOutputStream("/Users/grishka/Downloads/fasp_error.html");
			out.write(resp.body());
			out.close();
			LOG.trace("Response: {}, {} bytes", resp, resp.body().length);
			if(resp.statusCode()/100!=2)
				throw new IOException("Response not successful: "+resp.statusCode());
			verifyRequestOrResponseSignature(provider, resp, resp.body());
			if(responseType==null)
				return null;
			String respBody=new String(resp.body(), StandardCharsets.UTF_8);
			R result=Utils.gson.fromJson(respBody, responseType);
			ObjectValidator.validate(result);
			return result;
		}catch(InterruptedException ignored){}

		return null;
	}

	private <R> R doRequest(FASPProvider provider, String pathAndQuery, String method, Class<R> responseType) throws IOException{
		return doRequest(provider, pathAndQuery, method, responseType, null);
	}

	private <B> void doRequestWithoutResult(FASPProvider provider, String pathAndQuery, String method, B body) throws IOException{
		doRequest(provider, pathAndQuery, method, null, body);
	}

	public <T> T signRequestOrResponse(FASPProvider provider, Object requestOrResponse, byte[] bodyBytes){
		byte[] sha256=CryptoUtils.sha256(bodyBytes);
		String contentDigestHeader=StructuredHttpHeaders.serialize(Map.of("sha-256", StructuredHttpHeaders.Item.of(sha256)));

		List<String> signedFields;
		List<String> signatureBaseLines=new ArrayList<>();
		if(requestOrResponse instanceof HttpRequest.Builder rb){
			signedFields=List.of("@method", "@target-uri", "content-digest");
			HttpRequest tmpRequest=rb.build();
			signatureBaseLines.add("\"@method\": "+tmpRequest.method().toUpperCase());
			signatureBaseLines.add("\"@target-uri\": "+tmpRequest.uri());
		}else if(requestOrResponse instanceof Response resp){
			signedFields=List.of("@status", "content-digest");
			signatureBaseLines.add("\"@status\": "+resp.status());
		}else{
			throw new IllegalArgumentException();
		}
		long sigTime=System.currentTimeMillis()/1000;
		StructuredHttpHeaders.InnerList signatureParameters=new StructuredHttpHeaders.InnerList(signedFields.stream().map(StructuredHttpHeaders.Item::of).toList());
		String signatureInputHeader=StructuredHttpHeaders.serialize(Map.of("sig1", signatureParameters
				.withParam("created", StructuredHttpHeaders.BareItem.of(sigTime))
				.withParam("keyid", StructuredHttpHeaders.BareItem.of(provider.remoteID))
		));

		signatureBaseLines.add("\"content-digest\": "+contentDigestHeader);
		signatureBaseLines.add("\"@signature-params\": "+signatureInputHeader.substring(signatureInputHeader.indexOf('=')+1));

		String signatureBase=String.join("\n", signatureBaseLines);

		String signatureHeader;
		try{
			Signature sig=Signature.getInstance("Ed25519");
			sig.initSign(provider.privateKey);
			sig.update(signatureBase.getBytes(StandardCharsets.US_ASCII));
			byte[] signature=sig.sign();
			signatureHeader=StructuredHttpHeaders.serialize(Map.of("sig1", StructuredHttpHeaders.Item.of(signature)));
		}catch(NoSuchAlgorithmException | InvalidKeyException | SignatureException x){
			throw new RuntimeException(x);
		}

		if(requestOrResponse instanceof HttpRequest.Builder rb){
			//noinspection unchecked
			return (T)rb.header("Content-Digest", contentDigestHeader)
					.header("Signature-Input", signatureInputHeader)
					.header("Signature", signatureHeader)
					.build();
		}else if(requestOrResponse instanceof Response resp){
			resp.header("Content-Digest", contentDigestHeader);
			resp.header("Signature-Input", signatureInputHeader);
			resp.header("Signature", signatureHeader);
			//noinspection unchecked
			return (T)resp;
		}else{
			throw new IllegalArgumentException();
		}
	}

	public FASPProvider verifyRequestOrResponseSignature(FASPProvider provider, Object requestOrResponse, byte[] bodyBytes){
		Map<String, String> headers;
		int status;
		URI url;
		String method;
		if(requestOrResponse instanceof HttpResponse<?> resp){
			headers=new HashMap<>();
			resp.headers().map().forEach((k, v)->headers.put(k.toLowerCase(), String.join(", ", v)));
			status=resp.statusCode();
			url=null;
			method=null;
		}else if(requestOrResponse instanceof Request req){
			headers=req.headers().stream().collect(Collectors.toMap(String::toLowerCase, req::headers));
			status=0;
			String query=req.queryString();
			if(query==null)
				query="";
			url=new UriBuilder(URI.create(req.url()+query)).scheme("https").build();
			method=req.requestMethod().toUpperCase();
		}else{
			throw new IllegalArgumentException();
		}

		String contentDigestHeader=headers.get("content-digest");
		if(contentDigestHeader==null)
			throw new UnauthorizedRequestException("Content-Digest header is missing");
		byte[] contentDigest;
		try{
			Map<String, StructuredHttpHeaders.ItemOrInnerList> parsedDict=StructuredHttpHeaders.parseDictionary(contentDigestHeader);
			if(!(parsedDict.get("sha-256") instanceof StructuredHttpHeaders.Item item))
				throw new UnauthorizedRequestException("Content-Digest does not contain sha-256");
			if(!(item.item() instanceof StructuredHttpHeaders.BareItem.ByteSequenceItem(byte[] value)))
				throw new UnauthorizedRequestException("sha-256 in Content-Digest is not a byte sequence (between :s)");
			contentDigest=value;
		}catch(IllegalArgumentException x){
			throw new UnauthorizedRequestException("Content-Digest header is malformed");
		}
		if(!Arrays.equals(CryptoUtils.sha256(bodyBytes), contentDigest))
			throw new UnauthorizedRequestException("Content-Digest sha-256 does not match the expected value");

		String signatureInputHeader=headers.get("signature-input");
		String signatureHeader=headers.get("signature");
		if(signatureInputHeader==null)
			throw new UnauthorizedRequestException("Signature-Input header is missing");
		if(signatureHeader==null)
			throw new UnauthorizedRequestException("Signature header is missing");

		String keyID;
		long createdAt;
		byte[] signature;
		List<String> signedHeaders;
		String signatureParams;
		try{
			Map<String, StructuredHttpHeaders.ItemOrInnerList> parsedInput=StructuredHttpHeaders.parseDictionary(signatureInputHeader);
			Map<String, StructuredHttpHeaders.ItemOrInnerList> parsedSig=StructuredHttpHeaders.parseDictionary(signatureHeader);
			String key=parsedInput.keySet().stream().toList().getFirst();
			if(!(parsedInput.get(key) instanceof StructuredHttpHeaders.InnerList input))
				throw new UnauthorizedRequestException("Signature-Input must contain an inner list");
			if(!(parsedSig.get(key) instanceof StructuredHttpHeaders.Item item && item.item() instanceof StructuredHttpHeaders.BareItem.ByteSequenceItem(byte[] signatureValue)))
				throw new UnauthorizedRequestException("Signature must contain a byte sequence item");
			if(!(input.parameters.get("created") instanceof StructuredHttpHeaders.BareItem.IntegerItem(long createdValue)))
				throw new UnauthorizedRequestException("'created' in Signature-Input must exist and be an integer");
			if(input.parameters.get("keyid") instanceof StructuredHttpHeaders.BareItem.StringItem(String keyIdValue)){
				keyID=keyIdValue;
			}else if(provider==null){
				throw new UnauthorizedRequestException("'keyid' in Signature-Input must exist and be a string");
			}else{
				keyID=null;
			}

			createdAt=createdValue;
			signature=signatureValue;
			signedHeaders=input.stream().map(i->{
				if(i.item() instanceof StructuredHttpHeaders.BareItem.StringItem(String value))
					return value;
				throw new UnauthorizedRequestException("Header names in Signature-Input must be strings");
			}).toList();
			int indexOfComma=signatureInputHeader.indexOf(',');
			signatureParams=signatureInputHeader.substring(signatureInputHeader.indexOf(key)+key.length()+1, indexOfComma==-1 ? signatureInputHeader.length() : indexOfComma);
		}catch(IllegalArgumentException x){
			throw new UnauthorizedRequestException("Signature or Signature-Input headers are malformed", x);
		}

		if(provider==null){
			long id=XTEA.decodeObjectID(keyID, ObfuscatedObjectIDType.FASP_PROVIDER);
			try{
				provider=getProvider(id);
			}catch(ObjectNotFoundException x){
				throw new UnauthorizedRequestException("Unknown keyid", x);
			}
		}

		long now=System.currentTimeMillis()/1000;
		if(Math.abs(now-createdAt)>300)
			throw new UnauthorizedRequestException("Time difference between 'created' and now exceeds 5 minutes");

		if(status==0){ // it's a request
			if(!signedHeaders.contains("@target-uri") || !signedHeaders.contains("@method") || !signedHeaders.contains("content-digest"))
				throw new UnauthorizedRequestException("Signed headers must contain at least @target-uri, @method, and content-digest");
		}else{ // it's a response
			if(!signedHeaders.contains("@status") || !signedHeaders.contains("content-digest"))
				throw new UnauthorizedRequestException("Signed headers must contain at least @status and content-digest");
		}
		ArrayList<String> signatureBaseLines=new ArrayList<>();
		for(String header:signedHeaders){
			String headerValue=switch(header){
				case "@method" -> method;
				case "@authority" -> url==null ? "" : url.getAuthority();
				case "@scheme" -> "https";
				case "@target-uri" -> url==null ? "" : url.toString();
				case "@request-target" -> url==null ? "" : url.getRawPath()+url.getRawQuery();
				case "@path" -> url==null ? "" : url.getRawPath();
				case "@query" -> url==null ? "" : url.getRawQuery();
				case "@query-param" -> throw new UnauthorizedRequestException("@query-param is not supported");
				case "@status" -> String.valueOf(status);
				default -> {
					if(header.startsWith("@"))
						throw new UnauthorizedRequestException("Unknown derived value "+header);
					yield headers.get(header);
				}
			};
			signatureBaseLines.add('"'+header+"\": "+headerValue);
		}
		signatureBaseLines.add("\"@signature-params\": "+signatureParams);
		String signatureBase=String.join("\n", signatureBaseLines);

		try{
			Signature sig=Signature.getInstance("Ed25519");
			sig.initVerify(provider.publicKey);
			sig.update(signatureBase.getBytes(StandardCharsets.US_ASCII));
			if(!sig.verify(signature))
				throw new UnauthorizedRequestException("Failed to verify signature");
		}catch(NoSuchAlgorithmException | InvalidKeyException | SignatureException x){
			throw new RuntimeException(x);
		}

		return provider;
	}
}
