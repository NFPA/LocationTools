#!/usr/bin/env bash
./bootstrap.sh
mkdir -p /opt/libpostal_data
./configure --datadir=/opt/libpostal_data
make
make install
ldconfig

pkg-config --cflags libpostal         # print compiler flags
pkg-config --libs libpostal           # print linker flags
pkg-config --cflags --libs libpostal  # print both
