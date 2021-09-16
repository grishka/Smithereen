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
	}

	static native int vips_init(String argv0);
	static native String vips_foreign_find_load(String filename);
	static native void vips_leak_set(boolean leak);
	static native Pointer vips_image_new_from_file(String name, Pointer _null);
	static native String vips_error_buffer();
	static native void vips_error_clear();
	static native int vips_image_get_width(Pointer img);
	static native int vips_image_get_height(Pointer img);
	static native int vips_resize(Pointer in, PointerByReference out, double scale, Pointer _null);
	static native int vips_resize(Pointer in, PointerByReference out, double scale, String _vscale, double vscale, Pointer _null);
	static native int vips_image_write_to_file(Pointer img, String fileName, Pointer _null);
	static native boolean vips_image_hasalpha(Pointer img);
	static native Pointer vips_array_double_new(double[] array, int n);
	static native void vips_area_unref(Pointer area);
	static native int vips_flatten(Pointer in, PointerByReference out, String bgName, Pointer bgValue, Pointer _null);
	static native int vips_crop(Pointer in, PointerByReference out, int left, int top, int width, int height, Pointer _null);
	static native int vips_image_get_bands(Pointer img);
	static native int vips_image_get_format(Pointer img);
	static native int vips_cast_uchar(Pointer img, PointerByReference out, Pointer _null);
	static native Pointer vips_image_get_fields(Pointer img);
	static native Pointer vips_image_get_typeof(Pointer img, String name);
	static native boolean vips_image_remove(Pointer img, String name);
	static native int vips_icc_transform(Pointer img, PointerByReference out, String outputProfile, Pointer _null);

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
}

