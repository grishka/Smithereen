#include <stdio.h>
#include <vips/vips.h>
#include <jni.h>
#include <stdint.h>

namespace{
	jfieldID nativePtrField;

	bool ensureNotReleased(VipsImage* img, JNIEnv* env){
		if(!img){
			env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "This VImage was released");
			return false;
		}
		return true;
	}
}


extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* jvm, void* reserved){
	printf("JNI_OnLoad\n");
	VIPS_INIT("");
	nativePtrField=NULL;
	return JNI_VERSION_1_4;
}

extern "C" JNIEXPORT jlong Java_smithereen_libvips_VImage_create(JNIEnv* env, jobject thiz, jstring _filename){
	if(!nativePtrField){
		nativePtrField=env->GetFieldID(env->GetObjectClass(thiz), "nativePtr", "J");
	}
	const char* filename=env->GetStringUTFChars(_filename, NULL);
	VipsImage* result=vips_image_new_from_file(filename, NULL);
	if(!result){
		env->ThrowNew(env->FindClass("java/io/IOException"), vips_error_buffer());
		vips_error_clear();
	}
	env->ReleaseStringUTFChars(_filename, filename);
	return (jlong)(intptr_t)result;
}

extern "C" JNIEXPORT void Java_smithereen_libvips_VImage_release(JNIEnv* env, jobject thiz){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return;
	g_object_unref(img);
	env->SetLongField(thiz, nativePtrField, 0);
}

extern "C" JNIEXPORT jint Java_smithereen_libvips_VImage_getWidth(JNIEnv* env, jobject thiz){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return 0;
	return vips_image_get_width(img);
}

extern "C" JNIEXPORT jint Java_smithereen_libvips_VImage_getHeight(JNIEnv* env, jobject thiz){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return 0;
	return vips_image_get_height(img);
}

extern "C" JNIEXPORT jobject Java_smithereen_libvips_VImage_resize(JNIEnv* env, jobject thiz, jdouble scale, jobjectArray outputs){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return NULL;
	VipsImage* resized;
	if(vips_resize(img, &resized, scale, NULL)!=0){
		env->ThrowNew(env->FindClass("java/io/IOException"), vips_error_buffer());
		vips_error_clear();
		return NULL;
	}
	jclass cls=env->GetObjectClass(thiz);
	return env->NewObject(cls, env->GetMethodID(cls, "<init>", "(J)V"), (jlong)(intptr_t)resized);
}

extern "C" JNIEXPORT jobject Java_smithereen_libvips_VImage_crop(JNIEnv* env, jobject thiz, jint left, jint top, jint width, jint height){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return NULL;
	VipsImage* cropped;
	if(vips_crop(img, &cropped, left, top, width, height, NULL)!=0){
			env->ThrowNew(env->FindClass("java/io/IOException"), vips_error_buffer());
			vips_error_clear();
			return NULL;
	}
	jclass cls=env->GetObjectClass(thiz);
	return env->NewObject(cls, env->GetMethodID(cls, "<init>", "(J)V"), (jlong)(intptr_t)cropped);
}

extern "C" JNIEXPORT void Java_smithereen_libvips_VImage_writeToFile(JNIEnv* env, jobject thiz, jobjectArray outputs){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return;
	for(unsigned int i=0;i<env->GetArrayLength(outputs);i++){
		jstring _out=(jstring) env->GetObjectArrayElement(outputs, i);
		const char* out=env->GetStringUTFChars(_out, NULL);
		if(vips_image_write_to_file(img, out, NULL)!=0){
			env->ReleaseStringUTFChars(_out, out);
			env->ThrowNew(env->FindClass("java/io/IOException"), vips_error_buffer());
			vips_error_clear();
			return;
		}
		env->ReleaseStringUTFChars(_out, out);
	}
}

extern "C" JNIEXPORT jboolean Java_smithereen_libvips_VImage_hasAlpha(JNIEnv* env, jobject thiz){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return false;
	return vips_image_hasalpha(img);
}

extern "C" JNIEXPORT jobject Java_smithereen_libvips_VImage_flatten(JNIEnv* env, jobject thiz, jdouble r, jdouble g, jdouble b){
	VipsImage* img=(VipsImage*)(intptr_t)env->GetLongField(thiz, nativePtrField);
	if(!ensureNotReleased(img, env))
		return NULL;
	VipsArrayDouble* bgArray=vips_array_double_newv(3, r, g, b);
	VipsImage* out;
	if(vips_flatten(img, &out, "background", bgArray, NULL)!=0){
		env->ThrowNew(env->FindClass("java/io/IOException"), vips_error_buffer());
		vips_error_clear();
		vips_area_unref((VipsArea*)bgArray);
		return NULL;
	}
	vips_area_unref((VipsArea*)bgArray);
	jclass cls=env->GetObjectClass(thiz);
	return env->NewObject(cls, env->GetMethodID(cls, "<init>", "(J)V"), (jlong)(intptr_t)out);
}

