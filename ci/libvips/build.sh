#!/usr/bin/env bash

# Adapted from https://github.com/lovell/sharp-libvips/blob/4591f6f6d86bb6208534a055ba3b08655a53384e/build/posix.sh
# Changes:
# - Removed librsvg and its dependencies because Smithereen doesn't need any of that
# - Removed libtiff for the same reason
# - Added libde265 to support HEIC because 99.99% of people don't care about patents
# - Minor tweaks to make the build work outside of Docker

set -e

if [ -z $PKG_CONFIG ]; then
  PKG_CONFIG="pkg-config --static"
fi
if [ -z $PLATFORM ]; then
  case "$(uname -m)" in
    aarch64)
      PLATFORM="linux-arm64v8"
      ;;
    x86_64)
      PLATFORM="linux-x64"
      ;;
    esac
fi
if [ -z $FLAGS ]; then
  case "$(uname -m)" in
    aarch64)
      FLAGS="-march=armv8-a"
      ;;
    x86_64)
      FLAGS="-march=nehalem"
      ;;
    esac
fi
if [ -z $MESON ]; then
  MESON="--cross-file=$PWD/$PLATFORM/meson.ini"
fi

if ! [ -x "$(command -v meson)" ]; then
	echo "Installing meson"
	pip3 install meson || exit 1
fi


# Dependency version numbers
if [ -f ./versions.properties ]; then
  source ./versions.properties
fi

# Remove patch version component
without_patch() {
  echo "${1%.[[:digit:]]*}"
}
# Remove prerelease suffix
without_prerelease() {
  echo "${1%-[[:alnum:]]*}"
}

# Environment / working directories
case ${PLATFORM} in
  linux*)
    LINUX=true
    DEPS=$PWD/deps
    TARGET=$PWD/target
    PACKAGE=$PWD/packaging
    ROOT=$PWD/$PLATFORM
    VIPS_CPP_DEP=libvips-cpp.so.$(without_prerelease $VERSION_VIPS)
    ;;
  darwin*)
    DARWIN=true
    DEPS=$PWD/deps
    TARGET=$PWD/target
    PACKAGE=$PWD/packaging
    ROOT=$PWD/$PLATFORM
    VIPS_CPP_DEP=libvips-cpp.$(without_prerelease $VERSION_VIPS).dylib
    ;;
esac

mkdir ${DEPS}
mkdir ${TARGET}

# Default optimisation level is for binary size (-Os)
# Overriden to performance (-O3) for select dependencies that benefit
export FLAGS+=" -Os -fPIC"

# Force "new" C++11 ABI compliance
# Remove async exception unwind/backtrace tables
# Allow linker to remove unused sections
if [ "$LINUX" = true ]; then
  export FLAGS+=" -D_GLIBCXX_USE_CXX11_ABI=1 -fno-asynchronous-unwind-tables -ffunction-sections -fdata-sections"
fi

# Common build paths and flags
export PKG_CONFIG_LIBDIR="${TARGET}/lib/pkgconfig"
export PATH="${PATH}:${TARGET}/bin"
export LD_LIBRARY_PATH="${TARGET}/lib"
export CFLAGS="${FLAGS}"
export CXXFLAGS="${FLAGS}"
export OBJCFLAGS="${FLAGS}"
export OBJCXXFLAGS="${FLAGS}"
export CPPFLAGS="-I${TARGET}/include"
export LDFLAGS="-L${TARGET}/lib"

# On Linux, we need to create a relocatable library
# Note: this is handled for macOS using the `install_name_tool` (see below)
if [ "$LINUX" = true ]; then
  export LDFLAGS+=" -Wl,--gc-sections -Wl,-rpath=\$ORIGIN/"
fi

if [ "$DARWIN" = true ]; then
  # Let macOS linker remove unused code
  export LDFLAGS+=" -Wl,-dead_strip"
fi

# Run as many parallel jobs as there are available CPU cores
if [ "$LINUX" = true ]; then
  export MAKEFLAGS="-j$(nproc)"
elif [ "$DARWIN" = true ]; then
  export MAKEFLAGS="-j$(sysctl -n hw.logicalcpu)"
fi

# We don't want to use any native libraries, so unset PKG_CONFIG_PATH
unset PKG_CONFIG_PATH

# Common options for curl
CURL="curl --silent --location --retry 3 --retry-max-time 30"

# Download and build dependencies from source

if [ "${PLATFORM%-*}" == "linuxmusl" ] || [ "$DARWIN" = true ]; then
  # musl and macOS requires the standalone intl support library of gettext, since it's not provided by libc (like GNU).
  # We use a stub version of gettext instead, since we don't need any of the i18n features.
  mkdir ${DEPS}/proxy-libintl
  $CURL https://github.com/frida/proxy-libintl/archive/${VERSION_PROXY_LIBINTL}.tar.gz | tar xzC ${DEPS}/proxy-libintl --strip-components=1
  cd ${DEPS}/proxy-libintl
  meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} ${MESON}
  meson install -C _build --tag devel
fi

mkdir ${DEPS}/zlib-ng
$CURL https://github.com/zlib-ng/zlib-ng/archive/${VERSION_ZLIB_NG}.tar.gz | tar xzC ${DEPS}/zlib-ng --strip-components=1
cd ${DEPS}/zlib-ng
CFLAGS="${CFLAGS} -O3" cmake -G"Unix Makefiles" \
  -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=FALSE -DZLIB_COMPAT=TRUE -DWITH_ARMV6=FALSE
make install/strip

mkdir ${DEPS}/ffi
$CURL https://github.com/libffi/libffi/releases/download/v${VERSION_FFI}/libffi-${VERSION_FFI}.tar.gz | tar xzC ${DEPS}/ffi --strip-components=1
cd ${DEPS}/ffi
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared --disable-dependency-tracking \
  --disable-builddir --disable-multi-os-directory --disable-raw-api --disable-structs --disable-docs
make install-strip

mkdir ${DEPS}/glib
$CURL https://download.gnome.org/sources/glib/$(without_patch $VERSION_GLIB)/glib-${VERSION_GLIB}.tar.xz | tar xJC ${DEPS}/glib --strip-components=1
cd ${DEPS}/glib
$CURL https://gist.github.com/kleisauke/284d685efa00908da99ea6afbaaf39ae/raw/12773e117bd557b83ba2a7410698db41813c3fda/glib-without-gregex.patch | patch -p1
meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} --datadir=${TARGET}/share ${MESON} \
  --force-fallback-for=gvdb -Dintrospection=disabled -Dnls=disabled -Dlibmount=disabled -Dsysprof=disabled -Dlibelf=disabled \
  -Dtests=false -Dglib_assert=false -Dglib_checks=false -Dglib_debug=disabled ${DARWIN:+-Dbsymbolic_functions=false}
# bin-devel is needed for glib-mkenums
meson install -C _build --tag bin-devel,devel

mkdir ${DEPS}/exif
$CURL https://github.com/libexif/libexif/releases/download/v${VERSION_EXIF}/libexif-${VERSION_EXIF}.tar.xz | tar xJC ${DEPS}/exif --strip-components=1
cd ${DEPS}/exif
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared --disable-dependency-tracking \
  --disable-nls --disable-docs --without-libiconv-prefix --without-libintl-prefix \
  CPPFLAGS="${CPPFLAGS} -DNO_VERBOSE_TAG_DATA"
make install-strip doc_DATA=

mkdir ${DEPS}/lcms
$CURL https://github.com/mm2/Little-CMS/releases/download/lcms${VERSION_LCMS}/lcms2-${VERSION_LCMS}.tar.gz | tar xzC ${DEPS}/lcms --strip-components=1
cd ${DEPS}/lcms
CFLAGS="${CFLAGS} -O3" meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} ${MESON} \
  -Dtests=disabled 
meson install -C _build --tag devel

mkdir ${DEPS}/aom
$CURL https://storage.googleapis.com/aom-releases/libaom-${VERSION_AOM}.tar.gz | tar xzC ${DEPS}/aom --strip-components=1
cd ${DEPS}/aom
# Downgrade minimum required CMake version to 3.13 - https://aomedia.googlesource.com/aom/+/597a35fbc9837e33366a1108631d9c72ee7a49e7
find . -name 'CMakeLists.txt' -o -name '*.cmake' | xargs sed -i'.bak' "/^cmake_minimum_required/s/3.16/3.13/"
mkdir aom_build
cd aom_build
AOM_AS_FLAGS="${FLAGS}" cmake -G"Unix Makefiles" \
  -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=MinSizeRel \
  -DBUILD_SHARED_LIBS=FALSE -DENABLE_DOCS=0 -DENABLE_TESTS=0 -DENABLE_TESTDATA=0 -DENABLE_TOOLS=0 -DENABLE_EXAMPLES=0 \
  -DCONFIG_PIC=1 -DENABLE_NASM=1 ${WITHOUT_NEON:+-DENABLE_NEON=0} \
  -DCONFIG_AV1_HIGHBITDEPTH=0 -DCONFIG_WEBM_IO=0 \
  ..
make install/strip

mkdir ${DEPS}/de265
$CURL https://github.com/strukturag/libde265/releases/download/v${VERSION_DE265}/libde265-${VERSION_DE265}.tar.gz | tar xzC ${DEPS}/de265 --strip-components=1
cd ${DEPS}/de265
# Do not build the dec265 and sherlock265 example programs.
./configure --disable-dec265 --disable-sherlock265
CFLAGS="${CFLAGS} -O3" CXXFLAGS="${CXXFLAGS} -O3" cmake -G"Unix Makefiles" \
  -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=FALSE
make install/strip

mkdir ${DEPS}/heif
$CURL https://github.com/strukturag/libheif/releases/download/v${VERSION_HEIF}/libheif-${VERSION_HEIF}.tar.gz | tar xzC ${DEPS}/heif --strip-components=1
cd ${DEPS}/heif
# Downgrade minimum required CMake version to 3.12 - https://github.com/strukturag/libheif/issues/975
sed -i'.bak' "/^cmake_minimum_required/s/3.16.3/3.12/" CMakeLists.txt
CFLAGS="${CFLAGS} -O3" CXXFLAGS="${CXXFLAGS} -O3" cmake -G"Unix Makefiles" \
  -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=FALSE -DBUILD_TESTING=0 -DENABLE_PLUGIN_LOADING=0 -DWITH_EXAMPLES=0 -DWITH_LIBDE265=1 -DWITH_X265=0 \
  -DCMAKE_DISABLE_FIND_PACKAGE_TIFF=1 -DCMAKE_DISABLE_FIND_PACKAGE_PNG=1 -DCMAKE_DISABLE_FIND_PACKAGE_JPEG=1
make install/strip

mkdir ${DEPS}/jpeg
$CURL https://github.com/mozilla/mozjpeg/archive/v${VERSION_MOZJPEG}.tar.gz | tar xzC ${DEPS}/jpeg --strip-components=1
cd ${DEPS}/jpeg
cmake -G"Unix Makefiles" \
  -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR:PATH=lib -DCMAKE_BUILD_TYPE=MinSizeRel \
  -DENABLE_STATIC=TRUE -DENABLE_SHARED=FALSE -DWITH_JPEG8=1 -DWITH_TURBOJPEG=FALSE -DPNG_SUPPORTED=FALSE
make install/strip

mkdir ${DEPS}/png
$CURL https://github.com/pnggroup/libpng/archive/v${VERSION_PNG}.tar.gz | tar xzC ${DEPS}/png --strip-components=1
cd ${DEPS}/png
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared --disable-dependency-tracking \
  --disable-tools --without-binconfigs --disable-unversioned-libpng-config
make install-strip dist_man_MANS=

mkdir ${DEPS}/spng
$CURL https://github.com/randy408/libspng/archive/v${VERSION_SPNG}.tar.gz | tar xzC ${DEPS}/spng --strip-components=1
cd ${DEPS}/spng
CFLAGS="${CFLAGS} -O3 -DSPNG_SSE=4" meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} ${MESON} \
  -Dstatic_zlib=true -Dbuild_examples=false
meson install -C _build --tag devel

mkdir ${DEPS}/imagequant
$CURL https://github.com/lovell/libimagequant/archive/v${VERSION_IMAGEQUANT}.tar.gz | tar xzC ${DEPS}/imagequant --strip-components=1
cd ${DEPS}/imagequant
CFLAGS="${CFLAGS} -O3" meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} ${MESON}
meson install -C _build --tag devel

mkdir ${DEPS}/webp
$CURL https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-${VERSION_WEBP}.tar.gz | tar xzC ${DEPS}/webp --strip-components=1
cd ${DEPS}/webp
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared --disable-dependency-tracking \
  --enable-libwebpmux --enable-libwebpdemux ${WITHOUT_NEON:+--disable-neon}
make install-strip bin_PROGRAMS= noinst_PROGRAMS= man_MANS=

if [ -z "$WITHOUT_HIGHWAY" ]; then
  mkdir ${DEPS}/hwy
  $CURL https://github.com/google/highway/archive/${VERSION_HWY}.tar.gz | tar xzC ${DEPS}/hwy --strip-components=1
  cd ${DEPS}/hwy
  CFLAGS="${CFLAGS} -O3" CXXFLAGS="${CXXFLAGS} -O3" cmake -G"Unix Makefiles" \
    -DCMAKE_TOOLCHAIN_FILE=${ROOT}/Toolchain.cmake -DCMAKE_INSTALL_PREFIX=${TARGET} -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=FALSE -DBUILD_TESTING=0 -DHWY_ENABLE_CONTRIB=0 -DHWY_ENABLE_EXAMPLES=0 -DHWY_ENABLE_TESTS=0
  make install/strip
fi

mkdir ${DEPS}/expat
$CURL https://github.com/libexpat/libexpat/releases/download/R_${VERSION_EXPAT//./_}/expat-${VERSION_EXPAT}.tar.xz | tar xJC ${DEPS}/expat --strip-components=1
cd ${DEPS}/expat
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared \
  --disable-dependency-tracking --without-xmlwf --without-docbook --without-getrandom --without-sys-getrandom \
  --without-libbsd --without-examples --without-tests
make install-strip dist_cmake_DATA= nodist_cmake_DATA=

mkdir ${DEPS}/archive
$CURL https://github.com/libarchive/libarchive/releases/download/v${VERSION_ARCHIVE}/libarchive-${VERSION_ARCHIVE}.tar.xz | tar xJC ${DEPS}/archive --strip-components=1
cd ${DEPS}/archive
./configure --host=${CHOST} --prefix=${TARGET} --enable-static --disable-shared --disable-dependency-tracking \
  --disable-bsdtar --disable-bsdcat --disable-bsdcpio --disable-bsdunzip --disable-posix-regex-lib --disable-xattr --disable-acl \
  --without-bz2lib --without-libb2 --without-iconv --without-lz4 --without-zstd --without-lzma \
  --without-lzo2 --without-cng --without-openssl --without-xml2 --without-expat
make install-strip libarchive_man_MANS=

mkdir ${DEPS}/cgif
$CURL https://github.com/dloebl/cgif/archive/v${VERSION_CGIF}.tar.gz | tar xzC ${DEPS}/cgif --strip-components=1
cd ${DEPS}/cgif
CFLAGS="${CFLAGS} -O3" meson setup _build --default-library=static --buildtype=release --strip --prefix=${TARGET} ${MESON} \
  -Dexamples=false -Dtests=false
meson install -C _build --tag devel

mkdir ${DEPS}/vips
$CURL https://github.com/libvips/libvips/releases/download/v${VERSION_VIPS}/vips-${VERSION_VIPS}.tar.xz | tar xJC ${DEPS}/vips --strip-components=1
cd ${DEPS}/vips
# Use version number in SONAME
#$CURL https://gist.githubusercontent.com/lovell/313a6901e9db1bf285f2a1f1180499e4/raw/3988223c7dfa4d22745d9392034b0117abef1446/libvips-cpp-soversion.patch | patch -p1
patch -p1 << PATCH
diff --git a/cplusplus/meson.build b/cplusplus/meson.build
index a47ad6ccf..e72bdd706 100644
--- a/cplusplus/meson.build
+++ b/cplusplus/meson.build
@@ -8,8 +8,8 @@ libvips_cpp_lib = library('vips-cpp',
     'VError.cpp',
     dependencies: libvips_dep,
     include_directories: libvips_cpp_includedir,
-    version: library_version,
-    darwin_versions: darwin_versions,
+    version: meson.project_version(),
+    soversion: meson.project_version(),
     gnu_symbol_visibility: 'hidden',
     install: true,
 )
PATCH

# Disable HBR support in heifsave
$CURL https://github.com/libvips/build-win64-mxe/raw/v${VERSION_VIPS}/build/patches/vips-8-heifsave-disable-hbr-support.patch | patch -p1
# Link libvips.so statically into libvips-cpp.so
sed -i'.bak' "s/library('vips'/static_&/" libvips/meson.build
sed -i'.bak' "/version: library_version/{N;d;}" libvips/meson.build
if [ "$LINUX" = true ]; then
  # Ensure libvips-cpp.so is linked with -z nodelete
  sed -i'.bak' "/gnu_symbol_visibility: 'hidden',/a link_args: nodelete_link_args," cplusplus/meson.build
  # Ensure symbols from external libs (except for libglib-2.0.a and libgobject-2.0.a) are not exposed
  EXCLUDE_LIBS=$(find ${TARGET}/lib -maxdepth 1 -name '*.a' ! -name 'libglib-2.0.a' ! -name 'libgobject-2.0.a' -printf "-Wl,--exclude-libs=%f ")
  EXCLUDE_LIBS=${EXCLUDE_LIBS%?}
  # Localize the g_param_spec_types symbol to avoid collisions with shared libraries
  # See: https://github.com/lovell/sharp/issues/2535#issuecomment-766400693
  printf "{local:g_param_spec_types;};" > vips.map
fi
# Disable building man pages, gettext po files, tools, and (fuzz-)tests
sed -i'.bak' "/subdir('man')/{N;N;N;N;d;}" meson.build
CFLAGS="${CFLAGS} -O3" CXXFLAGS="${CXXFLAGS} -O3" meson setup _build --default-library=shared --buildtype=release --strip --prefix=${TARGET} ${MESON} \
  -Ddeprecated=false -Dexamples=false -Dintrospection=disabled -Dmodules=disabled -Dcfitsio=disabled -Dfftw=disabled -Djpeg-xl=disabled \
  ${WITHOUT_HIGHWAY:+-Dhighway=disabled} -Dorc=disabled -Dmagick=disabled -Dmatio=disabled -Dnifti=disabled -Dopenexr=disabled \
  -Dopenjpeg=disabled -Dopenslide=disabled -Dpdfium=disabled -Dpoppler=disabled -Dquantizr=disabled -Drsvg=disabled -Dtiff=disabled \
  -Dppm=false -Danalyze=false -Dradiance=false \
  ${LINUX:+-Dcpp_link_args="$LDFLAGS -Wl,-Bsymbolic-functions -Wl,--version-script=$DEPS/vips/vips.map $EXCLUDE_LIBS"}
meson install -C _build --tag runtime,devel

# Cleanup
rm -rf ${TARGET}/lib/{pkgconfig,.libs,*.la,cmake}

mkdir ${TARGET}/lib-filtered
mv ${TARGET}/lib/glib-2.0 ${TARGET}/lib-filtered

# Pack only the relevant libraries
# Note: we can't use ldd on Linux, since that can only be executed on the target machine
# Note 2: we modify all dylib dependencies to use relative paths on macOS
function copydeps {
  local base=$1
  local dest_dir=$2

  cp -L $base $dest_dir/$base
  chmod 644 $dest_dir/$base

  if [ "$LINUX" = true ]; then
    local dependencies=$(readelf -d $base | grep NEEDED | awk '{ print $5 }' | tr -d '[]')
  elif [ "$DARWIN" = true ]; then
    local dependencies=$(otool -LX $base | awk '{print $1}' | grep $TARGET)

    install_name_tool -id @rpath/$base $dest_dir/$base
  fi

  for dep in $dependencies; do
    base_dep=$(basename $dep)

    [ ! -f "$PWD/$base_dep" ] && echo "$base_dep does not exist in $PWD" && continue
    echo "$base depends on $base_dep"

    if [ ! -f "$dest_dir/$base_dep" ]; then
      if [ "$DARWIN" = true ]; then
        install_name_tool -change $dep @rpath/$base_dep $dest_dir/$base
      fi

      # Call this function (recursive) on each dependency of this library
      copydeps $base_dep $dest_dir
    fi
  done;
}

cd ${TARGET}/lib
if [ "$LINUX" = true ]; then
  # Check that we really linked with -z nodelete
  readelf -Wd ${VIPS_CPP_DEP} | grep -qF NODELETE || (echo "$VIPS_CPP_DEP was not linked with -z nodelete" && exit 1)
fi
copydeps ${VIPS_CPP_DEP} ${TARGET}/lib-filtered
cd ${TARGET}
# Create the tarball
ls -al lib
rm -rf lib
mv lib-filtered lib
