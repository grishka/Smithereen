#!/bin/bash

if [ "$(uname)" != "Linux" ]; then
	echo "This script only supports Linux" >&2
	exit 1
fi

cd imgproxy_build
workDir=$PWD

cp -r -v $(readlink -f "$workDir/../ci/libvips_bin") ./libvips || exit 1
touch libvips/include/vips/vips7compat.h # imgproxy includes this but doesn't use it (?)
ln -s $PWD/libvips/lib/libvips-cpp.so.* libvips/lib/libvips-cpp.so
ln -s $PWD/libvips/lib/libvips.so.* libvips/lib/libvips.so

echo "Creating fake pkg-config files for libvips"
mkdir pkgconfig
cat > pkgconfig/vips.pc <<- EOF
Name: vips
Version: 2.0
Description: adsf
Libs: -L${PWD}/libvips/lib -lvips -Wl,-rpath,.
Cflags: -I${PWD}/libvips/include -I${PWD}/libvips/include/glib-2.0 -I${PWD}/libvips/lib/glib-2.0/include
EOF
cat > pkgconfig/glib-2.0.pc <<- EOF
Name: glib
Version: 2.0
Description: adsf
Libs:
Cflags: -I${PWD}/libvips/include/glib-2.0 -I${PWD}/libvips/lib/glib-2.0/include
EOF

echo "Building imgproxy"
cd src
PKG_CONFIG_PATH=$workDir/pkgconfig go build -ldflags "-s -w -static" -o ../imgproxy || exit 1
cd $workDir

echo "All done:"
mkdir out
mv -v libvips/lib/libvips-cpp.so.* out/
mv -v libvips/lib/libvips.so.* out/
mv -v imgproxy out/
ls -l out
