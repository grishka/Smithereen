ifeq ($(OS),Windows_NT)
	LIB_EXT := dll
JNI_PLATFORM := win32
else
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
	LIB_EXT := jnilib
	JNI_PLATFORM := darwin
else
	LIB_EXT := so
	JNI_PLATFORM := linux
endif
endif

# https://stackoverflow.com/questions/44239211/portable-makefile-for-jni-applications
ifeq ($(JAVA_HOME),)
ifeq ($(OS),Windows_NT)
which = $(shell where $1)
else
which = $(shell which $1)
endif
JAVAC ?= $(realpath $(call which,javac))
JAVA_HOME = $(abspath $(dir $(JAVAC))..)
endif

ifneq ($(JAVA_HOME),)
JNI_INCLUDEDIR ?= $(JAVA_HOME)/include
endif

ifeq ($(JNI_INCLUDEDIR),)
$(error could not determine JNI include dir, try specifying either \
    JAVA_HOME or JNI_INCLUDEDIR)
endif

OBJECTS := libvips_jni.o blurhash.o
CPPFLAGS += $(shell pkg-config --cflags vips) -I$(JNI_INCLUDEDIR) -I$(JNI_INCLUDEDIR)/$(JNI_PLATFORM) -fPIC
LDFLAGS += -shared
LDFLAGS += $(shell pkg-config --libs vips)

libvips_jni.$(LIB_EXT): $(OBJECTS)
	$(CXX) $(CFLAGS) $(OBJECTS) -o $@ $(LDFLAGS)

libvips_jni.o: libvips_jni.cpp blurhash.h
blurhash.o: blurhash.cpp blurhash.h

.PHONY: clean
clean:
	-rm -f libvips_jni.$(LIB_EXT) $(OBJECTS)
