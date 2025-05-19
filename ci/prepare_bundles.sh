#!/bin/bash

function doOneArch {
	mkdir bundles/$1
	cp -v target/smithereen.jar bundles/$1/
	cp -v -R target/lib bundles/$1/lib
	cp -v imgproxy-libvips-$1/* bundles/$1/
	chmod +x bundles/$1/imgproxy # permissions get lost in the process of artifact download
	# TODO install script?
}

mkdir bundles
doOneArch "amd64"
doOneArch "arm64"