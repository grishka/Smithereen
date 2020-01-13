package smithereen;

import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.owasp.html.ElementPolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamRenderer;
import org.owasp.html.PolicyFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.CRC32;

import smithereen.data.Account;
import smithereen.data.SessionInfo;
import smithereen.data.UserNotifications;
import smithereen.lang.Lang;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class Utils{

	private static final List<String> RESERVED_USERNAMES=Arrays.asList("account", "settings", "feed", "activitypub", "api", "system", "users", "groups", "posts", "session");
	private static final PolicyFactory HTML_SANITIZER;
	private static final SimpleDateFormat ISO_DATE_FORMAT;

	static{
		HTML_SANITIZER=new HtmlPolicyBuilder()
				.allowStandardUrlProtocols()
				.allowElements("b", "strong", "i", "em", "u", "s", "p", "code", "br")
				.allowElements(new ElementPolicy(){
					@Override
					public String apply(String el, List<String> attrs){
						int hrefIndex=attrs.indexOf("href");
						if(hrefIndex!=-1 && attrs.size()>hrefIndex+1){
							String href=attrs.get(hrefIndex+1).toLowerCase();
							try{
								URI uri=new URI(href);
								if(uri.isAbsolute() && !Config.isLocal(uri)){
									attrs.add("target");
									attrs.add("_blank");
								}
							}catch(URISyntaxException x){
								attrs.add("target");
								attrs.add("_blank");
							}
						}
						return "a";
					}
				}, "a")
				.allowAttributes("href").onElements("a")
				.toFactory();

		ISO_DATE_FORMAT=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
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
				jsConfig.put("csrf", info.csrfToken);
				jsConfig.put("uid", info.account.user.id);
				try{
					UserNotifications notifications=UserStorage.getNotificationsForUser(account.user.id);
					model.with("userNotifications", notifications);
				}catch(SQLException x){
					throw new RuntimeException(x);
				}
			}
		}
		TimeZone tz=timeZoneForRequest(req);
		jsConfig.put("timeZone", tz!=null ? tz.getID() : null);
		model.with("locale", localeForRequest(req)).with("timeZone", tz!=null ? tz : TimeZone.getDefault()).with("jsConfig", jsConfig.toString());
	}

	public static String renderTemplate(Request req, String name, JtwigModel model){
		addGlobalParamsToTemplate(req, model);
		JtwigTemplate template=JtwigTemplate.classpathTemplate("templates/desktop/"+name+".twig", Main.jtwigEnv);
		return template.render(model);
	}

	public static boolean requireAccount(Request req, Response resp){
		if(req.session(false)==null || req.session().attribute("info")==null || ((SessionInfo)req.session().attribute("info")).account==null){
			resp.redirect("/");
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

	public static int parseIntOrDefault(String s, int d){
		if(s==null)
			return d;
		try{
			return Integer.parseInt(s);
		}catch(NumberFormatException x){
			return d;
		}
	}

	public static String wrapError(Request req, String errorKey, Object... formatArgs){
		SessionInfo info=req.session().attribute("info");
		Lang l=Lang.get(localeForRequest(req));
		return renderTemplate(req, "generic_error", JtwigModel.newModel().with("error", formatArgs.length>0 ? l.get(errorKey, formatArgs) : l.get(errorKey)).with("back", info.history.last()));
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
		StringBuilder sb=new StringBuilder();
		HtmlSanitizer.sanitize(src, HTML_SANITIZER.apply(new SimpleHtmlStreamRenderer(sb)));
		return sb.toString();
	}

	public static String formatDateAsISO(Date date){
		return ISO_DATE_FORMAT.format(date);
	}

	public static Date parseISODate(String date){
		try{
			return ISO_DATE_FORMAT.parse(date);
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
		String ref=req.headers("referer");
		if(ref!=null)
			return ref;
		return sessionInfo(req).history.last();
	}
}
