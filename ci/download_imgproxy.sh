#!/bin/bash

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
