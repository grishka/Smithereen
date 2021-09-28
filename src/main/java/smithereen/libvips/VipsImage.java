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
			"VipsForeignLoadHeifFile");

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
		nativePtr=vips_image_new_from_file(filePath, Pointer.NULL);
		if(nativePtr==Pointer.NULL){
			throwError();
		}
	}

	private VipsImage(Pointer ptr){
		nativePtr=ptr;
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
		if(vips_resize(nativePtr, out, scale, Pointer.NULL)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public VipsImage resize(double hscale, double vscale) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_resize(nativePtr, out, hscale, "vscale", vscale, Pointer.NULL)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public VipsImage crop(int left, int top, int width, int height) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_crop(nativePtr, out, left, top, width, height, Pointer.NULL)!=0){
			throwError();
		}
		return new VipsImage(out.getValue());
	}

	public void writeToFile(String fileName) throws IOException{
		ensureNotReleased();
		if(vips_image_write_to_file(nativePtr, fileName, Pointer.NULL)!=0){
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
			if(vips_flatten(nativePtr, out, "background", arr, Pointer.NULL)!=0){
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
		if(vips_cast_uchar(nativePtr, out, Pointer.NULL)!=0)
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

	public boolean hasField(String name){
		ensureNotReleased();
		return vips_image_get_typeof(nativePtr, name)!=Pointer.NULL;
	}

	public VipsImage iccTransform(String outputProfile) throws IOException{
		ensureNotReleased();
		PointerByReference out=new PointerByReference();
		if(vips_icc_transform(nativePtr, out, outputProfile, Pointer.NULL)!=0)
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
}
