#!/bin/bash

(trap 'kill 0' SIGINT; postcss style.scss -o ../resources/public/res/style.css --watch & tsc -p common_ts -w)