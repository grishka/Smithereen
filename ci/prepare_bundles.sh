#!/bin/bash

function doOneArch {
	mkdir bundles/$1
	cp target/smithereen.jar bundles/$1/
	cp -R target/lib/ bundles/$1/lib/
	unzip imgproxy-libvips-$1.zip -d bundles/$1
	# TODO install script?
}

mkdir bundles
doOneArch "amd64"
doOneArch "arm64"