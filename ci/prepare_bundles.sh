#!/bin/bash

function doOneArch {
	mkdir bundles/$1
	cp -v target/smithereen.jar bundles/$1/
	cp -v -R target/lib/ bundles/$1/lib/
	cp -v -R imgproxy-libvips-$1 bundles/$1
	# TODO install script?
}

mkdir bundles
doOneArch "amd64"
doOneArch "arm64"