package smithereen.storage;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import smithereen.Config;
import smithereen.model.SizedImage;

public class ImgProxy{

	private static final Base64.Encoder BASE64=Base64.getUrlEncoder().withoutPadding();

	public static String generateUrl(String url, String resize, int width, int height, String gravity, int enlarge, String extension){
		try{
			String encodedUrl=Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
			String path="/"+resize+"/"+width+"/"+height+"/"+gravity+"/"+enlarge+"/"+encodedUrl+"."+extension;
			Mac hmac=Mac.getInstance("HmacSHA256");
			SecretKeySpec keySpec=new SecretKeySpec(Config.imgproxyKey, "HmacSHA256");
			hmac.init(keySpec);
			hmac.update(Config.imgproxySalt);
			String hash=BASE64.encodeToString(hmac.doFinal(path.getBytes(StandardCharsets.UTF_8)));
			return Config.imgproxyUrl+'/'+hash+path;
		}catch(NoSuchAlgorithmException|InvalidKeyException x){
			throw new RuntimeException(x);
		}
	}

	public static class UrlBuilder{

		private static final DecimalFormat DECIMAL_FORMAT=new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.US));

		private String imageUrl;
		private String extension;
		private ArrayList<String> options=new ArrayList<>();
		private boolean qualitySet=false;
		private SizedImage.Format format;

		public UrlBuilder(String imageUrl){
			this.imageUrl=imageUrl;
		}

		public UrlBuilder resize(ResizingType type, int width, int height, boolean enlarge, boolean extend){
			options.add("rs:"+type.val+":"+width+":"+height+(enlarge || extend ? (":"+bool(enlarge)+":"+bool(extend)) : ""));
			return this;
		}

		public UrlBuilder resize(int width, int height){
			return resize(ResizingType.FIT, width, height, false, false);
		}

		public UrlBuilder crop(int x, int y, int width, int height){
			options.add("c:"+width+":"+height+":nowe:"+x+":"+y);
			return this;
		}

		public UrlBuilder dpr(float dpr){
			options.add("dpr:"+DECIMAL_FORMAT.format(dpr));
			return this;
		}

		public UrlBuilder format(SizedImage.Format format){
			extension=format.fileExtension();
			this.format=format;
			return this;
		}

		public UrlBuilder quality(int quality){
			options.add("q:"+quality);
			qualitySet=true;
			return this;
		}

		public URI build(){
			if(format==null){
				throw new IllegalArgumentException("Output file format not set");
			}
			if(!qualitySet){
				switch(format){
					case JPEG:
						quality(93);
						break;
					case WEBP:
						quality(90);
						break;
					case AVIF:
						quality(85);
						break;
				}
			}

			StringBuilder paramsSb=new StringBuilder();
			options.sort(String::compareTo);
			for(String s:options){
				paramsSb.append('/');
				paramsSb.append(s);
			}
			paramsSb.append('/');
			paramsSb.append(BASE64.encodeToString(imageUrl.getBytes(StandardCharsets.UTF_8)));
			paramsSb.append('.');
			paramsSb.append(extension);
			String params=paramsSb.toString();

			String signature;
			try{
				Mac hmac=Mac.getInstance("HmacSHA256");
				SecretKeySpec keySpec=new SecretKeySpec(Config.imgproxyKey, "HmacSHA256");
				hmac.init(keySpec);
				hmac.update(Config.imgproxySalt);
				signature=BASE64.encodeToString(hmac.doFinal(params.getBytes(StandardCharsets.UTF_8)));
			}catch(Exception x){
				throw new RuntimeException(x);
			}

			return Config.localURI(Config.imgproxyUrl+"/"+signature+params);
		}


		private String bool(boolean b){
			return b ? "1" : "0";
		}
	}

	public enum ResizingType{
		FIT("fit"),
		FILL("fill"),
		AUTO("auto");

		private final String val;

		ResizingType(String val){
			this.val=val;
		}
	}
}
