package smithereen.data;

import java.net.URI;

import smithereen.storage.ImgProxy;

public interface SizedImage{
	URI getUriForSizeAndFormat(Type size, Format format);
	Dimensions getOriginalDimensions();
	default Dimensions getDimensionsForSize(Type size){
		return size.getResizedDimensions(getOriginalDimensions());
	}

	enum Type{
		/**
		 * Photos: 400x400
		 */
		SMALL("s", 400, 400, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 800x800
		 */
		MEDIUM("m", 800, 800, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 1280x1280
		 */
		LARGE("l", 1280, 1280, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 2560x2560
		 */
		XLARGE("xl", 2560, 2560, ImgProxy.ResizingType.FIT),
		/**
		 * Photos: 200x200
		 */
		XSMALL("xs", 200, 200, ImgProxy.ResizingType.FIT),

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

		public static Type fromSuffix(String s){
			switch(s){
				case "xs":
					return XSMALL;
				case "s":
					return SMALL;
				case "m":
					return MEDIUM;
				case "l":
					return LARGE;
				case "xl":
					return XLARGE;
				case "rl":
					return RECT_LARGE;
				case "rxl":
					return RECT_XLARGE;
				case "sqs":
					return SQUARE_SMALL;
				case "sqm":
					return SQUARE_MEDIUM;
				case "sql":
					return SQUARE_LARGE;
				case "sqxl":
					return SQUARE_XLARGE;
			}
			return null;
		}
	}

	enum Format{
		JPEG,
		WEBP,
		AVIF;

		public String fileExtension(){
			switch(this){
				case JPEG:
					return "jpg";
				case WEBP:
					return "webp";
				case AVIF:
					return "avif";
				default:
					throw new IllegalArgumentException();
			}
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
