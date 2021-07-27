package smithereen.libvips;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import static smithereen.libvips.LibVips.*;

public class VipsRegion{
	private Pointer nativePtr;
	private boolean released;

	public VipsRegion(VipsImage img){
		img.ensureNotReleased();
		nativePtr=vips_region_new(img.nativePtr);
	}

	public byte[] fetch(int left, int top, int width, int height){
		ensureNotReleased();
		IntByReference len=new IntByReference();
		Pointer pixels=vips_region_fetch(nativePtr, left, top, width, height, len);
		byte[] p=pixels.getByteArray(0, len.getValue());
		LibGLib.g_free(pixels);
		return p;
	}

	public void release(){
		ensureNotReleased();
		LibGObject.g_object_unref(nativePtr);
		released=true;
	}

	void ensureNotReleased(){
		if(released)
			throw new IllegalStateException("This VipsRegion was released");
	}
}
