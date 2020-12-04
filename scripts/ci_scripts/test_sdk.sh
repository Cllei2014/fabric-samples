#!/bin/bash

# cache for go build
BUILD_DIR=.build
mkdir -p ${BUILD_DIR}/bin ${BUILD_DIR}/pkg ${BUILD_DIR}/gocache

docker run --rm \
    -v "$PWD:$PWD" \
    -v "$PWD/${BUILD_DIR}/bin:/opt/gopath/bin" \
    -v "$PWD/${BUILD_DIR}/pkg:/opt/gopath/pkg" \
    -v "$PWD/${BUILD_DIR}/gocache:/opt/gopath/cache" \
    -v "$PWD/first-network/zhonghuan-ce:/var/zhonghuan" \
    -e GOCACHE=/opt/gopath/cache \
    -e "ZHONGHUAN_CE_CONFIG" \
    -e "ZHONGHUAN_CE_ON" \
    -w "$PWD/fabcar" \
    --network net_byfn \
    twblockchain/fabric-baseimage:latest \
    $1
