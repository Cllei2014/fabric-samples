#!/bin/bash

GOLANG_VERSION=1.14.12
CURRENT_USER_ID=$(id -u)
DOCKER_GROUP_ID=$(getent group docker | cut -d ':' -f 3)

# config for go build
BUILD_DIR=.build
mkdir -p ${BUILD_DIR}/bin ${BUILD_DIR}/pkg ${BUILD_DIR}/gocache

docker run --rm \
    -u "$CURRENT_USER_ID:$DOCKER_GROUP_ID" \
    -v "$PWD:$PWD" \
    -v "$(command -v docker):$(command -v docker)" \
    -v "$(command -v docker-compose):$(command -v docker-compose)" \
    -v "/var/run/docker.sock:/var/run/docker.sock" \
    -v "$PWD/${BUILD_DIR}/bin:/opt/gopath/bin" \
    -v "$PWD/${BUILD_DIR}/pkg:/opt/gopath/pkg" \
    -v "$PWD/${BUILD_DIR}/gocache:/opt/gopath/cache" \
    -e "GOCACHE=/opt/gopath/cache" \
    -w "$PWD/fabcar" \
    --network net_byfn \
    golang:${GOLANG_VERSION} \
    $1
