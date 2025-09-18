#!/bin/bash

# Note: Github actions build libvips as a separate step, to share the built binaries in the cache across all actions.
# This wrapper script is only needed for when you're building a Docker image outside of CI.

if [ -d "ci/libvips_bin_docker_cached" ]; then
	cp -vr ci/libvips_bin_docker_cached ci/libvips_bin
fi

if ls ci/libvips_bin/lib/libvips* 1> /dev/null 2>&1; then
	echo "libvips already built"
else
	echo "Installing packages"
	if [ $UID = 0 ]; then
		SUDO=""
	else
		SUDO="sudo"
	fi
	if [ "$(dpkg -l | awk '/man-db/ {print }'|wc -l)" -ge 1 ]; then
		echo "set man-db/auto-update false" | $SUDO debconf-communicate
		$SUDO dpkg-reconfigure man-db
    fi
    $SUDO apt-get update
	$SUDO apt-get -y install pkg-config cmake python3 autoconf automake binutils git patch ninja-build || exit 1
	if ! [ -x "$(command -v gcc)" ]; then
		$SUDO apt-get -y install gcc g++ || exit 1
	fi
    if [ "$PLATFORM" = "linux-x64" ]; then
		if ! [ -x "$(command -v nasm)" ]; then
			$SUDO apt-get install nasm || exit 1
    	fi
    fi
	echo "Building libvips"
	cd ci/libvips
	./build.sh || exit 1
	cd ../..
	cp -vr ci/libvips_bin/* ci/libvips_bin_docker_cached
fi
