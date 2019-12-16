package smithereen;

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
import smithereen.data.UserNotifications;
import smithereen.lang.Lang;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class Utils{

	private static final List<String> RESERVED_USERNAMES=Arrays.asList("account", "settings", "feed", "activitypub", "api", "system");
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
		if(req.session(false)!=null){
			model.with("csrf", req.session().attribute("csrf"));
			Account account=req.session().attribute("account");
			if(account!=null){
				model.with("currentUser", account.user);
				try{
					UserNotifications notifications=UserStorage.getNotificationsForUser(account.user.id);
					model.with("userNotifications", notifications);
				}catch(SQLException x){
					throw new RuntimeException(x);
				}
			}
		}
		model.with("locale", localeForRequest(req));
	}

	public static String renderTemplate(Request req, String name, JtwigModel model){
		addGlobalParamsToTemplate(req, model);
		JtwigTemplate template=JtwigTemplate.classpathTemplate("templates/desktop/"+name+".twig", Main.jtwigEnv);
		return template.render(model);
	}

	public static boolean requireAccount(Request req, Response resp){
		if(req.session(false)==null || req.session().attribute("account")==null){
			resp.redirect("/");
			return false;
		}
		return true;
	}

	public static boolean verifyCSRF(Request req, Response resp){
		String reqCsrf=req.queryParams("csrf");
		if(reqCsrf!=null && reqCsrf.equals(req.session().attribute("csrf"))){
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
		Lang l=Lang.get((Locale) req.session().attribute("locale"));
		return renderTemplate(req, "generic_error", JtwigModel.newModel().with("error", formatArgs.length>0 ? l.get(errorKey, formatArgs) : l.get(errorKey)));
	}

	public static Locale localeForRequest(Request req){
		if(req.session(false)!=null && req.session().attribute("locale")!=null){
			return req.session().attribute("locale");
		}
		return Locale.US;
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

	/**
	 * Convenience method that returns a scaled instance of the
	 * provided {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance,
	 *    in pixels
	 * @param targetHeight the desired height of the scaled instance,
	 *    in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step
	 *    scaling technique that provides higher quality than the usual
	 *    one-step technique (only useful in downscaling cases, where
	 *    {@code targetWidth} or {@code targetHeight} is
	 *    smaller than the original dimensions, and generally only when
	 *    the {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage getScaledInstance(BufferedImage img,
										   int targetWidth,
										   int targetHeight,
										   Object hint,
										   boolean higherQuality)
	{
		int type = (img.getTransparency() == Transparency.OPAQUE) ?
				BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = (BufferedImage)img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

	public static String sanitizeHTML(String src){
		StringBuilder sb=new StringBuilder();
		HtmlSanitizer.sanitize(src, HTML_SANITIZER.apply(HtmlStreamRenderer.create(sb, null)));
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
}
