package smithereen.libvips;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.IOException;
import java.util.List;

import smithereen.Config;
import smithereen.util.BlurHash;

class LibVips{
	static{
		Native.register("vips-cpp");
		boolean isWindows=System.getProperty("os.name").startsWith("Windows");
		Native.register(LibGObject.class, isWindows ? "libgobject-2.0-0" : "vips-cpp");
		Native.register(LibGLib.class, isWindows ? "libglib-2.0-0" : "vips-cpp");

		vips_init("");
		if(Config.DEBUG)
			vips_leak_set(true);

		varArgsWrapper=Native.load(LibVipsVarArgsWrapper.class);
	}

	private static LibVipsVarArgsWrapper varArgsWrapper;

	static native int vips_init(String argv0);
	static native String vips_foreign_find_load(String filename);
	static native void vips_leak_set(boolean leak);
	static Pointer vips_image_new_from_file(String name){
		return varArgsWrapper.vips_image_new_from_file(name, Pointer.NULL);
	}
	static native String vips_error_buffer();
	static native void vips_error_clear();
	static native int vips_image_get_width(Pointer img);
	static native int vips_image_get_height(Pointer img);
	static int vips_resize(Pointer in, PointerByReference out, double scale){
		return varArgsWrapper.vips_resize(in, out, scale, Pointer.NULL);
	}
	static int vips_resize(Pointer in, PointerByReference out, double scale, double vscale){
		return varArgsWrapper.vips_resize(in, out, scale, "vscale", vscale, Pointer.NULL);
	}
	static int vips_image_write_to_file(Pointer img, String fileName){
		return varArgsWrapper.vips_image_write_to_file(img, fileName, Pointer.NULL);
	}
	static native boolean vips_image_hasalpha(Pointer img);
	static native Pointer vips_array_double_new(double[] array, int n);
	static native void vips_area_unref(Pointer area);
	static int vips_flatten(Pointer in, PointerByReference out, Pointer bgValue){
		return varArgsWrapper.vips_flatten(in, out, "background", bgValue, Pointer.NULL);
	}
	static int vips_crop(Pointer in, PointerByReference out, int left, int top, int width, int height){
		return varArgsWrapper.vips_crop(in, out, left, top, width, height, Pointer.NULL);
	}
	static native int vips_image_get_bands(Pointer img);
	static native int vips_image_get_format(Pointer img);
	static int vips_cast_uchar(Pointer img, PointerByReference out){
		return varArgsWrapper.vips_cast_uchar(img, out, Pointer.NULL);
	}
	static native Pointer vips_image_get_fields(Pointer img);
	static native Pointer vips_image_get_typeof(Pointer img, String name);
	static native boolean vips_image_remove(Pointer img, String name);
	static int vips_icc_transform(Pointer img, PointerByReference out, String outputProfile){
		return varArgsWrapper.vips_icc_transform(img, out, outputProfile, Pointer.NULL);
	}

	static native Pointer vips_region_new(Pointer img);
	static native int vips_region_prepare(Pointer region, VipsRect rect);
	static native Pointer vips_region_fetch(Pointer region, int left, int top, int width, int height, IntByReference len);

	static void throwError() throws IOException{
		String err=vips_error_buffer();
		vips_error_clear();
		throw new IOException(err);
	}

	static class LibGObject{
		static native void g_object_unref(Pointer obj);
	}

	static class LibGLib{
		static native void g_free(Pointer ptr);
		static native void g_strfreev(Pointer ptr);
	}

	private interface LibVipsVarArgsWrapper extends Library{
		Pointer vips_image_new_from_file(String name, Object... args);
		int vips_resize(Pointer in, PointerByReference out, double scale, Object... args);
		int vips_flatten(Pointer in, PointerByReference out, Object... args);
		int vips_crop(Pointer in, PointerByReference out, int left, int top, int width, int height, Object... args);
		int vips_image_write_to_file(Pointer img, String fileName, Object... args);
		int vips_cast_uchar(Pointer img, PointerByReference out, Object... args);
		int vips_icc_transform(Pointer img, PointerByReference out, String outputProfile, Object... args);
	}
}

