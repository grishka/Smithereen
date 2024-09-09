package smithereen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.UserErrorException;
import smithereen.model.CaptchaInfo;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.SessionInfo;
import smithereen.model.StatsPoint;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FormValidationException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.EmailCodeActionType;
import smithereen.util.FloodControl;
import smithereen.util.InstantMillisJsonAdapter;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.LocaleJsonAdapter;
import smithereen.util.TimeZoneJsonAdapter;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.MimeParse;
import spark.utils.StringUtils;

public class Utils{

	private static final Set<String> RESERVED_USERNAMES=Set.of("account", "settings", "feed", "activitypub", "api", "system", "users", "groups", "posts", "session", "robots.txt", "my", "activitypub_service_actor", "healthz",
			"albums", "photos", "videos", "audios", "topics", "apps", "docs", "res", "files", "comments");
	private static final Random rand=new Random();

	private static final Pattern SIGNATURE_HEADER_PATTERN=Pattern.compile("([!#$%^'*+\\-.^_`|~0-9A-Za-z]+)=(?:(?:\\\"((?:[^\\\"\\\\]|\\\\.)*)\\\")|([!#$%^'*+\\-.^_`|~0-9A-Za-z]+))\\s*([,;])?\\s*");
	private static final Pattern NON_ASCII_PATTERN=Pattern.compile("\\P{ASCII}");

	private static final int[] GRAPH_COLORS={0x597da3, 0xb05c91, 0x4d9fab, 0x569567, 0xac4c4c, 0xc9c255, 0xcd9f4d, 0x876db3,
			0x6f9fc4, 0xc77bb1, 0x70c5c8, 0x80bb88, 0xce5e5e, 0xe8e282, 0xedb24a, 0xae97d3,
			0x6391bc, 0xc77bb1, 0x62b1bc, 0x80bb88, 0xb75454, 0xc9c255, 0xdca94f, 0x997fc4,
			0x85afd0, 0xc77bb1, 0x8ecfce, 0x80bb88, 0xe47070, 0xc9c255, 0xf7be5a, 0xbeaadf};

	public static final Gson gson=new GsonBuilder()
			.disableHtmlEscaping()
			.enableComplexMapKeySerialization()
			.registerTypeAdapter(Instant.class, new InstantMillisJsonAdapter())
			.registerTypeAdapter(Locale.class, new LocaleJsonAdapter())
			.registerTypeHierarchyAdapter(ZoneId.class, new TimeZoneJsonAdapter())
			.create();


	public static String csrfTokenFromSessionID(byte[] sid){
		CRC32 crc=new CRC32();
		crc.update(sid, 10, 10);
		long v1=crc.getValue();
		crc.update(sid, 5, 10);
		long v2=crc.getValue();
		return String.format(Locale.ENGLISH, "%08x%08x", v1, v2);
	}

	public static boolean requireAccount(Request req, Response resp){
		if(req.session(false)==null || req.session().attribute("info")==null || ((SessionInfo)req.session().attribute("info")).account==null){
			String to=req.pathInfo();
			String query=req.queryString();
			if(resp!=null){
				if(StringUtils.isNotEmpty(query))
					to+="?"+query;
				resp.redirect("/account/login?to="+URLEncoder.encode(to));
			}
			return false;
		}
		return true;
	}

	public static boolean verifyCSRF(Request req, Response resp){
		SessionInfo info=req.session().attribute("info");
		String reqCsrf=req.queryParams("csrf");
		if(reqCsrf!=null && reqCsrf.equals(info.csrfToken)){
			return true;
		}
		resp.status(403);
		return false;
	}

	public static void jsLangKey(Request req, String... keys){
		ArrayList<String> k=req.attribute("jsLang");
		if(k==null)
			req.attribute("jsLang", k=new ArrayList<>());
		Collections.addAll(k, keys);
	}

	public static int parseIntOrDefault(String s, int d){
		if(s==null)
			return d;
		try{
			return Integer.parseInt(s);
		}catch(NumberFormatException x){
			return d;
		}
	}

	/**
	 * Parse a decimal integer from string without throwing any exceptions.
	 * @param s The string to parse.
	 * @return The parsed integer if successful, 0 on failure or if s was null.
	 */
	public static int safeParseInt(String s){
		return parseIntOrDefault(s, 0);
	}

	public static long safeParseLong(String s){
		try{
			return Long.parseLong(s);
		}catch(NumberFormatException x){
			return 0;
		}
	}

	public static Object wrapError(Request req, Response resp, String errorKey){
		return wrapError(req, resp, errorKey, null);
	}

	public static Object wrapError(Request req, Response resp, String errorKey, Map<String, Object> formatArgs){
		Lang l=lang(req);
		String msg=formatArgs!=null ? l.get(errorKey, formatArgs) : l.get(errorKey);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).messageBox(l.get("error"), msg, l.get("close"));
		}
		return new RenderedTemplateResponse("generic_error", req).with("error", msg).with("back", back(req)).with("title", l.get("error"));
	}

	public static String wrapErrorString(Request req, Response resp, String errorKey){
		return wrapErrorString(req, resp, errorKey, null);
	}

	public static String wrapErrorString(Request req, Response resp, String errorKey, Map<String, Object> formatArgs){
		Lang l=lang(req);
		String msg=formatArgs!=null ? l.get(errorKey, formatArgs) : l.get(errorKey);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).messageBox(l.get("error"), msg, l.get("close")).json();
		}else if(isActivityPub(req)){
			return msg;
		}
		return new RenderedTemplateResponse("generic_error", req).with("error", msg).with("back", back(req)).with("title", l.get("error")).renderToString();
	}

	public static Object wrapForm(Request req, Response resp, String templateName, String formAction, String title, String buttonKey, RenderedTemplateResponse templateModel){
		Lang l=lang(req);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).formBox(title, templateModel.renderToString(), formAction, l.get(buttonKey));
		}else{
			templateModel.with("contentTemplate", templateName).with("formAction", formAction).with("submitButton", l.get(buttonKey)).with("title", title);
			templateModel.setName("form_page");
			return templateModel;
		}
	}

	public static Object wrapForm(Request req, Response resp, String templateName, String formAction, String title, String buttonKey, String formID, List<String> fieldNames, Function<String, Object> fieldValueGetter, String message){
		return wrapForm(req, resp, templateName, formAction, title, buttonKey, formID, fieldNames, fieldValueGetter, message, null);
	}

	public static Object wrapForm(Request req, Response resp, String templateName, String formAction, String title, String buttonKey, String formID, List<String> fieldNames, Function<String, Object> fieldValueGetter, String message, Map<String, Object> extraTemplateArgs){
		if(isAjax(req) && StringUtils.isNotEmpty(message)){
			WebDeltaResponse wdr=new WebDeltaResponse(resp);
			wdr.keepBox().show("formMessage_"+formID).setContent("formMessage_"+formID, TextProcessor.escapeHTML(message));
			return wdr;
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse(templateName, req);
		if(StringUtils.isNotEmpty(message)){
			model.with(formID+"Message", message);
		}
		if(fieldValueGetter==null){
			fieldValueGetter=req::queryParams;
		}
		for(String name:fieldNames){
			model.with(name, fieldValueGetter.apply(name));
		}
		if(extraTemplateArgs!=null){
			for(Map.Entry<String, Object> e:extraTemplateArgs.entrySet()){
				model.with(e.getKey(), e.getValue());
			}
		}
		return wrapForm(req, resp, templateName, formAction, title, buttonKey, model);
	}

	public static String requireFormField(Request req, String field, String errorKey){
		String value=req.queryParams(field);
		if(StringUtils.isEmpty(value))
			throw new FormValidationException(errorKey==null ? ("Required field missing: "+field) : lang(req).get(errorKey));
		return value;
	}

	public static String requireFormFieldLength(Request req, String field, int minLength, String errorKey){
		String value=requireFormField(req, field, errorKey);
		if(value.length()<minLength)
			throw new FormValidationException(lang(req).get(errorKey));
		return value;
	}

	public static <E extends Enum<E>> E requireFormField(Request req, String field, String errorKey, Class<E> enumClass){
		String value=req.queryParams(field);
		if(StringUtils.isEmpty(value))
			throw new FormValidationException(errorKey==null ? ("Required field missing: "+field) : lang(req).get(errorKey));
		try{
			return Enum.valueOf(enumClass, value);
		}catch(IllegalArgumentException x){
			throw new FormValidationException(errorKey==null ? ("Required field missing: "+field) : lang(req).get(errorKey));
		}
	}

	public static Locale localeForRequest(Request req){
		String langParam=req.queryParams("lang");
		if(StringUtils.isNotEmpty(langParam)){
			return Locale.forLanguageTag(langParam);
		}
		SessionInfo info=sessionInfo(req);
		if(info!=null){
			if(info.account!=null && info.account.prefs.locale!=null)
				return info.account.prefs.locale;
			if(info.preferredLocale!=null)
				return info.preferredLocale;
		}
		if(req.raw().getLocale()!=null)
			return req.raw().getLocale();
		return Locale.US;
	}

	public static ZoneId timeZoneForRequest(Request req){
		SessionInfo info=sessionInfo(req);
		if(info!=null){
			if(info.account!=null && info.account.prefs.timeZone!=null)
				return info.account.prefs.timeZone;
			if(info.timeZone!=null)
				return info.timeZone;
		}
		return ZoneId.systemDefault();
	}

	public static Lang lang(Request req){
		return Lang.get(localeForRequest(req));
	}

	public static boolean isValidUsername(String username){
		return username.matches("^[a-zA-Z][a-zA-Z0-9._-]+$");
	}

	public static boolean isReservedUsername(String username){
		return RESERVED_USERNAMES.contains(username.toLowerCase()) || username.toLowerCase().matches("^(id|club|event)\\d+$");
	}

	public static boolean isValidEmail(String email){
		return email.matches("^[^@]+@[^@]+\\.[^@]{2,}$");
	}

	public static String byteArrayToHexString(byte[] arr){
		char[] chars=new char[arr.length*2];
		char[] hex={'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
		for(int i=0;i<arr.length;i++){
			chars[i*2]=hex[((int)arr[i] >> 4) & 0x0F];
			chars[i*2+1]=hex[(int)arr[i] & 0x0F];
		}
		return String.valueOf(chars);
	}

	public static byte[] hexStringToByteArray(String hex){
		if(hex.length()%2!=0)
			throw new IllegalArgumentException("Even-length string required");
		byte[] res=new byte[hex.length()/2];
		for(int i=0;i<res.length;i++){
			res[i]=(byte)((Character.digit(hex.charAt(i*2), 16) << 4) | (Character.digit(hex.charAt(i*2+1), 16)));
		}
		return res;
	}

	public static String formatDateAsISO(Instant date){
		return DateTimeFormatter.ISO_INSTANT.format(date);
	}

	public static Instant parseISODate(String date){
		try{
			Instant instant=DateTimeFormatter.ISO_INSTANT.parse(date, Instant::from);
			if(instant.getEpochSecond()<1)
				return Instant.ofEpochSecond(1);
			return instant;
		}catch(DateTimeParseException x){
			return null;
		}
	}

	public static URI userIdFromKeyId(URI keyID){
		try{
			// most AP servers use key IDs in the form http://example.com/users/username#main-key so removing the URI fragment is enough
			if(keyID.getFragment()!=null){
				return new URI(keyID.getScheme(), keyID.getSchemeSpecificPart(), null);
			}
			// Misskey does this: https://misskey.io/users/7rkrarq81i/publickey
			URI uri=keyID.resolve("./");
			return new URI(uri.getScheme(), uri.getSchemeSpecificPart().replaceAll("/$", ""), null);
		}catch(URISyntaxException x){
			throw new RuntimeException("checked exceptions are stupid");
		}
	}

	public static long parseFileSize(String size){
		size=size.toUpperCase();
		if(!size.matches("^\\d+[KMGT]?$"))
			throw new IllegalArgumentException("String '"+size+"' does not have the correct format");
		char unit=size.charAt(size.length()-1);
		if(Character.isDigit(unit)){
			return Long.parseLong(size);
		}
		long n=Long.parseLong(size.substring(0, size.length()-1));
		switch(unit){
			case 'K':
				n*=1024L;
				break;
			case 'M':
				n*=1024L*1024L;
				break;
			case 'G':
				n*=1024L*1024L*1024L;
				break;
			case 'T':
				n*=1024L*1024L*1024L*1024L;
				break;
		}
		return n;
	}

	public static String getLastPathSegment(URI uri){
		String path=uri.getPath();
		int index=path.lastIndexOf('/');
		if(index==-1)
			return null;
		return path.substring(index+1);
	}

	public static SessionInfo sessionInfo(Request req){
		Session sess=req.session(false);
		if(sess==null)
			return null;
		SessionInfo info=sess.attribute("info");
		return info;
	}

	@NotNull
	public static SessionInfo requireSession(Request req){
		SessionInfo info=sessionInfo(req);
		if(info==null)
			throw new UserActionNotAllowedException();
		return info;
	}

	public static int[] deserializeIntArray(byte[] a){
		if(a==null || a.length%4!=0)
			return null;
		int[] result=new int[a.length/4];
		try{
			DataInputStream in=new DataInputStream(new ByteArrayInputStream(a));
			for(int i=0;i<result.length;i++)
				result[i]=in.readInt();
		}catch(IOException ignore){}
		return result;
	}

	public static byte[] serializeIntArray(int[] a){
		if(a==null || a.length==0)
			return null;
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try{
			DataOutputStream out=new DataOutputStream(os);
			for(int i:a)
				out.writeInt(i);
		}catch(IOException ignore){}
		return os.toByteArray();
	}

	public static List<Integer> deserializeIntList(byte[] a){
		if(a==null)
			return List.of();
		return Arrays.stream(deserializeIntArray(a)).boxed().toList();
	}

	public static byte[] serializeIntList(Collection<Integer> a){
		if(a==null || a.isEmpty())
			return null;
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try{
			DataOutputStream out=new DataOutputStream(os);
			for(int i:a)
				out.writeInt(i);
		}catch(IOException ignore){}
		return os.toByteArray();
	}

	public static Set<Integer> deserializeIntSet(byte[] a){
		if(a==null)
			return Set.of();
		return Arrays.stream(deserializeIntArray(a)).boxed().collect(Collectors.toSet());
	}

	public static String back(Request req){
		String redir=req.queryParams("_redir");
		if(redir!=null)
			return redir;
		String ref=req.headers("referer");
		if(ref!=null)
			return ref;
		SessionInfo sessionInfo=sessionInfo(req);
		return sessionInfo!=null ? sessionInfo.history.last() : "/";
	}

	public static boolean isAjax(Request req){
		return req.queryParams("_ajax")!=null;
	}

	public static boolean isMobile(Request req){
		return req.attribute("mobile")!=null;
	}

	public static boolean isActivityPub(Request req){
		String accept=req.headers("accept");
		if(StringUtils.isEmpty(accept))
			return false;
		String matched=MimeParse.bestMatch(Set.of("application/activity+json", "application/ld+json", ""), accept);
		return StringUtils.isNotEmpty(matched);
	}

	public static boolean isMobileUserAgent(String ua){
		ua=ua.toLowerCase();
		return ua.matches("(?i).*((android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino).*")
				|| (ua.length()>4 && ua.substring(0,4).matches("(?i)1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-"));
	}

	public static void ensureUserNotBlocked(User self, Actor target) throws SQLException{
		if(target instanceof User user){
			if(self instanceof ForeignUser && UserStorage.isDomainBlocked(user.id, self.domain))
				throw new UserActionNotAllowedException();
			if(UserStorage.isUserBlocked(user.id, self.id))
				throw new UserActionNotAllowedException();
		}else if(target instanceof Group group){
			if(self instanceof ForeignUser && GroupStorage.isDomainBlocked(group.id, self.domain))
				throw new UserActionNotAllowedException();
			if(GroupStorage.isUserBlocked(group.id, self.id))
				throw new UserActionNotAllowedException();
		}
	}

	public static List<Map<String, String>> parseSignatureHeader(String header){
		Matcher matcher=SIGNATURE_HEADER_PATTERN.matcher(header);
		ArrayList<Map<String, String>> res=new ArrayList<>();
		HashMap<String, String> curMap=new HashMap<>();
		while(matcher.find()){
			String key=matcher.group(1);

			String value=matcher.group(2);
			if(value == null) {
				value = matcher.group(3);
			}
			value = value.replace("\\\"", "\"").replace("\\\\", "\\");

			String separator=matcher.group(4);
			curMap.put(key, value);
			if(separator==null || ";".equals(separator)){
				res.add(curMap);
				if(";".equals(separator))
					curMap=new HashMap<>();
			}
		}
		return res;
	}

	public static String serializeSignatureHeader(List<Map<String, String>> sig){
		return sig.stream().map(map->{
			return map.entrySet().stream().map(e->e.getKey()+"=\""+e.getValue().replace("\\", "\\\\").replace("\"", "\\\"")+"\"").collect(Collectors.joining(","));
		}).collect(Collectors.joining(";"));
	}

	public static boolean isURL(String in){
		if(in==null)
			return false;
		Matcher matcher=TextProcessor.URL_PATTERN.matcher(in);
		return matcher.find() && matcher.start()==0 && matcher.end()==in.length();
	}

	public static String normalizeURLDomain(String in){
		Matcher matcher=TextProcessor.URL_PATTERN.matcher(in);
		if(!matcher.find())
			throw new IllegalStateException("use isURL() first");
		String host=matcher.group(2);
		if(NON_ASCII_PATTERN.matcher(host).find()){
			return in.substring(0, matcher.start(2))+convertIdnToAsciiIfNeeded(host)+in.substring(matcher.end(2));
		}else{
			return in;
		}
	}

	public static boolean isUsernameAndDomain(String in){
		if(in==null)
			return false;
		Matcher matcher=TextProcessor.USERNAME_DOMAIN_PATTERN.matcher(in);
		return matcher.find() && matcher.start()==0 && matcher.end()==in.length();
	}

	public static void stopExecutorBlocking(ExecutorService executor, Logger log){
		executor.shutdown();
		int attempts=0;
		try{
			while(!executor.awaitTermination(1, TimeUnit.SECONDS)){
				log.info("Still waiting...");
				attempts++;
				if(attempts==10){
					List<Runnable> tasks=executor.shutdownNow();
					log.warn("Force shutting down executor. Remaining tasks: {}", tasks);
					return;
				}
			}
		}catch(InterruptedException ignore){}
	}

	public static String randomAlphanumericString(int length){
		char[] chars=new char[length];
		String alphabet="1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
		for(int i=0;i<length;i++){
			chars[i]=alphabet.charAt(rand.nextInt(alphabet.length()));
		}
		return new String(chars);
	}

	public static byte[] randomBytes(int length){
		byte[] res=new byte[length];
		rand.nextBytes(res);
		return res;
	}

	public static String convertIdnToAsciiIfNeeded(String domain) throws IllegalArgumentException{
		if(domain==null)
			return null;
		if(NON_ASCII_PATTERN.matcher(domain).find())
			return IDN.toASCII(domain);
		return domain;
	}

	public static int offset(Request req){
		String offset=req.queryParams("offset");
		if(StringUtils.isEmpty(offset))
			return 0;
		return Math.max(parseIntOrDefault(offset, 0), 0);
	}

	@NotNull
	public static ApplicationContext context(Request req){
		ApplicationContext context=req.attribute("context");
		if(context==null)
			throw new IllegalStateException("context==null");
		return context;
	}

	@NotNull
	public static Instant instantFromDateAndTime(Request req, String dateStr, String timeStr){
		LocalDate date=DateTimeFormatter.ISO_LOCAL_DATE.parse(dateStr, LocalDate::from);
		LocalTime time=DateTimeFormatter.ISO_LOCAL_TIME.parse(timeStr, LocalTime::from);
		return LocalDateTime.of(date, time).atZone(timeZoneForRequest(req)).toInstant();
	}

	public static <E extends Enum<E>> long serializeEnumSet(EnumSet<E> set){
		long result=0;
		for(E value:set){
			int ordinal=value.ordinal();
			if(ordinal>=64)
				throw new IllegalArgumentException("this enum has more than 64 constants");
			result|=1L << ordinal;
		}
		return result;
	}

	public static <E extends Enum<E>> void deserializeEnumSet(EnumSet<E> set, Class<E> cls, long l){
		set.clear();
		E[] consts=cls.getEnumConstants();
		if(consts.length>64)
			throw new IllegalArgumentException("this enum has more than 64 constants");
		for(E e:consts){
			if((l&1)==1)
				set.add(e);
			l >>= 1;
		}
	}

	public static <E extends Enum<E>> byte[] serializeEnumSetToBytes(EnumSet<E> set){
		BitSet result=new BitSet();
		for(E value:set){
			result.set(value.ordinal());
		}
		return result.toByteArray();
	}

	public static <E extends Enum<E>> void deserializeEnumSet(EnumSet<E> set, Class<E> cls, byte[] serialized){
		set.clear();
		E[] consts=cls.getEnumConstants();
		BitSet.valueOf(serialized).stream().mapToObj(i->consts[i]).forEach(set::add);
	}

	/**
	 * Convert a string to an enum value
	 * @param val
	 * @param cls
	 * @param <E>
	 * @return
	 */
	public static <E extends Enum<E>> E enumValue(String val, Class<E> cls){
		if(val==null)
			throw new BadRequestException("null value");
		try{
			return Enum.valueOf(cls, val);
		}catch(IllegalArgumentException x){
			throw new BadRequestException("'"+val+"' is not a valid value for "+cls.getSimpleName());
		}
	}

	public static <E extends Enum<E>> E enumValueOpt(String val, Class<E> cls){
		if(val==null || val.isEmpty())
			return null;
		try{
			return Enum.valueOf(cls, val);
		}catch(IllegalArgumentException x){
			return null;
		}
	}

	/**
	 * Ensure that the request has required query parameters to avoid any surprise NPEs.
	 * @param req The request
	 * @param params The parameter names
	 * @throws BadRequestException if any of the parameters aren't present
	 */
	public static void requireQueryParams(Request req, String... params){
		for(String param:params){
			if(StringUtils.isEmpty(req.queryParams(param)))
				throw new BadRequestException("Required parameter '"+param+"' not present");
		}
	}

	public static String getRequestPathAndQuery(Request req){
		String path=req.pathInfo();
		String query=req.queryString();
		if(StringUtils.isNotEmpty(query)){
			path+="?"+query;
			if(query.contains("_ajax=1")){
				path=new UriBuilder(path).removeQueryParam("_ajax").build().toString();
			}
		}
		return path;
	}

	public static InetAddress getRequestIP(Request req){
		String forwardedFor=req.headers("X-Forwarded-For");
		String ip;
		if(StringUtils.isNotEmpty(forwardedFor)){
			ip=forwardedFor.split(",")[0].trim();
		}else{
			ip=req.ip();
		}
		try{
			return InetAddress.getByName(ip);
		}catch(UnknownHostException e){
			throw new RuntimeException(e); // should never happen
		}
	}

	public static JsonArray makeGraphData(List<String> categoryTitles, List<List<StatsPoint>> data, ZoneId userTimeZone){
		if(categoryTitles.size()!=data.size())
			throw new IllegalArgumentException("categoryTitles.size != data.size");
		JsonArrayBuilder bldr=new JsonArrayBuilder();
		LocalDate today=LocalDate.now(userTimeZone);
		for(int i=0;i<categoryTitles.size();i++){
			JsonArrayBuilder d=new JsonArrayBuilder();
			JsonObjectBuilder obj=new JsonObjectBuilder()
					.add("name", categoryTitles.get(i))
					.add("c", GRAPH_COLORS[i%GRAPH_COLORS.length]);
			for(StatsPoint pt:data.get(i)){
				JsonArrayBuilder jpt=new JsonArrayBuilder()
						.add(pt.date().atTime(12, 0, 0).atZone(userTimeZone).toEpochSecond())
						.add(pt.count());
				if(pt.date().equals(today))
					jpt.add("-");
				d.add(jpt);
			}
			obj.add("d", d);
			bldr.add(obj);
		}
		return bldr.build();
	}

	public static byte[] packLong(long x){
		byte[] r=new byte[8];
		for(int i=7;i>=0;i--){
			r[i]=(byte)x;
			x>>=8;
		}
		return r;
	}

	public static String encodeLong(long x){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(packLong(x));
	}

	public static long unpackLong(byte[] x){
		return unpackLong(x, 0);
	}

	public static long unpackLong(byte[] x, int offset){
		if(x==null || x.length-offset<8)
			return 0;
		long r=0;
		for(int i=0;i<8;i++){
			r<<=8;
			r|=((long)x[i+offset]) & 0xFFL;
		}
		return r;
	}

	public static long decodeLong(String x){
		try{
			return unpackLong(Base64.getUrlDecoder().decode(x));
		}catch(Exception _x){
			return 0;
		}
	}

	public static byte[] serializeLongCollection(Collection<Long> a){
		byte[] res=new byte[a.size()*8];
		int i=0;
		for(long x:a){
			System.arraycopy(packLong(x), 0, res, i*8, 8);
			i++;
		}
		return res;
	}

	public static void deserializeLongCollection(byte[] b, Collection<Long> dest){
		if(b==null)
			return;
		if(b.length%8!=0 || dest==null)
			throw new IllegalArgumentException();
		int count=b.length/8;
		for(int i=0;i<count;i++){
			dest.add(unpackLong(b, i*8));
		}
	}

	/**
	 * Serialize an {@link InetAddress} for storage in the database.
	 * <s>No reverse method exists because {@link InetAddress#getByAddress(byte[])} takes IPv4-mapped IPv6 addresses and returns an Inet4Address.</s>
	 * Actually no, it exists now because checked exceptions are a pain in the ass
	 * @param ip
	 * @return 16 bytes. IPv6 addresses are returned as-is, IPv4 are mapped into IPv6 (::ffff:x.x.x.x)
	 */
	public static byte[] serializeInetAddress(InetAddress ip){
		return switch(ip){
			case null -> null;
			case Inet4Address ipv4 -> {
				byte[] a=new byte[16];
				a[11]=a[10]=-1;
				System.arraycopy(ipv4.getAddress(), 0, a, 12, 4);
				yield a;
			}
			case Inet6Address ipv6 -> ipv6.getAddress();
			default -> throw new IllegalStateException("Unexpected value: "+ip); // TODO why is this required for a sealed hierarchy?
		};
	}

	public static InetAddress deserializeInetAddress(byte[] ip){
		if(ip==null)
			return null;
		try{
			return InetAddress.getByAddress(ip);
		}catch(UnknownHostException e){
			throw new IllegalArgumentException(e);
		}
	}

	public static long hashUserAgent(String ua){
		try{
			MessageDigest md5=MessageDigest.getInstance("MD5");
			byte[] hash=md5.digest(ua.getBytes(StandardCharsets.UTF_8));
			return unpackLong(hash, 0) ^ unpackLong(hash, 8);
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
	}

	public static Object sendEmailConfirmationCode(Request req, Response resp, EmailCodeActionType type, String formAction){
		SessionInfo info=Objects.requireNonNull(sessionInfo(req));
		FloodControl.ACTION_CONFIRMATION.incrementOrThrow(info.account);
		Random rand=ThreadLocalRandom.current();
		char[] _code=new char[5];
		for(int i=0;i<_code.length;i++){
			_code[i]=(char)('0'+rand.nextInt(10));
		}
		String code=new String(_code);
		req.session().attribute("emailCodeInfo", new EmailConfirmationCodeInfo(code, type, Instant.now()));
		Mailer.getInstance().sendActionConfirmationCode(req, info.account, lang(req).get(type.actionLangKey()), code);
		RenderedTemplateResponse model=new RenderedTemplateResponse("email_confirmation_code_form", req);
		model.with("maskedEmail", info.account.getCurrentEmailMasked()).with("action", type.actionLangKey());
		return wrapForm(req, resp, "email_confirmation_code_form", formAction, lang(req).get("action_confirmation"), "next", model);
	}

	public static void checkEmailConfirmationCode(Request req, EmailCodeActionType type){
		EmailConfirmationCodeInfo info=req.session().attribute("emailCodeInfo");
		req.session().removeAttribute("emailCodeInfo");
		if(info==null || info.actionType!=type || !Objects.equals(info.code, req.queryParams("code")) || info.sentAt.plus(10, ChronoUnit.MINUTES).isBefore(Instant.now()))
			throw new UserErrorException("action_confirmation_incorrect_code");
	}

	public static void copyBytes(InputStream from, OutputStream to) throws IOException{
		byte[] buffer=new byte[10240];
		int read;
		while((read=from.read(buffer))>0){
			to.write(buffer, 0, read);
		}
	}

	public static Object ajaxAwareRedirect(Request req, Response resp, String destination){
		if(isAjax(req))
			return new WebDeltaResponse(resp).replaceLocation(destination);
		resp.redirect(destination);
		return "";
	}

	public static Object wrapConfirmation(Request req, Response resp, String title, String message, String action){
		if(isAjax(req)){
			return new WebDeltaResponse(resp).confirmBox(title, message, action);
		}
		req.attribute("noHistory", true);
		Lang l=lang(req);
		String back=back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", message).with("formAction", action).with("back", back).pageTitle(title);
	}

	public static void verifyCaptcha(Request req){
		String captcha=requireFormField(req, "captcha", "err_wrong_captcha");
		String sid=requireFormField(req, "captchaSid", "err_wrong_captcha");
		LruCache<String, CaptchaInfo> captchas=req.session().attribute("captchas");
		if(captchas==null)
			throw new UserErrorException("err_wrong_captcha");
		CaptchaInfo info=captchas.remove(sid);
		if(info==null)
			throw new UserErrorException("err_wrong_captcha");
		if(!info.answer().equals(captcha) || System.currentTimeMillis()-info.generatedAt().toEpochMilli()<3000)
			throw new UserErrorException("err_wrong_captcha");
	}

	public static String decodeFourCC(int code){
		return new String(new char[]{(char)((code >> 24) & 0xFF), (char)((code >> 16) & 0xFF), (char)((code >> 8) & 0xFF), (char)(code & 0xFF)});
	}

	private record EmailConfirmationCodeInfo(String code, EmailCodeActionType actionType, Instant sentAt){}
}
