package smithereen.libvips;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static smithereen.libvips.LibVips.*;

public class VipsImage{
	private static final List<String> LOADER_WHITELIST=List.of(
			"VipsForeignLoadJpegFile",
//			"VipsForeignLoadPng",
			"VipsForeignLoadPngFile",
			"VipsForeignLoadGifFile",
			"VipsForeignLoadWebpFile",
			"VipsForeignLoadHeifFile",
			"VipsForeignLoadNsgifFile"
	);
	public static final int MAX_SIZE=10_000;

	Pointer nativePtr;
	private boolean released;

	private static final Logger LOG=LoggerFactory.getLogger(VipsImage.class);

	public VipsImage(String filePath) throws IOException{
		String loader=vips_foreign_find_load(filePath);
		if(loader==null)
			throw new IOException("File format not supported");
		if(!LOADER_WHITELIST.contains(loader)){
			LOG.warn("libvips loader not allowed: {}", loader);
			throw new IOException("File format not supported");
		}
		if(loader.equals("VipsForeignLoadJpegFile")){
			filePath+="[autorotate=true]";
		}
		nativePtr=vips_image_new_from_file(filePath);
		if(nativePtr==Pointer.NULL){
			throwError();
		}

		int width=getWidth();
		int height=getHeight();
		if(width>MAX_SIZE || height>MAX_SIZE){
			release();
			throw new IOException("Image size "+width+"x"+height+" exceeds the limit of "+MAX_SIZE+" on largest side");
		}
	}

	private VipsImage(Pointer ptr){
		nativePtr=ptr;
	}

	@Override
	public boolean equals(Object obj){
		return obj instanceof VipsImage other && nativePtr.equals(other.nativePtr);
	}

	public int getWidth(){
		ensureNotReleased();
		return vips_image_get_width(nativePtr);
	}

	public int getHeight(){
		ensureNotReleased();
		return vips_image_get_height(nativePtr);
	}

	public void release(){
		ensureNotReleased();
		LibGObject.g_object_unref(nativePtr);
		released=true;
	}

	public VipsImage resize(double scale) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_resize(nativePtr, out, scale)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public VipsImage resize(double hscale, double vscale) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_resize(nativePtr, out, hscale, vscale)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public VipsImage crop(int left, int top, int width, int height) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_crop(nativePtr, out, left, top, width, height)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public void writeToFile(String fileName) throws IOException{
		ensureNotReleased();
		if(vips_image_write_to_file(nativePtr, fileName)!=0){
			throwError();
		}
	}

	public boolean hasAlpha(){
		ensureNotReleased();
		return vips_image_hasalpha(nativePtr);
	}

	public VipsImage flatten(double r, double g, double b) throws IOException{
		ensureNotReleased();
		Pointer arr=vips_array_double_new(new double[]{r, g, b}, 3);
		PointerByReference out=new PointerByReference();
		try{
			if(vips_flatten(nativePtr, out, arr)!=0){
				throwError();
			}
		}finally{
			vips_area_unref(arr);
		}
		return new VipsImage(out.getValue());
	}

	public int getBands(){
		ensureNotReleased();
		return vips_image_get_bands(nativePtr);
	}

	public BandFormat getFormat(){
		ensureNotReleased();
		return BandFormat.valueOf(vips_image_get_format(nativePtr));
	}

	public VipsImage castUChar() throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_cast_uchar(nativePtr, out)!=0)
			throwError();
		return new VipsImage(out.getValue());
	}

	public List<String> getFields(){
		ensureNotReleased();
		Pointer strings=vips_image_get_fields(nativePtr);
		Pointer[] strarr=strings.getPointerArray(0);
		ArrayList<String> res=new ArrayList<>(strarr.length);
		for(Pointer ptr:strarr){
			res.add(ptr.getString(0));
		}
		LibGLib.g_strfreev(strings);
		return res;
	}

	public boolean removeField(String name){
		ensureNotReleased();
		return vips_image_remove(nativePtr, name);
	}

	public String getField(String name){
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_image_get_string(nativePtr, name, out)==-1)
			throw new IllegalStateException("Failed to get field '"+name+"'");
		return out.getPointer().getString(0);
	}

	public boolean hasField(String name){
		ensureNotReleased();
		return vips_image_get_typeof(nativePtr, name)!=Pointer.NULL;
	}

	public Interpretation getInterpretation(){
		ensureNotReleased();
		return Interpretation.valueOf(vips_image_get_interpretation(nativePtr));
	}

	public VipsImage iccTransform(String outputProfile) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_icc_transform(nativePtr, out, outputProfile)!=0)
			throwError();
		return new VipsImage(out.getValue());
	}

	public VipsImage colorspace(Interpretation interpretation) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_colourspace(nativePtr, out, interpretation.intValue())!=0)
			throwError();
		return new VipsImage(out.getValue());
	}

	public boolean hasColorProfile(){
		return hasField("icc-profile-data");
	}

	void ensureNotReleased(){
		if(released)
			throw new IllegalStateException("This VipsImage was released");
	}

	public enum BandFormat{
		NOTSET,
		UCHAR,
		CHAR,
		USHORT,
		SHORT,
		UINT,
		INT,
		FLOAT,
		COMPLEX,
		DOUBLE,
		DPCOMPLEX,
		LAST;

		private static BandFormat valueOf(int v){
			return switch(v){
				case -1 -> NOTSET;
				case 0 -> UCHAR;
				case 1 -> CHAR;
				case 2 -> USHORT;
				case 3 -> SHORT;
				case 4 -> UINT;
				case 5 -> INT;
				case 6 -> FLOAT;
				case 7 -> COMPLEX;
				case 8 -> DOUBLE;
				case 9 -> DPCOMPLEX;
				case 10 -> LAST;
				default -> throw new IllegalStateException("Unexpected value: "+v);
			};
		}
	}

	public enum Interpretation{
		ERROR,
		MULTIBAND,
		B_W,
		HISTOGRAM,
		XYZ,
		LAB,
		CMYK,
		LABQ,
		RGB,
		CMC,
		LCH,
		LABS,
		sRGB,
		YXY,
		FOURIER,
		RGB16,
		GREY16,
		MATRIX,
		scRGB,
		HSV,
		OKLAB,
		OKLCH;

		private static Interpretation valueOf(int v){
			return switch(v){
				case -1 -> ERROR;
				case 0 -> MULTIBAND;
				case 1 -> B_W;
				case 10 -> HISTOGRAM;
				case 12 -> XYZ;
				case 13 -> LAB;
				case 15 -> CMYK;
				case 16 -> LABQ;
				case 17 -> RGB;
				case 18 -> CMC;
				case 19 -> LCH;
				case 21 -> LABS;
				case 22 -> sRGB;
				case 23 -> YXY;
				case 24 -> FOURIER;
				case 25 -> RGB16;
				case 26 -> GREY16;
				case 27 -> MATRIX;
				case 28 -> scRGB;
				case 29 -> HSV;
				case 30 -> OKLAB;
				case 31 -> OKLCH;
				default -> throw new IllegalStateException("Unexpected value: " + v);
			};
		}

		public int intValue(){
			return switch(this){
				case ERROR -> -1;
				case MULTIBAND->  0;
				case B_W->  1;
				case HISTOGRAM -> 10;
				case XYZ -> 12;
				case LAB -> 13;
				case CMYK -> 15;
				case LABQ -> 16;
				case RGB -> 17;
				case CMC -> 18;
				case LCH -> 19;
				case LABS -> 21;
				case sRGB -> 22;
				case YXY -> 23;
				case FOURIER -> 24;
				case RGB16 -> 25;
				case GREY16 -> 26;
				case MATRIX -> 27;
				case scRGB -> 28;
				case HSV -> 29;
				case OKLAB -> 30;
				case OKLCH -> 31;
			};
		}
	}
}
