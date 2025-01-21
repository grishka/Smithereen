package smithereen.storage.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;

import smithereen.Config;
import smithereen.Utils;
import smithereen.http.ExtendedHttpClient;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.media.MediaFileID;
import smithereen.storage.ImgProxy;
import smithereen.storage.utils.Pair;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import smithereen.util.XmlParser;

public class S3MediaFileStorageDriver extends MediaFileStorageDriver{
	private static final Logger LOG=LoggerFactory.getLogger(S3MediaFileStorageDriver.class);
	private static final DateTimeFormatter DATE_TIME_FORMATTER=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
	private static final DateTimeFormatter DATE_FORMATTER=DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String XMLNS="http://s3.amazonaws.com/doc/2006-03-01/";

	private final Config.S3Configuration config;
	private final String publicHostname, apiHostname;
	private final HttpClient httpClient;

	public S3MediaFileStorageDriver(Config.S3Configuration config){
		this.config=config;
		if(config.aliasHost()!=null){
			publicHostname=config.aliasHost();
		}else if(config.hostname()!=null){
			publicHostname=config.hostname();
		}else{
			publicHostname="s3-"+config.region()+".amazonaws.com";
		}
		if(config.endpoint()!=null){
			apiHostname=config.endpoint();
		}else{
			apiHostname="s3."+config.region()+".amazonaws.com";
		}
		LOG.trace("Loaded configuration: {}", config.toString().replace(config.secretKey(), "***"));

		httpClient=ExtendedHttpClient.newHttpClient();
	}

	@Override
	public void storeFile(File localFile, MediaFileID id, boolean keepLocalFile) throws IOException{
		HttpRequest req=HttpRequest.newBuilder(getApiUrl().appendPath(getObjectName(id)).build())
				.PUT(HttpRequest.BodyPublishers.ofFile(localFile.toPath()))
				.header("Content-Type", id.type().getMimeType())
				.timeout(Duration.ofSeconds(60))
				.build();
		try(FileInputStream in=new FileInputStream(localFile)){
			req=signRequest(req, in);
		}
		executeRequest(req, HttpResponse.BodyHandlers.ofString());
		if(!keepLocalFile)
			localFile.delete();
	}

	@Override
	public InputStream openStream(MediaFileID id) throws IOException{
		HttpRequest req=HttpRequest.newBuilder(getObjectURL(id)).timeout(Duration.ofSeconds(60)).build();
		return executeRequest(req, HttpResponse.BodyHandlers.ofInputStream()).body();
	}

	@Override
	public void deleteFile(MediaFileID id) throws IOException{
		HttpRequest req=HttpRequest.newBuilder(getApiUrl().appendPath(getObjectName(id)).build())
				.DELETE()
				.timeout(Duration.ofSeconds(60))
				.build();
		req=signRequest(req, null);
		executeRequest(req, HttpResponse.BodyHandlers.ofString());
	}

	@Override
	public Set<MediaFileID> deleteFiles(Collection<MediaFileID> ids){
		if(ids.isEmpty()){
			return Set.of();
		}
		if(ids.size()==1){
			MediaFileID id=ids.iterator().next();
			try{
				deleteFile(id);
				return Set.of(id);
			}catch(IOException x){
				LOG.warn("Failed to delete file {}", id, x);
				return Set.of();
			}
		}
		HashSet<MediaFileID> deletedIDs=new HashSet<>();
		Document doc=XmlParser.newDocumentBuilder().newDocument();
		Element root=doc.createElementNS(XMLNS, "Delete");
		doc.appendChild(root);
		HashMap<String, MediaFileID> idsByKey=new HashMap<>();
		for(MediaFileID id:ids){
			String key=getObjectName(id);
			idsByKey.put(key, id);
			Element objEl=doc.createElement("Object");
			Element keyEl=doc.createElement("Key");
			keyEl.setTextContent(key);
			objEl.appendChild(keyEl);
			root.appendChild(objEl);
		}
		String xmlStr=XmlParser.serialize(doc);
		HttpRequest req=HttpRequest.newBuilder(getApiUrl().queryParam("delete", "").build())
				.POST(HttpRequest.BodyPublishers.ofString(xmlStr))
				.header("Content-MD5", md5Base64(xmlStr))
				.timeout(Duration.ofSeconds(60))
				.build();
		try{
			req=signRequest(req, xmlStr);
		}catch(IOException x){ // Shouldn't happen anyway
			throw new RuntimeException(x);
		}
		try(InputStream in=executeRequest(req, HttpResponse.BodyHandlers.ofInputStream()).body()){
			Document resp=XmlParser.newDocumentBuilder().parse(in);
			Element respEl=resp.getDocumentElement();
			if("DeleteResult".equals(respEl.getTagName())){
				for(Node child:XmlParser.iterateNodes(respEl.getChildNodes())){
					if(!(child instanceof Element el))
						continue;
					switch(el.getTagName()){
						case "Deleted" -> {
							if(el.getElementsByTagName("Key").item(0) instanceof Element keyEl){
								MediaFileID id=idsByKey.get(keyEl.getTextContent());
								LOG.trace("File {} deleted successfully", id);
								if(id!=null)
									deletedIDs.add(id);
							}
						}
						case "Error" -> {
							if(el.getElementsByTagName("Key").item(0) instanceof Element keyEl){
								LOG.warn("Failed to delete file {}: {}", keyEl.getTextContent(), getServerErrorMessage(el));
							}
						}
					}
				}
			}else{
				LOG.warn("Unexpected tag name in bulk delete result: {}", respEl.getTagName());
			}
		}catch(IOException | SAXException x){
			LOG.warn("Failed to bulk delete files", x);
			return deletedIDs;
		}

		return deletedIDs;
	}

	@Override
	public ImgProxy.UrlBuilder getImgProxyURL(MediaFileID id){
		return new ImgProxy.UrlBuilder(getObjectURL(id).toString());
	}

	@Override
	public URI getFilePublicURL(MediaFileID id){
		return getObjectURL(id);
	}

	private URI getObjectURL(MediaFileID id){
		UriBuilder url=new UriBuilder()
				.scheme(config.protocol())
				.authority(publicHostname);
		if(config.aliasHost()==null)
			url.appendPath(config.bucket());
		url.appendPath(getObjectName(id));
		return url.build();
	}

	private UriBuilder getApiUrl(){
		UriBuilder url=new UriBuilder()
				.scheme(config.protocol());
		if(config.endpoint()==null || config.overridePathStyle())
			url.authority(config.bucket()+"."+apiHostname);
		else
			url.authority(apiHostname).appendPath(config.bucket());
		return url;
	}

	private String getObjectName(MediaFileID id){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(id.randomID())+"_"+
				Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(id.id(), ObfuscatedObjectIDType.MEDIA_FILE)))+
				"."+id.type().getFileExtension();
	}

	private <T> HttpResponse<T> executeRequest(HttpRequest req, HttpResponse.BodyHandler<T> bodyHandler) throws IOException{
		LOG.trace("Executing request: {}", req);
		try{
			HttpResponse<T> resp=httpClient.send(req, bodyHandler);
			LOG.trace("Response: {}", resp);
			if(resp.statusCode()/100!=2){
				String contentType=resp.headers().firstValue("content-type").orElse("");
				if(contentType.startsWith("application/xml") || contentType.startsWith("text/xml")){
					String bodyStr=switch(resp.body()){
						case String s -> s;
						case byte[] ba -> new String(ba, StandardCharsets.UTF_8);
						case InputStream in -> {
							BufferedReader reader=new BufferedReader(new InputStreamReader(in));
							StringBuilder sb=new StringBuilder();
							String line;
							while((line=reader.readLine())!=null){
								sb.append(line);
							}
							yield sb.toString();
						}
						default -> throw defaultException(resp);
					};
					LOG.debug("Response: {}, body: {}", resp, bodyStr);
					DocumentBuilder builder=XmlParser.newDocumentBuilder();
					try{
						Document doc=builder.parse(new InputSource(new StringReader(bodyStr)));
						Element docEl=doc.getDocumentElement();
						if("Error".equals(docEl.getTagName())){
							String fullMessage=getServerErrorMessage(docEl);
							if(fullMessage.isEmpty())
								throw defaultException(resp);
							throw new RemoteServerException(fullMessage);
						}else{
							throw defaultException(resp);
						}
					}catch(SAXException x){
						throw defaultException(resp);
					}
				}else{
					throw defaultException(resp);
				}
			}
			return resp;
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}

	private String getServerErrorMessage(Element errorEl){
		String code=switch(errorEl.getElementsByTagName("Code").item(0)){
			case Element el -> el.getTextContent();
			case null, default -> null;
		};
		String message=switch(errorEl.getElementsByTagName("Message").item(0)){
			case Element el -> el.getTextContent();
			case null, default -> null;
		};
		return Stream.of(code, message).filter(Objects::nonNull).collect(Collectors.joining(": "));
	}

	private RemoteServerException defaultException(HttpResponse<?> resp){
		return new RemoteServerException("Response was not successful: status "+resp.statusCode());
	}

	private static String uriEncode(String input){
		StringBuilder sb=new StringBuilder();
		HexFormat hex=HexFormat.of().withPrefix("%").withUpperCase();
		byte[] bytes=input.getBytes(StandardCharsets.UTF_8);
		int i=0;
		for(byte b:bytes){
			int chr=b & 0xFF;
			if((chr>='A' && chr<='Z') || (chr>='a' && chr <='z') || (chr>='0' && chr<='9') || chr=='-' || chr=='.' || chr=='_' || chr=='~'){
				sb.append((char)chr);
			}else{
				hex.formatHex(sb, bytes, i, i+1);
			}
			i++;
		}
		return sb.toString();
	}

	private static byte[] hmacSha256(byte[] key, String input){
		try{
			Mac mac=Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
		}catch(NoSuchAlgorithmException | InvalidKeyException x){
			throw new RuntimeException(x);
		}
	}

	private static String sha256(String input){
		try{
			MessageDigest md=MessageDigest.getInstance("SHA256");
			return Utils.byteArrayToHexString(md.digest(input.getBytes(StandardCharsets.UTF_8)));
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
	}

	private static String md5Base64(String input){
		try{
			MessageDigest md=MessageDigest.getInstance("MD5");
			return Base64.getEncoder().encodeToString(md.digest(input.getBytes(StandardCharsets.UTF_8)));
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
	}

	private HttpRequest signRequest(HttpRequest req, Object body) throws IOException{
		URI uri=req.uri();
		// https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
		/*
		Step 1: Create a canonical request

		<HTTPMethod>\n
		<CanonicalURI>\n
		<CanonicalQueryString>\n
		<CanonicalHeaders>\n
		<SignedHeaders>\n
		<HashedPayload>
		*/
		StringBuilder canonicalRequest=new StringBuilder(req.method().toUpperCase());
		canonicalRequest.append('\n');
		String path=uri.getPath();
		if("/".equals(path)){
			canonicalRequest.append("/\n");
		}else{
			int i=0;
			for(String segment:path.substring(1).split("/")){
				//if(i>0)
				canonicalRequest.append('/');
				canonicalRequest.append(uriEncode(segment));
				i++;
			}
			canonicalRequest.append('\n');
		}
		canonicalRequest.append(UriBuilder.parseQueryString(uri.getRawQuery())
				.entrySet()
				.stream()
				.map(e->new Pair<>(uriEncode(e.getKey()), uriEncode(e.getValue())))
				.sorted(Comparator.comparing(Pair::first))
				.map(e->e.first()+"="+e.second())
				.collect(Collectors.joining("&")));
		canonicalRequest.append('\n');

		HashMap<String, String> headers=new HashMap<>();
		String contentHash;
		LocalDateTime now=LocalDateTime.now(ZoneId.of("UTC"));
		String dateTime=DATE_TIME_FORMATTER.format(now);
		String date=DATE_FORMATTER.format(now);
		if(body instanceof InputStream in){
			try{
				MessageDigest md=MessageDigest.getInstance("SHA256");
				byte[] buf=new byte[8192];
				int read;
				while((read=in.read(buf))>0){
					md.update(buf, 0, read);
				}
				contentHash=Utils.byteArrayToHexString(md.digest());
			}catch(NoSuchAlgorithmException x){
				throw new RuntimeException(x);
			}
		}else if(body instanceof String str){
			contentHash=sha256(str);
		}else{
			contentHash="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // sha256 of empty string
		}
		headers.put("x-amz-content-sha256", contentHash);
		headers.put("host", uri.getAuthority());
		req.headers()
				.map()
				.entrySet()
				.stream()
				.filter(e->{
					String key=e.getKey().toLowerCase();
					return key.equals("content-type") || key.startsWith("x-amz");
				})
				.forEach(e->headers.put(e.getKey().toLowerCase(), String.join(",", e.getValue())));
		canonicalRequest.append(headers.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.map(e->e.getKey()+":"+e.getValue().trim())
				.collect(Collectors.joining("\n")));
		canonicalRequest.append("\n\n");
		String signedHeaders=headers.keySet().stream().sorted().collect(Collectors.joining(";"));
		canonicalRequest.append(signedHeaders);
		canonicalRequest.append('\n');
		canonicalRequest.append(contentHash);

		// Step 2: Create a hash of the canonical request
		String canonicalRequestHash=sha256(canonicalRequest.toString());

		/*
		Step 3: Create a string to sign

		Algorithm \n
		RequestDateTime \n
		CredentialScope  \n
		HashedCanonicalRequest
		*/
		String strToSign="AWS4-HMAC-SHA256\n"+dateTime+
				'\n'+
				date+
				'/'+
				config.region()+
				"/s3/aws4_request\n"+
				canonicalRequestHash;

		// Step 4: Calculate the signature
		byte[] dateKey=hmacSha256(("AWS4"+config.secretKey()).getBytes(StandardCharsets.UTF_8), date);
		byte[] dateRegionKey=hmacSha256(dateKey, config.region());
		byte[] dateRegionServiceKey=hmacSha256(dateRegionKey, "s3");
		byte[] signingKey=hmacSha256(dateRegionServiceKey, "aws4_request");
		byte[] signature=hmacSha256(signingKey, strToSign);

		// Step 5: Add the signature to the request
		return HttpRequest.newBuilder(req, (n, v)->true)
				.header("x-amz-content-sha256", contentHash)
				.header("x-amz-date", dateTime)
				.header("Authorization", "AWS4-HMAC-SHA256 Credential="+config.keyID()+"/"+date+"/"+config.region()+"/s3/aws4_request, SignedHeaders="+signedHeaders+", Signature="+Utils.byteArrayToHexString(signature))
				.build();
	}

	public static class RemoteServerException extends IOException{
		public RemoteServerException(){
			super();
		}

		public RemoteServerException(String message){
			super(message);
		}

		public RemoteServerException(String message, Throwable cause){
			super(message, cause);
		}

		public RemoteServerException(Throwable cause){
			super(cause);
		}
	}
}
