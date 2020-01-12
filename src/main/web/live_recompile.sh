#!/bin/bash

(trap 'kill 0' SIGINT; postcss style.css -o ../resources/public/res/style.css --watch & tsc -p common_ts -w)