#!/bin/bash

(trap 'kill 0' SIGINT; postcss desktop.scss -o ../../../target/classes/public/res/desktop.css --watch & postcss mobile.scss -o ../../../target/classes/public/res/mobile.css --watch & tsc -p common_ts --outFile ../../../target/classes/public/res/common.js -w)