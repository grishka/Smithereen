#!/bin/bash

function failWithError {
    echo -e "\033[1;31mError:\033[0m $1" >&2
    exit 1
}

cd $(dirname $0)

installLocation=$1
if [ -z "$installLocation" ]; then installLocation="/opt/smithereen"; fi

if [[ ! -d $installLocation ]]; then
	failWithError "$installLocation does not exist."
fi
if [[ ! -f "$installLocation/smithereen.jar" ]]; then
	failWithError "$installLocation does not appear to contain a Smithereen installation."
fi

echo "This script will now:"
echo "- Stop Smithereen and imgproxy services"
echo "- Copy new files over the old ones in $installLocation"
echo "- Start the services"
read -p "Press Enter to continue..."

service smithereen stop
service smithereen_imgproxy stop

rm -rf "$installLocation/lib"
cp -v -R smithereen.jar lib libvips* imgproxy $installLocation || failWithError "Unable to copy files to the $installLocation"

service smithereen start
service smithereen_imgproxy start
