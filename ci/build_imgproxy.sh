#!/bin/bash

if [ "$(uname)" != "Linux" ]; then
	echo "This script only supports Linux" >&2
	exit 1
fi

case "$(uname -m)" in
	aarch64)
		vipsArch="arm64v8"
		;;
	x86_64)
		vipsArch="x64"
		;;
	*)
		echo "CPU architecture not supported" >&2
		exit 1
		;;
esac

if ! [ -x "$(command -v jq)" ]; then
	echo "Installing jq"
	apt-get install -y jq
fi

mkdir imgproxy_build
cd imgproxy_build
workDir=$PWD

echo "Downloading latest imgproxy release"
curl -L -o src.tar.gz $(curl https://api.github.com/repos/imgproxy/imgproxy/releases/latest | jq -r '.tarball_url') || exit 1
tar -xzf src.tar.gz
mv imgproxy-imgproxy-* src

vipsTag=$(curl https://api.github.com/repos/lovell/sharp-libvips/releases/latest | jq -r '.tag_name')
vipsVersion=${vipsTag:1}
echo "Downloading prebuilt libvips, version $vipsVersion"
curl -L -o libvips.tar.gz "https://github.com/lovell/sharp-libvips/releases/download/$vipsTag/libvips-$vipsVersion-linux-$vipsArch.tar.gz" || exit 1
mkdir libvips
tar -xzf libvips.tar.gz -C libvips
touch libvips/include/vips/vips7compat.h # imgproxy includes this but doesn't use it (?)
ln -s $PWD/libvips/lib/libvips-cpp.so.* libvips/lib/libvips-cpp.so

echo "Creating fake pkg-config files for libvips"
mkdir pkgconfig
cat > pkgconfig/vips.pc <<- EOF
Name: vips
Version: 2.0
Description: adsf
Libs: -L${PWD}/libvips/lib -lvips-cpp -Wl,-rpath,.
Cflags: -I${PWD}/libvips/include -I${PWD}/libvips/include/glib-2.0 -I${PWD}/libvips/lib/glib-2.0/include -Dvips_pngload_buffer=_ZN4vips6VImage14pngload_bufferEP9_VipsBlobPNS_7VOptionE -Dvips_pngsave_buffer=_ZNK4vips6VImage14pngsave_bufferEPNS_7VOptionE
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
PKG_CONFIG_PATH=$workDir/pkgconfig go build -ldflags "-s -w" -o ../imgproxy || exit 1
cd ..
cp libvips/lib/libvips-cpp.so.* .
