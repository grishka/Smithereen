#!/bin/bash

function doOneArch {
	mkdir bundles/$3
	cp -v -R target/smithereen.jar target/lib imgproxy-libvips-$1/* schema.sql examples/nginx.conf bundles/$3
	chmod +x bundles/$3/imgproxy # permissions get lost in the process of artifact download
	cp ci/bundle_install.sh bundles/$3/install.sh
	sed -i -e "s/__arch__/$2/g" bundles/$3/install.sh
	chmod +x bundles/$3/install.sh
	pushd bundles/$3
	ln -s libvips-cpp.so.* libvips-cpp.so
	popd
}

mkdir bundles
doOneArch "amd64" "x86_64" "x86_64"
doOneArch "arm64" "aarch64" "arm64"

ls -lR bundles
tar -cvzf bundles.tar.gz bundles
