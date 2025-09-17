#!/bin/bash

# Note: Github actions build libvips as a separate step, to share the built binaries in the cache across all actions.
# This wrapper script is only needed for when you're building a Docker image outside of CI.

if [ -f ci/libvips_bin/libvips.so.* ]; then
	echo "libvips already built"
else
	echo "Building libvips"
	cd ci/libvips
	./build.sh || exit 1
	cd ../..
fi
