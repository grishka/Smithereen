#!/bin/bash

cd $(dirname $0)

installLocation=$1
if [ -z "$installLocation" ]; then installLocation="/opt/smithereen"; fi

if [[ ! -d $installLocation ]]; then
	echo "$installLocation does not exist."
	exit 1
fi
if [[ ! -f "$installLocation/smithereen.jar" ]]; then
	echo "$installLocation does not appear to contain a Smithereen installation."
	exit 1
fi

echo "This script will now:"
echo "- Stop Smithereen and imgproxy services"
echo "- Copy new files over the old ones in $installLocation"
echo "- Start the services"
read -p "Press Enter to continue..."

service smithereen stop
service smithereen_imgproxy stop

rm -rf "$installLocation/lib"
cp -v -R smithereen.jar lib libvips* imgproxy $installLocation

service smithereen start
service smithereen_imgproxy start
