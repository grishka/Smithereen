package smithereen;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.CRC32;

import smithereen.data.Account;
import smithereen.data.SessionInfo;
import smithereen.data.UserNotifications;
import smithereen.data.WebDeltaResponseBuilder;
import smithereen.lang.Lang;
import smithereen.storage.NotificationsStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class Utils{

	private static final List<String> RESERVED_USERNAMES=Arrays.asList("account", "settings", "feed", "activitypub", "api", "system", "users", "groups", "posts", "session", "robots.txt", "my");
	private static final Whitelist HTML_SANITIZER=new MicroFormatAwareHTMLWhitelist();
	private static final ThreadLocal<SimpleDateFormat> ISO_DATE_FORMAT=new ThreadLocal<>();
	public static final String staticFileHash;

	static{
		staticFileHash=String.format(Locale.US, "%016x", new Random().nextLong());
	}


	public static String csrfTokenFromSessionID(byte[] sid){
		CRC32 crc=new CRC32();
		crc.update(sid, 10, 10);
		long v1=crc.getValue();
		crc.update(sid, 5, 10);
		long v2=crc.getValue();
		return String.format(Locale.ENGLISH, "%08x%08x", v1, v2);
	}

	public static void addGlobalParamsToTemplate(Request req, JtwigModel model){
		JSONObject jsConfig=new JSONObject();
		if(req.session(false)!=null){
			SessionInfo info=req.session().attribute("info");
			if(info==null){
				info=new SessionInfo();
				req.session().attribute("info", info);
			}
			Account account=info.account;
			if(account!=null){
				model.with("currentUser", account.user);
				model.with("csrf", info.csrfToken);
				model.with("userAccessLevel", account.accessLevel);
				jsConfig.put("csrf", info.csrfToken);
				jsConfig.put("uid", info.account.user.id);
				try{
					UserNotifications notifications=NotificationsStorage.getNotificationsForUser(account.user.id, account.prefs.lastSeenNotificationID);
					model.with("userNotifications", notifications);
				}catch(SQLException x){
					throw new RuntimeException(x);
				}
			}
		}
		TimeZone tz=timeZoneForRequest(req);
		jsConfig.put("timeZone", tz!=null ? tz.getID() : null);
		JSONObject jsLang=new JSONObject();
		ArrayList<String> k=req.attribute("jsLang");
		if(k!=null){
			Lang lang=lang(req);
			for(String key:k){
				jsLang.put(key, lang.raw(key));
			}
		}
		model.with("locale", localeForRequest(req)).with("timeZone", tz!=null ? tz : TimeZone.getDefault()).with("jsConfig", jsConfig.toString()).with("jsLangKeys", jsLang).with("staticHash", staticFileHash).with("serverName", Config.getServerDisplayName());
	}

	public static String renderTemplate(Request req, String name, JtwigModel model){
		addGlobalParamsToTemplate(req, model);
		String templateDir="desktop";
		if(req.attribute("templateDir")!=null)
			templateDir=req.attribute("templateDir");
		JtwigTemplate template=JtwigTemplate.classpathTemplate("templates/"+templateDir+"/"+name+".twig", Main.jtwigEnv);
		if(req.queryParams("_ajax")==null)
			req.attribute("isTemplate", true);
		return template.render(model);
	}

	public static boolean requireAccount(Request req, Response resp){
		if(req.session(false)==null || req.session().attribute("info")==null || ((SessionInfo)req.session().attribute("info")).account==null){
			String to=req.pathInfo();
			String query=req.queryString();
			if(StringUtils.isNotEmpty(query))
				to+="?"+query;
			resp.redirect("/account/login?to="+URLEncoder.encode(to));
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

	public static String wrapError(Request req, Response resp, String errorKey, Object... formatArgs){
		SessionInfo info=req.session().attribute("info");
		Lang l=lang(req);
		String msg=formatArgs.length>0 ? l.get(errorKey, formatArgs) : l.get(errorKey);
		if(isAjax(req)){
			return new WebDeltaResponseBuilder(resp).messageBox(l.get("error"), msg, l.get("ok")).json().toString();
		}
		return renderTemplate(req, "generic_error", JtwigModel.newModel().with("error", msg).with("back", info!=null && info.history!=null ? info.history.last() : "/").with("title", l.get("error")));
	}

	public static String wrapForm(Request req, Response resp, String templateName, String formAction, String title, String buttonKey, JtwigModel templateModel){
		Lang l=lang(req);
		if(isAjax(req)){
			return new WebDeltaResponseBuilder(resp).formBox(title, renderTemplate(req, templateName, templateModel), formAction, l.get(buttonKey)).json().toString();
		}else{
			templateModel.with("contentTemplate", templateName).with("formAction", formAction).with("submitButton", l.get(buttonKey)).with("title", title);
			return renderTemplate(req, "form_page", templateModel);
		}
	}

	public static Locale localeForRequest(Request req){
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

	public static TimeZone timeZoneForRequest(Request req){
		SessionInfo info=sessionInfo(req);
		if(info!=null){
			if(info.account!=null && info.account.prefs.timeZone!=null)
				return info.account.prefs.timeZone;
			if(info.preferredLocale!=null)
				return info.timeZone;
		}
		return null;
	}

	public static Lang lang(Request req){
		return Lang.get(localeForRequest(req));
	}

	public static boolean isValidUsername(String username){
		return username.matches("^[a-zA-Z][a-zA-Z0-9._-]+$");
	}

	public static boolean isReservedUsername(String username){
		return RESERVED_USERNAMES.contains(username.toLowerCase());
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

	public static String sanitizeHTML(String src){
		return Jsoup.clean(src, HTML_SANITIZER);
	}

	public static String sanitizeHTML(String src, URI documentLocation){
		return Jsoup.clean(src, documentLocation.toString(), HTML_SANITIZER);
	}

	private static SimpleDateFormat isoDateFormat(){
		SimpleDateFormat format=ISO_DATE_FORMAT.get();
		if(format!=null)
			return format;
		format=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		ISO_DATE_FORMAT.set(format);
		return format;
	}

	public static String formatDateAsISO(Date date){
		return isoDateFormat().format(date);
	}

	public static Date parseISODate(String date){
		try{
			return isoDateFormat().parse(date);
		}catch(ParseException e){
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
		SessionInfo info=req.session().attribute("info");
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

	public static String back(Request req){
		String redir=req.queryParams("_redir");
		if(redir!=null)
			return redir;
		String ref=req.headers("referer");
		if(ref!=null)
			return ref;
		return sessionInfo(req).history.last();
	}

	public static String truncateOnWordBoundary(String s, int maxLen){
		s=Jsoup.clean(s, Whitelist.none());
		if(s.length()<=maxLen+20)
			return s;
		int len=Math.max(0, Math.min(s.indexOf(' ', maxLen), maxLen+20));
		return s.substring(0, len)+"...";
	}

	public static boolean isAjax(Request req){
		return req.queryParams("_ajax")!=null;
	}

	public static String escapeHTML(String s){
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
