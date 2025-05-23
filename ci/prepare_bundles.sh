#!/bin/bash

function doOneArch {
	mkdir bundles/$1
	cp -v -R target/smithereen.jar target/lib imgproxy-libvips-$1/* schema.sql examples/nginx.conf bundles/$1
	chmod +x bundles/$1/imgproxy # permissions get lost in the process of artifact download
	cp ci/bundle_install.sh bundles/$1/install.sh
	sed -i -e "s/__arch__/$2/g" bundles/$1/install.sh
	chmod +x bundles/$1/install.sh
	cd bundles/$1
	ln -s libvips-cpp.so.* libvips-cpp.so
}

mkdir bundles
doOneArch "amd64" "x86_64"
doOneArch "arm64" "aarch64"