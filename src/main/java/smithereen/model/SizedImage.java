package smithereen.model;

import java.net.URI;
import java.util.List;

import smithereen.storage.ImgProxy;
import spark.utils.StringUtils;

public interface SizedImage{
	URI getUriForSizeAndFormat(Type size, Format format);
	Dimensions getOriginalDimensions();
	default Dimensions getDimensionsForSize(Type size){
		return size.getResizedDimensions(getOriginalDimensions());
	}

	default String generateHTML(Type size, List<String> additionalClasses, String styleAttr, int width, int height){
		StringBuilder sb=new StringBuilder("<picture>");
		appendHtmlForFormat(size, Format.WEBP, sb);
		appendHtmlForFormat(size, Format.JPEG, sb);
		sb.append("<img src=\"");
		sb.append(getUriForSizeAndFormat(size, Format.JPEG));
		sb.append('"');
		if(additionalClasses!=null && !additionalClasses.isEmpty()){
			sb.append(" class=\"");
			boolean first=true;
			for(String cls:additionalClasses){
				if(!first){
					sb.append(' ');
				}else{
					first=false;
				}
				sb.append(cls);
			}
			sb.append('"');
		}
		if(StringUtils.isNotEmpty(styleAttr)){
			sb.append(" style=\"");
			sb.append(styleAttr);
			sb.append('"');
		}
		if(width>0){
			sb.append(" width=\"");
			sb.append(width);
			sb.append('"');
		}
		if(height>0){
			sb.append(" height=\"");
			sb.append(height);
			sb.append('"');
		}
		sb.append("/></picture>");
		return sb.toString();
	}

	private void appendHtmlForFormat(Type size, Format format, StringBuilder sb){
		sb.append("<source srcset=\"");
		sb.append(getUriForSizeAndFormat(size, format));
		sb.append(", ");
		sb.append(getUriForSizeAndFormat(size.get2xType(), format));
		sb.append(" 2x\" type=\"");
		sb.append(format.contentType());
		sb.append("\"/>");
	}

	enum Type{
		/**
		 * Photos: 256x256
		 */
		SMALL("s", 256, 256, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 512x512
		 */
		MEDIUM("m", 512, 512, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 1024x1024
		 */
		LARGE("l", 1024, 1024, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 2560x2560
		 */
		XLARGE("xl", 2560, 2560, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 128x128
		 */
		XSMALL("xs", 128, 128, ImgProxy.ResizingType.FIT),

		/**
		 * Avatars: 200xH
		 */
		RECT_LARGE("rl", 200, 2560, ImgProxy.ResizingType.FIT),
		/**
		 * Avatars: 400xH
		 */
		RECT_XLARGE("rxl", 400, 2560, ImgProxy.ResizingType.FIT),

		/**
		 * Avatars: 50x50 square
		 */
		SQUARE_SMALL("sqs", 50, 50, ImgProxy.ResizingType.FILL),
		/**
		 * Avatars: 100x100 square
		 */
		SQUARE_MEDIUM("sqm", 100, 100, ImgProxy.ResizingType.FILL),
		/**
		 * Avatars: 200x200 square
		 */
		SQUARE_LARGE("sql", 200, 200, ImgProxy.ResizingType.FILL),
		/**
		 * Avatars: 400x400 square
		 */
		SQUARE_XLARGE("sqxl", 400, 400, ImgProxy.ResizingType.FILL);

		private final String suffix;
		private final int maxWidth, maxHeight;
		private final ImgProxy.ResizingType resizingType;

		Type(String suffix, int maxWidth, int maxHeight, ImgProxy.ResizingType resizingType){
			this.suffix=suffix;
			this.maxWidth=maxWidth;
			this.maxHeight=maxHeight;
			this.resizingType=resizingType;
		}

		public String suffix(){
			return suffix;
		}

		public int getMaxWidth(){
			return maxWidth;
		}

		public int getMaxHeight(){
			return maxHeight;
		}

		public ImgProxy.ResizingType getResizingType(){
			return resizingType;
		}

		public Dimensions getResizedDimensions(Dimensions in){
			if(resizingType==ImgProxy.ResizingType.FILL){
				return new Dimensions(maxWidth, maxHeight);
			}
			if(maxWidth==maxHeight){
				float ratio=maxWidth/(float)Math.max(in.width, in.height);
				return new Dimensions(Math.round(in.width*ratio), Math.round(in.height*ratio));
			}else{
				float ratio=maxWidth/(float)in.width;
				return new Dimensions(Math.round(in.width*ratio), Math.round(in.height*ratio));
			}
		}

		public Type get2xType(){
			return switch(this){
				case XSMALL -> SMALL;
				case SMALL -> MEDIUM;
				case MEDIUM -> LARGE;
				case LARGE, XLARGE -> XLARGE;
				case RECT_XLARGE, RECT_LARGE -> RECT_XLARGE;
				case SQUARE_SMALL -> SQUARE_MEDIUM;
				case SQUARE_MEDIUM -> SQUARE_LARGE;
				case SQUARE_LARGE, SQUARE_XLARGE -> SQUARE_XLARGE;
			};
		}

		public boolean isRect(){
			return this==RECT_LARGE || this==RECT_XLARGE;
		}

		public static Type fromSuffix(String s){
			return switch(s){
				case "xs" -> XSMALL;
				case "s" -> SMALL;
				case "m" -> MEDIUM;
				case "l" -> LARGE;
				case "xl" -> XLARGE;
				case "rl" -> RECT_LARGE;
				case "rxl" -> RECT_XLARGE;
				case "sqs" -> SQUARE_SMALL;
				case "sqm" -> SQUARE_MEDIUM;
				case "sql" -> SQUARE_LARGE;
				case "sqxl" -> SQUARE_XLARGE;
				default -> null;
			};
		}
	}

	enum Format{
		JPEG,
		WEBP,
		AVIF,
		PNG;

		public String fileExtension(){
			return switch(this){
				case JPEG -> "jpg";
				case WEBP -> "webp";
				case AVIF -> "avif";
				case PNG -> "png";
			};
		}

		public String contentType(){
			return switch(this){
				case JPEG -> "image/jpeg";
				case WEBP -> "image/webp";
				case AVIF -> "image/avif";
				case PNG -> "image/png";
			};
		}
	}

	class Dimensions{
		public static final Dimensions UNKNOWN=new Dimensions(-1, -1);

		public final int width, height;

		public Dimensions(int width, int height){
			this.width=width;
			this.height=height;
		}
	}
}
