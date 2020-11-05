package smithereen;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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

import smithereen.data.SessionInfo;
import smithereen.data.WebDeltaResponseBuilder;
import smithereen.lang.Lang;
import smithereen.templates.RenderedTemplateResponse;
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
		return new RenderedTemplateResponse("generic_error").with("error", msg).with("back", info!=null && info.history!=null ? info.history.last() : "/").with("title", l.get("error")).renderToString(req);
	}

	public static String wrapForm(Request req, Response resp, String templateName, String formAction, String title, String buttonKey, RenderedTemplateResponse templateModel){
		Lang l=lang(req);
		if(isAjax(req)){
			return new WebDeltaResponseBuilder(resp).formBox(title, templateModel.renderToString(req), formAction, l.get(buttonKey)).json().toString();
		}else{
			templateModel.with("contentTemplate", templateName).with("formAction", formAction).with("submitButton", l.get(buttonKey)).with("title", title);
			templateModel.setName("form_page");
			return templateModel.renderToString(req);
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

	public static boolean isMobileUserAgent(String ua){
		ua=ua.toLowerCase();
		return ua.matches("(?i).*((android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino).*")
				|| (ua.length()>4 && ua.substring(0,4).matches("(?i)1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-"));
	}
}
