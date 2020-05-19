package smithereen.data;

import java.net.URI;
import java.text.Format;

public class PhotoSize{
	public URI src;
	public int width;
	public int height;
	public Type type;
	public Format format;

	public PhotoSize(URI src, int width, int height, Type type, Format format){
		this.src=src;
		this.width=width;
		this.height=height;
		this.type=type;
		this.format=format;
	}

	public static final int UNKNOWN=-1;

	public enum Type{
		/**
		 * Avatars: 50x50
		 * Photos: 400x400
		 */
		SMALL,
		/**
		 * Avatars: 100x100
		 * Photos: 800x800
		 */
		MEDIUM,
		/**
		 * Avatars: 200x200
		 * Photos: 1280x1280
		 */
		LARGE,
		/**
		 * Avatars: 400x400
		 * Photos: 2560x2560
		 */
		XLARGE,
		/**
		 * Photos: 200x200
		 */
		XSMALL,
		/**
		 * Avatars: 200xH
		 */
		RECT_LARGE,
		/**
		 * Avatars: 400xH
		 */
		RECT_XLARGE;

		public String suffix(){
			switch(this){
				case XSMALL:
					return "xs";
				case SMALL:
					return "s";
				case MEDIUM:
					return "m";
				case LARGE:
					return "l";
				case XLARGE:
					return "xl";
				case RECT_LARGE:
					return "rl";
				case RECT_XLARGE:
					return "rxl";
				default:
					throw new IllegalArgumentException();
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
			}
			throw new IllegalArgumentException("Unknown size suffix "+s);
		}
	}

	public enum Format{
		JPEG,
		WEBP;

		public String fileExtension(){
			switch(this){
				case JPEG:
					return "jpg";
				case WEBP:
					return "webp";
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
