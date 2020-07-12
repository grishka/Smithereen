#!/bin/bash

(trap 'kill 0' SIGINT; postcss desktop.scss -o ../resources/public/res/desktop.css --watch & postcss mobile.scss -o ../resources/public/res/mobile.css --watch & tsc -p common_ts -w)