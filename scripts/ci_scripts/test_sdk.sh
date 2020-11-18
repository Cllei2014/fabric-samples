#!/bin/bash

# cache for go build
BUILD_DIR=.build
mkdir -p ${BUILD_DIR}/bin ${BUILD_DIR}/pkg ${BUILD_DIR}/gocache

docker run --rm \
    -v "$PWD:$PWD" \
    -v "$PWD/${BUILD_DIR}/bin:/opt/gopath/bin" \
    -v "$PWD/${BUILD_DIR}/pkg:/opt/gopath/pkg" \
    -v "$PWD/${BUILD_DIR}/gocache:/opt/gopath/cache" \
    -e "GOCACHE=/opt/gopath/cache" \
    -w "$PWD/fabcar" \
    --network net_byfn \
    twblockchain/fabric-baseimage:0.4.22 \
    $1
