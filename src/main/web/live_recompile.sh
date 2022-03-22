#!/bin/bash

(trap 'kill 0' SIGINT; npx postcss desktop.scss -o ../../../target/classes/public/res/desktop.css --watch & npx postcss mobile.scss -o ../../../target/classes/public/res/mobile.css --watch & npx tsc -p common_ts --outFile ../../../target/classes/public/res/common.js -w)
