#!/bin/bash

function doOneArch {
	mkdir bundles/$3
	cp -v -R target/smithereen.jar target/lib imgproxy-libvips-$1/* schema.sql examples/nginx.conf bundles/$3
	chmod +x bundles/$3/imgproxy # permissions get lost in the process of artifact download
	cp ci/bundle_install.sh bundles/$3/install.sh
	sed -i -e "s/__arch__/$2/g" bundles/$3/install.sh
	chmod +x bundles/$3/install.sh
	cp ci/bundle_update.sh bundles/$3/update.sh
	chmod +x bundles/$3/update.sh
	pushd bundles/$3
	ln -s libvips-cpp.so.* libvips-cpp.so
	ln -s libvips.so.* libvips.so
	tar -cvzf ../../smithereen-bundle-$3.tar.gz .
	popd
}

mkdir bundles
doOneArch "amd64" "x86_64" "x86_64"
doOneArch "arm64" "aarch64" "arm64"

ls -lR bundles