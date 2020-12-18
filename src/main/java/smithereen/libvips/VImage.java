package smithereen.libvips;

import java.io.IOException;

public class VImage{

	static{
		System.loadLibrary("vips_jni");
	}

	private long nativePtr;

	public VImage(String filePath) throws IOException{
		nativePtr=create(filePath);
	}

	private VImage(long ptr){
		nativePtr=ptr;
	}

	public native int getWidth();
	public native int getHeight();
	public native void release();
	public native VImage resize(double scale) throws IOException;
	public native VImage crop(int left, int top, int width, int height) throws IOException;
	public native void writeToFile(String[] fileNames) throws IOException;
	public native boolean hasAlpha();
	public native VImage flatten(double r, double g, double b) throws IOException;
	public native String blurHash(int xComponents, int yComponents) throws IOException;

	private native long create(String filePath) throws IOException;
}
