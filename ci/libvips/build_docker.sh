#!/bin/bash

# Note: Github actions build libvips as a separate step, to share the built binaries in the cache across all actions.
# This wrapper script is only needed for when you're building a Docker image outside of CI.

if [ -d "ci/libvips_bin_docker_cached" ]; then
	cp -vr ci/libvips_bin_docker_cached ci/libvips_bin
fi

if ls ci/libvips_bin/lib/libvips* 1> /dev/null 2>&1; then
	echo "libvips already built"
else
	echo "Building libvips"
	cd ci/libvips
	./build.sh || exit 1
	cd ../..
	cp -vr ci/libvips_bin/* ci/libvips_bin_docker_cached
fi
